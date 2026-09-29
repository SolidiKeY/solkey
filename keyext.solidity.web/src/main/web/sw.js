const CACHE = 'solkey-@BUILD_ID@';
const CORE = [
  './',
  'index.html',
  'app.js',
  'editor.js',
  'runtime.js',
  'inspector.js',
  'style.css',
  'worker.js',
  'soljson.js',
  'solkey.js',
  'solkey.js.wasm',
  'starter.sol',
  'examples.json',
  'pwa.js',
  'split.js',
  'theme.js',
  'manifest.webmanifest',
  'icons/icon.svg',
  'icons/icon-192.png',
  'icons/apple-touch-icon.png',
];

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(CORE)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', (event) => {
  event.waitUntil(caches.keys()
    .then((keys) => Promise.all(keys.filter((k) => k.startsWith('solkey-') && k !== CACHE)
      .map((k) => caches.delete(k))))
    .then(() => self.clients.claim()));
});

self.addEventListener('fetch', (event) => {
  const request = event.request;
  if (request.method !== 'GET' || new URL(request.url).origin !== self.location.origin) return;
  event.respondWith(caches.open(CACHE).then(async (cache) => {
    const cached = await cache.match(request, { ignoreSearch: true });
    if (cached) return cached;
    try {
      const response = await fetch(request);
      if (response.ok) cache.put(request, response.clone());
      return response;
    } catch (e) {
      const fallback = request.mode === 'navigate' ? await cache.match('index.html') : undefined;
      if (fallback) return fallback;
      throw e;
    }
  }));
});
