package ch.ethz.sis.elnlims.dropbox;

import java.nio.file.Path;
import java.util.Properties;

import ch.ethz.sis.openbis.generic.OpenBIS;

public interface IFolderListener
{

    void configure(Properties properties);

    void process(OpenBIS openBIS, Path incoming);

}
