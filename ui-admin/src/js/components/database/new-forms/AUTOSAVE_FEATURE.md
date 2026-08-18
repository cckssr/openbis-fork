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

### 1) Preference key (per entity, or per entity-type slot in CREATE mode)

**Purpose**: “Should auto-save be enabled for this entity?”

Used by: `useEntityAutoSaveFlow`

Key format:
- EDIT/VIEW: `new-forms:auto-save-enabled:${user}:${entityKind}:${permId || 'unknown'}`
- CREATE: `new-forms:auto-save-enabled:${user}:${entityKind}:${entityType}` (no `permId`, but
  `entityType` instead — see "CREATE mode" below)

Value:
- Stored only when enabled: `'true'`
- **Removed** when disabled (default is false), to avoid storing useless `"false"` entries.

**Reasoning**
- Preference is **UI/session behavior**, not part of the entity's domain model.
- Per entity gives the most intuitive UX: "I want auto-save for this object, not globally for everything."
- Storing only `'true'` keeps `localStorage` clean and avoids unlimited key growth with false values.
- Fallback to `'unknown'` handles cases where `permId` is undefined.

### 2) Draft key (per entity, or per entity-type slot in CREATE mode)

**Purpose**: Store the **unsaved draft data** (only dirty fields) for the current entity.

Used by: `useAutoSave`

Key format:
- EDIT/VIEW: `form-data-${entityKind}-${permId || 'new'}-${user}`
- CREATE: `form-data-${entityKind}-${entityType}-${user}` (no `permId`, but `entityType` instead —
  see "CREATE mode" below)

Value (shape):
```typescript
{
  data: Partial<Form>,        // Only dirty fields
  dirtyFields: string[],      // Field IDs that were changed
  timestamp: number,           // Unix timestamp
  entityPermId: string,       // Entity identifier for mismatch/ownership detection
  version: number              // Form version for schema validation
}
```

**Reasoning**
- Separating preference vs draft prevents coupling: clearing a draft should not reset preference.
- Key includes user + entity identifiers to avoid cross-user and cross-entity collisions.
- Fallback to `'new'` handles cases where `permId` is undefined (EDIT/VIEW only).

---

## Data saving strategy: "dirty fields only"

### What is saved

`useAutoSave` computes diffs between `form` and `originalForm` and saves **only changed fields**.

### Save Triggers

Auto-save triggers on:
- **Periodic intervals** (configurable, default: 5000ms)
- **Page unload** (`beforeunload` event) - ensures data is saved even if user closes tab/window

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

### CREATE/EDIT mode: explicit, via a restore/discard dialog

CREATE-mode drafts are **not** auto-applied. Because the draft key is shared by every open tab
creating that entity type (see below), silently applying whatever happens to be in storage could
mean pulling in a different tab's in-progress content, or a leftover draft from days ago. Instead,
`useEntityAutoSaveFlow` checks once (the first time the freshly-loaded default `form` is
available) whether a non-stale draft exists for the shared key, and if so exposes it as
`pendingDraft` / `hasPendingDraft`. `EntityFormContextProvider` renders `RestoreDraftDialog` for
this, offering **Restore Draft** / **Discard Draft** - the user decides, nothing happens
automatically.

This check is gated by the same `isSlotTakenByAnother()` liveness check the auto-save toggle uses
(see "Ownership" below): if another tab of this exact entity type is currently live, the prompt is
skipped entirely - that draft is still being actively worked on there, not an abandoned one to
recover, and offering to "restore" it into a second tab would just create two tabs editing the
same content. Once that other tab is no longer live (closed, refreshed, or its heartbeat timed
out), a freshly opened tab of that type will see the prompt as normal.

#### Matching draft fields across tabs: `name`, not `id`

A CREATE-mode form's field `id`s are prefixed with that tab's ephemeral tmp `permId` (e.g.
`"2-newObject-code"`), which is essentially never the same string across two separate tab-open
events. Matching a restored draft's fields by `id` (as EDIT mode does, and as the original
implementation of this dialog did) would therefore silently match nothing, making restore
look like it "does not work at all". Static field getters (`getCodeField`, `getDescriptionField`,
`getSpaceField`, etc. in `formFieldGetters.ts`) and property fields (already, via
`mapAssignmentToFormField`) all set a stable `name` (`'code'`, `'description'`, a property code,
...) independent of `permId`. `useAutoSave.getDirtyFields` persists that `name` alongside `id` in
the draft, and the CREATE-mode merge in `useEntityAutoSaveFlow` matches saved fields to the
current form's fields by `name` (falling back to `id` only for the rare field with no `name`).

### How infinite loops are avoided

Restoration is a classic source of React loops:
- effect loads draft → calls `setForm` → re-render → effect triggers again → loop

`useAutoSaveRestore` uses refs to:
- detect “already restored for this entity”
- restore only once per entry-to-EDIT / entity change

**Reasoning**
- The provider must stay simple; restore complexity belongs in a dedicated hook.
- Loop prevention is easiest when encapsulated and tested once.

CREATE mode doesn't need this machinery: it never auto-restores (see above), and the one-time
"is there a draft to offer" check in `useEntityAutoSaveFlow` uses a plain ref
(`hasCheckedForDraftRef`) rather than mode-transition tracking, since a CREATE form is already in
CREATE mode on mount - there's no transition to key off in the first place.

### CREATE mode: one shared slot per entity type, one owner at a time

New (not-yet-saved) entities also get drafts: if the user enables auto-save and closes the tab (or
navigates away) with unsaved changes, reopening an equivalent "new entity" form offers to restore
them (see the restore/discard dialog above).

CREATE-mode `permId` is a small per-tab counter baked in at form-construction time (e.g.
`"1-newObject"`), not a stable server identity — it gets reused once all tabs of that kind close.
Rather than trying to disambiguate it, CREATE-mode preference/draft keys deliberately drop
`permId` entirely — there is exactly **one** auto-save slot per new-entity **type** (per user),
shared by every open tab creating that type of entity, mirroring that "only one form's toggle
should really work" at a time.

**Important distinction**: "type" here means `entityType` (the concrete type code, e.g. a
specific sample type like `"YEAST"`), not `entityKind` (the broad category constant, e.g.
`EntityKind.NEW_OBJECT` = `"newObject"`). `entityKind` alone is identical for *every* sample type
- keying only by `entityKind` would put a "New Object" of type YEAST and one of type PLASMID in
the same slot, so opening the second would immediately (and wrongly) offer to restore the first's
draft. Keys are therefore always `user` + `entityKind` + `entityType` in CREATE mode. Kinds with
no real sub-type (Project, Space) just have an empty `entityType`, which correctly collapses back
to one slot per kind for those.

#### Ownership: a liveness heartbeat, not just "does a draft exist"

A naive "does *any* draft already exist under the shared key" check breaks the moment the owning
tab goes away without cleanly unmounting (a hard refresh, closing the whole browser): the draft
content itself is intentionally kept fresh-looking (its `beforeunload` handler writes it one last
time so it can be restored later), so it would look "still owned" forever, permanently blocking
every other tab from ever enabling auto-save for that entity type.

To answer "does the owning tab actually still exist" instead of "does a draft still exist",
ownership is tracked by a **separate heartbeat key**
(`new-forms:auto-save-heartbeat:${user}:${entityKind}:${entityType}`, value
`{ entityPermId, tabId, timestamp }`), decoupled from the draft content key:

**Ownership identity is `tabId`, not `entityPermId`.** A CREATE-mode `permId` is a small counter
reset per page load (`AppController.objectNew` counts currently-open tabs of that type *within
that page's own in-memory state*) - it is not synchronized across separate browser tabs/windows.
Two independently opened tabs each creating the first entity of a given type will therefore both
compute the identical tmp permId (e.g. `"1-newObject"`), which would make them indistinguishable
to the ownership check. `tabId` (`utils/tabIdentityUtil.ts`, `getTabInstanceId()`) is instead a
random id generated once per browser tab and cached in that tab's `sessionStorage` - stable across
reloads/navigation within the tab, but never shared with (or colliding with) another tab, which is
what ownership arbitration actually needs. `entityPermId` is still written to the heartbeat, but
only for debugging - it plays no role in the "taken by another" comparison.
- While a CREATE-mode tab is actively driving the draft flow (`isDraftFlowEnabled`), it writes its
  own `entityPermId` + `Date.now()` to the heartbeat key immediately and then every
  `HEARTBEAT_INTERVAL_MS` (5s, same cadence as the draft auto-save).
- The heartbeat is **released** (removed, if it's still this tab's own) both on clean React
  unmount (tab closed via the UI) and on `beforeunload` (covers a full page refresh/navigation) -
  so ownership is freed essentially instantly in both of those cases.
- As a fallback for the rarer case neither of those fires (browser/OS crash), a heartbeat older
  than `HEARTBEAT_STALE_AFTER_MS` (15s - three missed beats) is treated as abandoned.

`isSlotTakenByAnother()` reads this heartbeat and reports "taken" only if it's fresh **and**
belongs to a different `entityPermId` than the current tab.

#### Two different reactions, only one of them user-visible

- **On mount / preference load**: if the shared preference is `'true'` but `isSlotTakenByAnother()`
  is true, this tab's `isAutoSaveEnabled` is initialized to `false` **silently** - no toast. Opening
  a second "new entity" tab of the same type while the first is actively auto-saving should not
  itself produce a warning; it should just not auto-enable in the second tab.
- **On an explicit `setAutoSaveEnabled(true)` call** (the user actually toggling the switch on):
  the same `isSlotTakenByAnother()` check runs, but if it's still taken, the call is a no-op (the
  switch stays off) and a warning toast is raised: *"Auto saved turned off because a new entity of
  the same type is already being auto saved."* This is also what happens if the user retries after
  the first attempt - if the first entity's tab is still live, it's blocked again with the same
  toast; once that tab's heartbeat has actually gone (closed, refreshed, or timed out), the retry
  succeeds and this tab claims the slot.

Dirty-field diffing for CREATE works unmodified: `originalForm` is set to the freshly-constructed
default form on load and never mutated by `updateField`, so diffing `form.fields` against it
already isolates exactly the fields the user typed — no special-casing needed in `useAutoSave`.

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
