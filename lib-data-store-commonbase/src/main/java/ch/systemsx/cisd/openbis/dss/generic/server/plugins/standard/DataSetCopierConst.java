package ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard;

public class DataSetCopierConst
{

    public static final String DESTINATION_KEY = "destination";

    public static final String RSYNC_PASSWORD_FILE_KEY = "rsync-password-file";

    public static final String RENAME_TO_DATASET_CODE_KEY = "rename-to-dataset-code";

    public static final String HARD_LINK_COPY_KEY = "hard-link-copy";

    public static final String GFIND_EXEC = "find";

    public static final String RSYNC_EXEC = "rsync";

    public static final String LN_EXEC = "ln";

    public static final String SSH_EXEC = "ssh";

    public static final long SSH_TIMEOUT_MILLIS = 15 * 1000; // 15s

    public static final String ALREADY_EXIST_MSG = "already exist";

    public static final String COPYING_FAILED_MSG = "copying failed";

}
