const TAB_INSTANCE_ID_KEY = 'new-forms:tab-instance-id';

function generateId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

/**
 * Returns an id that is stable for the lifetime of this browser tab (survives reloads/
 * navigation within the tab via sessionStorage) but unique across separate tabs/windows,
 * even ones showing the same page at the same time. Unlike a CREATE-mode form's tmp permId
 * (a small in-memory counter reset per page load - see AppController.objectNew), this can't
 * collide between two independently opened tabs, so it's safe to use as the "who am I" identity
 * for cross-tab coordination (e.g. the CREATE-mode auto-save slot heartbeat).
 */
export function getTabInstanceId(): string {
  try {
    const existing = sessionStorage.getItem(TAB_INSTANCE_ID_KEY);
    if (existing) {
      return existing;
    }
    const id = generateId();
    sessionStorage.setItem(TAB_INSTANCE_ID_KEY, id);
    return id;
  } catch {
    // sessionStorage unavailable (private mode / quota) - fall back to a per-call id. Cross-tab
    // arbitration degrades gracefully to "always treat as a different tab" in that case.
    return generateId();
  }
}
