import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands';
import { bracketMatching, indentOnInput, syntaxHighlighting } from '@codemirror/language';
import { lintGutter, setDiagnostics } from '@codemirror/lint';
import { highlightSelectionMatches, searchKeymap } from '@codemirror/search';
import {
  Annotation, Compartment, EditorState, RangeSet, StateEffect, StateField,
} from '@codemirror/state';
import {
  drawSelection, EditorView, gutter, GutterMarker, highlightActiveLine, highlightActiveLineGutter,
  keymap, lineNumbers,
} from '@codemirror/view';
import { classHighlighter } from '@lezer/highlight';
import { solidity } from '@replit/codemirror-lang-solidity';

const SYMBOLS = { pass: '✓', fail: '✗', error: '!', running: '…' };
const TITLES = { pass: 'proof closed', fail: 'proof not closed', error: 'error', running: 'proving' };

class VerdictMarker extends GutterMarker {
  constructor(status) {
    super();
    this.status = status;
  }

  eq(other) {
    return other.status === this.status;
  }

  toDOM() {
    const span = document.createElement('span');
    span.className = `verdict verdict-${this.status}`;
    span.textContent = SYMBOLS[this.status];
    span.title = TITLES[this.status];
    return span;
  }
}

const programmatic = Annotation.define();

const setVerdicts = StateEffect.define();

const verdicts = StateField.define({
  create: () => RangeSet.empty,
  update(value, tr) {
    value = value.map(tr.changes);
    for (const effect of tr.effects) {
      if (effect.is(setVerdicts)) value = effect.value;
    }
    return value;
  },
});

const verdictGutter = gutter({
  class: 'cm-verdict-gutter',
  markers: (view) => view.state.field(verdicts),
  lineMarkerChange: (update) => update.startState.field(verdicts) !== update.state.field(verdicts),
  initialSpacer: () => new VerdictMarker('pass'),
});

function escapeRegExp(text) {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

export function createEditor(parent, { onChange, onRun }) {
  const readOnly = new Compartment();
  const view = new EditorView({
    parent,
    state: EditorState.create({
      doc: '',
      extensions: [
        lineNumbers(),
        verdicts,
        verdictGutter,
        lintGutter(),
        highlightActiveLineGutter(),
        history(),
        drawSelection(),
        indentOnInput(),
        bracketMatching(),
        highlightActiveLine(),
        highlightSelectionMatches(),
        syntaxHighlighting(classHighlighter),
        solidity,
        EditorState.tabSize.of(4),
        readOnly.of(EditorState.readOnly.of(false)),
        keymap.of([
          { key: 'Mod-Enter', run: () => { onRun(); return true; } },
          indentWithTab,
          ...defaultKeymap,
          ...historyKeymap,
          ...searchKeymap,
        ]),
        EditorView.updateListener.of((update) => {
          if (update.docChanged && !update.transactions.some((t) => t.annotation(programmatic))) {
            onChange();
          }
        }),
        EditorView.contentAttributes.of({ 'aria-label': 'Solidity source', spellcheck: 'false' }),
      ],
    }),
  });

  function lineOfFunction(name) {
    const match = new RegExp(`\\bfunction\\s+${escapeRegExp(name)}\\s*\\(`).exec(view.state.doc.toString());
    return match ? view.state.doc.lineAt(match.index) : null;
  }

  return {
    view,
    getValue: () => view.state.doc.toString(),
    setValue(text) {
      view.dispatch({
        changes: { from: 0, to: view.state.doc.length, insert: text },
        annotations: programmatic.of(true),
        effects: setVerdicts.of(RangeSet.empty),
      });
      view.dispatch(setDiagnostics(view.state, []));
      view.scrollDOM.scrollTop = 0;
    },
    setReadOnly(value) {
      view.dispatch({ effects: readOnly.reconfigure(EditorState.readOnly.of(value)) });
    },
    setErrors(errors) {
      const doc = view.state.doc;
      const diagnostics = errors.filter((e) => e.line >= 1 && e.line <= doc.lines).map((e) => {
        const line = doc.line(e.line);
        const from = Math.min(line.from + Math.max(0, e.column - 1), line.to);
        const to = from < line.to ? Math.min(line.to, from + Math.max(1, e.length || 1)) : from;
        return { from, to, severity: e.severity, message: e.message };
      });
      view.dispatch(setDiagnostics(view.state, diagnostics));
    },
    setVerdicts(statusByFunction) {
      const marks = [];
      for (const [name, status] of Object.entries(statusByFunction)) {
        const line = lineOfFunction(name);
        if (line && SYMBOLS[status]) marks.push(new VerdictMarker(status).range(line.from));
      }
      marks.sort((a, b) => a.from - b.from);
      view.dispatch({ effects: setVerdicts.of(RangeSet.of(marks, true)) });
    },
    revealFunction(name) {
      const line = lineOfFunction(name);
      if (!line) return;
      view.dispatch({
        selection: { anchor: line.from, head: line.to },
        effects: EditorView.scrollIntoView(line.from, { y: 'center' }),
      });
      view.focus();
    },
    focus: () => view.focus(),
  };
}
