## Public web demo deployment (Netlify / Vercel)

### Frontend (Compose WASM)

- **Production build command**: `./gradlew :composeApp:prepareWebDemo`
- **Output folder**: `composeApp/build/webDemo`

That folder contains `index.html`, JS bundle, WASM files, and SPA routing rules.

### Configure API URL (production)

Edit `composeApp/src/wasmJsMain/resources/index.html` and set:

```js
window.__KINETIC_API_BASE__ = "https://YOUR_BACKEND_URL";
window.__KINETIC_ENV__ = "production";
window.__KINETIC_DEMO_MODE__ = "true";
```

Then rebuild.

### Netlify

This repo includes `netlify.toml`:
- build: `./gradlew :composeApp:prepareWebDemo`
- publish: `composeApp/build/webDemo`

### Vercel

Use **Framework Preset: Other**.
- Build command: `./gradlew :composeApp:prepareWebDemo`
- Output directory: `composeApp/build/webDemo`

`vercel.json` is included to rewrite all routes to `index.html` (so refresh works).

### Backend demo mode (seed demo users)

Run backend with:

```bash
DEMO_MODE=true
```

Demo users:
- **Admin**: `admin@demo.com` / `1234`
- **Member**: `member@demo.com` / `1234` (uses DNI login)

