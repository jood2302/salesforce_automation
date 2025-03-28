package com.aquiva.autotests.rc.page.salesforce.psorder;

import com.aquiva.autotests.rc.page.salesforce.GenericSalesforceModal;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;

/**
 * A modal window that is opened after clicking 'Change Order' button on the {@link ProServProjectRecordPage}.
 */
public class ProServChangeOrderModal extends GenericSalesforceModal {
    public final SelenideElement iframeElement = $x("//iframe[@title='ChangeOrder']");
    public final SelenideElement spinnerContainer = $x("//*[@class='slds-spinner_container']");

    //  Notifications
    public final SelenideElement successNotification = $("notifications .slds-theme_success");
}
