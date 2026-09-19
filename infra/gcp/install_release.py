"""Extract the CI archive safely and execute deployment on the GCE VM."""
import os
from pathlib import Path
import re
import subprocess
import sys
import tarfile


def main():
    archive, directory, *args = sys.argv[1:]
    if not re.fullmatch(r'/opt/taskmanager/releases/[0-9]+-[0-9]+', directory):
        raise ValueError('Invalid release path')
    os.umask(0o077)
    target = Path(directory)
    target.mkdir(parents=True, exist_ok=False)
    expected = {'compose.yaml', 'deploy.py', 'write-runtime.py', 'install_release.py', 'release.env'}
    with tarfile.open(archive) as bundle:
        members = bundle.getmembers()
        if {m.name for m in members} != expected or len(members) != len(expected) or any(not m.isfile() for m in members):
            raise ValueError('Unexpected release archive contents')
        bundle.extractall(target, members=members, filter='data')
    Path(archive).unlink()
    # Keep uploaded installer until execution finishes, then remove only itself.
    try:
        subprocess.run(['python3', 'deploy.py', *args], cwd=target, check=True)
    finally:
        Path(__file__).unlink()


if __name__ == '__main__':
    main()
