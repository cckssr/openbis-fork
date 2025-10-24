package ch.ethz.sis.openbis.generic.foldermonitor.v3;

import java.nio.file.Path;
import java.util.Properties;

public interface FolderMonitorTask
{

    void configure(Properties properties) throws Exception;

    void process(Path incoming) throws Exception;

}
