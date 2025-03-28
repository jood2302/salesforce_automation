package com.aquiva.autotests.rc.page.salesforce;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.support.ui.Quotes;

import static com.codeborne.selenide.Selenide.$x;

/**
 * Default Salesforce modal window.
 * <br/>
 * Usually contains close button, some text or actionable content, action buttons ("OK", "Cancel").
 */
public class GenericSalesforceModal {
    protected SelenideElement dialogContainer;

    /**
     * Constructor that defines default location for the modal window.
     */
    public GenericSalesforceModal() {
        this.dialogContainer = $x("//div[contains(@class,'slds-modal__container')]");
    }

    /**
     * Constructor that defines a location for the modal window
     * using full/partial header's text.
     *
     * @param modalWindowHeaderSubstring string that header's title of the modal window contains
     */
    public GenericSalesforceModal(String modalWindowHeaderSubstring) {
        this.dialogContainer =
                $x("//div[contains(@class,'slds-modal__container')]" +
                        "[.//*" +
                        "[contains(@class,'heading') or contains(@class, 'title')]" +
                        "[contains(text()," + Quotes.escape(modalWindowHeaderSubstring) + ")]" +
                        "]");
    }

    /**
     * Constructor that defines a location for the modal window.
     * Good choice for a non-standard modal window's layout.
     *
     * @param dialogContainer web element that locates the container for the modal window
     */
    public GenericSalesforceModal(SelenideElement dialogContainer) {
        this.dialogContainer = dialogContainer;
    }

    /**
     * Get the text content in the modal window, if any.
     *
     * @return web element for the text in the main part of the modal window.
     */
    public SelenideElement getTextContent() {
        return dialogContainer.$(".detail");
    }

    /**
     * Close the modal window by clicking "X" button.
     */
    public void closeWindow() {
        dialogContainer.$("[title='Cancel and close']").click();
    }
}
