package com.aquiva.autotests.rc.page.salesforce;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$x;
import static java.time.Duration.ofSeconds;

/**
 * Abstract record page that defines pages that exist in iframes.
 * Contains useful wrapper methods for switching to-and-fro the page's iframe, reloading iframe, etc...
 */
public abstract class IframePage {
    protected SelenideElement iframeElement;

    /**
     * Default no-arg constructor.
     * Defines a page that <i>could be in the iframe</i> without the specific iframe.
     * <br/>
     * Warning: calling {@link #switchToIFrame()} method will throw an expected NPE here.
     * Use it only for pages when it is accessed directly, without any frames.
     */
    public IframePage() {
    }

    /**
     * Constructor by iframe's title.
     *
     * @param iframeTitleSubstring substring in iframe's {@code title} HTML attribute.
     *                             iframe's {@code title} attribute should CONTAIN the provided title's substring.
     */
    public IframePage(String iframeTitleSubstring) {
        this($x("//iframe[contains(@title, '" + iframeTitleSubstring + "')]"));
    }

    /**
     * Constructor by iframe's web element.
     *
     * @param iframeElement iframe's web element initiated as SelenideElement.
     */
    public IframePage(SelenideElement iframeElement) {
        this.iframeElement = iframeElement;
    }

    /**
     * Switch the current context into the page's iframe.
     */
    public void switchToIFrame() {
        iframeElement.shouldBe(visible, ofSeconds(100)).scrollIntoView("{block: \"center\"}");
        Selenide.switchTo().frame(iframeElement);
    }

    /**
     * Switch the current context from the page's iframe into the outer frame.
     */
    public void switchFromIFrame() {
        Selenide.switchTo().parentFrame();
    }

    /**
     * Reload the page's iframe.
     * The current context remains inside the iframe.
     * <br/>
     * <b> Note: before calling this method
     * make sure that the current context is inside the page's iframe! </b>
     */
    public void reloadIFrame() {
        Selenide.executeJavaScript("location.reload()");
    }
}
