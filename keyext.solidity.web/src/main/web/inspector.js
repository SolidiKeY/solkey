const $ = (id) => document.getElementById(id);

let current = null;

export function inspectorOpen() {
  return current !== null;
}

export async function openInspector({ prover, session, title, problem, pretty, onSave, onError }) {
  closeInspector();
  current = { prover, session, pretty, onSave, onError, selected: null, nodes: [], children: new Map() };
  $('inspectorTitle').textContent = title;
  $('problemPane').textContent = '';
  $('inspector').hidden = false;
  document.body.classList.add('inspecting');
  showTab('tree');
  const context = current;
  if (problem) {
    problem().then((text) => { if (current === context) $('problemPane').textContent = text; });
  } else {
    $('problemPane').textContent = 'A loaded .key or .proof file carries its own problem.';
  }
  await refresh();
}

export function closeInspector() {
  if (!current) return;
  current.prover.call('drop', { session: current.session });
  current = null;
  $('inspector').hidden = true;
  $('ruleMenu').hidden = true;
  document.body.classList.remove('inspecting');
}

async function call(type, args = {}) {
  const result = await current.prover.call(type, { session: current.session, ...args });
  if (result.error) current.onError(result.error);
  return result;
}

async function refresh(select) {
  const tree = await call('tree');
  if (tree.error) return;
  current.nodes = tree.nodes;
  current.children = new Map();
  for (const [serial, parent] of tree.nodes) {
    if (!current.children.has(parent)) current.children.set(parent, []);
    current.children.get(parent).push(serial);
  }
  showSummary(tree);
  renderTree();
  renderGoals();
  const open = tree.nodes.find((n) => n[4] === 'open');
  const target = select ?? (open ? open[0] : tree.nodes[0][0]);
  await selectNode(target, true);
}

function showSummary(result) {
  const goals = result.openGoalCount;
  $('inspectorSummary').textContent = `${result.closed ? 'closed' : `${goals} open goal${goals === 1 ? '' : 's'}`}`
    + ` · ${result.stats.nodes} nodes · ${result.stats.branches} branches`;
  $('inspectorSummary').className = result.closed ? 'pass-text' : 'fail-text';
}

function entry(serial) {
  return current.nodes.find((n) => n[0] === serial);
}

function stateIcon(state) {
  return { closed: '✓', open: '●', inner: '' }[state] || '';
}

function nodeRow(serial) {
  const [, , name, , state] = entry(serial);
  const row = document.createElement('button');
  row.type = 'button';
  row.className = `tree-node ${state}`;
  row.dataset.serial = serial;
  row.textContent = `${serial}: ${name}`;
  const icon = stateIcon(state);
  if (icon) {
    const mark = document.createElement('span');
    mark.className = `mark ${state}`;
    mark.textContent = icon;
    row.prepend(mark, ' ');
  }
  row.addEventListener('click', () => selectNode(serial));
  return row;
}

function branchClosed(serial) {
  const stack = [serial];
  while (stack.length) {
    const s = stack.pop();
    const kids = current.children.get(s) || [];
    if (!kids.length && entry(s)[4] === 'open') return false;
    stack.push(...kids);
  }
  return true;
}

function containsOpen(serial) {
  return !branchClosed(serial);
}

function renderBranch(start, container) {
  let serial = start;
  for (;;) {
    container.append(nodeRow(serial));
    const kids = current.children.get(serial) || [];
    if (kids.length === 1) {
      serial = kids[0];
      continue;
    }
    kids.forEach((child, i) => {
      const details = document.createElement('details');
      details.className = 'branch';
      const summary = document.createElement('summary');
      const label = entry(child)[3] || `Case ${i + 1}`;
      const closed = branchClosed(child);
      const mark = document.createElement('span');
      mark.className = `mark ${closed ? 'closed' : 'open'}`;
      mark.textContent = closed ? '✓' : '●';
      summary.append(mark, ` ${label}`);
      details.append(summary);
      const body = document.createElement('div');
      body.className = 'branch-body';
      details.append(body);
      let rendered = false;
      details.addEventListener('toggle', () => {
        if (details.open && !rendered) {
          rendered = true;
          renderBranch(child, body);
        }
      });
      container.append(details);
      if (!closed) details.open = true;
    });
    return;
  }
}

function renderTree() {
  const pane = $('treePane');
  pane.replaceChildren();
  const root = current.children.get(-1)?.[0];
  if (root != null) renderBranch(root, pane);
  if (!containsOpen(root)) pane.prepend(Object.assign(document.createElement('p'), { className: 'muted', textContent: 'All branches are closed.' }));
}

function renderGoals() {
  const pane = $('goalsPane');
  pane.replaceChildren();
  const open = current.nodes.filter((n) => n[4] === 'open');
  if (!open.length) pane.append(Object.assign(document.createElement('p'), { className: 'muted', textContent: 'No open goals.' }));
  for (const [serial] of open) pane.append(nodeRow(serial));
}

function revealInTree(serial) {
  const chain = [];
  for (let s = serial; s !== -1 && s != null; s = entry(s)?.[1]) chain.unshift(s);
  for (const s of chain) {
    const row = $('treePane').querySelector(`.tree-node[data-serial="${s}"]`);
    if (row) continue;
    const parent = entry(s)[1];
    const siblings = current.children.get(parent) || [];
    if (siblings.length > 1) {
      const branch = [...$('treePane').querySelectorAll('.tree-node')].find((r) => Number(r.dataset.serial) === parent)
        ?.parentElement;
      const index = siblings.indexOf(s);
      const details = [...(branch?.children || [])].filter((c) => c.tagName === 'DETAILS')[index];
      if (details && !details.open) details.open = true;
    }
  }
  $('treePane').querySelectorAll('.tree-node.selected, #goalsPane .tree-node.selected').forEach((r) => r.classList.remove('selected'));
  document.querySelectorAll(`.tree-node[data-serial="${serial}"]`).forEach((r) => {
    r.classList.add('selected');
    r.scrollIntoView({ block: 'nearest' });
  });
}

async function selectNode(serial, quiet) {
  $('ruleMenu').hidden = true;
  const view = await call('node', { serial, pretty: current.pretty() });
  if (view.error) return;
  current.selected = view;
  revealInTree(serial);
  $('nodeTitle').textContent = `Node ${serial} · ${view.openGoal ? 'open goal' : view.children ? 'inner node' : 'closed goal'}`
    + (view.rule ? ` · rule ${view.rule}` : '') + (view.branchLabel ? ` · ${view.branchLabel}` : '');
  $('nodeAuto').hidden = !view.openGoal;
  $('nodePrune').hidden = view.children === 0 || branchClosed(serial);
  $('nodeHint').hidden = !view.openGoal;
  renderSequent(view.sequent);
  $('tacletBox').hidden = !view.taclet;
  $('tacletSummary').textContent = view.rule ? `Applied rule: ${view.rule}${view.ruleName && view.ruleName !== view.rule ? ` (${view.ruleName})` : ''}` : 'Applied rule';
  $('taclet').textContent = view.taclet || '';
  if (!quiet) $('sequent').focus({ preventScroll: true });
}

function renderSequent(text, start = -1, end = -1) {
  const pre = $('sequent');
  pre.replaceChildren();
  if (start >= 0 && end > start) {
    const mark = document.createElement('mark');
    mark.textContent = text.slice(start, end);
    pre.append(text.slice(0, start), mark, text.slice(end));
  } else {
    pre.append(text);
  }
}

function offsetAt(event) {
  const pre = $('sequent');
  let node;
  let offset;
  if (document.caretPositionFromPoint) {
    const pos = document.caretPositionFromPoint(event.clientX, event.clientY);
    node = pos?.offsetNode;
    offset = pos?.offset;
  } else if (document.caretRangeFromPoint) {
    const range = document.caretRangeFromPoint(event.clientX, event.clientY);
    node = range?.startContainer;
    offset = range?.startOffset;
  }
  if (!node || !pre.contains(node)) return -1;
  let total = 0;
  const walker = document.createTreeWalker(pre, NodeFilter.SHOW_TEXT);
  while (walker.nextNode()) {
    if (walker.currentNode === node) return total + offset;
    total += walker.currentNode.textContent.length;
  }
  return -1;
}

async function showRules(event, offset) {
  const view = current.selected;
  if (!view?.openGoal) return;
  const at = await call('rules', { serial: view.serial, offset });
  if (at.error) return;
  renderSequent(view.sequent, at.start, at.end);
  const menu = $('ruleMenu');
  menu.replaceChildren();
  const heading = document.createElement('div');
  heading.className = 'menu-head';
  heading.textContent = at.term ? `Rules for ${at.term.length > 60 ? `${at.term.slice(0, 57)}…` : at.term}` : 'Rules for the whole sequent';
  menu.append(heading);
  const termRules = at.rules.filter((r) => !r.sequentWide);
  const sequentRules = at.rules.filter((r) => r.sequentWide);
  if (!at.rules.length) menu.append(Object.assign(document.createElement('div'), { className: 'muted menu-empty', textContent: 'No applicable rules here.' }));
  for (const rule of termRules) menu.append(ruleItem(view.serial, rule));
  if (sequentRules.length) {
    const more = document.createElement('details');
    const summary = document.createElement('summary');
    summary.textContent = `Sequent rules (${sequentRules.length})`;
    more.append(summary);
    for (const rule of sequentRules) more.append(ruleItem(view.serial, rule));
    if (!termRules.length) more.open = true;
    menu.append(more);
  }
  const box = $('inspector').getBoundingClientRect();
  menu.hidden = false;
  const x = Math.min(event.clientX - box.left, box.width - menu.offsetWidth - 8);
  const y = Math.min(event.clientY - box.top + 8, box.height - menu.offsetHeight - 8);
  menu.style.left = `${Math.max(8, x)}px`;
  menu.style.top = `${Math.max(8, y)}px`;
}

function ruleItem(serial, rule) {
  const item = document.createElement('button');
  item.type = 'button';
  item.className = 'menu-item';
  item.setAttribute('role', 'menuitem');
  item.textContent = rule.displayName + (rule.missing?.length ? ' …' : '');
  item.title = rule.missing?.length ? `Needs: ${rule.missing.join(', ')}` : rule.name;
  item.addEventListener('click', () => (rule.missing?.length ? askInstantiations(serial, rule) : apply(serial, rule, {})));
  return item;
}

function askInstantiations(serial, rule) {
  const menu = $('ruleMenu');
  menu.replaceChildren();
  const form = document.createElement('form');
  form.className = 'inst-form';
  const heading = document.createElement('div');
  heading.className = 'menu-head';
  heading.textContent = `Instantiate ${rule.displayName}`;
  form.append(heading);
  for (const sv of rule.missing) {
    const label = document.createElement('label');
    label.textContent = sv;
    const input = document.createElement('input');
    input.name = sv;
    input.required = true;
    input.spellcheck = false;
    label.append(input);
    form.append(label);
  }
  const buttons = document.createElement('div');
  buttons.className = 'inst-buttons';
  const ok = Object.assign(document.createElement('button'), { type: 'submit', className: 'primary', textContent: 'Apply' });
  const cancel = Object.assign(document.createElement('button'), { type: 'button', textContent: 'Cancel' });
  cancel.addEventListener('click', () => { menu.hidden = true; });
  buttons.append(ok, cancel);
  form.append(buttons);
  form.addEventListener('submit', (e) => {
    e.preventDefault();
    const values = Object.fromEntries(new FormData(form).entries());
    apply(serial, rule, values);
  });
  menu.append(form);
  form.querySelector('input')?.focus();
}

async function apply(serial, rule, instantiations) {
  $('ruleMenu').hidden = true;
  const result = await call('apply', { serial, index: rule.index, instantiations });
  if (!result.error) await refresh();
}

async function run(serial) {
  const result = await call('auto', serial == null ? {} : { serial });
  if (!result.error) await refresh();
}

function showTab(tab) {
  for (const button of document.querySelectorAll('.inspector .tab')) button.classList.toggle('active', button.dataset.tab === tab);
  $('treePane').hidden = tab !== 'tree';
  $('goalsPane').hidden = tab !== 'goals';
  $('problemPane').hidden = tab !== 'problem';
}

$('sequent').addEventListener('click', (event) => {
  if (!current?.selected?.openGoal) return;
  const offset = offsetAt(event);
  showRules(event, offset);
});
$('sequent').addEventListener('keydown', (event) => {
  if ((event.key === 'Enter' || event.key === ' ') && current?.selected?.openGoal) {
    event.preventDefault();
    const rect = $('sequent').getBoundingClientRect();
    showRules({ clientX: rect.left + 16, clientY: rect.top + 16 }, -1);
  }
});
document.addEventListener('keydown', (event) => {
  if (event.key !== 'Escape' || !current) return;
  if (!$('ruleMenu').hidden) $('ruleMenu').hidden = true;
  else closeInspector();
});
document.addEventListener('click', (event) => {
  if (!$('ruleMenu').hidden && !$('ruleMenu').contains(event.target) && event.target !== $('sequent') && !$('sequent').contains(event.target)) {
    $('ruleMenu').hidden = true;
  }
});
for (const button of document.querySelectorAll('.inspector .tab')) button.addEventListener('click', () => showTab(button.dataset.tab));
$('inspectorClose').addEventListener('click', closeInspector);
$('inspectorAuto').addEventListener('click', () => run(null));
$('nodeAuto').addEventListener('click', () => run(current.selected.serial));
$('nodePrune').addEventListener('click', async () => {
  const serial = current.selected.serial;
  const result = await call('prune', { serial });
  if (!result.error) await refresh(serial);
});
$('inspectorSave').addEventListener('click', async () => {
  const result = await call('save');
  if (!result.error) current.onSave(result);
});
