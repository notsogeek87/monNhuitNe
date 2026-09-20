# Architecture

Deux clients complètement indépendants, qui ne partagent aucun code ni
processus, chacun avec sa propre façon de parler à n8n :

- **Android natif** (`android/`) — décrit dans "Android natif" ci-dessous.
- **PWA** (`app/` + `server/`) — décrit dans "PWA" ci-dessous, reprend le
  design initial du projet.

## Android natif (`android/`)

```
┌─────────────────────────┐        HTTPS (direct)        ┌──────────────┐
│  App Android (Kotlin/    │ ─────────────────────────────▶│  n8n API      │
│  Compose), OkHttp         │  /api/v1/*                    │  auto.lielu.eu│
└─────────────────────────┘ ◀─────────────────────────────  └──────────────┘
        ▲
        │ notification FCM (topic "workflow-failures")
        │
┌──────────────────────────┐
│  n8n Error Workflow       │  HTTP Request → https://fcm.googleapis.com/...
│  (côté serveur, dans n8n) │
└──────────────────────────┘
```

Pourquoi c'est plus simple que la PWA :

- **Pas de CORS.** CORS est une règle appliquée par les moteurs de navigateur
  (et donc par la WebView d'une app Capacitor/Cordova) à toute requête
  cross-origin. OkHttp, utilisé ici, est un client HTTP natif qui ne rend
  aucun DOM et n'exécute aucune politique de même origine — il fait de
  simples requêtes serveur-à-serveur, comme le ferait `curl`. Résultat :
  aucun backend proxy n'est nécessaire, l'app appelle
  `https://auto.lielu.eu/api/v1/...` directement avec l'en-tête
  `X-N8N-API-KEY`.
- **Pas de calcul serveur pour la santé globale.** La fonction
  `computeHealthSummary` (portée de `server/src/healthPoll.ts` vers
  `android/.../data/HealthCalculator.kt`) tourne directement sur l'appareil,
  à partir des mêmes appels n8n (`/workflows`, `/executions`).
- **Notifications via Firebase Cloud Messaging (FCM), sans backend.** L'app
  s'abonne au démarrage à un topic FCM fixe (`workflow-failures`,
  `android/.../push/MonNhuitNeMessagingService.kt`). Le déclenchement se fait
  **depuis n8n lui-même** : l'Error Workflow ajoute un node HTTP Request qui
  appelle l'API FCM pour publier sur ce topic — n8n a juste besoin d'un accès
  authentifié à l'API Google (voir `docs/NOTIFICATIONS.md`). Aucun serveur
  ne connaît ni ne stocke de token d'appareil : la diffusion par topic évite
  ce problème entièrement.
- **Stockage local.** `SettingsStore.kt` utilise
  `EncryptedSharedPreferences` (Android Keystore, matériel sur la plupart des
  appareils) pour l'URL n8n et la clé API. Contrairement à la PWA (navigateur
  sans coffre-fort matériel, d'où le PBKDF2+AES-GCM dérivé du PIN), le PIN ici
  n'est qu'un verrou d'écran (un hash comparé) — le chiffrement au repos est
  déjà garanti par le système, indépendamment du PIN.

## PWA (`app/` + `server/`)

### Vue d'ensemble

```
┌─────────────────────────┐        HTTPS        ┌──────────────────────────┐        HTTPS        ┌──────────────┐
│   PWA (SvelteKit SPA)   │ ───────────────────▶ │  Backend minimal          │ ───────────────────▶ │  n8n API      │
│   app/                  │  /api/n8n/*          │  (Fastify)                │  /api/v1/*           │  (VPS existant│
│   iOS Safari / Android  │  /hooks/*            │  server/                  │                      │   auto.lielu.eu)│
│   Chrome, installée     │  /push/*             │  - proxy CORS             │                      │              │
│   en PWA                │  /health/summary     │  - VAPID / web-push       │                      │              │
└─────────────────────────┘ ◀─────────────────── │  - poller santé/échecs    │                      └──────────────┘
        ▲   push               Web Push (VAPID)   └──────────────────────────┘
        │                                                    ▲
        └──── Service Worker ───────────────────────────────┘
                                  webhook "Error Workflow" n8n → /hooks/n8n-error
```

### Pourquoi ce découpage

- **Frontend 100% statique** (`adapter-static`, SPA fallback) : rien à faire tourner
  côté serveur pour l'UI, se déploie comme un dossier de fichiers derrière n'importe
  quel reverse proxy (Caddy/nginx) déjà en place pour n8n.
- **Backend séparé, volontairement minimal** : deux responsabilités seulement —
  1. proxy CORS transparent vers l'API n8n (n8n n'émet pas de headers CORS pensés
     pour un appel navigateur cross-origin) ;
  2. Web Push auto-hébergé (VAPID) + calcul de la santé globale, réutilisé à la fois
     par l'app (`GET /health/summary`) et par le poller de fallback interne.
- **Aucune base de données** : les abonnements push sont un fichier JSON
  (`server/data/subscriptions.json`), le volume (quelques appareils personnels)
  ne justifie pas plus.
- **Clé API n8n jamais côté serveur** : le backend est un simple relais — c'est le
  client qui envoie sa clé dans l'en-tête `X-N8N-API-KEY` à chaque requête, le
  backend la transmet à n8n sans la stocker (sauf pour amorcer son propre poller
  de santé, gardé en mémoire process, jamais persisté).

### Couches côté frontend (`app/src/lib`)

| Dossier | Rôle |
|---|---|
| `api/` | Client HTTP pur (fetch), zéro dépendance UI, testable (`workflows.test.ts`). |
| `storage/` | Chiffrement AES-GCM (WebCrypto) + persistance IndexedDB de la config (URL backend, clé API). |
| `stores/` | État Svelte : session déverrouillée (en mémoire), cache des workflows. |
| `notifications/` | Détection de capacité (`capabilities.ts`), abonnement Web Push, polling de repli. |
| `components/` | UI pure, ne fait aucun appel réseau elle-même (reçoit ses données en props). |
| `routes/` | Écrans SvelteKit : liste (`/`), détail (`/workflows/[id]`), santé (`/health`), réglages (`/settings`). |

### Sécurité du stockage local

Le PIN n'est jamais stocké. Il sert uniquement à dériver (PBKDF2, 210k itérations,
SHA-256) une clé AES-256-GCM tenue en mémoire le temps de chiffrer/déchiffrer le
blob IndexedDB `{ backendBaseUrl, n8nProxyBaseUrl, apiKey }`. Un PIN incorrect fait
échouer le déchiffrement (auth-tag GCM invalide) plutôt que de renvoyer des données
corrompues silencieusement.

### Déploiement suggéré

- `app/` : build statique (`npm run build --workspace app`) servi par le reverse
  proxy existant sous un sous-domaine dédié (ex. `monnhuitne.lielu.eu`), HTTPS
  obligatoire pour l'installabilité PWA et le Web Push.
- `server/` : process Node long-lived (`pm2`/`systemd`) sur le même VPS, exposé
  sous un autre sous-domaine (ex. `pwa-api.lielu.eu`), CORS restreint à l'origine
  de la PWA.
- Un **Error Workflow** n8n (voir `server/n8n-error-workflow.example.json`) assigné
  par défaut à tous les workflows de prod, qui POST vers `/hooks/n8n-error` dès
  qu'une exécution échoue.
