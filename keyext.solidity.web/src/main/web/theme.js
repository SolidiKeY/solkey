const STORAGE_KEY = 'solkey.theme';
const BAR_COLOR = { light: '#f5f5f9', dark: '#111118' };
const root = document.documentElement;

function applyTheme(choice) {
  if (choice === 'light' || choice === 'dark') root.dataset.theme = choice;
  else delete root.dataset.theme;
  for (const meta of document.querySelectorAll('meta[name="theme-color"]')) {
    meta.content = BAR_COLOR[root.dataset.theme ?? (meta.media.includes('dark') ? 'dark' : 'light')];
  }
  const current = root.dataset.theme ?? 'system';
  for (const button of document.querySelectorAll('[data-theme-choice]')) {
    button.setAttribute('aria-pressed', String(button.dataset.themeChoice === current));
  }
}

export function setupTheme() {
  for (const button of document.querySelectorAll('[data-theme-choice]')) {
    button.addEventListener('click', () => {
      const choice = button.dataset.themeChoice;
      applyTheme(choice);
      try {
        if (choice === 'system') localStorage.removeItem(STORAGE_KEY);
        else localStorage.setItem(STORAGE_KEY, choice);
      } catch {}
    });
  }
  applyTheme(root.dataset.theme ?? 'system');
}
