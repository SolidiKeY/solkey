import { existsSync, readdirSync, readFileSync, mkdirSync } from 'node:fs';
import { readFile } from 'node:fs/promises';
import http from 'node:http';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { chromium } from 'playwright-core';

const site = path.resolve(process.argv[2]);
const contracts = path.join(path.dirname(fileURLToPath(import.meta.url)), 'contracts');
const shots = process.env.SOLKEY_SHOTS;
const TYPES = {
  '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css', '.json': 'application/json',
  '.sol': 'text/plain', '.wasm': 'application/wasm',
};

const server = http.createServer(async (req, res) => {
  const url = decodeURIComponent(new URL(req.url, 'http://localhost').pathname);
  const file = path.join(site, url.endsWith('/') ? `${url}index.html` : url);
  if (!file.startsWith(site)) {
    res.writeHead(403).end();
    return;
  }
  try {
    const body = await readFile(file);
    res.writeHead(200, { 'content-type': TYPES[path.extname(file)] || 'application/octet-stream' });
    res.end(body);
  } catch {
    res.writeHead(404).end();
  }
});
await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
const origin = site.endsWith('.html') ? pathToFileURL(site).href : `http://localhost:${server.address().port}/`;

function headlessShell() {
  if (process.env.SOLKEY_CHROMIUM) return process.env.SOLKEY_CHROMIUM;
  const cache = path.join(os.homedir(), '.cache', 'ms-playwright');
  if (!existsSync(cache)) return undefined;
  const found = readdirSync(cache).filter((d) => d.startsWith('chromium_headless_shell-')).sort().reverse()
    .map((d) => path.join(cache, d, 'chrome-headless-shell-linux64', 'chrome-headless-shell'))
    .find(existsSync);
  return found;
}

async function launch() {
  try {
    return await chromium.launch();
  } catch (e) {
    const executablePath = headlessShell();
    if (!executablePath) throw e;
    return chromium.launch({ executablePath });
  }
}

const browser = await launch();
let failures = 0;
const check = (label, ok) => {
  console.log(`${ok ? 'ok  ' : 'FAIL'} ${label}`);
  if (!ok) failures++;
};
if (shots) mkdirSync(shots, { recursive: true });
const shot = async (page, name) => {
  if (shots) await page.screenshot({ path: path.join(shots, `${name}.png`), fullPage: true });
};

const status = (page) => page.textContent('#statusText');
const functions = (page) => page.$$eval('#functions tbody tr[data-fn]', (rows) => rows.map((r) => r.dataset.fn));
const results = (page) => page.$$eval('#functions tbody tr[data-fn]',
  (rows) => rows.map((r) => `${r.dataset.fn}=${r.cells[2].textContent}`).join());
const listed = (page, count) => page.waitForFunction(
  (n) => document.querySelectorAll('#functions tbody tr[data-fn]').length === n, count, { timeout: 180000 })
  .catch(async (e) => {
    const state = await page.evaluate(() => ({
      status: document.getElementById('statusText').textContent,
      error: document.getElementById('error').textContent.slice(0, 300),
      rows: [...document.querySelectorAll('#functions tbody tr[data-fn]')].map((r) => r.dataset.fn),
      editor: document.querySelector('.cm-content').innerText.slice(0, 120),
    }));
    throw new Error(`expected ${count} functions: ${JSON.stringify(state)}`);
  });
const verified = async (page) => {
  await page.click('#verify');
  await page.waitForFunction(() => !document.getElementById('summary').hidden, null, { timeout: 600000 });
};
const editorText = (page) => page.$eval('.cm-content', (el) => el.innerText);

async function setOption(page, id, value) {
  await page.evaluate(() => { document.querySelector('.options').open = true; });
  if (id === 'strategyArith') await page.selectOption('select[data-strategy="NON_LIN_ARITH_OPTIONS_KEY"]', value);
  else await page.fill(`#${id}`, value);
  await page.evaluate(() => { document.querySelector('.options').open = false; });
}

async function typeSource(page, file) {
  await page.click('.cm-content');
  await page.keyboard.press('Control+A');
  await page.keyboard.press('Delete');
  await page.keyboard.insertText(readFileSync(path.join(contracts, file), 'utf8'));
}

async function desktop() {
  const context = await browser.newContext({ viewport: { width: 1280, height: 860 }, colorScheme: 'light', acceptDownloads: true });
  const page = await context.newPage();
  const problems = [];
  page.on('pageerror', (e) => problems.push(e.message));
  await page.goto(origin);
  await listed(page, 4);
  check(`starter listed: ${await functions(page)}`, (await functions(page)).join() === 'setThenIncrement,localArithmetic,branches,wrongClaim');
  await shot(page, 'desktop-1-starter');

  await verified(page);
  check(`starter verdicts: ${await results(page)}`, await results(page) === 'setThenIncrement=PASS,localArithmetic=PASS,branches=PASS,wrongClaim=FAIL');
  const marked = await page.waitForFunction(() => {
    const shown = (kind) => [...document.querySelectorAll(`.cm-verdict-gutter .verdict-${kind}`)]
      .filter((m) => m.closest('.cm-gutterElement').style.visibility !== 'hidden').length;
    return shown('pass') === 3 && shown('fail') === 1;
  }, null, { timeout: 10000 })
    .then(() => true, () => false);
  check('editor gutter shows 3 ✓ and 1 ✗', marked);
  await page.click('tr[data-fn="wrongClaim"] button.link');
  const selected = await page.evaluate(() => getSelection().toString());
  check(`clicking a function selects it in the editor (${selected.trim()})`, selected.includes('function wrongClaim'));
  await page.click('tr.detail summary');
  await shot(page, 'desktop-2-verified');

  check('proof statistics are shown', /nodes/.test(await page.textContent('tr[data-fn="setThenIncrement"] td.time')));

  await page.click('#evm');
  await page.waitForFunction(() => document.querySelectorAll('td.evm-col .badge:not(.running)').length === 4, null, { timeout: 120000 });
  const evm = await page.$$eval('#functions tbody tr[data-fn]', (rows) => rows.map((r) => `${r.dataset.fn}=${r.cells[3].textContent}`).join());
  check(`EVM run: ${evm}`, evm === 'setThenIncrement=OK,localArithmetic=OK,branches=SKIP,wrongClaim=ASSERT');

  await page.click('tr[data-fn="wrongClaim"] button[title^="Inspect"]');
  await page.waitForSelector('#inspector:not([hidden])', { timeout: 120000 });
  await page.waitForFunction(() => document.querySelectorAll('#treePane .tree-node').length > 1, null, { timeout: 60000 });
  check(`inspector: ${await page.textContent('#inspectorSummary')}`, /1 open goal/.test(await page.textContent('#inspectorSummary')));
  await shot(page, 'desktop-5-inspector');
  await page.click('#treePane .tree-node >> nth=0');
  await page.waitForFunction(() => /Node 0/.test(document.getElementById('nodeTitle').textContent));
  await page.click('#nodePrune');
  await page.waitForFunction(() => /· 1 nodes/.test(document.getElementById('inspectorSummary').textContent), null, { timeout: 60000 });
  check('pruning at the root leaves one node', true);
  const box = await page.evaluate(() => {
    const pre = document.getElementById('sequent');
    const text = pre.textContent;
    const at = text.indexOf('wrongClaim');
    const walker = document.createTreeWalker(pre, NodeFilter.SHOW_TEXT);
    let total = 0;
    while (walker.nextNode()) {
      const node = walker.currentNode;
      if (at < total + node.textContent.length) {
        const range = document.createRange();
        range.setStart(node, at - total + 2);
        range.setEnd(node, at - total + 3);
        const r = range.getBoundingClientRect();
        return { x: r.left + r.width / 2, y: r.top + r.height / 2 };
      }
      total += node.textContent.length;
    }
    return null;
  });
  await page.mouse.click(box.x, box.y);
  await page.waitForSelector('#ruleMenu:not([hidden]) .menu-item', { timeout: 60000 });
  check(`clicking the program lists rules: ${(await page.$$eval('#ruleMenu .menu-item', (i) => i.slice(0, 3).map((x) => x.textContent))).join(', ')}`, true);
  await shot(page, 'desktop-6-rules');
  await page.click('#ruleMenu > .menu-item:not([title^="Needs"]) >> nth=0');
  await page.waitForFunction(() => !/· 1 nodes/.test(document.getElementById('inspectorSummary').textContent), null, { timeout: 60000 });
  check(`applying a rule by hand: ${await page.textContent('#inspectorSummary')}`, true);
  await page.click('#inspectorAuto');
  await page.waitForFunction(() => document.querySelectorAll('#treePane .tree-node').length > 5, null, { timeout: 120000 });
  check(`running the prover again: ${await page.textContent('#inspectorSummary')}`, /1 open goal/.test(await page.textContent('#inspectorSummary')));
  await page.click('.inspector .tab[data-tab="problem"]');
  await page.waitForFunction(() => /\\problem/.test(document.getElementById('problemPane').textContent), null, { timeout: 60000 });
  check('the generated problem is shown', true);
  await page.click('#inspectorClose');

  await setOption(page, 'strategyArith', 'NON_LIN_ARITH_COMPLETION');
  await page.uncheck('#all');
  await page.check('tr[data-fn="localArithmetic"] input');
  await verified(page);
  check(`a strategy option (model search) still proves: ${await results(page)}`, (await results(page)).includes('localArithmetic=PASS'));
  await setOption(page, 'strategyArith', '');

  await typeSource(page, 'Simple.sol');
  await listed(page, 3);
  await verified(page);
  check(`typed contract: ${await results(page)}`, await results(page) === 'incrementHolds=PASS,arithmeticHolds=PASS,wrongAssertFails=FAIL');
  check(`summary ${await page.textContent('#summary')}`, (await page.textContent('#summary')) === '2/3 closed');

  await setOption(page, 'timeLimit', '0.001');
  await page.uncheck('#all');
  await page.check('tr[data-fn="incrementHolds"] input');
  await verified(page);
  check(`a 1 ms time limit stops the proof: ${await results(page)}`, (await results(page)).startsWith('incrementHolds=TIMEOUT'));
  await setOption(page, 'timeLimit', '60');

  await page.setInputFiles('#file', path.join(contracts, 'Simple.sol'));
  await listed(page, 3);
  await verified(page);
  const [download] = await Promise.all([
    page.waitForEvent('download', { timeout: 120000 }),
    page.click('tr[data-fn="incrementHolds"] button[title^="Download"]'),
  ]);
  const proof = readFileSync(await download.path(), 'utf8');
  check(`proof download ${download.suggestedFilename()}`, download.suggestedFilename() === 'Simple.incrementHolds.proof'
    && proof.includes('\\programSource "Simple.sol"') && proof.includes('\\proof') && !proof.includes('/solkey-web/'));

  await typeSource(page, 'Broken.sol');
  await page.waitForFunction(() => !document.getElementById('error').hidden, null, { timeout: 60000 });
  check('a compile error is shown and underlined in the editor',
    await page.isHidden('#functions') && (await page.$$('.cm-lintRange-error')).length > 0);
  await shot(page, 'desktop-3-error');

  await page.setInputFiles('#file', [
    { name: 'Simple.sol', mimeType: 'text/plain', buffer: readFileSync(path.join(contracts, 'Simple.sol')) },
    { name: 'Simple.incrementHolds.proof', mimeType: 'text/plain', buffer: Buffer.from(proof) },
  ]);
  await page.waitForSelector('#inspector:not([hidden])', { timeout: 120000 });
  await page.waitForFunction(() => /closed/.test(document.getElementById('inspectorSummary').textContent), null, { timeout: 120000 });
  check(`a downloaded .proof loads and replays: ${await page.textContent('#inspectorSummary')}`, /^closed/.test(await page.textContent('#inspectorSummary')));
  await page.click('#inspectorClose');

  await page.setInputFiles('#file', path.join(contracts, 'NoProvable.sol'));
  await page.waitForFunction(() => /cannot be proved/.test(document.getElementById('statusText').textContent), null, { timeout: 60000 });
  const reason = await page.textContent('tr.unprovable .reason');
  check(`a function that cannot be proved is listed with its reason (${reason})`,
    await page.isDisabled('tr.unprovable input') && /return/i.test(reason));

  await page.setInputFiles('#file', path.join(contracts, 'Two.sol'));
  await page.waitForFunction(() => /choose one/.test(document.getElementById('statusText').textContent), null, { timeout: 60000 });
  check('a file with two contracts asks for one', await page.isVisible('#contract'));
  await page.selectOption('#contract', 'Second');
  await listed(page, 2);
  check(`choosing a contract lists its functions: ${await functions(page)}`, (await functions(page)).join() === 'two,three');

  await page.selectOption('#example', 'examples/contracts/PiggyBank.sol');
  await page.waitForFunction(() => !document.getElementById('invariants').hidden, null, { timeout: 120000 });
  const spec = await page.textContent('tr[data-fn="addMoney"] .spec');
  check(`specifications are shown: ${spec.slice(0, 60)}…`, /requires/.test(spec) && /ensures/.test(spec)
    && /net\(owner\)/.test(await page.textContent('#invariants')));
  await page.waitForSelector('select[data-choice="transferSemantics"]', { state: 'attached', timeout: 60000 });
  check('taclet options are offered as choices', (await page.$$eval('select[data-choice="transferSemantics"] option', (o) => o.length)) >= 3);

  await page.setInputFiles('#file', path.join(contracts, 'Simple.sol'));
  await listed(page, 3);
  await page.click('#share');
  await page.waitForFunction(() => location.hash.startsWith('#c='));
  const link = page.url();
  const fresh = await browser.newContext({ viewport: { width: 1280, height: 860 } });
  const other = await fresh.newPage();
  await other.goto(link);
  await listed(other, 3);
  check('a share link opens the same contract in a fresh browser', (await editorText(other)).includes('contract Simple'));
  await fresh.close();

  await page.goto(origin);
  await listed(page, 3);
  check('the contract is restored after a reload', (await editorText(page)).includes('contract Simple'));

  await page.selectOption('#example', 'examples/TestSuite.sol');
  await page.waitForFunction(() => document.querySelectorAll('#functions tbody tr[data-fn]').length > 100, null, { timeout: 180000 });
  await setOption(page, 'workers', '3');
  await page.click('#verify');
  await page.waitForFunction(() => document.querySelectorAll('.badge.running').length === 3, null, { timeout: 180000 });
  check('3 parallel provers work at once on a large contract', true);
  await page.waitForFunction(() => document.querySelectorAll('.badge.pass,.badge.fail,.badge.error').length >= 1, null, { timeout: 600000 });
  await page.click('#stop');
  await page.waitForFunction(() => !document.getElementById('summary').hidden, null, { timeout: 60000 });
  check(`stop: ${await page.textContent('#summary')}`, /^Stopped/.test(await page.textContent('#summary')));
  await page.waitForFunction(() => /Ready/.test(document.getElementById('statusText').textContent), null, { timeout: 180000 });
  check('the prover restarts after stop and keeps the results', (await page.$$('.badge.pass,.badge.fail')).length >= 1);
  await shot(page, 'desktop-4-stopped');

  if (!origin.startsWith('file:')) {
    const manifest = await page.evaluate(async () => {
      const url = new URL(document.querySelector('link[rel=manifest]').href);
      const data = await (await fetch(url)).json();
      const icons = await Promise.all(data.icons.map(async (icon) => {
        const response = await fetch(new URL(icon.src, url));
        return `${icon.sizes}:${icon.purpose}:${response.ok}`;
      }));
      return { name: data.short_name, display: data.display, start: data.start_url, icons };
    });
    check(`installable: ${JSON.stringify(manifest)}`, manifest.display === 'standalone' && manifest.start === './'
      && manifest.icons.includes('512x512:maskable:true') && manifest.icons.includes('192x192:any:true')
      && manifest.icons.every((i) => i.endsWith(':true')));
    await page.evaluate(() => navigator.serviceWorker.ready);
    await page.waitForFunction(async () => {
      const names = await caches.keys();
      for (const name of names) {
        if ((await (await caches.open(name)).match('solkey.js.wasm'))) return true;
      }
      return false;
    }, null, { timeout: 180000, polling: 500 });
    await context.setOffline(true);
    await page.reload();
    await listed(page, 413);
    check('offline: the page and the prover load from the cache', /provable/.test(await status(page)));
    await context.setOffline(false);
  }

  const width = await page.evaluate(() => [document.documentElement.scrollWidth, innerWidth]);
  check(`desktop: no horizontal page scroll ${width}`, width[0] <= width[1]);
  check(`no page errors ${problems.join(' | ')}`, problems.length === 0);
  await context.close();
}

async function phone() {
  const context = await browser.newContext({ viewport: { width: 390, height: 844 }, colorScheme: 'dark' });
  const page = await context.newPage();
  await page.goto(origin);
  await listed(page, 4);
  check('phone: the code view shows the editor and hides the results', await page.isVisible('#editor') && !(await page.isVisible('#functions')));
  await page.click('#tabVerify');
  await page.waitForFunction(() => !document.getElementById('summary').hidden, null, { timeout: 600000 });
  check('phone: the tab bar verifies and switches to the results', await page.isVisible('#functions') && !(await page.isVisible('#editor')));
  check(`phone: results tab shows ${await page.textContent('#tabCount')}`, (await page.textContent('#tabCount')) === '3/4');
  check(`phone: starter verdicts ${await results(page)}`, await results(page) === 'setThenIncrement=PASS,localArithmetic=PASS,branches=PASS,wrongClaim=FAIL');
  await page.click('tr.detail summary');
  await shot(page, 'phone-1-verified');
  await page.click('tr[data-fn="wrongClaim"] button.link');
  check('phone: tapping a function opens it in the code view', await page.isVisible('#editor'));
  const width = await page.evaluate(() => [document.documentElement.scrollWidth, innerWidth]);
  check(`phone: no horizontal page scroll ${width}`, width[0] <= width[1]);
  await context.close();
}

try {
  await desktop();
  await phone();
} catch (e) {
  failures++;
  console.log(`FAIL ${e.stack || e}`);
} finally {
  await browser.close();
  server.close();
}
console.log(failures ? `${failures} failed` : 'all passed');
process.exit(failures ? 1 : 0);
