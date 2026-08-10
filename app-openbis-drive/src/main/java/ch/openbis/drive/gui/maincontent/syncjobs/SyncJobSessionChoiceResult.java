package ch.openbis.drive.gui.maincontent.syncjobs;

import ch.openbis.drive.util.OpenBISQueryUtil;

public record SyncJobSessionChoiceResult (
  boolean next,
  OpenBISQueryUtil.AvailableSession availableSession
) {}
