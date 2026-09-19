"""Render Secret Manager JSON into Compose raw env files; never print values."""
import json
import os
from pathlib import Path
import sys


def render(source, destination):
    data = json.loads(Path(source).read_text())
    required = ('MYSQL_PASSWORD', 'MYSQL_ROOT_PASSWORD', 'JWT_SECRET', 'CODE_SECRET',
                'RESEND_API_KEY', 'RESEND_FROM')
    for key in required:
        value = data.get(key)
        if not isinstance(value, str) or not value.strip() or any(c in value for c in '\r\n\0'):
            raise ValueError(f'Missing or invalid {key}')
    for key in ('JWT_SECRET', 'CODE_SECRET'):
        if len(data[key].encode()) < 32:
            raise ValueError(f'{key} must have at least 32 bytes')
    if data['JWT_SECRET'] == data['CODE_SECRET']:
        raise ValueError('JWT_SECRET and CODE_SECRET must be distinct')
    target = Path(destination)
    target.mkdir(parents=True, exist_ok=True)
    groups = {
        'api.env': {k: data[k] for k in required if k != 'MYSQL_ROOT_PASSWORD'},
        'mysql.env': {'MYSQL_DATABASE': 'taskmanager', 'MYSQL_USER': 'taskmanager',
                      **{k: data[k] for k in ('MYSQL_PASSWORD', 'MYSQL_ROOT_PASSWORD')}},
    }
    for filename, values in groups.items():
        path = target / filename
        fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
        os.fchmod(fd, 0o600)
        with os.fdopen(fd, 'w') as stream:
            stream.write(''.join(f'{k}={v}\n' for k, v in values.items()))


if __name__ == '__main__':
    render(sys.argv[1], sys.argv[2])
