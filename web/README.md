# Task Manager — Web

Frontend du test de recrutement : React, Vite, TypeScript (TSX), Tailwind CSS, shadcn/ui et Redux Toolkit + React Redux. Les appels HTTP utilisent la Fetch API native. Le mobile est hors périmètre.

La palette de marque est centralisée dans `src/index.css` : blanc, turquoise `#0A9A9A` et corail `#EA6A54`. Les variantes de composants utilisent des valeurs de survol légèrement plus foncées pour préserver la lisibilité.

## Démarrer

Node.js 22.22.2 (voir `.nvmrc`) et npm.

```sh
cd web
npm ci
cp .env.example .env.local
npm run dev
```

Ouvrir l’URL affichée par Vite (par défaut http://localhost:5173).

```sh
npm run lint
npm run build
npm run preview
```

`build` vérifie TypeScript et produit `dist/`. `preview` sert uniquement à vérifier le build localement.

## Organisation

- `src/app/` : store Redux et hooks typés.
- `src/features/auth/` : inscription, connexion, vérification, récupération de mot de passe et session JWT en mémoire.
- `src/features/tasks/` : dashboard, recherche, filtres, vues liste/grille et CRUD des tâches.
- `src/features/notifications/` : panneau de notifications, pagination, marquage lu et flux SSE.
- `src/features/ui/` : état partagé de présentation (liste/grille).
- `src/components/ui/` : composants shadcn/ui locaux, modifiables dans le projet.
- `components.json` : configuration shadcn/ui et alias `@/*`.
- `src/lib/api.ts` : client Fetch, erreurs HTTP, réponses JSON/texte/vides, token Bearer optionnel et annulation via `signal`.
- `src/App.tsx` : routage public et layout de chargement de session.

Vite est l’outil de développement et de build, pas un framework comme Next.js : cette base est une application React exécutée dans le navigateur, sans rendu serveur ni routage automatique par fichiers. Redux est compatible avec Vite. Il est destiné aux états partagés entre composants ; les champs de formulaire et autres états locaux peuvent utiliser `useState`.

## Connexion à Spring Boot

En développement, `/api/*` est transmis à `http://localhost:8080` en conservant le préfixe `/api`. Modifier `API_PROXY_TARGET` dans `.env.local` si nécessaire.

Le client accepte les options natives de Fetch. Pour envoyer du JSON, fournir `body: JSON.stringify(payload)` et l’en-tête `Content-Type: application/json`. Il renvoie `unknown` : les fonctionnalités devront valider les réponses selon le contrat backend. Les erreurs HTTP lèvent `ApiError` avec `status` et `details` ; les erreurs réseau et d’annulation sont propagées.

En production, le proxy Vite n’existe pas : configurer un reverse proxy pour `/api`, ou définir `VITE_API_BASE_URL` lors du build vers l’API et autoriser l’origine frontend côté Spring. Les variables `VITE_*` sont publiques, intégrées au bundle : ne pas y mettre de secrets.

## Suite de l’implémentation

### Fonctionnement avec l’API

L’API dans `../api` fournit l’authentification, le CRUD des tâches et les notifications. Le dashboard utilise les statuts `TODO`, `IN_PROGRESS`, `DONE`, les réponses paginées et les erreurs ProblemDetail documentées par Swagger.

Le token d’accès reste uniquement en mémoire. Le renouvellement utilise le cookie HttpOnly avec `credentials: 'include'`, et `navigator.locks` sérialise les renouvellements entre onglets. Les changements de tâches sont répercutés par SSE, puis la liste est rechargée via Fetch.

`npm test` vérifie les contrats Zod, le décodage SSE et les scénarios de renouvellement de session. Le test de contrat réel reste opt-in : définir `API_TEST_BASE`, `API_TEST_EMAIL` et `API_TEST_PASSWORD`, puis lancer `node tests/api-contract.mjs` avec l’API démarrée.
