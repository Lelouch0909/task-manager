# API Task Manager

Java 17, Spring Boot 4.1.1, Spring Security, Spring Data JPA, MySQL 8, Flyway et Spring Modulith. L’organisation reprend Virall : domaines métier, interfaces de services et implémentations, interfaces de contrôleurs documentées avec Swagger.

## Architecture

- `auth` : comptes, codes de vérification/récupération, mots de passe, JWT et sessions.
- `tasks` : tâches personnelles, filtrage, recherche et pagination.
- `notification` : interface d’envoi d’email et adaptateur HTTP Resend.
- `common` : configuration partagée, OpenAPI et erreurs HTTP.

Dans chaque domaine, les packages nécessaires sont `controller/api`, `controller`, `dto/request`, `dto/response`, `model`, `repository`, `service`, `service/impl`, `exception` et `config`. Les entités JPA ne sont pas retournées par les contrôleurs. Les interfaces exposées entre domaines sont déclarées avec `@NamedInterface`, et un test Modulith vérifie les dépendances.

`AuthService` orchestre les emails après le commit des opérations `AuthStateService`. Les tentatives de code invalides et les révocations pour rejeu sont volontairement committées même quand une erreur métier est retournée. Les opérations concurrentes sur les sessions et codes sont sérialisées par verrou pessimiste sur le compte.

## Configuration et démarrage

Suivre le [README racine](../README.md). Le `.env` est recherché à la racine, que l’API soit lancée depuis `api/` ou depuis le dépôt. En production, injecter les paramètres par variables d’environnement.

| Paramètre | Usage |
| --- | --- |
| `DATABASE_URL` | URL JDBC complète optionnelle ; sinon localhost, `MYSQL_PORT` et `MYSQL_DATABASE` |
| `MYSQL_USER`, `MYSQL_PASSWORD` | Identifiants applicatifs |
| `MYSQL_ROOT_PASSWORD` | Utilisé seulement par Compose |
| `JWT_SECRET` | Secret HS256 aléatoire, au moins 32 caractères ASCII |
| `CODE_SECRET` | Secret HMAC distinct pour les codes, au moins 32 caractères ASCII |
| `RESEND_API_KEY`, `RESEND_FROM` | Clé et expéditeur Resend propres à ce projet |
| `RESEND_BASE_URL` | `https://api.resend.com` ; surcharge pour tests uniquement |
| `ALLOWED_ORIGINS` | Origines exactes séparées par virgules ; inclure le frontend et Swagger |
| `COOKIE_SECURE` | `true` par défaut ; `false` uniquement pour HTTP local |
| `SWAGGER_ENABLED` | `false` par défaut, `true` dans l’exemple local |

Flyway crée le schéma au démarrage ; Hibernate vérifie sa conformité (`ddl-auto=validate`). Dates en UTC, identifiants UUID, propriétaire référencé par identifiant et clé étrangère MySQL sans association JPA entre domaines.

## Contrats

La documentation complète et les schémas sont dans `/v3/api-docs` et `/swagger-ui/index.html` lorsque Swagger est activé. Les erreurs utilisent `application/problem+json` avec `status`, `detail`, `instance`, `code` et, si applicable, `errors` par champ.

### Authentification

| Méthode et chemin | Corps / résultat |
| --- | --- |
| `POST /api/auth/register` | `displayName`, `email`, `password` ; profil en attente, 201 |
| `POST /api/auth/email/verify` | `email`, `code` ; 204 |
| `POST /api/auth/email/resend` | `email` ; 202 |
| `POST /api/auth/login` | `email`, `password` ; JWT et cookie, 200 |
| `POST /api/auth/refresh` | Cookie `refresh_token` ; JWT et nouveau cookie, 200 |
| `POST /api/auth/logout` | Cookie `refresh_token` ; révocation et suppression du cookie, 204 |
| `GET /api/auth/me` | Bearer JWT ; profil connecté, 200 |
| `POST /api/auth/password/forgot` | `email` ; 202 |
| `POST /api/auth/password/reset` | `email`, `code`, nouveau `password` ; 204 |

Inscription : nom de 1 à 100 caractères, email unique normalisé en minuscules, mot de passe de 8 caractères minimum et 72 **octets UTF-8** maximum (BCrypt). Les tokens et empreintes ne sont jamais inclus dans le profil.

La réponse de connexion contient `accessToken`, `tokenType=Bearer`, `expiresIn=900` et `user`. Le JWT est conservé en mémoire par le frontend. Le refresh token est dans un cookie HttpOnly, SameSite=Lax, chemin `/api/auth`, Secure hors développement. Utiliser `credentials: 'include'` avec Fetch pour connexion, renouvellement et déconnexion.

Le JWT expire après 15 minutes ; la session après 7 jours sans prolongation. Chaque requête Bearer vérifie aussi la session en base. Déconnexion et réinitialisation invalident immédiatement les accès concernés. Un ancien refresh token réutilisé révoque la session : le frontend doit sérialiser les refresh, y compris entre onglets. Les routes d’authentification refusent les origines navigateur non autorisées ; les clients sans `Origin` ni métadonnées navigateur (curl, tests) restent utilisables. Le déploiement web attendu est sur le même site que l’API ; un déploiement cross-site demanderait de revoir cookies et protection CSRF.

Codes : six chiffres, empreinte HMAC liée au compte et à l’usage, durée de 10 minutes, cinq essais, un seul usage. Renvoi au plus une fois par minute et cinq fois par heure, compte et usage. Les codes de vérification et de récupération sont distincts. Réinitialiser un mot de passe ne vérifie pas l’email et ne crée pas de session.

La limitation complémentaire par IP et par email à la connexion est **en mémoire, pour une instance** ; elle est réinitialisée au redémarrage. Les compteurs d’envoi et d’essais de codes sont persistants. Les en-têtes `X-Forwarded-For` ne sont pas approuvés : derrière un proxy, définir une politique de proxy de confiance avant de distribuer le service. Les réponses ordinaires de récupération et de renvoi pour une adresse inconnue sont génériques ; les délais, cooldowns et erreurs fournisseur ne constituent pas une garantie contre toute inférence d’existence de compte.

### Emails

L’envoi Resend est synchrone, timeout de connexion 3 secondes et de réponse 5 secondes. Sans configuration Resend, ou si l’envoi échoue, l’API retourne `503 email_unavailable`. Le compte et le nouveau code ont déjà été enregistrés ; utiliser le renvoi après 60 secondes. Le code précédent reste invalidé même si l’envoi du nouveau a échoué. L’API n’effectue pas de retry automatique et ne dispose pas d’une file d’envoi.

Une réponse positive signifie que Resend a accepté l’email, pas que le destinataire l’a reçu. Les tests simulent le fournisseur ; la livraison réelle nécessite une clé et un domaine expéditeur vérifié.

### Tâches

`GET /api/tasks?page=0&size=20&status=TODO&search=texte` retourne `items`, `page`, `size`, `totalElements`, `totalPages`. Taille de 1 à 100. Recherche littérale sans distinction de casse dans titre/description (maximum 200 caractères), combinable avec le statut. Tri `createdAt DESC, id DESC`.

`POST /api/tasks` reçoit `title`, `description` facultative, `status` facultatif (TODO par défaut) et retourne la tâche avec 201. `PUT /api/tasks/{id}` remplace ces trois champs ; titre et statut obligatoires, description absente ou null pour l’effacer. `DELETE /api/tasks/{id}` supprime définitivement et retourne 204.

Titre de 1 à 200 caractères après trim, description jusqu’à 5 000 caractères. Statuts `TODO`, `IN_PROGRESS`, `DONE`, transitions libres. Le propriétaire provient exclusivement du JWT. Une tâche inconnue ou appartenant à un autre compte retourne 404. Modifications concurrentes : dernière écriture gagnante.

## Tests

### Notifications et temps réel

Chaque création, modification, changement de statut ou suppression de tâche crée une notification personnelle, y compris pour l’auteur de l’action. La notification est enregistrée dans la même transaction que la tâche ; elle reste consultable après suppression de celle-ci. Aucun événement SSE n’est diffusé si la transaction est annulée.

| Route | Fonction |
| --- | --- |
| `GET /api/notifications?page=0&size=20&unreadOnly=false` | Historique paginé et compteur global `unreadCount` |
| `GET /api/notifications/unread-count` | Compteur non lu |
| `PUT /api/notifications/{id}/read` | Corps `{ "read": true }` ou `false`, idempotent |
| `PUT /api/notifications/read-all` | Tout marquer lu, 204 |
| `GET /api/events` | Flux SSE personnel, authentifié par Bearer |

Le flux émet `event: sync` avec `data: {"tasks":true,"notifications":true}` (ou uniquement notifications à true pour un changement de lecture). Il indique quelles ressources REST recharger. Un premier événement complet est envoyé à chaque connexion pour récupérer les changements manqués. Il n’y a pas de replay `Last-Event-ID` ; MySQL reste la source de vérité.

Heartbeat de 15 secondes, fermeture à l’expiration du JWT, validation de la session avant chaque envoi : une session révoquée ne reçoit plus de données et sa connexion inactive est fermée au heartbeat suivant. Limites : huit flux par compte, mille par instance. Le client reconnecte avec backoff et utilise le mécanisme central de renouvellement JWT. Ne pas transmettre le token dans l’URL.

Cette diffusion est prévue pour **une instance API** : le registre des connexions est en mémoire. Plusieurs réplicas nécessiteraient un relais partagé. Un crash entre commit et diffusion est récupéré au reconnect via REST. Derrière un reverse proxy, désactiver le buffering SSE et prévoir un timeout supérieur au heartbeat (`X-Accel-Buffering: no` est envoyé). Il ne s’agit pas de notifications système lorsque le navigateur est fermé.

`./mvnw test` : JUnit, Mockito et architecture Modulith, sans Docker. `./mvnw verify -Pintegration` ajoute un serveur HTTP complet, migrations sur MySQL réel via Testcontainers et faux Resend local. La base Compose et les comptes réels ne sont pas utilisés. MySQL doit déjà exister localement sous `mysql:8.0` ; le téléchargement de cette image est désactivé dans les tests.

Testcontainers utilise aussi son conteneur de nettoyage Ryuk. Pour réutiliser une version déjà présente : `TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.14.0 ./mvnw verify -Pintegration`.

Les tests couvrent notamment l’isolation entre comptes, pagination/recherche, expiration, consommation unique des codes, persistance des essais invalides, rotation concurrente des refresh tokens, révocation immédiate, erreurs/timeout Resend et contrats OpenAPI. Les secrets employés dans les tests sont fictifs.

Le frontend est dans `../web` et les fichiers de déploiement GCP dans `../infra/gcp`. Le mobile reste hors périmètre. L’API ne propose pas encore de purge automatique des anciens codes et sessions : une politique de rétention devra être ajoutée avant une exploitation prolongée.
