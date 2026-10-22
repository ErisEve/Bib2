import { build, context } from 'esbuild';

const options = {
  entryPoints: { app: 'frontend/src/main.jsx', theme: 'frontend/src/theme-init.js' },
  outdir: 'src/main/resources/static/app',
  bundle: true,
  minify: true,
  jsx: 'automatic',
  target: ['es2020'],
  define: { 'process.env.NODE_ENV': '"production"' },
  legalComments: 'linked',
};

if (process.argv.includes('--watch')) {
  const ctx = await context(options);
  await ctx.watch();
  console.log('Watching React sources. Serve the app through the existing Spring/gateway service.');
} else {
  await build(options);
}
