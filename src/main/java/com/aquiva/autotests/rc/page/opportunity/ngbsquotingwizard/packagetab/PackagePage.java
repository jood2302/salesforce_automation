package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.packagetab;

import com.aquiva.autotests.rc.page.components.packageselector.PackageSelector;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static java.time.Duration.ofSeconds;

/**
 * 'Select Package' tab in {@link NGBSQuotingWizardPage}
 * that contains {@link PackageSelector} component.
 */
public class PackagePage extends NGBSQuotingWizardPage {

    //  Package Selector Section
    public final PackageSelector packageSelector = new PackageSelector();

    //  Package actions
    public final SelenideElement saveAndContinueButton = $("[data-ui-auto='save-and-continue']");

    /**
     * Open the Select Package tab by clicking on the tab's button.
     */
    public PackagePage openTab() {
        packageTabButton.click();
        packageSelector.packageFilter.servicePicklist.shouldBe(visible, ofSeconds(30));
        return this;
    }

    /**
     * Press 'Save and Continue' button.
     * <br/>
     * Method also waits for the progress bar to appear and disappear.
     */
    public void saveChanges() {
        saveAndContinueButton.click();

        progressBar.shouldBe(visible, ofSeconds(10));
        progressBar.shouldBe(hidden, ofSeconds(PROGRESS_BAR_TIMEOUT_AFTER_SAVE));
        errorNotification.shouldBe(hidden);
    }
}
