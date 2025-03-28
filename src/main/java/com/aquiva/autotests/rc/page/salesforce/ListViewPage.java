package com.aquiva.autotests.rc.page.salesforce;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Selenide.$;

/**
 * Abstract list view page that represents a typical Salesforce List View Page,
 * like related records, lists of standard objects (Accounts, Contacts...), etc...
 * <br/>
 * Defines some common elements, like action buttons or list elements.
 */
public abstract class ListViewPage extends SalesforcePage {

    public final SelenideElement container = $("[data-aura-class='forceRelatedListDesktop']");
    public final ElementsCollection actionButtons = container.$$("ul.branding-actions > li a");

    /**
     * Click on the list view's action button.
     * <br/><br/>
     * This method searches the button among Lightning Experience actions
     * in the upper right corner of the page.
     *
     * @param buttonLabel button's label as shown in UI (e.g. "New", "Approve", etc...)
     */
    public void clickListViewPageButton(String buttonLabel) {
        actionButtons.findBy(attribute("title", buttonLabel)).click();
    }
}
