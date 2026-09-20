# monNhuitNe

PWA de supervision et pilotage des workflows n8n (auto.lielu.eu) depuis mobile
(Android + iOS/Safari), sans passer par l'interface web n8n complète.

## Structure

```
app/      SvelteKit (adapter-static, SPA) — l'application installable
server/   Backend minimal (Fastify) — proxy CORS vers l'API n8n + Web Push
android/  Projet Capacitor natif — génère l'APK Android
docs/     Architecture détaillée et plan de notifications Android/iOS
```

Voir [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) pour le détail des choix
techniques et [`docs/NOTIFICATIONS.md`](docs/NOTIFICATIONS.md) pour le
fonctionnement des notifications selon la plateforme.

## 🆘 Aide : je ne sais pas où trouver une information

Si un champ de configuration bloque, voici concrètement **où aller cliquer**
pour chaque type d'information demandée plus bas.

### Ce qu'il faut avoir sous la main avant de commencer

1. **L'accès à votre instance n8n** (`auto.lielu.eu`) — c'est-à-dire pouvoir
   vous connecter à son interface web avec un compte administrateur. Si vous
   ne l'avez pas, c'est la personne qui a installé n8n (vous-même dans le
   passé, ou un prestataire) qui a créé ce compte.
2. **L'accès au serveur (VPS)** où tourne n8n — un terminal (SSH) ou le
   panneau de contrôle de votre hébergeur (OVH, Hetzner, Scaleway...). C'est
   là que le backend (`server/`) doit être lancé en permanence, à côté de
   n8n. Sans cet accès, seule la partie "développement en local sur votre
   ordinateur" (section Démarrage) est possible.
3. **Un compte GitHub** avec accès à ce dépôt — pour déclencher/consulter les
   builds d'APK (section APK Android) et modifier le code.

Si vous n'avez **aucun** de ces trois accès, ce projet ne peut pas encore être
mis en service : il faut d'abord les obtenir (ou identifier qui les détient)
avant de continuer.

### Où trouver l'URL de mon instance n8n (`N8N_BASE_URL`)

C'est l'adresse que vous tapez dans un navigateur pour ouvrir n8n — ici
`https://auto.lielu.eu`. Ajoutez `/api/v1` au bout pour obtenir la valeur du
`.env` : `https://auto.lielu.eu/api/v1`.

### Comment me connecter au serveur pour lancer les commandes (`npm run dev:server`, etc.)

Ces commandes (section **Démarrage**) s'exécutent dans un terminal — soit sur
votre ordinateur (pour tester en local), soit sur le VPS via SSH (pour le
faire tourner en continu). Si "terminal" ou "SSH" ne vous parle pas, c'est un
prérequis technique plus large que ce README ne couvre pas : voir par exemple
le [guide DigitalOcean "How To Connect To Your Droplet with SSH"](https://www.digitalocean.com/community/tutorials/how-to-connect-to-your-droplet-with-ssh)
(la logique est la même chez tous les hébergeurs), ou demandez à la personne
qui gère le serveur de lancer `npm run build:server && npm start` (dans
`server/`) pour vous, en tâche de fond (`pm2` ou service `systemd`).

### Où récupérer la clé API n8n, étape par étape

1. Ouvrez `https://auto.lielu.eu` et connectez-vous.
2. Cliquez sur votre avatar/nom en bas à gauche → **Settings** (ou l'icône
   d'engrenage selon la version).
3. Menu **n8n API** dans la colonne de gauche des réglages.
4. Bouton **Create an API key** → donnez-lui un nom (ex. "monNhuitNe") →
   copiez la clé affichée **immédiatement** (elle ne sera plus jamais
   visible en entier ensuite).
5. Collez-la dans l'écran **Réglages** de l'app (champ "Clé API n8n").

Capture d'écran officielle et détails : [documentation n8n — API keys](https://docs.n8n.io/api/authentication/).

### Où récupérer l'APK une fois le build terminé

Sur GitHub, dans ce dépôt : onglet **Releases** (colonne de droite de la page
principale du dépôt, ou `https://github.com/<compte>/<dépôt>/releases`).
Chaque release (`v1.0.x` pour `main`, `debug-<branche>` pour les autres)
contient le fichier `.apk` à télécharger directement sur le téléphone Android
(autoriser "Installer des applications inconnues" lors de la première
installation). Alternative : onglet **Actions** → un run récent → section
"Artifacts" en bas de page (nécessite d'être connecté à GitHub).

### Glossaire des termes techniques utilisés dans ce README

| Terme | Explication simple |
|---|---|
| **VPS** | Un serveur loué chez un hébergeur (OVH, Hetzner...), toujours allumé, où tournent n8n et le backend. |
| **Reverse proxy** (Caddy/nginx) | Un logiciel installé sur le VPS qui reçoit toutes les requêtes web et les redirige vers le bon service (n8n, backend...) selon l'adresse demandée ; c'est aussi lui qui gère le HTTPS. |
| **CORS** | Règle de sécurité des navigateurs qui bloque par défaut les appels d'un site web (la PWA) vers un autre domaine (le backend) — d'où `ALLOWED_ORIGIN`, la liste blanche. |
| **PWA** | "Progressive Web App" : un site web qui peut s'installer sur le téléphone comme une vraie app. |
| **VAPID** | Le système de clés qui permet au backend d'envoyer des notifications push sans dépendre de Google/Apple. |
| **Keystore** | Le fichier contenant la clé cryptographique qui signe l'APK Android ; obligatoire pour publier sur le Play Store. |
| **CI / CI-CD** | "Intégration continue" : les étapes automatiques (ici, le build de l'APK) qui s'exécutent sur les serveurs de GitHub à chaque `push`, sans rien faire sur votre machine. |
| **`workflow_dispatch`** | Le bouton "Run workflow" qui permet de déclencher le build manuellement depuis l'onglet Actions de GitHub, sans faire de `push`. |
| **Secret GitHub** | Une valeur sensible (mot de passe, clé) stockée de façon chiffrée dans les réglages du dépôt GitHub, utilisable par les workflows sans apparaître dans le code. |

Si une information précise vous manque encore après avoir lu cette section,
dites exactement à quelle étape vous êtes bloqué·e (quel écran, quel champ)
et on complétera ce guide en conséquence.

## Configuration

### 1. Backend (`server/.env`)

Copier `server/.env.example` en `server/.env` et remplir :

| Variable | Description | Où la trouver / comment la générer |
|---|---|---|
| `N8N_BASE_URL` | URL de l'API REST n8n (ex. `https://auto.lielu.eu/api/v1`) | C'est l'URL de votre instance n8n suivie de `/api/v1`. |
| `ALLOWED_ORIGIN` | Origine autorisée en CORS (l'URL où la PWA est servie) | Doit correspondre exactement à l'URL publique de `app/` (schéma + domaine, sans slash final). |
| `PORT` | Port d'écoute du backend | Libre, `8787` par défaut. |
| `VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY` | Clés Web Push | À générer **une seule fois** avec `npx web-push generate-vapid-keys` (package [`web-push`](https://www.npmjs.com/package/web-push)), puis à figer dans `.env` — ne pas les régénérer ensuite, cela invaliderait tous les abonnements déjà enregistrés. |
| `VAPID_SUBJECT` | Contact associé aux clés VAPID | Une adresse `mailto:` ou une URL `https://`, exigée par la [spec Web Push](https://www.rfc-editor.org/rfc/rfc8292). |
| `HEALTH_POLL_INTERVAL_MINUTES` | Fréquence du sondage de secours | En minutes, `10` par défaut (voir `docs/NOTIFICATIONS.md`). |
| `STALE_WORKFLOW_DAYS` | Seuil d'inactivité "workflow figé" | En jours, `7` par défaut. |
| `SUBSCRIPTIONS_FILE` | Chemin du fichier JSON des abonnements push | Chemin local, le dossier doit exister/être accessible en écriture. |

### 2. Clé API n8n

Générée depuis n8n : **Réglages → n8n API** (ou `/settings/api` dans l'UI n8n),
bouton "Create an API key". Voir la
[documentation officielle n8n sur l'authentification API](https://docs.n8n.io/api/authentication/).
Cette clé n'est **jamais stockée côté backend** : elle est saisie une fois dans
l'app (écran Réglages) et chiffrée localement sur l'appareil (cf. `docs/ARCHITECTURE.md`).

### 3. Notifications (Web Push + Error Workflow n8n)

Voir la checklist complète dans [`docs/NOTIFICATIONS.md`](docs/NOTIFICATIONS.md#checklist-de-mise-en-service) :
générer les clés VAPID (étape 1 ci-dessus), importer
`server/n8n-error-workflow.example.json` dans n8n et l'assigner comme
[*Error Workflow*](https://docs.n8n.io/flow-logic/error-handling/) par défaut
(`Settings → Workflows → Error Workflow` dans l'UI n8n, ou par workflow
individuellement), en adaptant l'URL du node HTTP Request à votre backend.

### 4. Application (écran **Réglages**, à la première ouverture)

| Champ | Valeur attendue |
|---|---|
| URL du backend (proxy + push) | URL publique de `server/`, ex. `https://pwa-api.lielu.eu` (sans chemin). |
| URL du proxy API n8n | URL backend + route de proxy, ex. `https://pwa-api.lielu.eu/api/n8n`. |
| Clé API n8n | La clé générée à l'étape 2. |
| PIN de déverrouillage | Un code local (4 chiffres minimum) qui **chiffre** ces informations sur l'appareil (PBKDF2 + AES-GCM, jamais transmis ni stocké tel quel — cf. `docs/ARCHITECTURE.md#sécurité-du-stockage-local`). En cas d'oubli, il n'y a pas de récupération : il faut réinitialiser via "Supprimer la configuration de cet appareil" et tout ressaisir. |

Sur iPhone, les notifications ne peuvent être activées qu'**après** avoir
installé l'app sur l'écran d'accueil (Safari → icône Partager → "Sur l'écran
d'accueil" — voir le
[guide MDN sur l'installation des PWA](https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/Guides/Making_PWAs_installable)),
et seulement à partir d'iOS 16.4 ; le bouton reste sinon désactivé avec un
message explicatif (détails dans `docs/NOTIFICATIONS.md`).

### 5. Déploiement (HTTPS obligatoire)

La PWA (installabilité + Web Push) et le backend (CORS) exigent tous les deux
d'être servis en HTTPS. Suggestion : passer chacun derrière le reverse proxy
déjà en place pour n8n, par exemple avec
[Caddy](https://caddyserver.com/docs/quick-starts/reverse-proxy) (HTTPS
automatique via Let's Encrypt) ou
[nginx](https://nginx.org/en/docs/http/ngx_http_proxy_module.html). Détails
des sous-domaines suggérés dans `docs/ARCHITECTURE.md#déploiement-suggéré`.

## Démarrage

```bash
npm install

# Backend : voir "Configuration" ci-dessus pour server/.env
npm run dev:server

# Frontend (dans un autre terminal)
npm run dev:app
```

## Tests

```bash
npm run test:app
```

## APK Android

Le workflow GitHub Actions [`.github/workflows/android.yml`](.github/workflows/android.yml)
build automatiquement l'APK à chaque push sur `main`/`staging`, chaque pull
request et manuellement via
[`workflow_dispatch`](https://docs.github.com/en/actions/using-workflows/manually-running-a-workflow)
(onglet **Actions** du dépôt GitHub → sélectionner le workflow "Android APK" →
bouton "Run workflow") :

- `main` → release GitHub permanente et versionnée (`v1.0.<run_number>`).
- autres branches / PR → release "debug-\<branche\>", remplacée à chaque run.

Aucun secret GitHub à configurer : le workflow utilise uniquement le
[`GITHUB_TOKEN` automatique](https://docs.github.com/en/actions/security-guides/automatic-token-authentication).

L'APK est signé avec la **clé de debug** committée dans le dépôt (pas de clé
de production) — suffisant pour une installation manuelle ("sources
inconnues") ou une distribution à des testeurs internes, mais **pas** pour le
Play Store. Le jour où c'est nécessaire, voir le guide officiel Capacitor sur
la [signature et le déploiement Android vers le Play Store](https://capacitorjs.com/docs/android/deploying-to-google-play)
(génération d'un keystore, configuration de `android/app/build.gradle`, puis
ajout du keystore et de son mot de passe comme
[secrets GitHub](https://docs.github.com/en/actions/security-guides/using-secrets-in-github-actions)
pour signer en CI).

Pour builder localement :

```bash
npm run cap:sync   # build de app/ + synchronisation dans android/
cd android && ./gradlew assembleDebug
```
