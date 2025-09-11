/*
 *  Copyright ETH 2012 - 2025 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package ch.systemsx.cisd.openbis.uitest.selenium;

import ch.ethz.sis.shared.log.standard.handlers.ConsoleHandler;
import ch.ethz.sis.shared.log.standard.handlers.CustomWriterHandler;
import ch.ethz.sis.shared.log.standard.handlers.PatternFormatter;
import ch.ethz.sis.shared.log.standard.core.Level;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.support.ui.FluentWait;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.not;

public abstract class SeleniumTest
{
    public static int IMPLICIT_WAIT = 30;

    public static String ADMIN_USER = "selenium";

    public static String ADMIN_PASSWORD = "selenium4CISD";

    public static WebDriver driver;

    private static UidGenerator uid;

    private static Application openbis;

    private static Ui ui;

    private static Ui defaultUi;

    private static Pages pages;

    private static String asUrl;

    private static String dssUrl;

    private static String dssUrl2;

    private static String startPage;

    private static Console console = new Console();

    @BeforeSuite
    public void initialization() throws Exception
    {
        deleteOldScreenShots();
        initializeLogging();
        uid = new DictionaryUidGenerator(new File("resource/corncob_lowercase.txt"));

        asUrl = getSystemPropertyOrNull("ui-test.as-url");
        dssUrl = getSystemPropertyOrNull("ui-test.dss-url");
        dssUrl2 = getSystemPropertyOrNull("ui-test.dss-url2");
        startPage = getSystemPropertyOrNull("ui-test.start-page");

        /* Run against sprint server */
        /*
         * asUrl = "https://sprint-openbis.ethz.ch/openbis"; dssUrl = "https://sprint-openbis.ethz.ch"; startPage = asUrl;
         */

        /* Run against local DSS and local AS in development mode */
        /* Firefox profile should be one with GWT dev mode plugin available */
        /*
         * asUrl = "http://127.0.0.1:8888"; dssUrl = "http://127.0.0.1:8889"; startPage = asUrl +
         * "/ch.systemsx.cisd.openbis.OpenBIS/index.html?gwt.codesvr=127.0.0.1:9997"; System.setProperty("webdriver.firefox.profile", "default");
         */

        if (asUrl == null)
        {
            asUrl = startApplicationServer();

            if (startPage == null)
            {
                startPage = asUrl;
            }
        }

        if (dssUrl == null)
        {
            dssUrl = startDataStoreServer();
        }

        if (dssUrl2 == null)
        {
            dssUrl2 = startDataStoreServer2();
        }

        System.out.println("asUrl: " + asUrl);
        System.out.println("dssUrl: " + dssUrl);
        System.out.println("dssUrl2: " + dssUrl2);
        System.out.println("startPage: " + startPage);

        pages = new Pages();
        openbis = new Application(asUrl, dssUrl, dssUrl2, pages, console);

    }

    private String getSystemPropertyOrNull(String propertyName)
    {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue == null || propertyValue.trim().length() == 0)
        {
            return null;
        } else
        {
            return propertyValue;
        }
    }

    protected String startApplicationServer() throws Exception
    {
        return StartApplicationServer.go();
    }

    protected String startDataStoreServer() throws Exception
    {
        return StartDataStoreServer.go();
    }

    protected String startDataStoreServer2() throws Exception
    {
        // FIXME dss2 is started together with dss1 in StartDataStoreServer.go()
        return "http://localhost:10002";
    }

    private void startWebDriver()
    {
        if (driver == null)
        {
            FirefoxOptions opts = new FirefoxOptions();
            // If you use headless option selenium will work fine but Firefox won't be visible on the screen.
            //opts.addArguments("--headless");
            opts.addArguments("--headless", "--window-size=1920,1080");
            LoggingPreferences logPrefs = new LoggingPreferences();
            logPrefs.enable(LogType.BROWSER, java.util.logging.Level.ALL);
            opts.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);
            driver = new FirefoxDriver(opts);
            setImplicitWaitToDefault();
            driver.manage().deleteAllCookies();
        }
        driver.get(startPage);
    }

    private void deleteOldScreenShots()
    {
        delete(new File("targets/dist"));

    }

    private void initializeLogging()
    {
        Logger rootLogger = Logger.getRootLogger();
        // TODO check why it expects no handlers
        rootLogger.removeAllHandlers();
        if (rootLogger.getHandlers().length > 0)
        {
            throw new IllegalStateException("log4j has appenders!");
        }
        rootLogger.setLevel(Level.INFO);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new PatternFormatter(
                "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%t]: %m%n"));
        rootLogger.addHandler(handler);

        CustomWriterHandler appender =
                new CustomWriterHandler(console);
        appender.setFormatter(new PatternFormatter(
                "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%t]: %m%n"));
        rootLogger.addHandler(appender);
    }

    @AfterSuite
    public void closeBrowser() throws Exception
    {
        if (driver != null)
        {
            driver.quit();
        }
    }

    protected Pages browser()
    {
        return pages;
    }

    @BeforeMethod(alwaysRun = true)
    public void initPageProxy(Method method) throws Exception
    {
        System.out.println("--- " + method.getDeclaringClass().getSimpleName() + "."
                + method.getName() + "() STARTS ---");

        ScreenShotter shotter;
        if (driver != null)
        {
            shotter =
                    new FileScreenShotter((TakesScreenshot) driver, "targets/dist/"
                            + this.getClass().getSimpleName() + "/" + method.getName());
        } else
        {
            shotter = new ScreenShotter()
            {
                @Override
                public void screenshot()
                {
                }
            };
        }

        System.out.println("SCREEN SHOTTER: " + shotter);
        pages.setScreenShotter(shotter);
    }

    @AfterMethod(alwaysRun = true)
    public void takeScreenShot(Method method) throws IOException
    {
        pages.screenshot();
        System.out.println("--- " + method.getDeclaringClass().getSimpleName() + "."
                + method.getName() + "() ENDS ---");
    }

    @AfterTest
    public void takeScreenShot()
    {
        pages.screenshot();
    }

    private void delete(File f)
    {
        if (f.isDirectory())
        {
            for (File c : f.listFiles())
                delete(c);
        }
        f.delete();
    }

    public <T> T using(Void anything, T t)
    {
        ui = defaultUi;
        return t;
    }


    public User user(User user)
    {
        openbis.changeLogin(user);

        if (ui.equals(Ui.WEB))
        {
            TabBar tabs = pages.load(TabBar.class);
            for (String tab : tabs.getTabs())
            {
                pages.load(TabBar.class).closeTab(tab);
            }

        }

        return user;
    }

    public static void setImplicitWait(long amount, TimeUnit unit)
    {
        driver.manage().timeouts().implicitlyWait(amount, unit);
    }

    public static void setImplicitWaitToDefault()
    {
        driver.manage().timeouts().implicitlyWait(IMPLICIT_WAIT, TimeUnit.SECONDS);
    }

    public boolean tabsContain(Location<?> location)
    {
        TabBar bar = assumePage(TabBar.class);
        return bar.getTabs().contains(location.getTabName());
    }

    public <T> T switchTabTo(Location<T> location)
    {
        TabBar bar = assumePage(TabBar.class);
        bar.selectTab(location.getTabName());
        return assumePage(location.getPage());
    }

    public void closeTab(Location<?> location)
    {
        TabBar bar = assumePage(TabBar.class);
        bar.closeTab(location.getTabName());
    }

    public <T> T assumePage(Class<T> pageClass)
    {
        return pages.load(pageClass);
    }

    public Void gui()
    {
        ui = Ui.WEB;
        return null;
    }

    public Void publicApi()
    {
        ui = Ui.PUBLIC_API;
        return null;
    }

    public Void dummyApplication()
    {
        ui = Ui.DUMMY;
        return null;
    }

    protected void useGui()
    {
        startWebDriver();
        ui = Ui.WEB;
        defaultUi = ui;
    }

    protected void usePublicApi()
    {
        ui = Ui.PUBLIC_API;
        defaultUi = ui;
    }

    public static void mouseOver(WebElement element)
    {
        pages.screenshot();
        Actions builder = new Actions(SeleniumTest.driver);
        builder.moveToElement(element).build().perform();
    }

    public static <U extends Widget> U initializeWidget(Class<U> widgetClass, WebElement context)
    {
        return pages.initializeWidget(widgetClass, context, false);
    }

    protected static String randomValue()
    {
        return UUID.randomUUID().toString();
    }

    public static void waitForVisibilityOf(String id) {
        WebElement webElement = SeleniumTest.driver.findElement(By.id(id));
        new FluentWait<WebElement>(webElement)
                .withTimeout(Duration.of(30, ChronoUnit.SECONDS))
                .pollingEvery(Duration.of(100, ChronoUnit.MILLIS))
                .until(
                        new Function<WebElement, Boolean>()
                        {

                            @Override
                            public Boolean apply(WebElement webElement)
                            {
                                return webElement.isDisplayed();
                            }
                        });
    }
}
