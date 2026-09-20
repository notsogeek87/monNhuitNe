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

## APK Android

Le workflow GitHub Actions [`.github/workflows/android.yml`](.github/workflows/android.yml)
build automatiquement l'APK à chaque push sur `main`/`staging`, chaque pull
request et manuellement via `workflow_dispatch` :

- `main` → release GitHub permanente et versionnée (`v1.0.<run_number>`).
- autres branches / PR → release "debug-\<branche\>", remplacée à chaque run.

Pour builder localement :

```bash
npm run cap:sync   # build de app/ + synchronisation dans android/
cd android && ./gradlew assembleDebug
```
