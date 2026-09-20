# Plan de notifications

Deux systèmes de notification totalement séparés, un par client :

- **App Android native** (`android/`) → Firebase Cloud Messaging (FCM),
  déclenché directement par n8n. Voir "Android natif" ci-dessous.
- **PWA** (`app/`+`server/`, navigateur/iOS Safari) → Web Push (VAPID),
  déclenché par le backend `server/`. Voir "PWA" plus bas (contenu
  historique de ce document, inchangé).

## Android natif (Firebase Cloud Messaging)

Pas de backend, pas de token d'appareil à gérer : l'app s'abonne une fois à
un topic FCM fixe (`workflow-failures`), et n8n publie directement dessus
quand un workflow échoue.

### Mise en service, étape par étape

1. **Créer un projet Firebase** (gratuit) : [console.firebase.google.com](https://console.firebase.google.com/)
   → "Ajouter un projet". Aucune carte bancaire requise pour Cloud Messaging.
2. **Ajouter une app Android** dans ce projet Firebase : nom de package
   `com.monnhuitne.app` (celui déclaré dans `android/app/build.gradle`,
   champ `applicationId`).
3. Télécharger le fichier **`google-services.json`** proposé à cette étape,
   et le placer dans `android/app/google-services.json` (jamais commité,
   voir `android/.gitignore`). Sans ce fichier, l'app compile et fonctionne
   normalement, seules les notifications restent inactives.
4. Relancer un build de l'APK (`cd android && ./gradlew assembleDebug`, ou
   laisser la CI le faire — voir README) : `app/build.gradle` détecte le
   fichier et active le plugin Firebase automatiquement.
5. **Autoriser un compte de service Google** à publier sur FCM, pour que
   n8n puisse appeler l'API :
   - Dans la [console Google Cloud](https://console.cloud.google.com/) du
     **même projet** que Firebase (Firebase crée un projet GCP du même nom
     automatiquement) : IAM et administration → Comptes de service → Créer
     un compte de service.
   - Rôle à lui donner : **Firebase Cloud Messaging Admin**.
   - Créer une clé JSON pour ce compte de service et la télécharger (bouton
     "Gérer les clés" → "Ajouter une clé" → JSON).
6. **Dans n8n**, créer une credential utilisant cette clé JSON (n8n a un
   type de credential dédié aux comptes de service Google — le nom exact
   dans le menu dépend de la version de n8n installée ; voir la
   [documentation n8n sur les credentials Google](https://docs.n8n.io/integrations/builtin/credentials/google/)
   pour la procédure à jour), puis ajouter un node **HTTP Request** dans
   l'Error Workflow (voir `server/n8n-error-workflow.example.json` pour la
   structure générale d'un Error Workflow — ce node FCM s'ajoute en
   parallèle du node existant, branché sur le même Error Trigger) :
   - Méthode : `POST`
   - URL : `https://fcm.googleapis.com/v1/projects/<PROJECT_ID>/messages:send`
     (remplacer `<PROJECT_ID>` par l'ID du projet Firebase, visible dans
     Firebase → Paramètres du projet)
   - Authentification : la credential de compte de service créée à l'étape
     précédente (scope requis : `https://www.googleapis.com/auth/firebase.messaging`)
   - Corps JSON :
     ```json
     {
       "message": {
         "topic": "workflow-failures",
         "notification": {
           "title": "Échec : {{ $json.workflow.name }}",
           "body": "{{ $json.execution.error.message }}"
         }
       }
     }
     ```
7. **Tester sans passer par n8n** d'abord : Firebase console → Cloud
   Messaging → "Envoyer votre premier message" → cibler le topic
   `workflow-failures` → vérifier que la notification arrive sur le
   téléphone (l'app doit avoir été ouverte au moins une fois, pour
   s'abonner au topic et pour que la permission Android 13+ ait été
   accordée).

### Pourquoi un topic plutôt que des tokens d'appareil

FCM propose deux modes : cibler un token d'appareil précis, ou publier sur un
*topic* auquel n'importe quel appareil peut s'abonner. Avec des tokens, il
faudrait un endroit pour les stocker et les tenir à jour (un appareil qui se
réinstalle en génère un nouveau) — exactement le genre de backend qu'on
cherche à éviter côté Android. Le topic supprime ce problème : l'app gère
son abonnement elle-même (`MonNhuitNeMessagingService.kt`), n8n n'a besoin
de connaître que le nom du topic, fixe et partagé par tous les appareils.

## PWA (navigateur / iOS Safari)

Tout ce qui suit décrit le système Web Push utilisé par la PWA (`app/` +
`server/`) — sans rapport avec l'app Android native ci-dessus. La ligne
"Android / Chrome" ne concerne que l'usage de la PWA dans un navigateur
Chrome (ou installée comme PWA) sur Android, pas l'app native.

### Résumé

| | Android / Chrome | iOS / Safari |
|---|---|---|
| Web Push sans installation | ✅ fonctionne (onglet ou PWA) | ❌ jamais |
| Web Push avec PWA installée | ✅ | ✅ **si iOS ≥ 16.4** |
| Notification app fermée/arrière-plan | ✅ | ✅ (si les conditions ci-dessus sont réunies) |
| Fallback nécessaire | Non (cas marginal seulement) | Oui, systématique en dessous de 16.4 ou sans installation |

### Ce qui marche nativement

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

### Ce qui nécessite un relais / fallback

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

### Double voie côté détection d'échec (backend)

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

### Checklist de mise en service

1. Générer les clés VAPID une fois : `npx web-push generate-vapid-keys`, coller
   dans `server/.env` (`VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY`).
2. Importer `server/n8n-error-workflow.example.json` dans n8n, adapter l'URL du
   node HTTP Request à votre sous-domaine backend, l'assigner comme Error
   Workflow par défaut.
3. Sur iPhone : Safari → icône Partager → **Sur l'écran d'accueil** avant
   d'ouvrir `/settings` et d'appuyer sur "Activer les notifications" (le bouton
   restera désactivé sinon, avec le message explicatif).
