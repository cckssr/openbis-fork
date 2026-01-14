import { useCallback, useEffect, useMemo, useState } from 'react'
import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts'
import { FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts'
import { useAutoSave } from '@src/js/components/database/new-forms/hooks/useAutoSave.tsx'
import { useAutoSaveRestore } from '@src/js/components/database/new-forms/hooks/useAutoSaveRestore.tsx'

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
  permId: string
  /** Callback invoked when a saved draft is restored */
  onRestore: (savedData: Form) => void
}

interface UseEntityAutoSaveFlowResult {
  isAutoSaveEnabled: boolean
  setAutoSaveEnabled: (enabled: boolean) => void
  /** Render-time overrides for `EntityForm` action definitions (no form mutation). */
  actionOverrides: Record<string, any>
  /** Clears the saved draft (form data) from localStorage */
  clearStorage: () => void
}

/**
 * Feature hook that encapsulates the full auto-save flow for a single form entity:
 * - per-entity user preference (localStorage, defaults to false; removes key when false)
 * - saving dirty fields (useAutoSave)
 * - restoration (useAutoSaveRestore)
 * - UI glue via actionOverrides for the auto-save switch
 *
 * This keeps the Provider clean and avoids duplicating state in `form.actions`.
 */
export function useEntityAutoSaveFlow({
  form,
  originalForm,
  mode,
  user,
  entityKind,
  permId,
  onRestore
}: UseEntityAutoSaveFlowProps): UseEntityAutoSaveFlowResult {
  // ----- Preference (per entity) -----
  const preferenceKey = useMemo(() => {
    return `new-forms:auto-save-enabled:${user}:${entityKind}:${permId || 'unknown'}`
  }, [user, entityKind, permId])

  // ----- Draft storage (per entity) -----
  const storageKey = useMemo(() => {
    // Draft (auto-saved form data) key — keep separate from preference key.
    return `form-data-${entityKind}-${permId || 'new'}-${user}`
  }, [entityKind, permId, user])

  const [isAutoSaveEnabled, setAutoSaveEnabledState] = useState(false)

  // Load preference on identity change
  useEffect(() => {
    try {
      const raw = localStorage.getItem(preferenceKey)
      setAutoSaveEnabledState(raw === 'true')
    } catch {
      setAutoSaveEnabledState(false)
    }
  }, [preferenceKey])

  // Persist preference (store only when true; remove when false)
  const setAutoSaveEnabled = useCallback(
    (enabled: boolean) => {
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
    [preferenceKey, storageKey]
  )

  const isDraftFlowEnabled = Boolean(
    isAutoSaveEnabled && mode === FormMode.EDIT && form && originalForm
  )

  const { loadFromStorage, clearStorage } = useAutoSave({
    form,
    originalForm,
    storageKey,
    isEnabled: isDraftFlowEnabled,
    interval: 5000
  })

  useAutoSaveRestore({
    form,
    mode,
    isEnabled: isAutoSaveEnabled,
    loadFromStorage,
    onRestore,
    onClearStorage: clearStorage
  })

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
    clearStorage
  }
}


