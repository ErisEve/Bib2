import { applyTheme, preferredTheme } from './theme.js';

// Runs in the document head before styles are painted.
applyTheme(preferredTheme());
