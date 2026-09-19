# API sur le VPS

Le déploiement demandé utilise `deploy@212.227.80.225`, séparément des conteneurs Virall.

- API : `https://taskmanager.212.227.80.225.sslip.io`
- Port public : **443**, en HTTPS via Traefik.
- Port technique local au serveur : `http://127.0.0.1:8082`.
- Disponibilité : `/actuator/health/readiness`.
- Dossier : `/home/deploy/taskmanager`.
- Projet Compose : `taskmanager-vps` ; MySQL privé dans son propre volume.

`api/Dockerfile.remote` compile sur le serveur. Les fichiers `api.env`, `mysql.env` et `release.env` restent privés et ne sont pas versionnés. L’image CI/GCP garde son Dockerfile distinct, basé sur le JAR déjà testé.

La route HTTPS est ajoutée au provider fichier Traefik existant par `register_proxy.py` (PyYAML requis), avec sauvegarde préalable et sans remplacer les routes des autres applications. Si un autre outil régénère le fichier Traefik, réappliquer cette route. Le nom sslip.io permet une URL de démonstration sans configuration DNS ; un domaine propre pourra le remplacer.

Depuis le serveur :

```console
cd /home/deploy/taskmanager/infra/vps
docker compose --env-file release.env ps
docker compose --env-file release.env logs --tail=100 api
```

## Web Vercel

Importer `Lelouch0909/task-manager`, choisir **Root Directory = web** et le preset **Vite**. Le fichier `web/vercel.json` configure le build, les routes React et le proxy `/api` vers l’API HTTPS.

Conserver `VITE_API_BASE_URL=/api` (valeur par défaut). Le navigateur utilise ainsi le domaine du web pour les requêtes et le cookie HttpOnly de renouvellement. Ne pas mettre l’IP HTTP comme URL publique du client.

L’origine autorisée actuellement est `https://task-manager.vercel.app`, avec `http://localhost:5173` pour le développement. Si Vercel attribue un autre domaine, modifier `ALLOWED_ORIGINS` dans le `release.env` du serveur puis relancer `docker compose --env-file release.env up -d api`. Les domaines de preview Vercel ne sont pas autorisés automatiquement.

La pipeline GCP reste disponible pour l’exercice. Le push sur `main` ne déclenche pas son déploiement GCP ; celui-ci est réservé à `preview` après configuration des variables du guide GCP.
