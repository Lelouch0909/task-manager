"""Upload a release through IAP, then invoke the remote Python deployer."""
import os
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import tarfile
import tempfile


def value(name, pattern):
    result = os.environ.get(name, '')
    if not re.fullmatch(pattern, result):
        raise ValueError(f'Missing or invalid {name}')
    return result


def main():
    project = value('GCP_PROJECT_ID', r'[a-z][a-z0-9-]+')
    instance = value('GCE_INSTANCE', r'[a-z][a-z0-9-]+')
    zone = value('GCE_ZONE', r'[a-z0-9-]+')
    secret = value('RUNTIME_SECRET', r'[a-zA-Z0-9_-]+')
    version = value('RUNTIME_SECRET_VERSION', r'[0-9]+')
    registry = value('REGISTRY', r'[a-z0-9-]+-docker\.pkg\.dev')
    domain = value('DOMAIN', r'[a-z0-9][a-z0-9.-]*\.[a-z]{2,}')
    email = value('ACME_EMAIL', r'[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+')
    api = value('API_IMAGE', r'[a-z0-9./_-]+@sha256:[a-f0-9]{64}')
    web = value('WEB_IMAGE', r'[a-z0-9./_-]+@sha256:[a-f0-9]{64}')
    release_id = value('GITHUB_RUN_ID', r'[0-9]+') + '-' + value('GITHUB_RUN_ATTEMPT', r'[0-9]+')
    remote = '/opt/taskmanager/releases/' + release_id
    args = ['--project=' + project, '--zone=' + zone, '--tunnel-through-iap', '--quiet']
    with tempfile.TemporaryDirectory(prefix='taskmanager-release-') as folder:
        stage = Path(folder)
        for filename in ('compose.yaml', 'deploy.py', 'write-runtime.py', 'install_release.py'):
            shutil.copy(Path('infra/gcp') / filename, stage / filename)
        (stage / 'release.env').write_text(f'API_IMAGE={api}\nWEB_IMAGE={web}\nDOMAIN={domain}\nACME_EMAIL={email}\n')
        archive = stage / 'release.tgz'
        with tarfile.open(archive, 'w:gz') as bundle:
            for path in stage.iterdir():
                if path != archive:
                    bundle.add(path, arcname=path.name)
        remote_archive = f'taskmanager-{release_id}.tgz'
        # Installer is copied separately so no shell extraction/chdir sequence is needed.
        remote_installer = f'taskmanager-install-{release_id}.py'
        for source, destination in [(archive, remote_archive), (stage / 'install_release.py', remote_installer)]:
            subprocess.run(['gcloud', 'compute', 'scp', str(source), instance + ':' + destination, *args], check=True)
        command = shlex.join(['sudo', 'python3', remote_installer, remote_archive, remote,
                              project, secret, version, registry])
        subprocess.run(['gcloud', 'compute', 'ssh', instance, *args, '--command=' + command], check=True)


if __name__ == '__main__':
    main()
