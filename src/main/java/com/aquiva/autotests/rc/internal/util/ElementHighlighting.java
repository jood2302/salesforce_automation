package com.aquiva.autotests.rc.internal.util;

import com.codeborne.selenide.Selenide;
import org.apache.commons.io.IOUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.events.WebDriverListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Custom web driver listener that "highlights" <b>ALL</b> web elements found by Selenium Web Driver API.
 * Works by coloring the found element for a short period of time.
 * This maybe useful during a test debugging.
 * <p></p>
 * <b> Warning: if this feature is ON, it considerably slows down the test execution!
 * (makes it ~2 times slower, even for a standard 200 ms for highlight duration).
 * So, don't use it in daily test runs, only for individual (or small-scale) test debugging! </b>
 * <p></p>
 */
public class ElementHighlighting implements WebDriverListener {
    /**
     * Is element highlighting ON or OFF.
     */
    public static final boolean IS_HIGHLIGHT_ELEMENTS =
            Boolean.parseBoolean(System.getProperty("highlight.elements", "false"));

    /**
     * Period of time that element is highlighted for.
     */
    private static final long HIGHLIGHT_DURATION =
            Long.parseLong(System.getProperty("highlight.duration", "200"));

    /**
     * Path to element highlight script.
     */
    private static final String SCRIPT_PATH = "js_inject/element_highlight_minified.js";

    /**
     * String representation of JS code to facilitate element highlighting.
     */
    private static final String SCRIPT_TO_INJECT;

    static {
        try {
            var scriptFileUrl = ElementHighlighting.class.getClassLoader().getResource(SCRIPT_PATH);
            if (scriptFileUrl != null) {
                SCRIPT_TO_INJECT = IOUtils.toString(scriptFileUrl, StandardCharsets.UTF_8);
            } else {
                throw new RuntimeException("Could not load script from " + SCRIPT_PATH);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load script from " + SCRIPT_PATH, e);
        }
    }

    /**
     * Main method to highlight the found element.
     * Method that fires before every Selenium search for the web element.
     *
     * @param by     locator used to locate element(s) within a document
     * @param driver current web driver interface
     */
    @Override
    public void beforeFindElement(WebDriver driver, By by) {
        if (!IS_HIGHLIGHT_ELEMENTS) {
            return;
        }
        Selenide.executeJavaScript(SCRIPT_TO_INJECT);
        highlightAllElements(by, driver);
    }

    /**
     * Highlight all found elements, stop test execution for a period of time,
     * and clear the highlight afterwards.
     *
     * @param by     locator used to locate element(s) within a document
     * @param driver current web driver interface
     */
    private static void highlightAllElements(By by, WebDriver driver) {
        var foundElements = driver.findElements(by);
        highlightElements(foundElements, driver);
        Selenide.sleep(HIGHLIGHT_DURATION);
        resetAllHighlights();
    }

    /**
     * Highlight the collection of found elements for a period of time.
     *
     * @param elements collection of found web elements
     * @param driver   current web driver interface
     */
    private static void highlightElements(List<WebElement> elements, WebDriver driver) {
        Selenide.executeJavaScript("if(window.NEODYMIUM){"
                        + "window.NEODYMIUM.highlightAllElements(arguments[0], document, "
                        + HIGHLIGHT_DURATION + ");"
                        + "}",
                elements, driver.getWindowHandle());
    }

    /**
     * Clear all currently highlighted elements.
     */
    private static void resetAllHighlights() {
        Selenide.executeJavaScript("if(window.NEODYMIUM){window.NEODYMIUM.resetHighlightElements(document);}");
    }
}
