package ch.ethz.sis.foldermonitor;

import java.nio.file.Path;
import java.util.Properties;

public interface FolderMonitorTask
{

    void configure(Properties properties) throws Exception;

    void process(Path incoming) throws Exception;

}
