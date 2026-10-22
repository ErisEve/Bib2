import { useState } from 'react';
import { applyTheme, preferredTheme } from './theme.js';

export function ThemeToggle() {
  const [theme, setTheme] = useState(preferredTheme);
  function toggle() {
    const next = theme === 'dark' ? 'light' : 'dark';
    applyTheme(next, true);
    setTheme(next);
  }
  return <button type="button" className="theme-toggle" aria-label="Tamni režim" aria-pressed={theme === 'dark'} onClick={toggle}>
    <span aria-hidden="true">{theme === 'dark' ? '☀' : '☾'}</span> {theme === 'dark' ? 'Svetli režim' : 'Tamni režim'}
  </button>;
}
