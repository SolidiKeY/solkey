import { Common, Hardfork, Mainnet } from '@ethereumjs/common';
import { createEVM } from '@ethereumjs/evm';
import { bytesToHex, createAccount, createAddressFromString, hexToBytes } from '@ethereumjs/util';

const SENDER = createAddressFromString('0x00000000000000000000000000000000cafebabe');
const CONTRACT = createAddressFromString('0x00000000000000000000000000000000deadbeef');
const GAS = 30000000n;
const PANIC_SELECTOR = '0x4e487b71';
const PANIC_NAMES = {
  0x01: 'assert failed',
  0x11: 'arithmetic overflow',
  0x12: 'division by zero',
  0x21: 'invalid enum value',
  0x22: 'corrupt storage byte array',
  0x31: 'pop on empty array',
  0x32: 'array index out of bounds',
  0x41: 'allocation too large',
  0x51: 'uninitialized function pointer',
};
const UNIT = 'Contract.sol';

export async function runChecks({ source, contract, functions, compile }) {
  const output = await compile({
    language: 'Solidity',
    sources: { [UNIT]: { content: source } },
    settings: {
      outputSelection: {
        '*': { '': ['ast'], '*': ['abi', 'evm.deployedBytecode.object', 'evm.methodIdentifiers'] },
      },
    },
  });
  const diagnostics = output.errors || [];
  const errors = diagnostics.filter((e) => e.severity === 'error');
  const warnings = diagnostics.filter((e) => e.severity !== 'error');
  if (errors.length) return { errors, warnings, verdicts: [] };

  const compiled = output.contracts?.[UNIT] || {};
  const names = Object.keys(compiled);
  const name = contract || (names.length === 1 ? names[0] : null);
  if (!name || !compiled[name]) {
    throw new Error(contract ? `the source declares no contract ${contract}; candidates: ${names.join(', ')}`
      : `several contracts: ${names.join(', ')}; choose one`);
  }
  const ast = output.sources[UNIT].ast;
  const contractNode = ast.nodes.find((n) => n.nodeType === 'ContractDefinition' && n.name === name);
  const deployment = deploymentLogic(contractNode);
  if (deployment) return { errors, warnings, verdicts: [{ function: name, outcome: 'SKIPPED', detail: deployment }] };

  const code = hexToBytes(`0x${compiled[name].evm.deployedBytecode.object}`);
  const identifiers = compiled[name].evm.methodIdentifiers;
  const verdicts = [];
  for (const fn of functions) {
    const definition = contractNode.nodes.find((n) => n.nodeType === 'FunctionDefinition' && n.name === fn);
    const signature = Object.keys(identifiers).find((s) => s.startsWith(`${fn}(`));
    if (!definition || !signature) {
      verdicts.push({ function: fn, outcome: 'SKIPPED', detail: 'not found in the compiled contract' });
      continue;
    }
    const args = pinnedArguments(definition);
    if (!args) {
      verdicts.push({ function: fn, outcome: 'SKIPPED', detail: 'takes parameters no leading require pins to a value' });
      continue;
    }
    verdicts.push({ function: fn, args: args.map(String), ...await call(code, identifiers[signature], args) });
  }
  return { errors, warnings, verdicts };
}

export function describe(verdict) {
  return verdict.detail ? `${verdict.outcome}: ${verdict.detail}` : verdict.outcome;
}

function deploymentLogic(contractNode) {
  for (const member of contractNode.nodes) {
    if (member.nodeType === 'FunctionDefinition' && member.kind === 'constructor') {
      return 'has a constructor, so a run from all-zero storage would not be the contract you deploy';
    }
    if (member.nodeType === 'VariableDeclaration' && member.stateVariable && member.value) {
      return `initializes ${member.name} at declaration, so a run from all-zero storage would not be the contract you deploy`;
    }
  }
  return null;
}

function pinnedArguments(definition) {
  if (definition.parameters.parameters.length === 0) return [];
  const pins = new Map();
  const locals = new Map();
  for (const statement of definition.body?.statements || []) {
    if (!collectPins(statement, locals, pins)) break;
  }
  const values = [];
  for (const parameter of definition.parameters.parameters) {
    const bounds = pins.get(parameter.name);
    if (!bounds || (bounds.low != null && bounds.high != null && bounds.low > bounds.high)) return null;
    values.push(bounds.low ?? bounds.high);
  }
  return values;
}

function collectPins(statement, locals, pins) {
  if (statement.nodeType === 'VariableDeclarationStatement') {
    const value = literalValue(statement.initialValue);
    if (statement.declarations.length === 1 && value != null) locals.set(statement.declarations[0].name, value);
    return true;
  }
  const call = statement.nodeType === 'ExpressionStatement' ? statement.expression : null;
  if (call?.nodeType === 'FunctionCall' && call.expression?.name === 'require') {
    collectConjuncts(call.arguments[0], locals, pins);
    return true;
  }
  return false;
}

function collectConjuncts(condition, locals, pins) {
  if (condition?.nodeType !== 'BinaryOperation') return;
  if (condition.operator === '&&') {
    collectConjuncts(condition.leftExpression, locals, pins);
    collectConjuncts(condition.rightExpression, locals, pins);
  } else if (!recordPin(condition.leftExpression, condition.operator, condition.rightExpression, locals, pins)) {
    recordPin(condition.rightExpression, mirrored(condition.operator), condition.leftExpression, locals, pins);
  }
}

function mirrored(operator) {
  return { '<': '>', '<=': '>=', '>': '<', '>=': '<=' }[operator] || operator;
}

function recordPin(identifier, operator, valueNode, locals, pins) {
  if (identifier?.nodeType !== 'Identifier') return false;
  let value = literalValue(valueNode);
  if (value == null && valueNode?.nodeType === 'Identifier') value = locals.get(valueNode.name);
  if (value == null) return false;
  if (!['==', '>=', '>', '<=', '<'].includes(operator)) return false;
  const bounds = pins.get(identifier.name) || {};
  pins.set(identifier.name, bounds);
  const max = (a, b) => (a == null || b > a ? b : a);
  const min = (a, b) => (a == null || b < a ? b : a);
  if (operator === '==') {
    bounds.low = value;
    bounds.high = value;
  } else if (operator === '>=') bounds.low = max(bounds.low, value);
  else if (operator === '>') bounds.low = max(bounds.low, value + 1n);
  else if (operator === '<=') bounds.high = min(bounds.high, value);
  else bounds.high = min(bounds.high, value - 1n);
  return true;
}

function literalValue(expression) {
  if (!expression) return null;
  if (expression.nodeType === 'UnaryOperation' && expression.operator === '-') {
    const operand = literalValue(expression.subExpression);
    return operand == null ? null : -operand;
  }
  if (expression.nodeType !== 'Literal' || expression.kind !== 'number' || expression.subdenomination) return null;
  try {
    return BigInt(expression.value);
  } catch {
    return null;
  }
}

function word(value) {
  const mask = (1n << 256n) - 1n;
  return (value & mask).toString(16).padStart(64, '0');
}

async function call(code, selector, args) {
  const evm = await createEVM({ common: new Common({ chain: Mainnet, hardfork: Hardfork.Cancun }) });
  await evm.stateManager.putAccount(SENDER, createAccount({ nonce: 0n, balance: 10n ** 18n }));
  await evm.stateManager.putAccount(CONTRACT, createAccount({ nonce: 1n, balance: 0n }));
  await evm.stateManager.putCode(CONTRACT, code);
  const result = await evm.runCall({
    caller: SENDER,
    origin: SENDER,
    to: CONTRACT,
    value: 0n,
    gasPrice: 0n,
    gasLimit: GAS,
    data: hexToBytes(`0x${selector}${args.map(word).join('')}`),
  });
  const error = result.execResult.exceptionError?.error;
  if (!error) return { outcome: 'OK', detail: '' };
  const data = bytesToHex(result.execResult.returnValue);
  if (error === 'revert') {
    if (data.length === 2 + 72 && data.startsWith(PANIC_SELECTOR)) {
      const code = Number(BigInt(`0x${data.slice(10)}`));
      const detail = `Panic(0x${code.toString(16).padStart(2, '0')} ${PANIC_NAMES[code] || 'unknown'})`;
      return { outcome: code === 1 ? 'ASSERT_FAILED' : 'PANIC', detail };
    }
    return { outcome: 'REVERTED', detail: 'a require stopped the call before the end' };
  }
  return { outcome: 'HALTED', detail: error };
}
