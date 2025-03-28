package com.aquiva.autotests.rc.page.salesforce.cases;

import com.aquiva.autotests.rc.page.salesforce.RecordPage;
import com.sforce.soap.enterprise.sobject.Case;

import static com.codeborne.selenide.Condition.visible;
import static java.time.Duration.ofSeconds;

/**
 * The Salesforce page that displays ({@link Case}) record information.
 */
public class CaseRecordPage extends RecordPage {

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitUntilLoaded() {
        detailsTab.shouldBe(visible, ofSeconds(30));
    }

    /**
     * Click on 'Create a Case' button.
     * <p></p>
     * This method searches "Create a Case" button among Lightning Experience actions
     * in the upper right corner of the page.
     */
    public void clickCreateCaseButton() {
        clickDetailPageButton("Create a Case");
    }
}
