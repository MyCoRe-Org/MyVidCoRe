/*
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 3
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */
package org.mycore.vidconv.selenium;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mycore.vidconv.Application;
import org.mycore.vidconv.common.config.ConfigurationDirSetup;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class SeleniumTestCase {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static final long MAX_WAIT_TIME = 30;

    protected static final long WAIT_TIME = 3000;

    protected static final long KEY_STROKE_DELAY = 75;

    protected static final long DEBOUNCE_DELAY = 300;

    protected static final int NUM_RETRIES = 3;

    protected static SeleniumDriverFactory factory;

    private static final int PORT_MIN = 10000;

    private static final int PORT_MAX = 20000;
    
    private static final int SCROLL_OFFSET = 100;

    protected static int port;

    protected WebDriver driver;

    protected String baseURL;

    private byte[] screenShot;

    private String sourceHTML;

    private String testURL;

    @Rule
    public TestWatcher errorLogger = new TestWatcher() {

        @Override
        protected void failed(Throwable e, Description description) {
            if (description.isTest()) {
                String className = description.getClassName();
                int p = className.lastIndexOf(".");
                if (p > 0 && (p < className.length() - 1))
                    className = className.substring(p + 1);
                String resultFolder = System.getProperty("ResultFolder", "target/result");

                File failedTestClassDirectory = new File(resultFolder, className);
                String child = description.getMethodName();
                if (child == null || child.isEmpty())
                    child = description.getMethodName();
                File failedTestDirectory = new File(failedTestClassDirectory, child);
                failedTestDirectory.mkdirs();
                if (e != null) {
                    File error = new File(failedTestDirectory, "error.txt");
                    try (FileOutputStream fout = new FileOutputStream(error);
                            OutputStreamWriter osw = new OutputStreamWriter(fout, "UTF-8");
                            PrintWriter pw = new PrintWriter(osw)) {
                        pw.println(testURL);
                        e.printStackTrace(pw);
                    } catch (IOException e1) {
                        throw new RuntimeException(e1);
                    }
                }
                File screenshot = new File(failedTestDirectory, "screenshot.png");
                try (FileOutputStream fout = new FileOutputStream(screenshot)) {
                    LOGGER.info("Saving screenshot to {}", screenshot.getAbsolutePath());
                    fout.write(screenShot);
                } catch (IOException e1) {
                    throw new RuntimeException(e1);
                }
                File html = new File(failedTestDirectory, "dom.html");
                try (FileOutputStream fout = new FileOutputStream(html);
                        OutputStreamWriter osw = new OutputStreamWriter(fout, "UTF-8")) {
                    LOGGER.info("Saving DOM to {}", html.getAbsolutePath());
                    osw.write(sourceHTML);
                } catch (IOException e1) {
                    throw new RuntimeException(e1);
                }
            }
            super.failed(e, description);
        }
    };

    @BeforeClass
    public static void setupClass() throws IOException {
        Path configDir = Paths.get(System.getProperty("java.io.tmpdir"), SeleniumTestCase.class.getSimpleName());
        if (Files.notExists(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        startApp(configDir);
    }

    @AfterClass
    public static void tearDownClass() {
        Application.exit();
    }

    protected static void startApp(Path configDir) throws IOException {
        Random rand = new Random();
        port = rand.nextInt((PORT_MAX - PORT_MIN) + 1) + PORT_MIN;

        Path inputDir = Files.createTempDirectory("input");
        Path outputDir = Files.createTempDirectory("output");
        Path tempDir = Files.createTempDirectory("temp");

        Application.main(new String[] { "--configDir", configDir.toAbsolutePath().toString(), "--watchDir",
                inputDir.toAbsolutePath().toString(), "--outputDir",
                outputDir.toAbsolutePath().toString(), "--tempDir",
                tempDir.toAbsolutePath().toString(), "--port",
                Integer.toString(port) });

        factory = new SeleniumDriverFactory();
    }

    @Before
    public void setUp() throws Exception {
        ConfigurationDirSetup.loadProperties();

        baseURL = "http://" + getHostName() + ":" + Integer.toString(port) + "/";
        driver = factory.driver();
        driver.get(baseURL);
    }

    @After
    public void tearDown() throws Exception {
        takeDriverState();

        if (driver != null) {
            driver.quit();
        }
    }

    public WebElement hasElement(By by) {
        try {
            return driver.findElement(by);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public WebElement waitAndClick(By by) {
        WebDriverWait wait = new WebDriverWait(driver, MAX_WAIT_TIME);
        WebElement elm = null;

        int retry = 0;
        do {
            try {
                if (wait.until(ExpectedConditions.and(ExpectedConditions.visibilityOfElementLocated(by),
                        ExpectedConditions.elementToBeClickable(by), webDriver -> isReady(webDriver)))) {
                    elm = driver.findElement(by);
                    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0," + (elm.getLocation().y - SCROLL_OFFSET) + ")");
                    elm.click();
                }
            } catch (WebDriverException e) {
                LOGGER.warn(e.getMessage(), e);
                elm = null;
                try {
                    Thread.sleep(WAIT_TIME);
                } catch (InterruptedException ex) {
                }
            }
            retry++;
        } while (elm == null && retry < NUM_RETRIES);

        return elm;
    }

    public WebElement waitAndSelectByValue(By by, String value) {
        WebDriverWait wait = new WebDriverWait(driver, MAX_WAIT_TIME);
        if (wait.until(ExpectedConditions.and(ExpectedConditions.visibilityOfElementLocated(by),
                ExpectedConditions.elementToBeClickable(by), webDriver -> isReady(webDriver)))) {
            WebElement elm = driver.findElement(by);
            new Select(elm).selectByValue(value);
            return elm;
        }

        return null;
    }

    public WebElement waitAndSelectByText(By by, String text) {
        WebDriverWait wait = new WebDriverWait(driver, MAX_WAIT_TIME);
        if (wait.until(ExpectedConditions.and(ExpectedConditions.visibilityOfElementLocated(by),
                ExpectedConditions.elementToBeClickable(by), webDriver -> isReady(webDriver)))) {
            WebElement elm = driver.findElement(by);
            new Select(elm).selectByVisibleText(text);
            return elm;
        }

        return null;
    }

    public WebElement waitAndSelectByIndex(By by, int index) {
        WebDriverWait wait = new WebDriverWait(driver, MAX_WAIT_TIME);
        if (wait.until(ExpectedConditions.and(ExpectedConditions.visibilityOfElementLocated(by),
                ExpectedConditions.elementToBeClickable(by), webDriver -> isReady(webDriver)))) {
            WebElement elm = driver.findElement(by);
            new Select(elm).selectByIndex(index);
            return elm;
        }

        return null;
    }

    public WebElement waitForElement(By by) {
        WebDriverWait wait = new WebDriverWait(driver, MAX_WAIT_TIME);
        if (wait.until(ExpectedConditions.and(ExpectedConditions.visibilityOfElementLocated(by),
                webDriver -> isReady(webDriver)))) {
            WebElement elm = driver.findElement(by);
            return elm;
        }

        return null;
    }

    public List<WebElement> waitForElements(By by) {
        WebDriverWait wait = new WebDriverWait(driver, MAX_WAIT_TIME);
        if (wait.until(ExpectedConditions.and(ExpectedConditions.visibilityOfElementLocated(by),
                webDriver -> isReady(webDriver)))) {
            List<WebElement> elm = driver.findElements(by);
            return elm;
        }

        return null;
    }

    public WebElement waitForInvisibleElement(By by) {
        WebDriverWait wait = new WebDriverWait(driver, MAX_WAIT_TIME);
        if (wait.until(ExpectedConditions.and(ExpectedConditions.invisibilityOfElementLocated(by),
                webDriver -> isReady(webDriver)))) {
            WebElement elm = driver.findElement(by);
            return elm;
        }

        return null;
    }

    public WebElement waitForInvisibleElement(By by, int retries) {
        for (int i = 0; i < retries; i++) {
            WebElement elm = waitForInvisibleElement(by);
            if (elm != null) {
                return elm;
            }
        }

        return null;
    }

    public WebElement waitForElementContainsText(By by, String text) {
        WebDriverWait wait = new WebDriverWait(driver, MAX_WAIT_TIME);
        if (wait.until(ExpectedConditions.and(ExpectedConditions.visibilityOfElementLocated(by),
                webDriver -> isReady(webDriver)))) {
            return driver.findElements(by).stream().filter(e -> e.getText().contains(text)).findFirst().orElse(null);
        }

        return null;
    }

    public WebElement reloadRetry(Callable<WebElement> callable) {
        return reloadRetry(callable, NUM_RETRIES);
    }

    public WebElement reloadRetry(Callable<WebElement> callable, int numRetries) {
        WebDriverWait wait = new WebDriverWait(driver, MAX_WAIT_TIME);
        WebElement elm = null;

        int retry = 0;
        do {
            try {
                if (wait.until(webDriver -> isReady(webDriver))) {
                    elm = callable.call();
                }
            } catch (WebDriverException e) {
                LOGGER.warn(e.getMessage(), e);
            } catch (Exception e) {
                LOGGER.warn(e.getMessage(), e);
            }

            if (elm == null) {
                LOGGER.info("Retry ({}) after page reload {}", retry, driver.getCurrentUrl());
                driver.navigate().refresh();
            }

            retry++;
        } while (elm == null && retry < numRetries);

        return elm;
    }

    public boolean isReady(WebDriver webDriver) {
        return ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete");
    }

    public void sendKeys(WebElement elm, CharSequence keysToSend) {
        sendKeys(elm, keysToSend, false);
    }

    public void sendKeys(WebElement elm, CharSequence keysToSend, boolean withTypeahead) {
        if (withTypeahead) {
            IntStream.range(0, keysToSend.length()).forEach(i -> {
                elm.sendKeys(keysToSend.subSequence(i, i + 1));
                try {
                    Thread.sleep(KEY_STROKE_DELAY);
                } catch (InterruptedException e) {
                }
            });

            try {
                Thread.sleep(DEBOUNCE_DELAY);
            } catch (InterruptedException e) {
            }
        } else {
            elm.sendKeys(keysToSend);
        }
    }

    private String getHostName() {
        String hostName = "localhost";
        try {
            hostName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return hostName;
    }

    private void takeDriverState() {
        sourceHTML = driver.getPageSource();
        testURL = driver.getCurrentUrl();
        screenShot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

}
