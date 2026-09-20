# Plan de notifications — Android vs iOS

## Résumé

| | Android / Chrome | iOS / Safari |
|---|---|---|
| Web Push sans installation | ✅ fonctionne (onglet ou PWA) | ❌ jamais |
| Web Push avec PWA installée | ✅ | ✅ **si iOS ≥ 16.4** |
| Notification app fermée/arrière-plan | ✅ | ✅ (si les conditions ci-dessus sont réunies) |
| Fallback nécessaire | Non (cas marginal seulement) | Oui, systématique en dessous de 16.4 ou sans installation |

## Ce qui marche nativement

Le Web Push standard (Push API + Notifications API + Service Worker) est supporté
par les deux plateformes. La PWA s'abonne via `PushManager.subscribe()` avec la
clé publique VAPID du backend, envoie l'abonnement à `POST /push/subscribe`, et le
backend utilise `web-push` (VAPID, sans Firebase/APNs direct — c'est le navigateur/OS
qui relaie vers son propre service de push en coulisses, de façon transparente pour
nous).

- **Android/Chrome** : aucune restriction particulière. La notification arrive même
  si Chrome est fermé, PWA installée ou non.
- **iOS/Safari** : Apple n'a activé le Web Push que depuis **iOS 16.4** (mars 2023),
  et **uniquement pour les PWA installées sur l'écran d'accueil** — jamais pour un
  onglet Safari classique, même sur iOS récent. La demande de permission
  (`Notification.requestPermission()`) doit elle-même être déclenchée depuis le
  contexte standalone (`display-mode: standalone`) ; appelée depuis un onglet
  Safari normal, elle est silencieusement no-op sur iOS.

## Ce qui nécessite un relais / fallback

Détection de capacité (`app/src/lib/notifications/capabilities.ts`) :

```ts
isStandalone   // matchMedia('(display-mode: standalone)') ou navigator.standalone
isIos          // sniffing UA
iosVersion     // extrait de l'UA (ex: "OS 16_4" → 16.4)
supportsPush   // 'PushManager' in window && 'Notification' in window
pushIsUsable   // sur iOS : supportsPush && isStandalone && iosVersion >= 16.4
               // sinon  : supportsPush
```

Selon `pushIsUsable` :

1. **`true`** → bouton "Activer les notifications" dans `/settings` actif, flux
   Web Push standard décrit ci-dessus.
2. **`false`** → le bouton reste désactivé avec un message explicite (iOS trop
   ancien, ou pas encore installé), et l'app bascule sur le **polling au
   premier plan** (`app/src/lib/notifications/polling.ts`) :
   - déclenché sur `visibilitychange`/`focus` (pas de minuteur en arrière-plan —
     impossible à faire fiablement sans push, surtout sur iOS où un Service
     Worker ne se réveille pas seul) ;
   - appelle `GET /health/summary`, compare `failuresLast24h` à la dernière
     valeur vue (`localStorage`) ;
   - si ça a augmenté, affiche un bandeau dans l'app renvoyant vers `/health`.
   - Limite acceptée : rien ne prévient l'utilisateur si l'app n'est pas
     rouverte. C'est une limitation d'iOS elle-même (pas de moyen contournable
     sans passer par une vraie app native ou par SMS/email, hors périmètre ici).

## Double voie côté détection d'échec (backend)

Indépendamment du device, deux mécanismes alimentent `notifyFailure()` :

1. **Instantané** : un *Error Workflow* n8n (`server/n8n-error-workflow.example.json`),
   assigné par défaut à tous les workflows de prod dans les réglages n8n
   (Settings → Workflows → Error Workflow, ou par workflow individuellement),
   POST vers `POST /hooks/n8n-error` dès qu'une exécution échoue.
2. **Sondage de secours** (`server/src/healthPoll.ts`, toutes les
   `HEALTH_POLL_INTERVAL_MINUTES` minutes, défaut 10) : requête
   `GET /executions?status=error` sur l'API n8n, notifie toute exécution pas
   déjà vue (`markExecutionNotified`) — couvre l'oubli de configurer l'Error
   Workflow sur un workflow donné, ou une panne n8n empêchant le webhook de
   partir.

## Checklist de mise en service

1. Générer les clés VAPID une fois : `npx web-push generate-vapid-keys`, coller
   dans `server/.env` (`VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY`).
2. Importer `server/n8n-error-workflow.example.json` dans n8n, adapter l'URL du
   node HTTP Request à votre sous-domaine backend, l'assigner comme Error
   Workflow par défaut.
3. Sur iPhone : Safari → icône Partager → **Sur l'écran d'accueil** avant
   d'ouvrir `/settings` et d'appuyer sur "Activer les notifications" (le bouton
   restera désactivé sinon, avec le message explicatif).
