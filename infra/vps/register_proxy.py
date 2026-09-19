"""Add the Task Manager route to the existing VPS Traefik file provider."""
from datetime import datetime, timezone
from pathlib import Path
import shutil
import yaml

path = Path('/home/deploy/.appwrite/gateway/dynamic.with-appwrite.yaml')
config = yaml.safe_load(path.read_text())
http = config.setdefault('http', {})
routers = http.setdefault('routers', {})
services = http.setdefault('services', {})
name = 'taskmanager-api'
router = {
    'rule': 'Host(`taskmanager.212.227.80.225.sslip.io`)',
    'entryPoints': ['websecure'],
    'service': name,
    'tls': {'certResolver': 'letsencrypt'},
}
service = {'loadBalancer': {'servers': [{'url': 'http://taskmanager-api:8080'}]}}
if name in routers and routers[name] != router:
    raise SystemExit('Existing Task Manager router differs; review before replacing.')
if name in services and services[name] != service:
    raise SystemExit('Existing Task Manager service differs; review before replacing.')
if routers.get(name) != router or services.get(name) != service:
    suffix = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    backup = path.with_name(path.name + '.taskmanager-' + suffix + '.bak')
    shutil.copy2(path, backup)
    routers[name] = router
    services[name] = service
    # Preserve the inode: this file is bind-mounted in the existing container.
    path.write_text(yaml.safe_dump(config, sort_keys=False))
    print('Task Manager route added; previous configuration backed up.')
else:
    print('Task Manager route already configured.')
