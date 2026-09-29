import { createRequire } from 'node:module';
import { readFileSync } from 'node:fs';
import path from 'node:path';

const site = path.resolve(process.argv[2]);
const require = createRequire(import.meta.url);

const solc = require(path.join(site, 'soljson.js'));
const compile = solc.cwrap('solidity_compile', 'string', ['string', 'number', 'number']);
const version = solc.cwrap('solidity_version', 'string', []);
globalThis.solkeySolc = { compile: (input) => compile(input, 0, 0), version: () => version() };

const ready = new Promise((resolve) => { globalThis.solkeyReady = resolve; });
require(path.join(site, 'solkey.js'));
await ready;

const call = (type, args) => JSON.parse(globalThis.solkey.call(type, JSON.stringify(args)));
let failed = 0;
const expect = (label, ok, detail) => {
  console.log(`${ok ? 'ok  ' : 'FAIL'} ${label}`);
  if (!ok) {
    failed++;
    console.log(JSON.stringify(detail, null, 2).slice(0, 3000));
  }
};

const source = `// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;
contract Smoke {
    function holds() public pure {
        uint8 x = 1;
        assert(x == 1);
    }
    function fails() public pure {
        uint8 x = 1;
        assert(x == 2);
    }
    function label(string memory text) public {}
}`;

const listed = call('functions', { source });
const names = (listed.functions || []).map((f) => `${f.name}:${f.provable}`).join();
expect(`functions ${names}`, names === 'holds:true,fails:true,label:false', listed);
expect('an unprovable function carries its reason', /string/.test(listed.functions?.[2]?.reason), listed);
const holds = call('verify', { source, function: 'holds' });
expect(`holds closes (${holds.millis} ms, ${holds.stats?.nodes} nodes)`, holds.closed === true && holds.stats?.nodes > 1, holds);
const fails = call('verify', { source, function: 'fails' });
expect('fails stays open', fails.closed === false && !fails.error && fails.openGoals.length > 0, fails);
const typo = call('verify', { source, function: 'holds', choices: 'transferSemantics:withCalback' });
expect('a mistyped taclet option is rejected', /known choices/.test(typo.error), typo);
const model = call('verify', { source, function: 'holds', strategy: { NON_LIN_ARITH_OPTIONS_KEY: 'NON_LIN_ARITH_COMPLETION' } });
expect('a strategy setting is applied', model.closed === true, model);
const problem = call('problem', { source, function: 'holds' });
expect('the generated problem is shown', /\\problem/.test(problem.text), problem);
const choices = call('choices', { source, function: 'holds' });
expect(`taclet option categories: ${choices.categories?.length}`,
  choices.categories?.some((c) => c.category === 'transferSemantics'), choices);

const kept = call('verify', { source, function: 'fails', keep: true });
const tree = call('tree', { session: kept.session });
const open = tree.nodes?.find((n) => n[4] === 'open');
expect(`proof tree with ${tree.nodes?.length} nodes`, tree.nodes?.length > 1 && open, tree);
const root = tree.nodes[0][0];
call('prune', { session: kept.session, serial: root });
const view = call('node', { session: kept.session, serial: root });
expect('the root sequent is printed', /fails\(\)/.test(view.sequent), view);
const rules = call('rules', { session: kept.session, serial: root, offset: view.sequent.indexOf('fails') });
const rule = rules.rules?.find((r) => !r.sequentWide && (r.missing || []).length === 0);
expect(`rules at the program: ${rules.rules?.length}`, rule, rules);
const applied = call('apply', { session: kept.session, serial: root, index: rule.index });
expect(`applying ${rule.displayName}`, applied.stats?.nodes > 1, applied);
const auto = call('auto', { session: kept.session });
expect('running the prover on the rest leaves the false assert open', auto.closed === false, auto);

const saved = call('verify', { source, function: 'holds', withProof: true });
const proof = saved.proof.split(`"${saved.sourcePath}"`).join('"Smoke.sol"');
const loaded = call('load', { main: 'Smoke.holds.proof',
  files: [{ name: 'Smoke.sol', text: source }, { name: 'Smoke.holds.proof', text: proof }] });
expect('a saved proof loads and replays', loaded.closed === true && !(loaded.replayErrors || []).length, loaded);

const suite = readFileSync(path.join(site, 'examples', 'TestSuite.sol'), 'utf8');
const suiteFunctions = call('functions', { source: suite }).functions?.filter((f) => f.provable) || [];
expect(`TestSuite.sol lists ${suiteFunctions.length} provable functions`, suiteFunctions.length > 0, suiteFunctions);
for (const fn of suiteFunctions.slice(0, 5)) {
  const result = call('verify', { source: suite, function: fn.name });
  expect(`TestSuite.${fn.name} closes (${result.millis} ms)`, result.closed === true, result);
}

process.exit(failed ? 1 : 0);
