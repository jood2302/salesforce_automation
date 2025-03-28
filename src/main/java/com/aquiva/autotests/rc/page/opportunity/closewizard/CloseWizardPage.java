package com.aquiva.autotests.rc.page.opportunity.closewizard;

import com.aquiva.autotests.rc.page.salesforce.VisualforcePage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$x;

/**
 * Close/Downgrade Wizard that is opened after the user clicks
 * 'Close' button on the Opportunity record page.
 * <br/.
 * Usually, after the user submits the info here,
 * the Opportunity's Stage becomes "7. Closed Won".
 */
public class CloseWizardPage extends VisualforcePage {
    //  Close Wizard messages
    public static final String DO_YOU_WANT_TO_CLOSE_THIS_OPPORTUNITY = "Do you want to close this Opportunity?";
    public static final String THIS_OPPORTUNITY_WILL_BE_CLOSED_AND_QUOTES_WILL_BE_LOCKED =
            "This Opportunity will be closed and Quotes under other not Closed or Downgraded Opportunities for this Account will be locked";

    //  For 'Downsell Reason' picklist
    public static final String COMPETITION_DOWNSELL_REASON = "Competition";

    public SelenideElement whyWeWonBlock = $x("//div[./*[text()='Why We Won?']]");
    public SelenideElement closeWizardHeader = $x("//div[contains(@class, 'slds-p-horizontal_large')]/h1");
    public SelenideElement closeWizardText = $x("//div[contains(@class, 'slds-p-horizontal_large')]/p");

    //  Downsell Disposition Wizard
    public SelenideElement downsellReasonPicklist = $x("//tr[.//*[text()='Downsell Reason']]//select");
    public SelenideElement downsellSubReasonPicklist = $x("//tr[.//*[text()='Downsell Sub Reason']]//select");
    
    public SelenideElement cancelButton = $x("//button[text()='Cancel']");
    public SelenideElement submitButton = $x("//button[text()='Submit']");
    public SelenideElement okButton = $x("//button[text()='Ok']");

    /**
     * Constructor that defines Close Wizard page's location
     * using its iframe's title.
     */
    public CloseWizardPage() {
        super("Close/Downgrade Wizard");
    }

    /**
     * Select one of the option in the multiselect in the Close Wizard.
     *
     * @param blockName  name of the block with the multiselect (e.g. "Why We Won?")
     * @param optionName name of the option to select (e.g. "Other")
     */
    public void selectOptionInMultiselect(String blockName, String optionName) {
        var block = $x("//div[./*[text()='" + blockName + "']]");

        block.$x(".//*[./span='Available']//*[text()='" + optionName + "']")
                .scrollIntoView("{block: \"center\"}")
                .click();
        block.$x(".//button[contains(@title, 'to Chosen')]").click();

        block.$x(".//*[./span='Chosen']//*[text()='" + optionName + "']").shouldBe(visible);
    }

    /**
     * Set the value in the text area input in the Close Wizard.
     *
     * @param textareaLabel name of the block with the text area input (e.g. "Additional details on why we won?")
     * @param text          any valid text to enter into the input
     */
    public void setValueInTextareaInput(String textareaLabel, String text) {
        $x("//lightning-textarea[./*[text()='" + textareaLabel + "']]//textarea").setValue(text);
    }

    /**
     * Populate all the required fields in the Close Wizard and submit the form (for Opportunity with ARR < 10500).
     * <br/>
     * Note: if some fields are missing and/or extra fields are present,
     * then there's something with the Opportunity's fields that are responsible
     * for validations on the Stage changes
     * (i.e. some required fields may already have the value).
     */
    public void submitCloseWizard() {
        selectOptionInMultiselect("Why We Won?", "Product Features");
        selectOptionInMultiselect("Final Competitor we won against?", "Other");
        setValueInTextareaInput("Additional details on why we won?", "Test details");

        submitButton.click();
    }

    /**
     * Populate all the required fields in the Close Wizard and submit the form (for Opportunity with ARR >= 10500).
     * <br/>
     * Note: if some fields are missing and/or extra fields are present,
     * then there's something with the Opportunity's fields that are responsible
     * for validations on the Stage changes
     * (i.e. some required fields may already have the value).
     */
    public void submitCloseWizardForBigDeal() {
        selectOptionInMultiselect("Why We Won?", "Product Features");
        selectOptionInMultiselect("Final Competitor we won against?", "Other");
        setValueInTextareaInput("Additional details on why we won?", "Test details");
        selectOptionInMultiselect("Incumbent Phone", "Unknown");
        selectOptionInMultiselect("Incumbent Video", "Unknown");
        selectOptionInMultiselect("Incumbent Message", "Unknown");
        selectOptionInMultiselect("Incumbent Contact Center", "Unknown");
        selectOptionInMultiselect("Incumbent Digital Channels", "Email");
        selectOptionInMultiselect("Key Deal Integration", "Unknown");
        selectOptionInMultiselect("Competitor Phone", "Unknown");
        selectOptionInMultiselect("Competitor Video", "Unknown");
        selectOptionInMultiselect("Competitor Message", "Unknown");
        selectOptionInMultiselect("Competitor Contact Center", "Unknown");
        selectOptionInMultiselect("Competitor Digital Channels", "Email");
        setValueInTextareaInput("Identified Risks & Mitigation Plan", "Test risks & plan");

        submitButton.click();
    }
}
