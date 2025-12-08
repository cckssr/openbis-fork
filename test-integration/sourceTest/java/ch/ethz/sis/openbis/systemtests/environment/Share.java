package ch.ethz.sis.openbis.systemtests.environment;

import java.util.Properties;

public class Share
{

    private final int shareNumber;

    private final Properties shareProperties;

    Share(int shareNumber, Properties shareProperties)
    {
        this.shareNumber = shareNumber;
        this.shareProperties = shareProperties;
    }

    public int getShareNumber()
    {
        return shareNumber;
    }

    public Properties getShareProperties()
    {
        return shareProperties;
    }
}
