package ch.openbis.drive.gui.maincontent.syncjobs;

import ch.openbis.drive.model.SyncJob;

import java.util.List;

public record SyncJobDialogContext(
        SyncJob toBeModified,
        List<SyncJob> currentSyncJobs,
        SyncJobSessionChoiceResult sessionChoiceResult,
        long acceptedValidityMillisLeftForPATs
) {}
