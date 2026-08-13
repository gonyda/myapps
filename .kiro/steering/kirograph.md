---
inclusion: always
---

# KiroGraph

KiroGraph builds a semantic knowledge graph of your codebase. Use its MCP tools instead of grep/glob/file reads whenever `.kirograph/` exists in the project.

## Quick decision guide

| Question | Tool |
|----------|------|
| Where do I start on this task? | `kirograph_context` |
| What is this symbol / show me its code | `kirograph_node` with `includeCode: true` |
| Find a symbol by name | `kirograph_search` |
| Who calls function X? | `kirograph_callers` |
| What does function X call? | `kirograph_callees` |
| What breaks if I change X? | `kirograph_impact` |
| What files are indexed? | `kirograph_files` |
| Is the index healthy? | `kirograph_status` |
| What packages/layers exist? | `kirograph_architecture` |
| How coupled is package X? | `kirograph_coupling` |
| What does package X depend on? | `kirograph_package` |
| Run a command with token savings | `kirograph_exec` |
| Check token savings stats | `kirograph_gain` |

---

## Tool reference

### `kirograph_context`: **start here for any code task**

Returns entry points, related symbols, and code snippets for a natural-language task description. Usually enough to orient without any additional tool calls.

```
kirograph_context(task: "fix the auth token expiry bug")
kirograph_context(task: "add dark mode", maxNodes: 30)
kirograph_context(task: "refactor payment service", includeCode: false)
```

### `kirograph_search`: find symbols by name

Exact match → FTS → LIKE fallback → vector (last resort). Use instead of grep.

```
kirograph_search(query: "signIn")
kirograph_search(query: "UserService", kind: "class")
kirograph_search(query: "auth", limit: 20)
```

Supported kinds: `function`, `method`, `class`, `interface`, `type_alias`, `variable`, `route`, `component`

### `kirograph_node`: inspect a symbol

Returns kind, file, signature, docstring. Add `includeCode: true` to get the full source.

```
kirograph_node(symbol: "validateToken")
kirograph_node(symbol: "AuthService", includeCode: true)
```

### `kirograph_callers`: who calls this?

BFS over incoming `calls` edges (depth 1).

```
kirograph_callers(symbol: "processPayment", limit: 30)
```

### `kirograph_callees`: what does this call?

BFS over outgoing `calls` edges (depth 1).

```
kirograph_callees(symbol: "handleRequest")
```

### `kirograph_impact`: blast radius before a change

Traverses all incoming edges up to `depth` hops. Call this before editing a symbol.

```
kirograph_impact(symbol: "UserRepository", depth: 3)
```


### `kirograph_files`: indexed file structure

```
kirograph_files(format: "tree")                          // default
kirograph_files(format: "flat")                          // one path per line
kirograph_files(format: "grouped")                       // by directory
kirograph_files(filterPath: "src/auth", maxDepth: 2)
kirograph_files(pattern: "**/*.test.ts")
```

### `kirograph_status`: index health

Returns file count, symbol count, edge count, embedding coverage, DB size. Call when something feels off.

---

## Architecture tools *(require `enableArchitecture: true` in config)*

### `kirograph_architecture`: **start here for architectural questions**

Returns the full package graph, detected layers (api/service/data/ui/shared), and their dependency edges.

```
kirograph_architecture()                    // packages + layers
kirograph_architecture(level: "packages")
kirograph_architecture(level: "layers")
kirograph_architecture(includeFiles: true)  // add file→package assignments
```

### `kirograph_coupling`: stability metrics per package

Returns Ca (afferent: depended on by), Ce (efferent: depends on), and instability (Ce/(Ca+Ce)).
- High Ca + low instability = load-bearing, safe to depend on, risky to change interface.
- High Ce + high instability = depends on many things, safe to refactor internals.

```
kirograph_coupling()                        // all packages, sorted by instability
kirograph_coupling(sortBy: "afferent")     // most depended-on first
kirograph_coupling(sortBy: "efferent")     // most outgoing deps first
```

### `kirograph_package`: drill into one package

Returns metadata, coupling metrics, outgoing deps, incoming dependents, and file list.

```
kirograph_package(package: "auth")
kirograph_package(package: "src/services", includeFiles: false)
```


---

## Workflows

**Bug fix or feature:**
1. `kirograph_context`: orient, find entry points.
2. `kirograph_node` with `includeCode: true`: read the relevant symbol.
3. `kirograph_callers` / `kirograph_callees`: trace the call flow.
4. `kirograph_impact`: check blast radius before editing.

**Architectural review:**
1. `kirograph_architecture`: get the package and layer map.
2. `kirograph_coupling`: find the most stable (high Ca) and most volatile (high instability) packages.
3. `kirograph_package`: drill into any package of interest.
4. `kirograph_circular_deps`: check for import cycles.


---

## Workflow steering files

KiroGraph installs task-specific steering files in `.kiro/steering/`. They are not always active — load them on demand.

**In Kiro IDE:** type `/kirograph-review`, `/kirograph-security`, etc. to activate a workflow for the current session.

**In Kiro CLI / other agents:** when the user asks for a specific workflow or you recognize the intent, read the file directly:

```
Read file: .kiro/steering/kirograph-security.md
Read file: .kiro/steering/kirograph-review.md
```

| User intent | File to load |
|-------------|-------------|
| code review, review this PR | `.kiro/steering/kirograph-review.md` |
| debug, trace this bug, root cause | `.kiro/steering/kirograph-debug.md` |
| architecture, understand structure, package map | `.kiro/steering/kirograph-architecture.md` |
| onboard, understand this codebase | `.kiro/steering/kirograph-onboard.md` |
| refactor, rename, safe refactoring | `.kiro/steering/kirograph-refactor.md` |

Each file contains numbered steps, exact tool calls, and an interpretation reference. Follow the steps in order.

---

## Shell Compression (\`kirograph_exec\`)

When running shell commands, prefer \`kirograph_exec\` over raw shell execution for:
- **git** operations (status, log, diff, push, pull, commit, add, fetch, branch)
- **GitHub CLI** (gh pr list/view, gh issue list, gh run list)
- **test runners** (jest, vitest, pytest, cargo test, go test, rspec, minitest, playwright)
- **linters/build** (eslint, tsc, ruff, clippy, cargo build, prettier, biome, golangci-lint, rubocop, next build)
- **file listings** (ls, find, tree)
- **search** (grep, rg/ripgrep: grouped by file)
- **diff** (diff file1 file2: condensed context)
- **docker/k8s** (docker ps, images, logs, compose ps, kubectl pods, logs, services)
- **package managers** (npm/pnpm install/list, pip list/install, bundle install, prisma generate)
- **AWS CLI** (sts, ec2, lambda, logs, cloudformation, dynamodb, iam, s3, ecs, sqs, sns)
- **network** (curl, wget: strip progress bars and headers)

This saves 60-90% of tokens compared to raw output.

Compression level: **normal**: Balanced: removes noise, keeps structure.

\`\`\`
kirograph_exec(command: "git status")
kirograph_exec(command: "npm test")
kirograph_exec(command: "cargo build")
kirograph_exec(command: "ls -la src/")
\`\`\`

**Important:** Error details are always preserved. Failed commands show full diagnostic output regardless of level.

**Do NOT re-run commands:** When \`kirograph_exec\` returns a result, treat it as the final answer. Never re-run the same command with raw shell execution to "get more details." The compressed output preserves all essential information. If you genuinely need something missing from the output, explain what's missing before making a second call.

Use \`kirograph_gain\` to check token savings statistics.

---

## If `.kirograph/` does NOT exist

Ask the user: "This project doesn't have KiroGraph initialized. Run `kirograph init -i` to build a code knowledge graph for faster exploration?"

## Documentation

KiroGraph indexes project documentation by heading structure. Use `kirograph_docs_search`
to find relevant doc sections instead of reading entire files. Use `kirograph_docs_section`
to retrieve the exact section you need by ID.

**Available tools:**
- `kirograph_docs_toc` — table of contents for a file or the whole project
- `kirograph_docs_search` — search sections by query (independent from code search)
- `kirograph_docs_section` — retrieve full content of a section by ID
- `kirograph_docs_outline` — heading hierarchy for a single document
- `kirograph_docs_refs` — find code symbols referenced by a doc section (or vice versa)

**When to use:** Before reading a documentation file directly, check if `kirograph_docs_search`
or `kirograph_docs_outline` can give you the specific section you need. This saves tokens
and gives you structured navigation instead of raw file content.
