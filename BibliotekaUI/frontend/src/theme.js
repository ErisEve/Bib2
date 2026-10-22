const storageKey = 'biblioteka-theme';

export function preferredTheme() {
  try {
    const saved = localStorage.getItem(storageKey);
    if (saved === 'light' || saved === 'dark') return saved;
  } catch { /* The theme still works when browser storage is unavailable. */ }
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export function applyTheme(theme, persist = false) {
  document.documentElement.dataset.theme = theme;
  if (persist) {
    try { localStorage.setItem(storageKey, theme); } catch { /* Keep the in-memory choice. */ }
  }
}
