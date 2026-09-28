# SolKey in the browser

`keyext.solidity.web` compiles the prover to WebAssembly with **GraalVM Web Image**
(`native-image --tool:svm-wasm`, WasmGC) and serves it as a static page. Proofs run entirely in the
visitor's browser, and `.github/workflows/web.yml` deploys the page to GitHub Pages.

## Pieces

| Piece | Role |
|---|---|
| `WebProver` | Web Image entry point; installs `globalThis.solkey.{functions,verify}` (request object in, JSON string out) |
| `JsSolcCompiler` | `SolcCompiler` backed by `globalThis.solkeySolc`, i.e. the page's `soljson.js` |
| `SolcWrapper.useCompiler` | replaces the GraalJS/GraalWasm solc, which cannot go into a Web Image |
| `SoliditySources` | in-memory `.sol` overlay; the page's contract lives at `/solkey-web/Contract.sol` |
| `SolidityVerifier` | proves one function on the calling thread (`ProofStarter`); Web Image has no threads |
| `ProofSession` | one proof, headless: validated taclet options (`TacletChoices`, shared with the CLI), strategy settings, statistics, tree walk, sequent printing, rules at a clicked offset, apply (with schema-variable completion), prune, run on a goal, save, `.key`/`.proof` load and replay |
| `WebProver.call(type, json)` | the whole browser API: `functions` (outline: contracts, provable or not with the reason, spec clauses, invariants), `verify`, `problem`, `choices`, `load`, `tree`, `node`, `rules`, `apply`, `prune`, `auto`, `save`, `drop`; sessions are kept per worker |
| `src/main/runtime/runtime.js` | the `--solc` EVM check in JavaScript (`@ethereumjs/evm`), same arguments and verdicts as `SolidityRuntimeCheck`; loaded on demand |
| `src/main/web/inspector.js` | proof inspector: tree, open goals, generated problem, sequent (click a term → rules), taclet text, prune, run on goal |
| `SolidityVerifier` (`withProof`) | also returns the saved `.proof` text; the page rewrites its `\programSource` to the file's own name |
| `PathUrls.toURL` | `Path` → `URL` that survives Web Image's in-memory file system (jimfs has no URL handler) |
| `src/main/web/app.js` | UI: function table, pool of prover workers (one per 4 queued functions, up to the *Parallel provers* option), time limit (TIMEOUT), share links (`#c=` deflate-raw + base64url of source/contract/options), autosave (`localStorage`), `.sol` and `.proof` download |
| `src/main/web/worker.js` | hosts `soljson.js` and `solkey.js` off the UI thread. `solkey.js` looks for `<its script>.wasm`, which in a worker is `worker.js.wasm`, so the worker answers that fetch with `solkey.js.wasm`, which it starts downloading before `soljson.js` loads |
| `src/main/web/pwa.js` | the installable app: install button (`beforeinstallprompt`, or the Add to Home Screen hint on iOS), the phone layout's Code/Results views and bottom Verify button, the system share sheet on touch devices, and `.sol`/`.key`/`.proof` files the installed app is opened with (`launchQueue`) |
| `src/main/web/theme.js` | System / Light / Dark switch in the app bar: sets `data-theme` on `<html>` (an inline script in `<head>` applies the stored choice before the first paint) and stores it as `solkey.theme`; `style.css` defines every colour with `light-dark()`, so the choice is just `color-scheme` |
| `src/main/web/split.js` | draggable dividers like KeYther's `JSplitPane`s: contract \| results and, in the inspector, proof tree \| sequent (stacked on a phone); ◂ ▸ arrows collapse and restore a side, double-click resets, arrow keys/Home/End from the keyboard; stored as `solkey.split.*` |
| `src/main/web/manifest.webmanifest`, `icons/` | web app manifest (standalone, file handlers) and icons; `icon.svg`/`maskable.svg` are the sources, the PNGs are rendered from them with `rsvg-convert` |
| `src/main/web/sw.js` | service worker: caches the site under `solkey-<build id>` (the id is stamped by `site`), so repeat visits and offline use load from the cache |
| `src/main/editor/editor.js` | CodeMirror 6 (Solidity mode, solc errors as diagnostics, ✓/✗ gutter per function); bundled by esbuild (`bundleEditor`) into `editor.js` |
| `src/test/smoke.mjs`, `src/test/browser.mjs` | `smokeTest` proves in Node; `browserTest` drives the page in headless Chromium (Playwright): verdicts, editor, parallel provers, timeout, proof download, share, autosave, offline, installability, theme switch, dividers, phone layout and tab bar. `-PsolkeyShots=DIR` keeps screenshots |
| `reachability-metadata.json` | the rule files as resources; public constructors of the reflectively built varconds |

The runtime classpath of the image drops Besu, GraalVM polyglot, picocli and logback.

## Build locally

Web Image needs **Oracle GraalVM 25.1+ or an Oracle GraalVM early-access build**
(github.com/graalvm/oracle-graalvm-ea-builds; GraalVM Community has no `lib/svm/tools/svm-wasm`)
and binaryen's `wasm-as` **version 119** on `PATH`.

```bash
export GRAALVM_HOME=/path/to/graalvm-ea         # settings.gradle includes the module only then
export PATH=/path/to/binaryen-version_119/bin:$PATH
./gradlew :keyext.solidity.web:site              # -> keyext.solidity.web/build/site
./gradlew :keyext.solidity.web:singleFile        # -> keyext.solidity.web/build/solkey.html
./gradlew :keyext.solidity.web:smokeTest         # proves a few functions in Node (25+)
./gradlew :keyext.solidity.web:browserTest       # drives the page in headless Chromium
python3 -m http.server -d keyext.solidity.web/build/site 8000
```

`singleFile` packs the site into one ~19 MB HTML file to send around: it opens from disk
(`file://`), with the gzipped wasm and soljson inlined as base64 and handed to each worker as blobs,
since a `file://` worker cannot read the page's blob URLs. `browser.mjs` given an `.html` file tests
it over `file://`.

`GRAALVM_HOME`/`BINARYEN_HOME` can also be given as `-PgraalvmHome=…`/`-PbinaryenHome=…`.
`webImage` runs the `native-image` driver on GraalVM's JVM (`lib/graalvm/svm-driver.jar`) rather
than through the `bin/native-image` launcher: the launcher is a native executable that always puts
its temporary directory in `/tmp`, while the JVM driver and its builder take `java.io.tmpdir` from
the build directory, so the build also works in a sandbox with a read-only `/tmp`.

`npm ci` (in `npmInstall`) fetches CodeMirror, esbuild and playwright-core from `package-lock.json`.
`browserTest` launches Playwright's `chromium-headless-shell` (`npx playwright-core install --only-shell
chromium`), falling back to any headless shell under `~/.cache/ms-playwright`, or `SOLKEY_CHROMIUM`.

The browser needs WasmGC and Wasm exception handling (exnref): current Chrome, Firefox or Safari.

## When the image fails at run time

A `ClassNotFoundException`/`NoSuchMethodException` for a class that is looked up reflectively, or a
missing rule file, means `reachability-metadata.json` is incomplete. Add the entry by hand, or
regenerate candidates with the tracing agent (`-agentlib:native-image-agent=config-output-dir=…`)
on a JVM run of `SolidityVerifier`.

## Limitations

Compared with KeYther: no proof-tree search, no drag-and-drop or term copy, no recent-files list, and
the sequent-wide rule list is not filtered by the clicked formula.
