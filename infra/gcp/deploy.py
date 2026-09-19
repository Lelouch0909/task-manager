"""Deploy a staged release on GCE. Invoked as root, without shell scripts."""
import fcntl
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import urllib.request
from importlib.util import spec_from_file_location, module_from_spec


def run(*args, **kwargs):
    return subprocess.run(args, check=True, **kwargs)


def compose(release, *args):
    run('docker', 'compose', '--env-file', 'release.env', *args, cwd=release)


def check_public(release):
    config = dict(line.split('=', 1) for line in (release / 'release.env').read_text().splitlines())
    for attempt in range(12):
        try:
            with urllib.request.urlopen('https://' + config['DOMAIN'] + '/api/health', timeout=10) as reply:
                if json.load(reply).get('status') != 'UP':
                    raise ValueError('API is not ready')
            with urllib.request.urlopen('https://' + config['DOMAIN'] + '/login', timeout=10) as reply:
                if reply.status != 200 or b'<div id="root">' not in reply.read():
                    raise ValueError('Frontend unavailable')
            return
        except Exception:
            if attempt == 11:
                raise RuntimeError('HTTPS readiness check failed') from None
            time.sleep(5)


def main():
    project, secret, version, registry = sys.argv[1:]
    for value, pattern in [(project, r'[a-z][a-z0-9-]+'), (secret, r'[a-zA-Z0-9_-]+'),
                           (version, r'[0-9]+'), (registry, r'[a-z0-9-]+-docker\.pkg\.dev')]:
        if not re.fullmatch(pattern, value):
            raise ValueError('Invalid deployment argument')
    os.umask(0o077)
    release = Path.cwd().resolve()
    if release.parent != Path('/opt/taskmanager/releases'):
        raise ValueError('Invalid release directory')
    with open('/var/lock/taskmanager-deploy.lock', 'w') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        current = Path('/opt/taskmanager/current')
        previous = current.resolve() if current.is_symlink() else None
        source = release / 'runtime.json'
        try:
            with source.open('w') as output:
                run('gcloud', 'secrets', 'versions', 'access', version,
                    '--secret=' + secret, '--project=' + project, stdout=output)
            spec = spec_from_file_location('runtime', release / 'write-runtime.py')
            runtime = module_from_spec(spec)
            spec.loader.exec_module(runtime)
            runtime.render(source, release)
        finally:
            source.unlink(missing_ok=True)
        run('gcloud', 'auth', 'configure-docker', registry, '--quiet')
        compose(release, 'config', '--quiet')
        compose(release, 'pull')
        try:
            compose(release, 'up', '-d', '--wait', '--wait-timeout', '300')
            check_public(release)
        except Exception:
            if previous and previous != release:
                compose(previous, 'up', '-d', '--wait', '--wait-timeout', '300')
                check_public(previous)
                print('Previous images restored; Flyway migrations are not reverted.', file=sys.stderr)
            raise SystemExit('Deployment failed; inspect container logs on the VM.') from None
        if previous and previous != release:
            old = Path('/opt/taskmanager/previous')
            old.unlink(missing_ok=True)
            old.symlink_to(previous)
        pending = Path('/opt/taskmanager/current.next')
        pending.unlink(missing_ok=True)
        pending.symlink_to(release)
        pending.replace(current)
        print('Deployment healthy: API, database and HTTPS frontend.')


if __name__ == '__main__':
    main()
