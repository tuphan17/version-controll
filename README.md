# tabular-hub (v1 scaffold)

Learning stack: a **C** `tvcs` CLI for **CSV snapshot checkpoints**, a **Java** Spring Boot **service** with a small REST API, and **SQL** (H2) for project metadata in the hub.

## Layout

| Layer | Role |
|--------|------|
| `native/` | **`tvcs`** — content-addressed CSV folders, ordered checkpoint chain, branch refs |
| `server/` | **Hub** — REST API, catalog DB, runs repo ops via native `tvcs` or embedded Java |
| `sql/catalog.sql` | **Documented** catalog schema (mirrored by JPA) |

### Boundary (v1)

- **C**: files under `<repo>/.tvcs/` (checkpoints, snapshot blobs, refs).
- **Java**: HTTP + JDBC catalog; **no** JNI. With `tabularhub.tvcs.mode=auto` (default), the hub uses the native `tvcs` executable when it resolves on `PATH` (or absolute path); otherwise a **built-in Java engine** writes the same layout so the service runs with only a JDK.
- **SQL**: `repo_registry` (id, name, slug, filesystem path, created_at).

### On-disk model (tvcs)

- `objects/snapshots/<snapshot_id>/` — copies of `*.csv` from a staging directory.
- `objects/commits/<commit_id>` — text: `parent`, `snapshot`, `time`, `message`; id = FNV-1a hex of body (demo-grade, not cryptographic).
- `refs/heads/<branch>` — tip commit hash or `NONE`.

## Build `tvcs`

```bash
cd native
cmake -S . -B build
cmake --build build
```

Building the native CLI is **optional** for the hub: the default `auto` mode falls back to pure Java when `tvcs` is not installed.

If you do build it, put `build/tvcs` (or `build\Release\tvcs.exe` with MSVC) on your `PATH`, or set `tabularhub.tvcs.executable` to the full path. Force behavior with `tabularhub.tvcs.mode=embedded` or `native`.

## Run the hub (Java 17+)

```bash
cd server
mvnw.cmd spring-boot:run
```

(On Unix, install Maven or add a `mvnw` script; `mvn spring-boot:run` also works if Maven is installed.)

Defaults: catalog at `./data/hub.mv.db`, repos under `./data/repos/<uuid>/`.

### Verify (tests)

```bash
cd server
mvnw.cmd test
```

## API (v1)

- `POST /api/v1/repos` — body `{"name":"My project"}` → creates repo + `tvcs init`.
- `POST /api/v1/repos/{id}/commits` — body:

```json
{
  "message": "load users",
  "tables": {
    "users.csv": "id,name\n1,Ada\n"
  }
}
```

- `GET /api/v1/repos/{id}/log` — plain text from `tvcs log`.
- `GET /api/v1/repos/{id}/head` — `branch <commit>`.
- `GET /api/v1/repos` / `GET /api/v1/repos/{id}` — catalog.

## CLI examples

```bash
tvcs init ./myrepo
echo 'id,name' > ./staging/users.csv
echo '1,Ada' >> ./staging/users.csv
tvcs commit ./myrepo "first import" ./staging
tvcs log ./myrepo
tvcs head ./myrepo
tvcs checkout ./myrepo main ./out
```

## Roadmap (if you extend this)

- Row-level diffs, three-way merge, stronger hashes (e.g. SHA-256), packfiles, authenticated upload/download, richer SQL serving — each is a sizable project.

## Publish to GitHub

This repo is already initialized with `main` and an initial commit. On GitHub, create a **new empty** repository (no README or license from the wizard). Then:

```bash
cd /path/to/mini-dolthub
git remote add origin https://github.com/<your-username>/<repo-name>.git
git push -u origin main
```

Replace `<your-username>` and `<repo-name>` (for example `tabular-hub`). If the folder is still named `mini-dolthub`, you can rename it later after closing anything that has the folder open.
