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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.safari.SafariDriver;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class SeleniumDriverFactory {

	public final static String CHROME_DRIVER = "chrome";

	public final static String EDGE_DRIVER = "edge";

	public final static String FIREFOX_DRIVER = "firefox";

	public final static String IE_DRIVER = "ie";

	public final static String OPERA_DRIVER = "opera";

	public final static String PHANTOMJS_DRIVER = "phantomjs";

	public final static String SAFARI_DRIVER = "safari";

	private final static String SELENIUM_DRIVER_ENV = "SELENIUM_DRIVER";

	private final static String SELENIUM_DRIVER_PROP = "seleniumDriver";

	private final static String SELENIUM_DRIVER_ENV_ARGS = "SELENIUM_DRIVER_ARGS";

	private final static String SELENIUM_DRIVER_PROP_ARGS = "seleniumDriverArgs";

	private final String driverName;

	private final List<String> driverArguments;

	public SeleniumDriverFactory() {
		this(Optional.ofNullable(System.getenv(SELENIUM_DRIVER_ENV))
				.orElseGet(() -> System.getProperty(SELENIUM_DRIVER_PROP)),
				Optional.ofNullable(System.getenv(SELENIUM_DRIVER_ENV_ARGS))
						.orElseGet(() -> System.getProperty(SELENIUM_DRIVER_PROP_ARGS)));
	}

	public SeleniumDriverFactory(String driverName, String driverArguments) {
		this.driverName = driverName;
		this.driverArguments = Optional.ofNullable(driverArguments).map(args -> Arrays.asList(args.split(" ")))
				.orElse(null);

		if (CHROME_DRIVER.equalsIgnoreCase(driverName)) {
			WebDriverManager.chromedriver().setup();
		} else if (EDGE_DRIVER.equalsIgnoreCase(driverName)) {
			WebDriverManager.edgedriver().setup();
		} else if (IE_DRIVER.equalsIgnoreCase(driverName)) {
			WebDriverManager.iedriver().setup();
		} else if (OPERA_DRIVER.equalsIgnoreCase(driverName)) {
			WebDriverManager.operadriver().setup();
		} else if (PHANTOMJS_DRIVER.equalsIgnoreCase(driverName)) {
			WebDriverManager.phantomjs().setup();
		} else if (SAFARI_DRIVER.equalsIgnoreCase(driverName)) {
			LogManager.getLogger().warn("See https://github.com/SeleniumHQ/selenium/wiki/SafariDriver");
		} else {
			WebDriverManager.firefoxdriver().setup();
		}
	}

	public WebDriver driver() {
		if (CHROME_DRIVER.equalsIgnoreCase(driverName)) {
			ChromeOptions options = new ChromeOptions();
			Optional.ofNullable(driverArguments).ifPresent(options::addArguments);
			return new ChromeDriver(options);
		} else if (EDGE_DRIVER.equalsIgnoreCase(driverName)) {
			return new EdgeDriver();
		} else if (IE_DRIVER.equalsIgnoreCase(driverName)) {
			return new EdgeDriver();
		} else if (OPERA_DRIVER.equalsIgnoreCase(driverName)) {
			return new OperaDriver();
		} else if (PHANTOMJS_DRIVER.equalsIgnoreCase(driverName)) {
			WebDriver driver = new PhantomJSDriver();

			// fix window size
			driver.manage().window().setSize(new Dimension(1280, 1024));

			return driver;
		} else if (SAFARI_DRIVER.equalsIgnoreCase(driverName)) {
			return new SafariDriver();
		}

		FirefoxOptions options = new FirefoxOptions();
		Optional.ofNullable(driverArguments).ifPresent(options::addArguments);
		return new FirefoxDriver(options);
	}
}
