// App 100% client-side (adapter-static, fallback SPA) : pas de SSR, la
// couche API n'a de sens que dans le navigateur (IndexedDB, WebCrypto, fetch
// vers le proxy avec la clé déchiffrée en mémoire).
export const ssr = false;
export const prerender = false;
