package com.aquiva.autotests.rc.page.components;

import com.aquiva.autotests.rc.page.components.packageselector.PackageFilter;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal.BillingDetailsAndTermsModal;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal.WarningConfirmationModal;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static java.time.Duration.ofMillis;

/**
 * The component that is used to control Contract selection on the Quote.
 * <br/>
 * Can be found on the {@link PackageFilter} and {@link  BillingDetailsAndTermsModal}.
 */
public class ContractSelector {
    public final LightningCheckbox lwcCheckboxElement = new LightningCheckbox($("[name='isContractActive']"));

    /**
     * Set 'Contract' checkbox to be selected or not.
     * <br/>
     * Additionally, handles a warning modals that may appear after the checkbox is deselected.
     *
     * @param isContractToBeSelected true, if the checkbox should be selected,
     *                               otherwise, it should be deselected.
     * @see LightningCheckbox#setSelected(boolean)
     */
    public void setSelected(boolean isContractToBeSelected) {
        lwcCheckboxElement.clickInput(isContractToBeSelected);

        //  Handle an additional modal window for the Existing Business quotes (see PBC-23672)
        var warningConfirmationModal = new WarningConfirmationModal();
        if (warningConfirmationModal.dialogContainer.is(visible, ofMillis(500))) {
            warningConfirmationModal.confirmButton.click();
            warningConfirmationModal.confirmButton.shouldBe(hidden);
        }

        lwcCheckboxElement.verifySelectedInput(isContractToBeSelected);
    }
}
