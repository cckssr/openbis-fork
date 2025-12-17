### Auto-save (New Forms) — Feature Documentation

This document explains the **auto-save feature** in `new-forms`: how it works, where the logic lives, and **why** key design choices were made (with trade-offs).

---

## Summary

Auto-save provides:
- **Per-entity auto-save preference** (enabled/disabled) stored in browser storage
- **Draft saving** (only dirty fields) to `localStorage` while editing
- **Draft restoration** when re-entering edit mode (only once, loop-safe)
- **Draft cleanup** after a successful save and when disabled

Core goals:
- **Keep `EntityFormContextProvider` clean**
- **Avoid duplicated state (“drift”)**
- **Keep UI action renderers generic**
- **Be resilient to common edge cases** (stale data, corrupted storage, entity mismatch, infinite loops)

---

## High-level architecture

### Components involved

- **`useEntityAutoSaveFlow`** (`hooks/useEntityAutoSaveFlow.tsx`)
  - Feature-level hook that orchestrates everything: preference + save + restore + UI glue.

- **`useAutoSave`** (`hooks/useAutoSave.tsx`)
  - Responsible for **saving/loading/clearing** the draft data (selective: dirty fields only).

- **`useAutoSaveRestore`** (`hooks/useAutoSaveRestore.tsx`)
  - Responsible for **restoration logic** (when to restore, how to avoid loops).

- **`SwitchActionRenderer`** (`components/actions/SwitchActionRenderer.tsx`)
  - Renders toggle actions (like auto-save). Uses `action.value` as “checked”.

- **`ActionHandlerDispatcher`** (`engine/ActionHandlerDispatcher.ts`)
  - Maps `'auto-save'` action to `CoreFormModel.autoSaveAction`.

- **`CoreFormModel.autoSaveAction`** (`engine/CoreFormModel.ts`)
  - Toggles auto-save using the action context: `setAutoSaveEnabled(!isAutoSaveEnabled)`.

- **`EntityForm`** (`components/EntityForm.tsx`)
  - Renders actions using `ComponentRegistry` and supports `actionOverrides` (render-time overrides, no form mutation).

---

## Storage model (two keys, two responsibilities)

### 1) Preference key (per entity)

**Purpose**: “Should auto-save be enabled for this entity?”

Used by: `useEntityAutoSaveFlow`

Key format:
- `new-forms:auto-save-enabled:${user}:${entityKind}:${permId}`

Value:
- Stored only when enabled: `'true'`
- **Removed** when disabled (default is false), to avoid storing useless `"false"` entries.

**Reasoning**
- Preference is **UI/session behavior**, not part of the entity’s domain model.
- Per entity gives the most intuitive UX: “I want auto-save for this object, not globally for everything.”
- Storing only `'true'` keeps `localStorage` clean and avoids unlimited key growth with false values.

### 2) Draft key (per entity)

**Purpose**: Store the **unsaved draft data** (only dirty fields) for the current entity.

Used by: `useAutoSave` / `useAutoSaveRestore`

Key format:
- `form-data-${entityKind}-${permId}-${user}`

Value (shape)
- Includes:
  - dirty fields + metadata needed to merge
  - `timestamp` (stale data handling)
  - entity identifiers (safety / mismatch detection)

**Reasoning**
- Separating preference vs draft prevents coupling: clearing a draft should not reset preference.
- Key includes user + entity identifiers to avoid cross-user and cross-entity collisions.

---

## Data saving strategy: “dirty fields only”

### What is saved

`useAutoSave` computes diffs between `form` and `originalForm` and saves **only changed fields**.

**Reasoning**
- Performance: less serialization, smaller `localStorage` footprint
- Robustness: fewer schema/shape compatibility issues across releases
- Practicality: most forms have many fields; saving everything is wasteful

**Trade-offs**
- You must be careful when merging drafts back:
  - The draft can’t fully reconstruct a form by itself; it is applied on top of the current server-loaded form.
  - This is intentional and safer than replacing the entire form with potentially stale structure.

---

## Restoration strategy

Restoration is handled by `useAutoSaveRestore`.

### When restore runs

Restore is attempted only when:
- mode is `EDIT`
- preference is enabled
- a draft exists for this entity

### How infinite loops are avoided

Restoration is a classic source of React loops:
- effect loads draft → calls `setForm` → re-render → effect triggers again → loop

`useAutoSaveRestore` uses refs to:
- detect “already restored for this entity”
- restore only once per entry-to-edit / entity change

**Reasoning**
- The provider must stay simple; restore complexity belongs in a dedicated hook.
- Loop prevention is easiest when encapsulated and tested once.

---

## UI wiring: why `actionOverrides` exists

The auto-save toggle is rendered using the generic `SwitchActionRenderer` which reads:
- `checked={!!action.value}`

We intentionally **do not store** the preference in `form.actions[].value` (to avoid state duplication).

Instead, `EntityForm` supports:
- `actionOverrides: Record<string, Partial<FormAction>>`

and at render time we apply:
- `actionOverrides['auto-save'] = { value: isAutoSaveEnabled }`

**Reasoning**
- Single source of truth: preference lives in the feature hook only
- No “drift”: we avoid synchronizing provider state back into form DTO
- Provider stays clean
- Action renderer stays generic

**Trade-offs**
- Requires one extra prop (`actionOverrides`) and a small merge step in `EntityForm`.
  - This is still cleaner than pushing UI-only state into the form model.

---

## Action handling: avoiding the “auto-save includes save” bug

Previously, save routing used `actionName.includes('save')`, which incorrectly treated `'auto-save'` as a save action.

Now save routing is strict:
- only `'save'` or `':save'` suffix is a “save entity” action.

**Reasoning**
- Keep action routing explicit and predictable
- Prevent subtle bugs caused by substring matching

---

## Lifecycle & cleanup rules

### Clearing draft after successful save

After a successful save (`EDIT` or `CREATE`), the provider calls `clearStorage()` to delete the draft key.

**Reasoning**
- Prevent restoring stale drafts after the data is safely persisted server-side

### Removing preference

When user disables auto-save, the preference key is removed:
- prevents accumulation of redundant false values

### Stale or corrupted drafts

`useAutoSave`:
- discards too-old drafts (based on max age)
- clears corrupted JSON automatically

**Reasoning**
- Avoid “mysterious restores” from weeks ago
- Repair localStorage corruption gracefully

---

## How to add auto-save to a form

1) Ensure the form includes an action:
- in `actionsFieldGetters.ts` (already present):
  - `{ name: 'auto-save', component: 'switch', ... }`

2) Ensure dispatcher knows the action:
- `ActionHandlerDispatcher.ts` maps `'auto-save'` → `CoreFormModel.autoSaveAction`

3) Provider must pass the auto-save flow hook:
- `EntityFormContextProvider` already uses `useEntityAutoSaveFlow`

---

## Debugging & common pitfalls

### “Auto-save starts even though I didn’t enable it”
Check:
- preference default is false
- localStorage key `new-forms:auto-save-enabled:${user}:${entityKind}:${permId}` is not present or not `'true'`

### “Switch doesn’t toggle”
Check:
- `SwitchActionRenderer` calls `onAction(action.name)` when `action.handler` is not provided
- `'auto-save'` exists in dispatcher mapping

### “Restore loops / can’t edit”
Restore loops should be prevented by `useAutoSaveRestore`.
If you modify it, ensure you keep the “restore once per entry” ref guard.

### “I see old drafts restored”
Check:
- stale draft TTL in `useAutoSave`
- ensure `clearStorage()` runs after successful save

---

## Why we chose this design (critical rationale)

### We avoid storing preference in `form.actions`

Storing preference both in provider state and `form.actions[].value` creates two sources of truth:
- UI can show ON while behavior is OFF (or vice-versa)
- form reloads can overwrite actions and reset toggle unexpectedly

We chose a single-source-of-truth approach:
- preference = feature hook state + localStorage
- UI reads via `actionOverrides` (derived)

This is **more consistent and maintainable** in the long run.


