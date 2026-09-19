import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('runtime', Path(__file__).with_name('write-runtime.py'))
runtime = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runtime)


class RuntimeTests(unittest.TestCase):
    def data(self):
        return {'MYSQL_PASSWORD': 'a$#"\' =pass', 'MYSQL_ROOT_PASSWORD': 'root-pass',
                'JWT_SECRET': 'a' * 40, 'CODE_SECRET': 'b' * 40,
                'RESEND_API_KEY': 'test-only', 'RESEND_FROM': 'Task Manager <test@example.com>'}

    def test_raw_values_permissions_and_minimum_secret_exposure(self):
        with tempfile.TemporaryDirectory() as folder:
            source = Path(folder) / 'runtime.json'
            source.write_text(json.dumps(self.data()))
            runtime.render(source, folder)
            api = Path(folder) / 'api.env'
            mysql = Path(folder) / 'mysql.env'
            self.assertIn('MYSQL_PASSWORD=a$#"\' =pass\n', api.read_text())
            self.assertNotIn('MYSQL_ROOT_PASSWORD', api.read_text())
            self.assertNotIn('RESEND_API_KEY', mysql.read_text())
            self.assertEqual(api.stat().st_mode & 0o777, 0o600)
            self.assertEqual(mysql.stat().st_mode & 0o777, 0o600)

    def test_reject_injection_missing_and_short_secrets(self):
        for key, value in [('MYSQL_PASSWORD', 'pass\nINJECT=value'), ('JWT_SECRET', 'short'),
                           ('RESEND_FROM', ''), ('CODE_SECRET', 'a' * 40)]:
            with self.subTest(key=key), tempfile.TemporaryDirectory() as folder:
                data = self.data()
                data[key] = value
                source = Path(folder) / 'runtime.json'
                source.write_text(json.dumps(data))
                with self.assertRaises(ValueError):
                    runtime.render(source, folder)
                self.assertFalse((Path(folder) / 'api.env').exists())
