# monNhuitNe

PWA de supervision et pilotage des workflows n8n (auto.lielu.eu) depuis mobile
(Android + iOS/Safari), sans passer par l'interface web n8n complète.

## Structure

```
app/      SvelteKit (adapter-static, SPA) — l'application installable
server/   Backend minimal (Fastify) — proxy CORS vers l'API n8n + Web Push
docs/     Architecture détaillée et plan de notifications Android/iOS
```

Voir [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) pour le détail des choix
techniques et [`docs/NOTIFICATIONS.md`](docs/NOTIFICATIONS.md) pour le
fonctionnement des notifications selon la plateforme.

## Démarrage

```bash
npm install

# Backend : copier server/.env.example → server/.env, remplir N8N_BASE_URL,
# ALLOWED_ORIGIN et les clés VAPID (npx web-push generate-vapid-keys)
npm run dev:server

# Frontend (dans un autre terminal)
npm run dev:app
```

À la première ouverture de l'app, l'écran **Réglages** demande l'URL du backend,
l'URL du proxy n8n, la clé API n8n, et un PIN local (chiffre la config sur
l'appareil, cf `docs/ARCHITECTURE.md`).

## Tests

```bash
npm run test:app
```
