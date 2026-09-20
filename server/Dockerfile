# Image du backend (server/) uniquement. Le frontend (app/) n'a pas besoin
# d'être servi séparément par ce projet : il est buildé dans l'APK Capacitor
# (webDir), et/ou déployé comme dossier statique par ailleurs (voir README).
#
# Contexte de build = racine du dépôt (docker-compose.yml y fait référence),
# car le monorepo npm workspaces a un seul package-lock.json partagé entre
# app/ et server/.
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
COPY app/package.json app/package.json
COPY server/package.json server/package.json
RUN npm ci --no-audit --no-fund
COPY server server
RUN npm run build --workspace server

FROM node:22-alpine
WORKDIR /app
COPY --from=build /app/node_modules node_modules
COPY --from=build /app/server/dist server/dist
COPY --from=build /app/server/package.json server/package.json
WORKDIR /app/server
ENV NODE_ENV=production
EXPOSE 8787
CMD ["node", "dist/index.js"]
