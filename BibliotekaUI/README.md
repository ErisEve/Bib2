# Biblioteka UI

The frontend is a React application served by the existing Spring Boot service.
The `/`, `/login`, `/dashboard`, and `/seat_reservation` routes render the same
React entry point with the existing page styles. API requests retain the existing
same-origin `/api/...` URLs and bearer-token authentication; run behind the
application gateway for the book, lending, user, and seat services.

## Frontend development

Use Node.js 22 or newer:

```sh
npm ci
npm test
npm run build
```

React source is in `frontend/src`. `npm run dev` watches and rebuilds the bundle;
serve it through Spring/the gateway and reload the browser after changes. If
running a packaged JAR or container, rebuild it to include changed resources.

**Commit generated build files** in `src/main/resources/static/app/` together with
the React sources, `package.json`, and `package-lock.json`. This includes `app.js`,
`app.css`, `theme.js`, and the generated license file. Never edit generated files by hand.
The esbuild configuration is in `frontend/build.mjs`; existing page styles remain
in `src/main/resources/static/css/`.

The theme toggle is available on every page. It defaults to the browser's system
preference and saves an explicit light/dark choice in local storage. The small
`theme.js` entry applies this preference before the page is painted.

Maven and Docker package these checked-in assets without requiring Node or an npm
download. After frontend changes run `npm run build` before `mvn package` or
building the Docker image. In CI, run `npm ci && npm test && npm run build`, then
`git diff --exit-code -- src/main/resources/static/app` to detect stale bundles.
