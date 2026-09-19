# Déploiement GCP

Le projet utilise l’option « VM Dockerisée » de l’exercice : Compute Engine, Artifact Registry et Docker Compose. Caddy sert le web en HTTPS et transmet /api à Spring. MySQL est conservé sur un disque persistant séparé. Une seule instance API suffit au flux SSE actuel.

## Préparer GCP

Dans un projet avec facturation activée :

1. Activer Compute Engine, Artifact Registry, Secret Manager, IAM Service Account Credentials, Security Token Service, OS Login et Identity-Aware Proxy.
2. Créer un dépôt Docker Artifact Registry `taskmanager`, par exemple en `europe-west1`.
3. Créer trois comptes de service : `taskmanager-build`, `taskmanager-deploy`, `taskmanager-vm`.
4. Créer la VM `taskmanager-preview` : Ubuntu 24.04 LTS x86_64, `e2-medium`, zone `europe-west1-b`. Lui associer le compte VM avec scope `cloud-platform` (IAM limite ses droits). Ajouter la métadonnée `enable-oslogin=TRUE`.
5. Attacher un disque vierge de 30 Go avec **device name** `taskmanager-data`, conservé lors de la suppression de la VM. Ne pas utiliser un disque contenant d’autres données.
6. Ajouter la métadonnée `user-data` avec le contenu de [cloud-init.yaml](cloud-init.yaml). Elle installe Docker, Compose, gcloud et monte le disque. Attendre « Task Manager VM ready » dans les logs cloud-init. Compose 2.30 minimum est nécessaire.
7. Réserver une IP externe fixe. Autoriser TCP 80/443 vers cette VM et TCP 22 uniquement depuis `35.235.240.0/20` pour IAP. Retirer les éventuelles règles SSH publiques héritées. Ne pas exposer 3306 ou 8080.
8. Faire pointer le DNS A du domaine vers l’IP fixe ; aucun AAAA ne doit pointer ailleurs. Caddy obtient ensuite le certificat HTTPS.

Ces ressources sont facturées. Le workflow applicatif ne crée pas l’infrastructure.

## IAM et GitHub

| Identité | Rôle | Périmètre |
| --- | --- | --- |
| build | Artifact Registry Writer | Dépôt taskmanager |
| VM | Artifact Registry Reader | Dépôt taskmanager |
| VM | Secret Manager Secret Accessor | Secret d’exécution |
| deploy | Compute Viewer | Projet |
| deploy | Compute OS Admin Login | VM cible |
| deploy | IAP-secured Tunnel User | Tunnel VM, port 22 |
| deploy | Service Account User | Compte taskmanager-vm |

Créer un Workload Identity Pool `taskmanager` et un fournisseur OIDC `github`, issuer `https://token.actions.githubusercontent.com`. Mapper `google.subject=assertion.sub` et `attribute.repository=assertion.repository`.

Condition du fournisseur :

```text
assertion.repository == 'Lelouch0909/task-manager' &&
assertion.repository_owner_id == 'ID_NUMERIQUE_GITHUB' &&
assertion.ref == 'refs/heads/preview' &&
assertion.event_name != 'pull_request'
```

L’ID numérique est obtenu avec `gh api users/Lelouch0909 --jq .id`.

Accorder `roles/iam.workloadIdentityUser` sur le compte build au membre :

```text
principalSet://iam.googleapis.com/projects/PROJECT_NUMBER/locations/global/workloadIdentityPools/taskmanager/attribute.repository/Lelouch0909/task-manager
```

Sur le compte deploy, utiliser le sujet de l’environnement protégé :

```text
principal://iam.googleapis.com/projects/PROJECT_NUMBER/locations/global/workloadIdentityPools/taskmanager/subject/repo:Lelouch0909/task-manager:environment:preview
```

Aucune clé JSON GCP n’est stockée dans GitHub. Voir le [guide de fédération GCP](https://docs.cloud.google.com/iam/docs/workload-identity-federation-with-deployment-pipelines) et [OS Login](https://docs.cloud.google.com/compute/docs/oslogin).

## Secret d’exécution

Créer `taskmanager-preview-runtime` dans Secret Manager. Le JSON suit [runtime.example.json](runtime.example.json) : mots de passe MySQL, secrets JWT/code et configuration Resend. Les secrets JWT/code doivent être distincts et comporter au moins 32 octets.

Depuis un fichier local `runtime.json` ignoré par Git :

```console
gcloud secrets versions add taskmanager-preview-runtime --data-file=runtime.json --project=PROJECT_ID
```

Noter le numéro de version. La VM lit cette version précise, produit des fichiers privés mode 600 et supprime le JSON temporaire. Le mot de passe root MySQL n’est pas transmis à Spring. Le format raw de Compose conserve les caractères spéciaux.

Changer le mot de passe dans Secret Manager ne change pas les comptes d’une base déjà initialisée : coordonner sa rotation avec MySQL. L’expéditeur Resend doit utiliser un domaine vérifié, sans guillemets ajoutés à l’intérieur de la valeur.

## Variables GitHub

Créer l’environnement `preview`, limité à la branche du même nom. Ajouter un required reviewer pour conserver la validation humaine de Virall ; sans reviewer, le déploiement est automatique après les tests.

Définir ces variables **au niveau du dépôt** :

| Variable | Valeur |
| --- | --- |
| GCP_PROJECT_ID | ID du projet |
| GCP_REGION | europe-west1 |
| ARTIFACT_REPOSITORY | taskmanager |
| GCP_WORKLOAD_IDENTITY_PROVIDER | projects/NUMERO/locations/global/workloadIdentityPools/taskmanager/providers/github |
| GCP_BUILD_SERVICE_ACCOUNT | taskmanager-build@PROJET.iam.gserviceaccount.com |
| GCP_DEPLOY_SERVICE_ACCOUNT | taskmanager-deploy@PROJET.iam.gserviceaccount.com |
| GCE_INSTANCE | taskmanager-preview |
| GCE_ZONE | europe-west1-b |
| RUNTIME_SECRET | taskmanager-preview-runtime |
| RUNTIME_SECRET_VERSION | Numéro de version du secret |
| PREVIEW_DOMAIN | Domaine sans protocole ni chemin |
| ACME_EMAIL | Adresse de contact pour TLS |

Ajouter le secret du dépôt `DISCORD_WEBHOOK_URL`. Son absence est signalée sans bloquer la livraison. Les PR n’envoient pas de notification.

## Livraison

Les PR et pushes vers develop, preview, main ou master exécutent tests, builds et constructions Docker. Seul preview publie les images et déploie. Le lancement manuel propose aussi le déploiement depuis preview.

Les images contiennent le JAR et le dist issus des jobs validés. Elles portent le SHA Git et sont déployées par digest. Le déploiement Python passe par SSH/IAP, télécharge les images, démarre Compose, attend MySQL et Spring, puis vérifie HTTPS sur /api/health et /login. Discord reçoit le résultat de tous les jobs.

Les releases sont conservées sous `/opt/taskmanager/releases/RUN-ATTEMPT`. Les liens current et previous désignent les versions validées. En cas d’échec, les images précédentes sont restaurées si disponibles et le job reste en échec. Le premier déploiement n’a pas de version de secours.

Depuis la VM, après `sudo -i` :

```console
cd /opt/taskmanager/current
docker compose --env-file release.env ps
docker compose --env-file release.env logs --tail=100 api
```

Pour restaurer manuellement les images précédentes :

```console
cd /opt/taskmanager/previous
docker compose --env-file release.env up -d --wait --wait-timeout 300
```

Vérifier HTTPS puis ajuster le lien current. Le rollback ne révoque pas les migrations Flyway : garder les migrations rétrocompatibles. Les fichiers privés de chaque release sont conservés pour cette opération.

La VM n’offre pas de haute disponibilité. Une livraison peut interrompre brièvement les connexions SSE, qui se reconnectent. Le disque persistant n’est pas une sauvegarde : aucune sauvegarde automatique n’est configurée ici. Prévoir une sauvegarde MySQL vérifiée avant une utilisation durable.
