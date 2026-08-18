import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts'
import { FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts'
import { useAutoSave } from '@src/js/components/database/new-forms/hooks/useAutoSave.tsx'
import { useAutoSaveRestore } from '@src/js/components/database/new-forms/hooks/useAutoSaveRestore.tsx'

const DRAFT_MAX_AGE_MS = 24 * 60 * 60 * 1000

// Liveness heartbeat for the shared CREATE-mode auto-save slot (see below). Written on the
// same cadence as the draft auto-save itself; a slot is only considered "taken" while its
// heartbeat is fresher than this.
const HEARTBEAT_INTERVAL_MS = 5000
const HEARTBEAT_STALE_AFTER_MS = 15000

const AUTO_SAVE_BLOCKED_MESSAGE =
  'Auto saved turned off because a new entity of the same type is already being auto saved'

interface UseEntityAutoSaveFlowProps {
  /** Current form state */
  form: Form | null
  /** Original form state (for dirty field detection) */
  originalForm: Form | null
  /** Current form mode */
  mode: FormMode
  /** Identity for per-entity preference & storage scoping */
  user: string
  entityKind: string
  /** The concrete entity type code (e.g. a specific sample type). Used only in CREATE mode, to
   * distinguish e.g. a "New Object" of type YEAST from one of type PLASMID - `entityKind` alone
   * ("newObject") is the same for both. Empty for kinds with no sub-type (Project, Space). */
  entityType: string
  permId: string
  /** Callback invoked when a saved draft is restored */
  onRestore: (savedData: Form) => void
  /** Callback invoked when the user explicitly tries to enable auto-save but another new
   * entity of the same type is already (live) auto-saving. Never called for the silent
   * auto-disable that happens on mount. */
  onAutoSaveBlocked?: (message: string) => void
}

interface UseEntityAutoSaveFlowResult {
  isAutoSaveEnabled: boolean
  setAutoSaveEnabled: (enabled: boolean) => void
  /** Render-time overrides for `EntityForm` action definitions (no form mutation). */
  actionOverrides: Record<string, any>
  /** Clears the saved draft (form data) from localStorage */
  clearStorage: () => void
  /** True while a CREATE-mode draft for this entity type was found and is awaiting the
   * user's restore/discard decision (see `restorePendingDraft` / `discardPendingDraft`). */
  hasPendingDraft: boolean
  /** Applies the pending draft's field values to the current form and dismisses the prompt. */
  restorePendingDraft: () => void
  /** Deletes the pending draft from storage and dismisses the prompt. */
  discardPendingDraft: () => void
  /** Dismisses the prompt without restoring or deleting the draft (e.g. dialog backdrop). */
  dismissPendingDraft: () => void
}

/**
 * Feature hook that encapsulates the full auto-save flow for a single form entity:
 * - per-entity user preference (localStorage, defaults to false; removes key when false)
 * - saving dirty fields (useAutoSave)
 * - restoration (useAutoSaveRestore)
 * - UI glue via actionOverrides for the auto-save switch
 *
 * This keeps the Provider clean and avoids duplicating state in `form.actions`.
 *
 * CREATE-mode slot sharing: a not-yet-saved entity has no stable identity, so unlike EDIT,
 * CREATE-mode preference/draft keys are scoped to `user` + `entityKind` + `entityType` (not
 * `permId`) - there is only one auto-save "slot" per concrete new-entity type, shared by every
 * open tab creating that type of entity. Only one live tab may actually own it at a time; see
 * the heartbeat mechanism below for how "live" is determined and how ownership is arbitrated.
 */
export function useEntityAutoSaveFlow({
  form,
  originalForm,
  mode,
  user,
  entityKind,
  entityType,
  permId,
  onRestore,
  onAutoSaveBlocked
}: UseEntityAutoSaveFlowProps): UseEntityAutoSaveFlowResult {
  const isCreateMode = mode === FormMode.CREATE

  // ----- Preference (per entity, or per entity-type slot in CREATE mode) -----
  // `entityKind` alone (e.g. "newObject") is the same for every concrete sample/experiment/
  // dataset type - `entityType` (e.g. "YEAST" vs "PLASMID") is the real per-type discriminator,
  // so CREATE-mode keys must include both. Kinds with no real sub-type (Project, Space) simply
  // have an empty `entityType`, which collapses back to one slot per kind - correct, since
  // there's nothing to disambiguate there.
  const preferenceKey = useMemo(() => {
    return isCreateMode
      ? `new-forms:auto-save-enabled:${user}:${entityKind}:${entityType}`
      : `new-forms:auto-save-enabled:${user}:${entityKind}:${permId || 'unknown'}`
  }, [user, entityKind, entityType, permId, isCreateMode])

  // ----- Draft storage (per entity, or per entity-type slot in CREATE mode) -----
  const storageKey = useMemo(() => {
    // Draft (auto-saved form data) key — keep separate from preference key.
    return isCreateMode
      ? `form-data-${entityKind}-${entityType}-${user}`
      : `form-data-${entityKind}-${permId || 'new'}-${user}`
  }, [entityKind, entityType, permId, user, isCreateMode])

  // ----- Slot ownership heartbeat (CREATE mode only) -----
  // Separate from the draft content key: the draft's own timestamp is refreshed on
  // `beforeunload` too (so its *content* survives a refresh for restoration), which would
  // make it look "fresh" right after a crash/refresh even though the owning tab is gone. The
  // heartbeat instead is actively released on clean unmount and on `beforeunload`, with a
  // short staleness window as a fallback for the (rarer) hard-crash case.
  const heartbeatKey = useMemo(() => {
    return `new-forms:auto-save-heartbeat:${user}:${entityKind}:${entityType}`
  }, [user, entityKind, entityType])

  // Returns the entityPermId that currently, live-ly owns the shared CREATE slot, or null.
  const readSlotOwner = useCallback((): string | null => {
    try {
      const raw = localStorage.getItem(heartbeatKey)
      if (!raw) {
        return null
      }
      const parsed = JSON.parse(raw)
      if (typeof parsed?.timestamp !== 'number' || Date.now() - parsed.timestamp > HEARTBEAT_STALE_AFTER_MS) {
        return null
      }
      return parsed?.entityPermId || null
    } catch {
      return null
    }
  }, [heartbeatKey])

  const isSlotTakenByAnother = useCallback((): boolean => {
    if (!isCreateMode) {
      return false
    }
    const owner = readSlotOwner()
    return !!owner && owner !== permId
  }, [isCreateMode, readSlotOwner, permId])

  const [isAutoSaveEnabled, setAutoSaveEnabledState] = useState(false)

  // Load preference on identity change. If another (live) new-entity tab of the same type
  // already owns the slot, silently don't enable here - no warning on mount, it only makes
  // sense once the user actually tries to turn it on (see setAutoSaveEnabled below).
  useEffect(() => {
    try {
      const raw = localStorage.getItem(preferenceKey)
      const preferredOn = raw === 'true'
      setAutoSaveEnabledState(preferredOn && !isSlotTakenByAnother())
    } catch {
      setAutoSaveEnabledState(false)
    }
  }, [preferenceKey, isSlotTakenByAnother])

  // Persist preference (store only when true; remove when false)
  const setAutoSaveEnabled = useCallback(
    (enabled: boolean) => {
      if (enabled && isSlotTakenByAnother()) {
        onAutoSaveBlocked?.(AUTO_SAVE_BLOCKED_MESSAGE)
        return
      }
      setAutoSaveEnabledState(enabled)
      try {
        if (enabled) {
          localStorage.setItem(preferenceKey, 'true')
        } else {
          localStorage.removeItem(preferenceKey)
          localStorage.removeItem(storageKey)
        }
      } catch {
        // ignore persistence failures (quota/private mode)
      }
    },
    [preferenceKey, storageKey, isSlotTakenByAnother, onAutoSaveBlocked]
  )

  const isDraftFlowEnabled = Boolean(
    isAutoSaveEnabled &&
    (mode === FormMode.EDIT || mode === FormMode.CREATE) &&
    form &&
    originalForm
  )

  // Claim/hold/release the heartbeat while this tab is actively driving the CREATE draft flow.
  useEffect(() => {
    if (!isCreateMode || !isDraftFlowEnabled) {
      return
    }

    const writeHeartbeat = () => {
      try {
        localStorage.setItem(heartbeatKey, JSON.stringify({ entityPermId: permId, timestamp: Date.now() }))
      } catch {
        // ignore persistence failures (quota/private mode)
      }
    }

    const releaseHeartbeat = () => {
      try {
        const raw = localStorage.getItem(heartbeatKey)
        if (!raw) {
          return
        }
        const parsed = JSON.parse(raw)
        if (parsed?.entityPermId === permId) {
          localStorage.removeItem(heartbeatKey)
        }
      } catch {
        // ignore
      }
    }

    writeHeartbeat()
    const handle = setInterval(writeHeartbeat, HEARTBEAT_INTERVAL_MS)
    window.addEventListener('beforeunload', releaseHeartbeat)

    return () => {
      clearInterval(handle)
      window.removeEventListener('beforeunload', releaseHeartbeat)
      releaseHeartbeat()
    }
  }, [isCreateMode, isDraftFlowEnabled, heartbeatKey, permId])

  const { loadFromStorage, clearStorage } = useAutoSave({
    form,
    originalForm,
    storageKey,
    isEnabled: isDraftFlowEnabled,
    interval: 5000,
    maxAge: DRAFT_MAX_AGE_MS
  })

  useAutoSaveRestore({
    form,
    mode,
    isEnabled: isAutoSaveEnabled,
    loadFromStorage,
    onRestore,
    onClearStorage: clearStorage
  })

  // ----- CREATE mode: offer to restore an existing draft, rather than auto-applying it -----
  // A not-yet-saved entity has no stable identity of its own, so - unlike EDIT, where a draft
  // is silently restored the moment the user re-enters EDIT mode - restoring here means
  // pulling in another tab's draft (or a previous session's, after a refresh). That should be
  // an explicit choice, not something that happens invisibly the moment the form opens.
  const [pendingDraft, setPendingDraft] = useState<Form | null>(null)
  const hasCheckedForDraftRef = useRef(false)

  useEffect(() => {
    hasCheckedForDraftRef.current = false
    setPendingDraft(null)
  }, [storageKey])

  useEffect(() => {
    if (!isCreateMode || !form || hasCheckedForDraftRef.current) {
      return
    }
    hasCheckedForDraftRef.current = true

    // Same rule as the auto-save toggle: if another live tab of this exact entity type
    // already owns the shared slot, don't offer its draft here either - it's still being
    // actively edited/auto-saved there, not an abandoned draft to recover.
    if (isSlotTakenByAnother()) {
      return
    }

    try {
      const raw = localStorage.getItem(storageKey)
      if (!raw) {
        return
      }
      const parsed = JSON.parse(raw)
      if (typeof parsed?.timestamp !== 'number' || Date.now() - parsed.timestamp > DRAFT_MAX_AGE_MS) {
        return
      }
      const savedFields: any[] = parsed?.data?.fields || []
      if (savedFields.length === 0) {
        return
      }

      // Match by the stable `name` (not `id`, which is prefixed with the tab-local tmp
      // permId and will essentially never match a draft saved by a different tab).
      let matchedAny = false
      const mergedFields = form.fields.map(field => {
        const savedField = field.name
          ? savedFields.find(sf => sf.name === field.name)
          : savedFields.find(sf => sf.id === field.id)
        if (savedField) {
          matchedAny = true
          return { ...field, value: savedField.value }
        }
        return field
      })

      if (matchedAny) {
        setPendingDraft({ ...form, fields: mergedFields })
      }
    } catch (e: any) {
      console.warn('Corrupted draft found.', e)
    }
  }, [isCreateMode, form, storageKey, isSlotTakenByAnother])

  const restorePendingDraft = useCallback(() => {
    if (pendingDraft) {
      onRestore(pendingDraft)
    }
    setPendingDraft(null)
  }, [pendingDraft, onRestore])

  const discardPendingDraft = useCallback(() => {
    clearStorage()
    setPendingDraft(null)
  }, [clearStorage])

  const dismissPendingDraft = useCallback(() => {
    setPendingDraft(null)
  }, [])

  // ----- UI glue: keep switch checked state derived from preference -----
  const actionOverrides = useMemo(() => {
    return {
      'auto-save': { value: isAutoSaveEnabled }
    }
  }, [isAutoSaveEnabled])

  return {
    isAutoSaveEnabled,
    setAutoSaveEnabled,
    actionOverrides,
    clearStorage,
    hasPendingDraft: pendingDraft !== null,
    restorePendingDraft,
    discardPendingDraft,
    dismissPendingDraft
  }
}
