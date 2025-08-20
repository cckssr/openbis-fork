/*
 * Copyright ETH 2012 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.jstest.layout;

import ch.systemsx.cisd.openbis.jstest.page.OpenbisJsCommonWebapp;
import ch.systemsx.cisd.openbis.uitest.selenium.Pages;
import ch.systemsx.cisd.openbis.uitest.selenium.SeleniumTest;
import ch.systemsx.cisd.openbis.uitest.selenium.UrlLocation;
import org.openqa.selenium.WebDriver;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author pkupczyk
 */
public class OpenbisV3JsWebappLocation implements UrlLocation<OpenbisJsCommonWebapp>
{
    private String webappUrl;

    @Override
    public void moveTo(Pages pages)
    {
        WebDriver driver = SeleniumTest.driver;
        String currentUrl = driver.getCurrentUrl();

        if (webappUrl == null)
        {
            try
            {
                //TODO check if this is always true
                URL url = new URL(currentUrl);
                String baseUrl = url.getProtocol() + "://" + url.getHost()
                        + ((url.getPort() != -1) ? ":" + url.getPort() : "") + "/openbis/";
                webappUrl = baseUrl + "webapp/openbis-v3-api-test/?webapp-code=openbis-v3-api-test";
            }
            catch (MalformedURLException e)
            {
                throw new RuntimeException("Failed to build webapp URL", e);
            }
        }

        driver.get(webappUrl);
    }

    @Override
    public String getTabName()
    {
        return null;
    }

    @Override
    public Class<OpenbisJsCommonWebapp> getPage()
    {
        return OpenbisJsCommonWebapp.class;
    }

    @Override
    public String getUrl()
    {
        return webappUrl;
    }
}