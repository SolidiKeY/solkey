self.performance = self.performance || { now: () => Date.now() };

const fetchFromHost = self.fetch.bind(self);
const assets = self.solkeyAssets || {};
const proverWasm = assets.wasm || new URL('solkey.js.wasm', self.location.href);
const proverWasmResponse = fetchFromHost(proverWasm);
self.fetch = (resource, options) => (String(resource).endsWith('.wasm')
  ? proverWasmResponse.then((response) => response.clone())
  : fetchFromHost(resource, options));

const describe = (e) => String(e && e.message ? e.message : e);
const pending = [];
let ready = false;

self.addEventListener('unhandledrejection', (event) => {
  if (!ready) postMessage({ type: 'failed', error: describe(event.reason) });
});

importScripts(assets.soljson || 'soljson.js');
const solcCompile = Module.cwrap('solidity_compile', 'string', ['string', 'number', 'number']);
const solcVersion = Module.cwrap('solidity_version', 'string', []);
self.solkeySolc = {
  compile: (input) => solcCompile(input, 0, 0),
  version: () => solcVersion(),
};

self.solkeyReady = () => {
  ready = true;
  postMessage({ type: 'ready', solc: solcVersion() });
  while (pending.length) handle(pending.shift());
};

function handle(request) {
  let result;
  try {
    result = request.type === 'solc'
      ? JSON.parse(solcCompile(JSON.stringify(request.args.input), 0, 0))
      : JSON.parse(self.solkey.call(request.type, JSON.stringify(request.args || {})));
  } catch (e) {
    result = { error: describe(e) };
  }
  postMessage({ type: 'result', id: request.id, result });
}

self.onmessage = (event) => {
  if (ready) handle(event.data);
  else pending.push(event.data);
};

try {
  importScripts(assets.solkey || 'solkey.js');
} catch (e) {
  postMessage({ type: 'failed', error: describe(e) });
}
