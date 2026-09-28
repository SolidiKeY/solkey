const clamp = (value, lo, hi) => Math.min(hi, Math.max(lo, value));
const STEP = 2;

export function makeSplit(container, { key, initial, initialVertical = initial, min = 160, verticalWhen = null }) {
  const splitter = container.querySelector(':scope > .splitter');
  const [toStart, toEnd] = splitter.querySelectorAll('button');
  const vertical = () => verticalWhen?.matches ?? false;
  const storageKey = () => `solkey.split.${key}.${vertical() ? 'v' : 'h'}`;
  const defaults = () => ({ split: vertical() ? initialVertical : initial, collapsed: '' });
  let state = defaults();

  function load() {
    try {
      state = { ...defaults(), ...JSON.parse(localStorage.getItem(storageKey()) || '{}') };
    } catch {
      state = defaults();
    }
  }

  function save() {
    try { localStorage.setItem(storageKey(), JSON.stringify(state)); } catch {}
  }

  function position() {
    if (state.collapsed === 'start') return 0;
    if (state.collapsed === 'end') return 100;
    return state.split;
  }

  function render() {
    const v = vertical();
    const split = state.collapsed === 'start' ? '0px'
      : state.collapsed === 'end' ? 'calc(100% - var(--gutter))'
      : `${state.split}%`;
    container.style.setProperty('--split', split);
    container.classList.toggle('collapse-start', state.collapsed === 'start');
    container.classList.toggle('collapse-end', state.collapsed === 'end');
    splitter.setAttribute('aria-orientation', v ? 'horizontal' : 'vertical');
    splitter.setAttribute('aria-valuemin', '0');
    splitter.setAttribute('aria-valuemax', '100');
    splitter.setAttribute('aria-valuenow', String(Math.round(position())));
    toStart.textContent = v ? '▴' : '◂';
    toEnd.textContent = v ? '▾' : '▸';
    toStart.title = state.collapsed === 'end' ? 'Restore' : `Collapse the ${v ? 'upper' : 'left'} pane`;
    toEnd.title = state.collapsed === 'start' ? 'Restore' : `Collapse the ${v ? 'lower' : 'right'} pane`;
  }

  function geometry() {
    const v = vertical();
    const rect = container.getBoundingClientRect();
    const style = getComputedStyle(container);
    const px = (name) => parseFloat(style[name]) || 0;
    const start = v ? rect.top + px('borderTopWidth') + px('paddingTop') : rect.left + px('borderLeftWidth') + px('paddingLeft');
    const size = v ? container.clientHeight - px('paddingTop') - px('paddingBottom')
      : container.clientWidth - px('paddingLeft') - px('paddingRight');
    const gutter = v ? splitter.offsetHeight : splitter.offsetWidth;
    return { start, size, gutter };
  }

  function moveTo(offset) {
    const { size, gutter } = geometry();
    if (size <= gutter) return;
    const lo = Math.min(min, (size - gutter) / 2);
    state = { split: (clamp(offset, lo, size - gutter - lo) / size) * 100, collapsed: '' };
    render();
  }

  function collapse(side) {
    state.collapsed = state.collapsed && state.collapsed !== side ? '' : side;
    render();
    save();
  }

  splitter.addEventListener('pointerdown', (e) => {
    if (e.button !== 0 || e.target.closest('button')) return;
    e.preventDefault();
    splitter.focus({ preventScroll: true });
    splitter.setPointerCapture(e.pointerId);
    const { start, gutter } = geometry();
    const coordinate = (ev) => (vertical() ? ev.clientY : ev.clientX);
    const grab = coordinate(e) - start - (position() / 100) * geometry().size;
    const grip = clamp(grab, 0, gutter);
    splitter.classList.add('dragging');
    document.body.classList.add('resizing');
    document.body.classList.toggle('resizing-v', vertical());
    const move = (ev) => moveTo(coordinate(ev) - start - grip);
    const end = () => {
      splitter.removeEventListener('pointermove', move);
      splitter.removeEventListener('pointerup', end);
      splitter.removeEventListener('pointercancel', end);
      splitter.classList.remove('dragging');
      document.body.classList.remove('resizing', 'resizing-v');
      save();
    };
    splitter.addEventListener('pointermove', move);
    splitter.addEventListener('pointerup', end);
    splitter.addEventListener('pointercancel', end);
  });

  splitter.addEventListener('dblclick', (e) => {
    if (e.target.closest('button')) return;
    state = defaults();
    render();
    save();
  });

  splitter.addEventListener('keydown', (e) => {
    const step = { ArrowLeft: -STEP, ArrowUp: -STEP, ArrowRight: STEP, ArrowDown: STEP }[e.key];
    if (step) {
      moveTo(((position() + step) / 100) * geometry().size);
      save();
    } else if (e.key === 'Home') {
      collapse('start');
    } else if (e.key === 'End') {
      collapse('end');
    } else {
      return;
    }
    e.preventDefault();
  });

  toStart.addEventListener('click', () => collapse('start'));
  toEnd.addEventListener('click', () => collapse('end'));
  verticalWhen?.addEventListener('change', () => {
    load();
    render();
  });

  load();
  render();
}
