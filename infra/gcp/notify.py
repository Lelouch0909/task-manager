"""Discord summary without commit messages or secrets in the payload."""
import json
import os
import urllib.request

url = os.environ.get('DISCORD_WEBHOOK_URL')
if not url:
    print('DISCORD_WEBHOOK_URL not configured; notification skipped.')
else:
    results = json.loads(os.environ['JOB_RESULTS'])
    lines = ['Task Manager · preview', *[f'{name}: {job["result"]}' for name, job in results.items()],
             os.environ['RUN_URL']]
    request = urllib.request.Request(url, data=json.dumps({
        'content': '\n'.join(lines), 'allowed_mentions': {'parse': []},
    }).encode(), headers={'Content-Type': 'application/json', 'User-Agent': 'TaskManager-CI'})
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            print(f'Discord status: {response.status}')
    except Exception:
        raise SystemExit('Discord notification failed; check webhook configuration.') from None
