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
package org.mycore.vidconv.frontend;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.mycore.vidconv.common.config.Configuration;
import org.mycore.vidconv.selenium.SeleniumTestCase;
import org.openqa.selenium.By;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class TestSharedHelpers extends SeleniumTestCase {

    protected static final Configuration CONFIG = Configuration.instance();

    private static final boolean USE_HASH = true;

    public void switchNavLink(String link) {
        switchNavLink(link, null, false);
    }

    public void switchNavLink(String link, boolean goHome) {
        switchNavLink(link, null, goHome);
    }

    public void switchNavLink(String link, String menu) {
        switchNavLink(link, menu, false);
    }

    public void switchNavLink(String link, String menu, boolean goHome) {
        waitForInvisibleElement(By.id("spinner"), 3);

        if (goHome) {
            switchNavLink("home");
            waitForElement(By.id("home"));
        }

        if (menu != null) {
            String xp = "//header[@id='header']//a[@aria-expanded='false' and @data-bs-toggle='collapse' and @href='#"
                    + menu
                    + "']|//nav[@id='sidebar']//a[@aria-expanded='false' and @data-bs-toggle='collapse' and @href='#"
                    + menu
                    + "']";
            if (hasElement(By.xpath(xp)) != null) {
                waitAndClick(By.xpath(xp));
            }
        }

        String href = (USE_HASH ? "#/" : "/") + Arrays.stream(link.split("\\.")).collect(Collectors.joining("/"));
        String refId = "ref-" + Arrays.stream(link.split("\\.")).collect(Collectors.joining("-"));

        waitAndClick(By.xpath("//header[@id='header']//a[@uisref='" + link + "' or @id='" + refId + "' or @href='"
                + href + "']|//nav[@id='sidebar']//a[@uisref='" + link + "' or @id='" + refId + "' or @href='" + href
                + "']"));
    }

    public void invokeAction(String ref) {
        invokeAction(ref, null);
    }

    public void invokeAction(String ref, String menu) {
        waitForInvisibleElement(By.id("spinner"), 3);

        if (menu != null) {
            String xp = "//*[@aria-expanded='false' and @data-bs-toggle='dropdown' and @id='" + menu + "']";
            if (hasElement(By.xpath(xp)) != null) {
                waitAndClick(By.xpath(xp));
            }
        }

        String href = "/" + Arrays.stream(ref.split("\\.")).collect(Collectors.joining("/"));
        String[] elmSel = new String[] { "*[uisref='" + ref + "']", "*[href='" + href + "']" };
        String sel = Optional.ofNullable(menu)
                .map(m -> Arrays.stream(elmSel).map(e -> "#" + m + " + * > " + e).collect(Collectors.joining(",")))
                .orElseGet(() -> Arrays.stream(elmSel).collect(Collectors.joining(",")));

        waitAndClick(By.cssSelector(sel));
    }

}
