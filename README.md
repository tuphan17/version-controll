# version-controll

side project — small spring app + a tiny C program (`tvcs`) that snapshots folders of csvs into a `.tvcs` directory. kinda git-ish but dumb on purpose.

**What's where:** `native/` is the cli, `server/` is the http api, `sql/catalog.sql` is mostly reference (jpa creates the real tables anyway).

The java side can run **without** building the C binary: if `tvcs` isn't on your PATH it falls back to java that writes the same files. Set `tabularhub.tvcs.mode=embedded` to force that, or `native` if you only want the exe.

**On disk:** commits live under `.tvcs/objects/commits`, csv copies under `objects/snapshots/<id>/`, branch tips in `refs/heads/...`. commit ids are fnv1a hex — fine for messing around, don't treat it like a real hash.

### build the C tool (optional)

```bash
cd native
cmake -S . -B build && cmake --build build
# or: make   (needs gcc/clang)
```

then put `tvcs` on PATH or point `tabularhub.tvcs.executable` at it.

### run the server

need jdk 17+.

```bash
cd server
./mvnw spring-boot:run    # unix — chmod +x mvnw if needed
```

windows: `mvnw.cmd spring-boot:run`

data ends up in `server/data/` (h2 file + repo folders).

tests: `mvnw test` / `mvnw.cmd test`

### http endpoints

- `POST /api/v1/repos` — `{"name":"whatever"}`
- `POST /api/v1/repos/{id}/commits` — json with `message` and `tables` map of `filename.csv -> contents`
- `GET .../log` and `.../head` — text-ish status

### cli quick test

```bash
tvcs init ./myrepo
mkdir -p staging && echo 'x,y' > staging/a.csv
tvcs commit ./myrepo "wip" ./staging
tvcs log ./myrepo
```

---

repo on github: [tuphan17/version-controll](https://github.com/tuphan17/version-controll)
