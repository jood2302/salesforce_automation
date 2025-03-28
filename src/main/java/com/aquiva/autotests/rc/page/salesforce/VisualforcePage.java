package com.aquiva.autotests.rc.page.salesforce;

import com.codeborne.selenide.SelenideElement;

/**
 * Abstract page that defines pages that contain Visualforce UI content.
 */
public abstract class VisualforcePage extends IframePage {

    /**
     * Constructor.
     * Defines the page's iframe location in the Lightning Experience.
     *
     * @param iframeTitleSubstring substring in the 'title' attribute for the VF page's iframe
     *                             in the Lightning Experience
     */
    public VisualforcePage(String iframeTitleSubstring) {
        super(iframeTitleSubstring);
    }

    /**
     * Constructor.
     * Defines the page's iframe location in the Lightning Experience.
     *
     * @param iframeElement web element for the VF page's iframe
     *                      in the Lightning Experience
     */
    public VisualforcePage(SelenideElement iframeElement) {
        super(iframeElement);
    }
}
