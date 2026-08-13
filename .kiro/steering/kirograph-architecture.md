---
inclusion: manual
---

# KiroGraph: Architecture Exploration Workflow

Follow these steps to understand the high-level structure of the codebase.

## Steps

1. **Get project overview**
   ```
   kirograph_status()
   ```

2. **View architecture**
   ```
   kirograph_architecture()
   ```

3. **Check coupling health**
   ```
   kirograph_coupling(sortBy: "instability")
   ```

## Interpretation
- High Ca (afferent) = load-bearing, risky to change interface
- High Ce (efferent) = depends on many things, safe to refactor internals
