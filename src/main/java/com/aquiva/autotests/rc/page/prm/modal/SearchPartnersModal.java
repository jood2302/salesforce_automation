package com.aquiva.autotests.rc.page.prm.modal;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$x;

/**
 * The modal window to search for a Partner Contact
 * in a new Deal Registration creation form.
 */
public class SearchPartnersModal {
    private final SelenideElement dialogContainer = $x("//div[contains(@class,'slds-modal__container')]");

    public final SelenideElement contactFullNameSearchInput =
            dialogContainer.$x(".//label[text()='Full Name']/following-sibling::div/input");
    public final SelenideElement contactSearchButton = dialogContainer.$x(".//button[text()='Search']");
    public final SelenideElement contactSearchFirstResult = dialogContainer.$x(".//th[@data-label='Name']/div/a");
}
