package ch.systemsx.cisd.openbis.jstest.layout;

import ch.systemsx.cisd.openbis.jstest.page.OpenbisJsCommonWebapp;
import ch.systemsx.cisd.openbis.uitest.selenium.Pages;
import ch.systemsx.cisd.openbis.uitest.selenium.SeleniumTest;
import ch.systemsx.cisd.openbis.uitest.selenium.UrlLocation;
import org.openqa.selenium.WebDriver;

import java.net.MalformedURLException;
import java.net.URL;

public class OpenbisJsWebappLocation implements UrlLocation<OpenbisJsCommonWebapp>
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
                webappUrl = baseUrl + "webapp/openbis-test/?webapp-code=openbis-test";
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
