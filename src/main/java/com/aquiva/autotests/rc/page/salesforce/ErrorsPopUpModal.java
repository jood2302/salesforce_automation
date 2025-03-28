package com.aquiva.autotests.rc.page.salesforce;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

/**
 * Standard Salesforce Lightning pop-up dialog/modal that displays validation errors,
 * page/field level errors, etc.
 * <br/>
 * Can be found on record pages, record creation modals, among others.
 */
public class ErrorsPopUpModal {
    private final SelenideElement errorsDialogContainer = $("div.forceFormPageError[role='dialog']");

    /**
     * Get the list of error notifications on the modal.
     *
     * @return list of web elements that contain error notifications
     */
    public ElementsCollection getErrorsList() {
        return errorsDialogContainer.$$x(".//records-record-edit-error//li");
    }

    /**
     * Get a button to close a modal.
     *
     * @return web element that represents 'close' button ('X')
     */
    public SelenideElement getCloseErrorListButton() {
        return errorsDialogContainer.$x(".//button[@title='Close error dialog']");
    }
}
