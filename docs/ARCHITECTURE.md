# Architecture

## Vue d'ensemble

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

## Pourquoi ce découpage

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

## Couches côté frontend (`app/src/lib`)

| Dossier | Rôle |
|---|---|
| `api/` | Client HTTP pur (fetch), zéro dépendance UI, testable (`workflows.test.ts`). |
| `storage/` | Chiffrement AES-GCM (WebCrypto) + persistance IndexedDB de la config (URL backend, clé API). |
| `stores/` | État Svelte : session déverrouillée (en mémoire), cache des workflows. |
| `notifications/` | Détection de capacité (`capabilities.ts`), abonnement Web Push, polling de repli. |
| `components/` | UI pure, ne fait aucun appel réseau elle-même (reçoit ses données en props). |
| `routes/` | Écrans SvelteKit : liste (`/`), détail (`/workflows/[id]`), santé (`/health`), réglages (`/settings`). |

## Sécurité du stockage local

Le PIN n'est jamais stocké. Il sert uniquement à dériver (PBKDF2, 210k itérations,
SHA-256) une clé AES-256-GCM tenue en mémoire le temps de chiffrer/déchiffrer le
blob IndexedDB `{ backendBaseUrl, n8nProxyBaseUrl, apiKey }`. Un PIN incorrect fait
échouer le déchiffrement (auth-tag GCM invalide) plutôt que de renvoyer des données
corrompues silencieusement.

## Déploiement suggéré

- `app/` : build statique (`npm run build --workspace app`) servi par le reverse
  proxy existant sous un sous-domaine dédié (ex. `monnhuitne.lielu.eu`), HTTPS
  obligatoire pour l'installabilité PWA et le Web Push.
- `server/` : process Node long-lived (`pm2`/`systemd`) sur le même VPS, exposé
  sous un autre sous-domaine (ex. `pwa-api.lielu.eu`), CORS restreint à l'origine
  de la PWA.
- Un **Error Workflow** n8n (voir `server/n8n-error-workflow.example.json`) assigné
  par défaut à tous les workflows de prod, qui POST vers `/hooks/n8n-error` dès
  qu'une exécution échoue.
