import { createEditor } from './editor.js';
import { closeInspector, inspectorOpen, openInspector } from './inspector.js';

const $ = (id) => document.getElementById(id);
const STORAGE_KEY = 'solkey.contract';

let busy = false;
let stopped = false;
let listGeneration = 0;
let listTimer;
let saveTimer;
let sourceName = 'Contract.sol';
let solcVersion = '';
let listing = null;
let choicesLoaded = false;
let pendingChoices = {};

const editor = createEditor($('editor'), {
  onChange: () => {
    $('example').value = '';
    clearFunctions();
    scheduleList();
    scheduleSave();
  },
  onRun: () => { if (!$('verify').disabled) verifySelected(); },
});

class Prover {
  constructor(onReady, onFailed) {
    this.nextId = 0;
    this.waiting = new Map();
    this.worker = new Worker('worker.js');
    this.ready = new Promise((resolve, reject) => {
      this.worker.onmessage = ({ data }) => {
        if (data.type === 'ready') {
          resolve();
          onReady(data);
        } else if (data.type === 'failed') {
          reject(new Error(data.error));
          onFailed(data.error);
        } else if (data.type === 'result') {
          this.waiting.get(data.id)(data.result);
          this.waiting.delete(data.id);
        }
      };
      this.worker.onerror = (e) => {
        reject(new Error(e.message || 'Worker error'));
        onFailed(e.message || 'Worker error');
      };
    });
    this.ready.catch(() => {});
  }

  call(type, args) {
    return new Promise((resolve) => {
      const id = this.nextId++;
      this.waiting.set(id, resolve);
      this.worker.postMessage({ id, type, args });
    });
  }

  terminate() {
    this.worker.terminate();
    for (const resolve of this.waiting.values()) resolve({ error: 'stopped' });
    this.waiting.clear();
  }
}

let provers = [];

function startMainProver() {
  provers = [new Prover((data) => {
    solcVersion = data.solc.split('+')[0];
    setStatus(`Ready · solc ${solcVersion}`);
    if (editor.getValue().trim() && $('functions').hidden) listFunctions();
  }, (error) => {
    setStatus('The prover could not start.');
    showError(error);
  })];
}

async function proverPool(size) {
  while (provers.length < size) provers.push(new Prover(() => {}, () => {}));
  const started = await Promise.allSettled(provers.slice(0, size).map((p) => p.ready));
  return provers.slice(0, size).filter((_, i) => started[i].status === 'fulfilled');
}

function defaultWorkers() {
  const cores = navigator.hardwareConcurrency || 2;
  return Math.max(1, Math.min(4, cores - 1));
}

function setStatus(text, spinning = false) {
  $('statusText').textContent = text;
  $('status').classList.toggle('busy', spinning);
}

function toast(text) {
  $('toast').textContent = text;
  $('toast').hidden = false;
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => { $('toast').hidden = true; }, 2500);
}

function showError(text) {
  $('error').textContent = text || '';
  $('error').hidden = !text;
}

function publicName(text) {
  return text.split('/solkey-web/Contract.sol').join(sourceName);
}

function selectedContract() {
  return $('contractLabel').hidden ? undefined : $('contract').value || undefined;
}

function strategy() {
  const settings = {};
  for (const select of document.querySelectorAll('[data-strategy]')) {
    if (select.value) settings[select.dataset.strategy] = select.value;
  }
  return settings;
}

function tacletChoices() {
  return [...document.querySelectorAll('[data-choice]')].map((s) => s.value).filter(Boolean).join(',');
}

function request() {
  const args = { source: editor.getValue() };
  if (selectedContract()) args.contract = selectedContract();
  return args;
}

function proverOptions() {
  const seconds = Math.max(0, Number($('timeLimit').value) || 0);
  return {
    maxSteps: Number($('maxSteps').value) || 10000,
    maxGoals: Math.max(0, Number($('maxGoals').value) || 0),
    timeout: seconds > 0 ? seconds * 1000 : -1,
    choices: tacletChoices(),
    strategy: strategy(),
  };
}

function solcErrors(text) {
  const errors = [];
  const pattern = /(\w*Error|Warning): ([^\n]*)\n\s*--> [^\n]*?:(\d+):(\d+):/g;
  for (const m of text.matchAll(pattern)) {
    errors.push({
      severity: m[1] === 'Warning' ? 'warning' : 'error',
      message: `${m[1]}: ${m[2]}`,
      line: Number(m[3]),
      column: Number(m[4]),
    });
  }
  return errors;
}

function clearFunctions() {
  $('functions').querySelector('tbody').replaceChildren();
  $('functions').hidden = true;
  $('summary').hidden = true;
  $('invariants').hidden = true;
  $('verify').disabled = true;
  $('evm').disabled = true;
  setEvmColumn(false);
  editor.setVerdicts({});
}

function scheduleList() {
  clearTimeout(listTimer);
  listTimer = setTimeout(listFunctions, 600);
}

function setContracts(names, chosen) {
  const select = $('contract');
  const previous = select.value;
  select.replaceChildren();
  if (names.length > 1 && !chosen) select.append(new Option('Choose…', ''));
  for (const name of names) select.append(new Option(name, name));
  select.value = chosen || (names.includes(previous) ? previous : '');
  $('contractLabel').hidden = names.length <= 1;
}

async function listFunctions() {
  if (busy || provers.length === 0) return;
  const generation = ++listGeneration;
  try {
    await provers[0].ready;
  } catch {
    return;
  }
  if (generation !== listGeneration || busy) return;
  showError('');
  editor.setErrors([]);
  if (!editor.getValue().trim()) {
    clearFunctions();
    $('empty').hidden = false;
    setStatus(`Ready · solc ${solcVersion}`);
    return;
  }
  setStatus('Compiling with solc…', true);
  const result = await provers[0].call('functions', request());
  if (generation !== listGeneration) return;
  clearFunctions();
  listing = result;
  $('empty').hidden = true;
  if (result.error && /declares no contract/.test(result.error) && selectedContract()) {
    setContracts([], '');
    listFunctions();
    return;
  }
  if (result.contracts) setContracts(result.contracts, result.contract);
  if (result.error) {
    const message = publicName(result.error);
    setStatus('The contract does not compile.');
    showError(message);
    editor.setErrors(solcErrors(message));
    return;
  }
  if (result.needContract) {
    setStatus(`${result.contracts.length} contracts: choose one above the editor.`);
    return;
  }
  if (result.invariants?.length || result.invariantError) {
    $('invariants').textContent = result.invariantError
      ? `Contract invariant cannot be read: ${result.invariantError}`
      : `Invariant${result.invariants.length > 1 ? 's' : ''} of ${result.contract}: ${result.invariants.join(' ∧ ')}`;
    $('invariants').hidden = false;
  }
  const provable = result.functions.filter((f) => f.provable);
  if (result.functions.length === 0) {
    setStatus('The contract has no public or external function.');
    return;
  }
  const body = $('functions').querySelector('tbody');
  for (const fn of result.functions) body.append(functionRow(fn));
  $('all').checked = provable.length > 0;
  $('functions').hidden = false;
  syncAll();
  const skipped = result.functions.length - provable.length;
  setStatus(`${provable.length} provable function${provable.length === 1 ? '' : 's'}`
    + (skipped ? `, ${skipped} that cannot be proved.` : '.'));
  if (provable.length && !choicesLoaded) loadChoices(provable[0].name);
}

function functionRow(fn) {
  const row = document.createElement('tr');
  row.dataset.fn = fn.name;
  if (!fn.provable) row.className = 'unprovable';
  const check = row.insertCell();
  check.className = 'check';
  const box = document.createElement('input');
  box.type = 'checkbox';
  box.checked = fn.provable;
  box.disabled = !fn.provable;
  box.setAttribute('aria-label', `Verify ${fn.name}`);
  box.addEventListener('change', syncAll);
  check.append(box);
  const name = row.insertCell();
  name.className = 'fn';
  const jump = document.createElement('button');
  jump.type = 'button';
  jump.className = 'link';
  jump.textContent = fn.name;
  jump.title = `Show in the editor (line ${fn.line})`;
  jump.addEventListener('click', () => editor.revealFunction(fn.name));
  name.append(jump);
  const spec = specText(fn);
  if (spec) {
    const small = document.createElement('div');
    small.className = 'spec';
    small.textContent = spec;
    small.title = spec;
    name.append(small);
  }
  if (!fn.provable) {
    const reason = document.createElement('div');
    reason.className = 'reason';
    reason.textContent = fn.reason || 'cannot be proved';
    name.append(reason);
  }
  row.insertCell().append(badge(fn.provable ? 'pending' : 'na', fn.provable ? '—' : 'N/A'));
  const evm = row.insertCell();
  evm.className = 'evm-col';
  evm.hidden = $('functions').querySelector('th.evm-col').hidden;
  row.insertCell().className = 'time';
  row.insertCell().className = 'actions';
  return row;
}

function specText(fn) {
  const parts = [];
  if (fn.spec?.requires?.length) parts.push(`requires ${fn.spec.requires.join(' ∧ ')}`);
  if (fn.spec?.ensures?.length) parts.push(`ensures ${fn.spec.ensures.join(' ∧ ')}`);
  if (fn.spec?.box) parts.push('box');
  return parts.join(' · ');
}

async function loadChoices(fn) {
  choicesLoaded = true;
  const result = await provers[0].call('choices', { ...request(), function: fn });
  if (result.error || !result.categories) {
    choicesLoaded = false;
    return;
  }
  const box = $('choices');
  box.replaceChildren();
  const first = (c) => (c.category === 'transferSemantics' ? 0 : 1);
  const categories = [...result.categories].sort((a, b) => first(a) - first(b) || a.category.localeCompare(b.category));
  for (const category of categories) {
    const label = document.createElement('label');
    label.textContent = category.category;
    const select = document.createElement('select');
    select.dataset.choice = category.category;
    const fallback = category.selected ? category.selected.split(':')[1] : 'default';
    select.append(new Option(`default (${fallback})`, ''));
    for (const choice of category.choices) select.append(new Option(choice.split(':')[1], choice));
    if (pendingChoices[category.category]) select.value = pendingChoices[category.category];
    select.addEventListener('change', scheduleSave);
    label.append(select);
    box.append(label);
  }
}

function badge(kind, text, title) {
  const span = document.createElement('span');
  span.className = `badge ${kind}`;
  span.textContent = text;
  if (title) span.title = title;
  return span;
}

function syncAll() {
  const boxes = [...$('functions').querySelectorAll('tbody input[type=checkbox]')]
    .filter((b) => !b.closest('tr').classList.contains('unprovable'));
  $('all').checked = boxes.length > 0 && boxes.every((b) => b.checked);
  const any = boxes.some((b) => b.checked);
  $('verify').disabled = busy || !any;
  $('evm').disabled = busy || !any;
}

function rows() {
  return [...$('functions').querySelectorAll('tbody tr[data-fn]')];
}

function selectedRows() {
  return rows().filter((row) => !row.classList.contains('unprovable') && row.querySelector('input').checked);
}

async function verifySelected() {
  const selected = selectedRows();
  if (selected.length === 0) return;
  $('functions').querySelectorAll('tr.detail').forEach((r) => r.remove());
  const verdicts = {};
  for (const row of rows()) {
    if (row.classList.contains('unprovable')) continue;
    row.cells[2].replaceChildren(badge('pending', selected.includes(row) ? 'queued' : '—'));
    row.cells[4].replaceChildren();
    row.cells[5].replaceChildren();
  }
  editor.setVerdicts({});
  $('summary').hidden = true;
  showError('');
  setBusy(true);
  stopped = false;
  const size = Math.min(Math.ceil(selected.length / 4), Math.max(1, Number($('workers').value) || 1));
  if (size > 1) setStatus(`Starting ${size} provers…`, true);
  const pool = await proverPool(size);
  const queue = [...selected];
  const args = { ...request(), ...proverOptions() };
  let closed = 0;
  let done = 0;
  let firstError = null;
  const work = async (prover) => {
    while (queue.length && !stopped) {
      const row = queue.shift();
      const fn = row.dataset.fn;
      row.cells[2].replaceChildren(badge('running', 'proving…'));
      verdicts[fn] = 'running';
      editor.setVerdicts(verdicts);
      setStatus(`Proving ${done + 1}/${selected.length}…`, true);
      const result = await prover.call('verify', { ...args, function: fn });
      if (stopped) return;
      done++;
      if (result.closed) closed++;
      const timedOut = !result.closed && !result.error && args.timeout > 0 && result.millis >= args.timeout;
      const kind = result.closed ? 'pass' : result.error ? 'error' : timedOut ? 'timeout' : 'fail';
      verdicts[fn] = kind === 'timeout' ? 'error' : kind;
      editor.setVerdicts(verdicts);
      row.cells[2].replaceChildren(badge(kind, kind.toUpperCase(),
        timedOut ? 'Stopped by the time limit (Options) before the proof closed' : undefined));
      timeCell(row.cells[4], result);
      if (!result.error) row.cells[5].replaceChildren(inspectButton(fn, args), proofButton(fn, args));
      if (result.error && !firstError) firstError = publicName(result.error);
      const detail = result.error ? publicName(result.error) : (result.openGoals || [])
        .map((goal, n) => `open goal ${n + 1}\n${explain(goal)}`).join('\n\n');
      if (!result.closed && detail) row.after(detailRow(result.error ? 'Error' : 'Open goals', detail));
    }
  };
  await Promise.all(pool.map(work));
  for (const row of selected) {
    if (row.querySelector('.badge.pending, .badge.running')) row.cells[2].replaceChildren(badge('pending', '—'));
  }
  for (const [fn, status] of Object.entries(verdicts)) {
    if (status === 'running') delete verdicts[fn];
  }
  editor.setVerdicts(verdicts);
  $('summary').textContent = stopped
    ? `Stopped after ${done} of ${selected.length}: ${closed} closed.`
    : `${closed}/${selected.length} closed`;
  $('summary').className = `summary ${closed === selected.length && !stopped ? 'pass' : 'fail'}`;
  $('summary').hidden = false;
  if (!stopped) setStatus(firstError && closed === 0 ? firstError.split('\n')[0] : 'Done.');
  setBusy(false);
}

function timeCell(cell, result) {
  cell.replaceChildren();
  if (result.millis == null) return;
  cell.append(`${(result.millis / 1000).toFixed(2)} s`);
  if (result.stats) {
    const small = document.createElement('div');
    small.className = 'stats';
    small.textContent = `${result.stats.nodes} nodes · ${result.stats.branches} br.`;
    cell.append(small);
    cell.title = `${result.stats.nodes} proof nodes, ${result.stats.branches} branches, ${result.stats.autoModeMillis} ms in the prover`;
  }
}

function inspectButton(fn, args) {
  const button = iconButton('⌕', `Inspect the proof of ${fn}`);
  button.addEventListener('click', async () => {
    if (busy) return;
    button.disabled = true;
    setStatus(`Opening the proof of ${fn}…`, true);
    const result = await provers[0].call('verify', { ...args, function: fn, keep: true });
    button.disabled = false;
    if (!result.session) {
      setStatus('The proof could not be opened.');
      showError(publicName(result.error || 'No proof returned.'));
      return;
    }
    setStatus('Done.');
    const problem = () => provers[0].call('problem', { ...args, function: fn }).then((r) => publicName(r.text || r.error || ''));
    await openInspector({
      prover: provers[0],
      session: result.session,
      title: `${selectedContract() || listing?.contract || ''}.${fn}`,
      problem,
      pretty: () => $('pretty').checked,
      onSave: (saved) => saveProof(saved, fn),
      onError: (error) => toast(publicName(error).split('\n')[0]),
    });
  });
  return button;
}

function proofButton(fn, args) {
  const button = iconButton('⤓', `Download the proof of ${fn}`);
  button.addEventListener('click', async () => {
    if (busy) return;
    button.disabled = true;
    setStatus(`Saving the proof of ${fn}…`, true);
    const result = await provers[0].call('verify', { ...args, function: fn, withProof: true });
    button.disabled = false;
    if (!result.proof) {
      setStatus('The proof could not be saved.');
      showError(publicName(result.error || 'No proof returned.'));
      return;
    }
    saveProof(result, fn);
  });
  return button;
}

function iconButton(symbol, label) {
  const button = document.createElement('button');
  button.type = 'button';
  button.className = 'icon';
  button.textContent = symbol;
  button.title = label;
  button.setAttribute('aria-label', label);
  return button;
}

function saveProof(result, fn) {
  const proof = result.sourcePath ? result.proof.split(`"${result.sourcePath}"`).join(`"${sourceName}"`) : result.proof;
  download(`${sourceName.replace(/\.(sol|key|proof)$/, '')}.${fn}.proof`, proof);
  setStatus(`Saved. Keep ${sourceName} next to the .proof file to load it in KeY.`);
}

function explain(goal) {
  if (goal.trim() !== '==>') return goal;
  return '==>\n\nAn empty sequent: nothing is left to assume and nothing to show, so this path '
    + 'reaches an assert that does not hold.';
}

function detailRow(title, text) {
  const row = document.createElement('tr');
  row.className = 'detail';
  const cell = row.insertCell();
  cell.colSpan = 6;
  const details = document.createElement('details');
  const summary = document.createElement('summary');
  summary.textContent = title;
  const pre = document.createElement('pre');
  pre.textContent = text;
  details.append(summary, pre);
  cell.append(details);
  return row;
}

function setEvmColumn(visible) {
  $('functions').querySelectorAll('.evm-col').forEach((c) => { c.hidden = !visible; });
}

async function runOnEvm() {
  const selected = selectedRows();
  if (!selected.length) return;
  setBusy(true);
  setEvmColumn(true);
  for (const row of rows()) row.cells[3].replaceChildren(selected.includes(row) ? badge('running', 'running…') : '');
  setStatus('Compiling with solc and running on the EVM…', true);
  try {
    const { runChecks, describe } = await import('./runtime.js');
    const result = await runChecks({
      source: editor.getValue(),
      contract: selectedContract() || listing?.contract,
      functions: selected.map((r) => r.dataset.fn),
      compile: (input) => provers[0].call('solc', { input }),
    });
    if (result.errors.length) {
      showError(publicName(result.errors.map((e) => e.formattedMessage || e.message).join('\n')));
      setStatus('The contract does not compile.');
      return;
    }
    const whole = result.verdicts.length === 1 && result.verdicts[0].function === (selectedContract() || listing?.contract)
      ? result.verdicts[0] : null;
    const byFunction = new Map(result.verdicts.map((v) => [v.function, v]));
    let failures = 0;
    let skipped = 0;
    for (const row of selected) {
      const verdict = whole || byFunction.get(row.dataset.fn);
      if (!verdict) {
        row.cells[3].replaceChildren();
        continue;
      }
      const kind = verdict.outcome === 'OK' ? 'pass' : verdict.outcome === 'SKIPPED' ? 'na' : 'fail';
      if (kind === 'fail') failures++;
      if (kind === 'na') skipped++;
      const label = { OK: 'OK', SKIPPED: 'SKIP', ASSERT_FAILED: 'ASSERT', PANIC: 'PANIC', REVERTED: 'REVERT', HALTED: 'HALT' }[verdict.outcome];
      const args = verdict.args?.length ? ` with (${verdict.args.join(', ')})` : '';
      row.cells[3].replaceChildren(badge(kind, label, `${describe(verdict)}${args}`));
    }
    const ran = selected.length - skipped;
    setStatus(whole ? `EVM: ${describe(whole)}.`
      : `EVM · solc ${solcVersion}: ${ran - failures}/${ran} ran without a failing assert`
        + (skipped ? ` (${skipped} not run)` : '') + (result.warnings.length ? ` · ${result.warnings.length} warning(s)` : '') + '.');
  } catch (e) {
    showError(publicName(String(e.message || e)));
    setStatus('The EVM run failed.');
  } finally {
    setBusy(false);
  }
}

function setBusy(value) {
  busy = value;
  $('stop').disabled = !value;
  editor.setReadOnly(value);
  for (const id of ['example', 'file', 'contract', 'all']) $(id).disabled = value;
  $('functions').querySelectorAll('tbody tr:not(.unprovable) input, tbody button.icon').forEach((b) => { b.disabled = value; });
  syncAll();
}

function stop() {
  stopped = true;
  closeInspector();
  for (const prover of provers) prover.terminate();
  setStatus('Stopped. Restarting the prover…', true);
  startMainProver();
}

function download(name, text) {
  const url = URL.createObjectURL(new Blob([text], { type: 'text/plain' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  document.body.append(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

async function compress(text) {
  const stream = new Blob([text]).stream().pipeThrough(new CompressionStream('deflate-raw'));
  const bytes = new Uint8Array(await new Response(stream).arrayBuffer());
  let binary = '';
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

async function decompress(encoded) {
  const binary = atob(encoded.replace(/-/g, '+').replace(/_/g, '/'));
  const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0));
  const stream = new Blob([bytes]).stream().pipeThrough(new DecompressionStream('deflate-raw'));
  return new Response(stream).text();
}

function snapshot() {
  const choices = {};
  for (const select of document.querySelectorAll('[data-choice]')) if (select.value) choices[select.dataset.choice] = select.value;
  return {
    source: editor.getValue(),
    contract: $('contract').value,
    name: sourceName,
    choices: { ...pendingChoices, ...choices },
    strategy: strategy(),
  };
}

async function share() {
  const url = new URL(location.href);
  url.hash = `c=${await compress(JSON.stringify(snapshot()))}`;
  history.replaceState(null, '', url);
  try {
    await navigator.clipboard.writeText(url.href);
    toast('Link copied to the clipboard.');
  } catch {
    toast('Link is in the address bar.');
  }
}

function scheduleSave() {
  clearTimeout(saveTimer);
  saveTimer = setTimeout(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(snapshot()));
    } catch {}
  }, 400);
}

function restore(state) {
  pendingChoices = typeof state.choices === 'object' && state.choices ? state.choices : {};
  for (const select of document.querySelectorAll('[data-strategy]')) select.value = state.strategy?.[select.dataset.strategy] || '';
  for (const select of document.querySelectorAll('[data-choice]')) select.value = pendingChoices[select.dataset.choice] || '';
  setSource(state.source || '', state.name);
  if (state.contract) {
    setContracts([state.contract], state.contract);
    $('contractLabel').hidden = false;
  }
}

async function initialContract() {
  const hash = new URLSearchParams(location.hash.slice(1)).get('c');
  if (hash) {
    try {
      restore(JSON.parse(await decompress(hash)));
      return;
    } catch {
      toast('The shared link could not be read.');
    }
  }
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');
    if (saved && saved.source && saved.source.trim()) {
      restore(saved);
      return;
    }
  } catch {}
  $('example').value = 'starter.sol';
  setSource(await (await fetch('starter.sol')).text(), 'Starter.sol');
}

async function loadExamples() {
  try {
    const examples = ['starter.sol', ...await (await fetch('examples.json')).json()];
    for (const path of examples) {
      const option = document.createElement('option');
      option.value = path;
      option.textContent = path === 'starter.sol' ? 'Starter.sol (a small tour)' : path.replace(/^examples\//, '');
      $('example').append(option);
    }
  } catch (e) {
    console.warn('no examples', e);
  }
}

function setSource(text, name) {
  sourceName = name || 'Contract.sol';
  editor.setValue(text);
  setContracts([], '');
  showError('');
  editor.setErrors([]);
  clearFunctions();
  scheduleSave();
  listFunctions();
}

async function openFiles(files) {
  const read = await Promise.all([...files].map(async (f) => ({ name: f.name, text: await f.text() })));
  const sol = read.find((f) => f.name.endsWith('.sol'));
  const main = read.find((f) => f.name.endsWith('.proof')) || read.find((f) => f.name.endsWith('.key'));
  if (sol) {
    $('example').value = '';
    setSource(sol.text, sol.name);
  }
  if (!main) return;
  await provers[0].ready;
  setStatus(`Loading ${main.name}…`, true);
  const result = await provers[0].call('load', {
    files: read, main: main.name, prove: main.name.endsWith('.key'), ...proverOptions(),
  });
  if (!result.session) {
    setStatus(`${main.name} could not be loaded.`);
    showError(result.error || 'No proof returned.');
    return;
  }
  const replay = result.replayErrors?.length ? ` · ${result.replayErrors.length} replay error(s)` : '';
  setStatus(`${main.name}: ${result.closed ? 'closed' : `${result.openGoalCount} open goal(s)`}${replay}.`);
  if (result.replayErrors?.length) showError(`Replay errors in ${main.name}:\n${result.replayErrors.join('\n')}`);
  await openInspector({
    prover: provers[0],
    session: result.session,
    title: main.name,
    problem: null,
    pretty: () => $('pretty').checked,
    onSave: (saved) => download(main.name.endsWith('.proof') ? main.name : main.name.replace(/\.key$/, '.proof'), saved.proof),
    onError: (error) => toast(error.split('\n')[0]),
  });
}

$('example').addEventListener('change', async (e) => {
  const value = e.target.value;
  if (!value) return;
  setSource(await (await fetch(value)).text(), value === 'starter.sol' ? 'Starter.sol' : value.split('/').pop());
  $('example').value = value;
});
$('file').addEventListener('change', async (e) => {
  if (e.target.files.length) await openFiles(e.target.files);
  e.target.value = '';
});
$('save').addEventListener('click', () => download(sourceName, editor.getValue()));
$('share').addEventListener('click', share);
$('contract').addEventListener('change', () => { clearFunctions(); listFunctions(); scheduleSave(); });
$('all').addEventListener('change', (e) => {
  $('functions').querySelectorAll('tbody tr:not(.unprovable) input[type=checkbox]').forEach((b) => { b.checked = e.target.checked; });
  syncAll();
});
$('verify').addEventListener('click', verifySelected);
$('evm').addEventListener('click', runOnEvm);
$('stop').addEventListener('click', stop);
for (const select of document.querySelectorAll('[data-strategy]')) select.addEventListener('change', scheduleSave);
$('fontSize').addEventListener('input', () => {
  const size = Math.min(24, Math.max(9, Number($('fontSize').value) || 13));
  document.documentElement.style.setProperty('--editor-font', `${size}px`);
  try { localStorage.setItem('solkey.fontSize', String(size)); } catch {}
});
try {
  const size = localStorage.getItem('solkey.fontSize');
  if (size) {
    $('fontSize').value = size;
    document.documentElement.style.setProperty('--editor-font', `${size}px`);
  }
} catch {}
$('workers').value = defaultWorkers();
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape' && !inspectorOpen() && $('options').open) $('options').open = false;
});

if ('serviceWorker' in navigator && (location.protocol === 'https:' || location.hostname === 'localhost')) {
  navigator.serviceWorker.register('sw.js').catch((e) => console.warn('no offline cache', e));
}

startMainProver();
await loadExamples();
await initialContract();
