/*
 * Copyright ETH 2008 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.systemsx.cisd.openbis.generic.shared.util;

/**
 * @author Pawel Glyzewski
 */
public class TestInstanceHostUtils
{
    private static final String OPENBIS_URL = "http://localhost";

    public static int getOpenBISPort()
    {
        return 8800 + getProjectNumber() + 8;
    }

    public static int getOpenBISProxyPort()
    {
        return getOpenBISPort() + 1000;
    }

    public static String getOpenBISUrl()
    {
        return OPENBIS_URL + ":" + getOpenBISPort();
    }

    public static String getOpenBISProxyUrl()
    {
        return OPENBIS_URL + ":" + getOpenBISProxyPort();
    }

    public static String getOpenBISPath()
    {
        return "/openbis/openbis";
    }

    public static int getDSSPort()
    {
        return 8800 + getProjectNumber() + 9;
    }

    public static String getDSSUrl()
    {
        return OPENBIS_URL + ":" + getDSSPort();
    }

    public static String getDSSPath()
    {
        return "/datastore_server";
    }

    public static int getAFSPort()
    {
        return 8800 + getProjectNumber() + 7;
    }

    public static int getAFSProxyPort()
    {
        return getAFSPort() + 1000;
    }

    public static String getAFSUrl()
    {
        return OPENBIS_URL + ":" + getAFSPort();
    }

    public static String getAFSProxyUrl()
    {
        return OPENBIS_URL + ":" + getAFSProxyPort();
    }

    public static String getAFSPath()
    {
        return "/afs-server";
    }

    public static int getRoCratePort()
    {
        return 8800 + getProjectNumber() + 6;
    }

    public static String getRoCrateUrl()
    {
        return OPENBIS_URL + ":" + getRoCratePort();
    }

    private static int getProjectNumber()
    {
        String projectName = System.getProperty("ant.project.name", "");

        if (projectName.equals("server-application-server"))
        {
            return 0;
        } else if (projectName.equals("server-original-data-store"))
        {
            return 10;
        } else if (projectName.equals("screening"))
        {
            return 20;
        } else if (projectName.equals("deep_sequencing_unit"))
        {
            return 30;
        } else if (projectName.equals("openbis_oai_pmh"))
        {
            return 40;
        } else if (projectName.equals("test-integration"))
        {
            return 50;
        }

        return 80;
    }
}
