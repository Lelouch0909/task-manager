# Task Manager

Application de gestion de tâches réalisée pour le test de recrutement. Chaque utilisateur dispose de son compte, de ses tâches et de ses notifications.

Le projet contient l’API Spring Boot et le frontend React. Le bonus mobile Flutter n’est pas réalisé.

## Fonctionnalités

- Inscription, vérification de l’adresse email, connexion et récupération du mot de passe.
- Création, modification et suppression de tâches.
- Recherche, filtrage par statut et pagination. Trois statuts : à faire, en cours, terminée.
- Affichage en liste ou en grille.
- Notifications persistantes, marquage lu/non lu et synchronisation entre les onglets par SSE.

## Stack et organisation

| Partie | Technologies |
| --- | --- |
| API | Java 17, Spring Boot, Spring Security, Spring Data JPA |
| Données | MySQL 8, migrations Flyway |
| Web | React, Vite, TypeScript, Tailwind CSS, shadcn/ui |
| État et HTTP | Redux Toolkit, Fetch API, validation Zod |
| Emails | Resend |
| Livraison | GitHub Actions, Docker, Artifact Registry, Compute Engine |

```text
api/                 API Spring Boot et tests
web/                 Application React et composants shadcn
infra/gcp/           Configuration VM, Compose et déploiement Python
.github/workflows/   Pipeline CI/CD
compose.yaml         MySQL pour le développement local
```

L’API est organisée par domaines : auth, tasks et notification. Chaque domaine regroupe ses modèles, repositories, DTO, contrôleurs et services, avec interfaces et implémentations. Spring Modulith vérifie les dépendances entre domaines.

Les contrôleurs exposent les contrats REST, les services portent les règles métier et les repositories accèdent à MySQL. Flyway crée le schéma ; Hibernate en vérifie la conformité au démarrage.

Côté web, Redux contient les données partagées et la session ; les formulaires gardent leur état local. Le JWT d’accès reste en mémoire et le renouvellement passe par un cookie HttpOnly. Le client utilise Fetch, y compris pour lire le flux SSE. Ce flux indique les données à recharger ; MySQL reste la source de vérité.

## Lancer le projet

Prérequis : Java 17, Node.js 22.22.2, npm et Docker Compose. Le wrapper Maven est fourni.

### Configuration

Depuis la racine :

```console
cp .env.example .env
```

Renseigner les mots de passe MySQL, JWT_SECRET et CODE_SECRET. Les deux derniers doivent être distincts, avec au moins 32 caractères aléatoires. Une commande comme `openssl rand -hex 32` permet de générer chaque secret.

Pour les emails, renseigner RESEND_API_KEY et RESEND_FROM :

```dotenv
RESEND_API_KEY=votre-cle
RESEND_FROM=Task Manager <noreply@votre-domaine-verifie.fr>
```

Le domaine expéditeur doit être vérifié dans Resend. Ne pas entourer la valeur de guillemets : Spring les transmettrait au fournisseur. Ne pas exécuter le .env comme un script et ne pas le committer.

En local, conserver COOKIE_SECURE=false et SWAGGER_ENABLED=true comme dans le fichier d’exemple.

### MySQL et API

Le Compose local utilise l’image mysql:8.0 déjà présente sur la machine. Si elle manque, la télécharger une première fois avec `docker pull mysql:8.0`.

```console
docker compose up -d --wait mysql
cd api
./mvnw spring-boot:run
```

L’API écoute sur http://localhost:8080. Elle charge le .env de la racine ; les variables d’environnement ont priorité. Dans IntelliJ, sélectionner Java 17 et lancer ApiApplication.

MySQL est accessible uniquement sur 127.0.0.1:3307. Les données restent dans le volume Compose après un arrêt ; ne pas utiliser `docker compose down -v` pour un simple redémarrage.

### Frontend

Dans un autre terminal :

```console
cd web
npm ci
npm run dev
```

Ouvrir http://localhost:5173. Vite transmet les appels /api à Spring sur le port 8080. Pour changer cette cible, copier web/.env.example vers web/.env.local et modifier API_PROXY_TARGET.

Créer un compte, saisir le code reçu par email, puis se connecter. Les notifications concernent les changements effectués sur les tâches de ce compte.

## API

Swagger est disponible en local : http://localhost:8080/swagger-ui/index.html.

| Méthode | Route | Usage |
| --- | --- | --- |
| POST | /api/auth/register | Inscription |
| POST | /api/auth/email/verify | Vérification email |
| POST | /api/auth/login | Connexion |
| POST | /api/auth/refresh | Renouvellement de session |
| POST | /api/auth/logout | Déconnexion |
| GET | /api/tasks | Liste, recherche, filtres |
| POST | /api/tasks | Création |
| PUT | /api/tasks/{id} | Modification |
| DELETE | /api/tasks/{id} | Suppression |
| GET | /api/notifications | Notifications |
| GET | /api/events | Flux SSE |

Les routes privées attendent un Bearer token. Les tâches sont isolées par utilisateur. Les erreurs renvoient un ProblemDetail avec un code métier et, si nécessaire, les erreurs de validation par champ. Les contrats complets sont dans [api/README.md](api/README.md) et Swagger.

## Tests et CI/CD

La [pipeline](.github/workflows/preview.yml) exécute :

1. Tests unitaires et d’intégration Java sur un MySQL jetable, puis packaging du JAR.
2. Tests du client Fetch contre l’API Spring.
3. Tests web, lint, vérification TypeScript et build Vite.
4. Construction des deux images Docker à partir du JAR et du dist validés.
5. Publication dans Artifact Registry et déploiement sur GCE pour la branche preview.
6. Contrôle de disponibilité HTTPS et notification Discord.

Les PR vers develop, preview, main ou master passent les validations sans publier ni déployer. L’environnement GitHub preview peut demander une approbation avant le déploiement. Les emails sont simulés pendant les tests automatisés.

Pour exécuter uniquement les tests locaux :

```console
cd api
./mvnw test
```

```console
cd web
npm test
npm run lint
```

Les tests d’intégration nécessitent Docker et l’image mysql:8.0 :

```console
cd api
./mvnw verify -Pintegration -Dweb.contract.tests=true
```

Cette dernière commande construit aussi le JAR. Les builds applicatifs et Docker sont prévus dans GitHub Actions.

## Déploiement

Le [guide GCP](infra/gcp/README.md) décrit la VM, les droits IAM, les secrets, le DNS et les variables GitHub à configurer. Le web et l’API partagent un domaine HTTPS. Les secrets viennent de Secret Manager ; GitHub s’authentifie par Workload Identity Federation.

Le déploiement utilise les digests des images et tente de restaurer la version précédente si les contrôles échouent. Il ne révoque pas les migrations SQL.

L’API dispose aussi d’un déploiement VPS : [API HTTPS](https://taskmanager.212.227.80.225.sslip.io/actuator/health/readiness), accessible sur le port public **443**. Le port 8082 reste local au serveur. Le [guide VPS et Vercel](infra/vps/README.md) décrit cette installation. Pour déployer le web sur Vercel, importer ce dépôt avec **Root Directory = web** et le preset **Vite** ; `web/vercel.json` configure les routes et le proxy API. L’origine prévue est `https://task-manager.vercel.app`.

## Limites actuelles

Le flux SSE et certains compteurs sont prévus pour une seule instance API. Les notifications ne sont pas des notifications système lorsque le navigateur est fermé. La VM n’est pas une installation haute disponibilité, et les sauvegardes MySQL doivent être configurées séparément.

Un succès d’envoi email indique l’acceptation par Resend, pas la réception dans la boîte du destinataire. En cas de 503 lors de l’inscription, le compte est conservé : corriger la configuration puis utiliser le renvoi après 60 secondes.
