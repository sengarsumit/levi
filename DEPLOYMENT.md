# Deploying the backend (free)

This covers deploying `levi/` to Render using Docker, with Aiven's free MySQL
plan as the database. Both are free with no credit card.

## What's new

- `Dockerfile` / `.dockerignore` — multi-stage build (Maven → JRE runtime), runs as a
  non-root user, listens on `${PORT:8080}` (Render injects `PORT` at runtime).
- `src/main/resources/application-docker.yaml` — a **new, git-tracked** config file
  used only in the container. It contains no secrets, only `${ENV_VAR}` references.
  Your local `src/main/resources/application.yaml` is untouched, stays gitignored,
  and is not read at all in the container.
- `SecurityConfig.java` — added CORS (previously there was none; it "worked" locally
  only because Vite's dev proxy makes requests same-origin). Allowed origins come
  from `APP_CORS_ALLOWED_ORIGINS`, default `http://localhost:5173`.
- `render.yaml` — a Render Blueprint so the service (and which env vars it needs)
  is defined in code.

Important: **the container never sees your local `application.yaml`.** The image is
activated with `SPRING_PROFILES_ACTIVE=docker` (baked into the Dockerfile), which
makes Spring Boot load `application-docker.yaml` instead. If a required env var
(`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_PASSWORD`, `APP_JWT_SECRET`, etc.) is
missing, the app fails to start immediately with a clear error, rather than
silently booting with a wrong/default value — check Render's logs if that happens.

I could not run `docker build` myself to verify the image — Docker isn't
available in this environment. Everything below was reviewed by hand; please
sanity-check the first build's logs on Render (or run `docker build .` yourself
if you have Docker) before relying on it.

## Part 1 — MySQL on Aiven (free)

1. Sign up at [aiven.io](https://aiven.io) (no card required).
2. **Create service** → **MySQL** → select the **Free** plan → pick any
   cloud/region → give it a name (e.g. `levi-db`) → **Create service**.
3. Wait for status to go green/"Running" (a minute or two).
4. Open the service's **Overview** tab and note: **Host**, **Port**, **User**
   (usually `avnadmin`), **Password**, **Default database name** (usually
   `defaultdb`).
5. Build the JDBC URL from those values:
   ```
   jdbc:mysql://<HOST>:<PORT>/<DATABASE>?sslmode=require
   ```
   Aiven requires SSL; `sslmode=require` is enough — no separate CA/truststore
   file needed with the `mysql-connector-j` driver this project already uses.

Note: Aiven may power off a free service that sees no activity for a long
stretch (you'll get a notification first, and can turn it back on). This is a
different thing from Render's cold-start sleep below — for a demo/portfolio
deployment it's not a practical problem, just don't expect an abandoned free
DB to necessarily still be running months later untouched.

## Part 2 — push these files

Done — committed (`a9d1387`) and pushed to `origin/master`.

## Part 3 — Render Blueprint

1. Sign up at [render.com](https://render.com) (no card required for free tier).
2. **New +** → **Blueprint** → connect your GitHub account → select the `levi`
   repo, branch `master`. Render will detect `render.yaml`.
3. It'll prompt you for the env vars marked `sync: false`:
   | Variable | Value |
   |---|---|
   | `SPRING_DATASOURCE_URL` | the `jdbc:mysql://...?sslmode=require` URL from Part 1 |
   | `SPRING_DATASOURCE_USERNAME` | Aiven's User (e.g. `avnadmin`) |
   | `SPRING_DATASOURCE_PASSWORD` | Aiven's Password |
   | `APP_JWT_SECRET` | a random 64-char hex string — see below |
   | `APP_CORS_ALLOWED_ORIGINS` | your Netlify site's URL, e.g. `https://<your-site>.netlify.app` (see Part 4 — a placeholder is fine for now, you can edit it later without redeploying code) |

   Generate `APP_JWT_SECRET`:
   ```bash
   openssl rand -hex 32
   ```
   PowerShell equivalent:
   ```powershell
   $bytes = New-Object byte[] 32; (New-Object Security.Cryptography.RNGCryptoServiceProvider).GetBytes($bytes); ($bytes | ForEach-Object { $_.ToString("x2") }) -join ''
   ```
4. **Apply** to build and deploy. First build takes a few minutes (downloads
   the JDK base images, runs the Maven build). Watch the logs for the app
   actually starting — first boot will also run Hibernate's `ddl-auto: update`,
   creating all tables on the empty Aiven database automatically. No manual
   migration step needed.
5. Once live, confirm: `https://<your-service>.onrender.com/auth/health` →
   `{"status":"UP"}`.

**Free-tier cold starts**: Render's free web services spin down after 15
minutes of no traffic and take ~30-60s to wake back up on the next request.
The first request after idle time will just look slow, not broken.

## Part 4 — connecting the frontend

Handled via a Netlify redirect proxy rather than a frontend code change —
see `levi-frontend/DEPLOYMENT.md` for the full steps. Short version: its
`netlify.toml` forwards `/api/*` and `/ws-levi/*` to this Render service
(mirroring what `vite.config.ts`'s dev proxy already does locally), and once
the Netlify site is live you come back here and set `APP_CORS_ALLOWED_ORIGINS`
to its real URL.

## Rollback / local dev impact

Nothing here changes local dev: `application.yaml` is untouched, `mvnw
spring-boot:run` still uses it exactly as before, and `application-docker.yaml`
is only ever loaded when `SPRING_PROFILES_ACTIVE=docker` is set (i.e. inside
the container).
