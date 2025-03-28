package com.aquiva.autotests.rc.page.salesforce.psorder;

import com.aquiva.autotests.rc.page.salesforce.RecordPage;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.ProServ_Project__c;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$x;
import static java.time.Duration.ofSeconds;

/**
 * Page Object for the {@link ProServ_Project__c} record page in Salesforce.
 */
public class ProServProjectRecordPage extends RecordPage {
    private final SelenideElement changeOrderButton = $x("//button[text()='Change Order']");

    //  Modal window
    public final ProServChangeOrderModal changeOrderModal = new ProServChangeOrderModal();

    /**
     * {@inheritDoc}
     */
    public void waitUntilLoaded() {
        detailsTab.shouldBe(visible, ofSeconds(60));
    }

    /**
     * Click the 'Change Order' button to open the Change Order modal window.
     */
    public void openChangeOrderModal() {
        changeOrderButton.click();
        changeOrderModal.iframeElement.shouldBe(visible, ofSeconds(20));
        Selenide.switchTo().frame(changeOrderModal.iframeElement);
    }
}
