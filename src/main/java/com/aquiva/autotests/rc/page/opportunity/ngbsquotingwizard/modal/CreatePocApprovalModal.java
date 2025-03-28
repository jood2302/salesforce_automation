package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Approval__c;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static org.openqa.selenium.Keys.BACK_SPACE;

/**
 * Modal window in {@link NGBSQuotingWizardPage} ("Main Quote" tab)
 * activated by clicking on "Create POC Approval" button (only for POC Quotes).
 * <p>
 * This dialog creates {@link Approval__c} object for POC Quote
 * that follows standard Approval process flow.
 * </p>
 */
public class CreatePocApprovalModal {

    //  String constants used on the form
    public static final String CREATE_POC_APPROVAL_HEADER = "Create POC Approval";
    public static final String LINK_TO_SIGNED_EVALUATION_AGREEMENT_LABEL = "Link to signed Evaluation Agreement";
    public static final String THIS_FIELD_IS_REQUIRED_ERROR = "This field is required";

    //  Page elements
    private final SelenideElement dialogContainer = $("create-poc-approval-modal");

    public final SelenideElement header = dialogContainer.$("h2");
    public final SelenideElement linkAgreementInput = dialogContainer.$("[formcontrolname='linkToSignedAgreement']");
    public final SelenideElement linkAgreementInputLabel = dialogContainer.$("label");
    public final SelenideElement linkAgreementInputRequired = linkAgreementInputLabel.$("[title='required']");
    public final SelenideElement linkAgreementInputError = dialogContainer.$(".slds-form-element__help");

    //  Buttons
    public final SelenideElement closeButton = dialogContainer.$("[title='Close']");
    public final SelenideElement createButton = dialogContainer.$(byText("Create"));
    public final SelenideElement cancelButton = dialogContainer.$(byText("Cancel"));

    /**
     * Clear 'Link to signed Evaluation Agreement' field by pressing "backspace" button for each entered character.
     * <p>
     * Useful to trigger validation check for required fields.
     */
    public void clearAgreementInputValue() {
        var value = linkAgreementInput.val();
        if (value != null && !value.isBlank()) {
            value.chars().forEach(c -> linkAgreementInput.sendKeys(BACK_SPACE));
        }
    }
}
