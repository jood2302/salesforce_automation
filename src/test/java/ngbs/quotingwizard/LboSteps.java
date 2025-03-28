package ngbs.quotingwizard;

import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import com.sforce.ws.ConnectionException;

import static base.Pages.quotePage;
import static base.Pages.wizardPage;
import static com.codeborne.selenide.Condition.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test methods for test cases related to LBO functionality (License Based Office):
 * 'Provision' toggle, LBO/non-LBO Provision types, etc...
 */
public class LboSteps {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    public final Integer thresholdQuantity;

    /**
     * New instance for the class with the test methods/steps
     * for test cases related to LBO functionality (License Based Office):
     * 'Provision' toggle, LBO/non-LBO Provision types, etc...
     */
    public LboSteps() {
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        thresholdQuantity = 100;
    }

    /**
     * Check that 'Provision' toggle is ON or OFF.
     *
     * @param isOn true, if toggle should be ON.
     */
    public void checkProvisionToggleOn(boolean isOn) {
        if (isOn) {
            quotePage.provisionToggle
                    .shouldHave(pseudo(":before", "\"\""))
                    .shouldHave(pseudo(":after", "\" \""));
        } else {
            quotePage.provisionToggle
                    .shouldHave(pseudo(":before", "none"))
                    .shouldHave(pseudo(":after", "\"\""));
        }
    }

    /**
     * Check that 'Provision' toggle is enabled or disabled.
     *
     * @param isEnabled true, if toggle should be enabled.
     */
    public void checkProvisionToggleEnabled(boolean isEnabled) {
        quotePage.provisionToggle
                .parent().preceding(0)
                .shouldBe(isEnabled ? enabled : disabled);
    }

    /**
     * Check if the <b>current</b> value in the Quote.Enabled_LBO__c field is equal to the expected value.
     * <br/>
     * Note: user has to have Quote Wizard/UQT opened with the given quote.
     *
     * @param expectedValue expected Quote.Enabled_LBO__c field value
     * @throws ConnectionException in case of errors while accessing API
     */
    public void checkEnableLboOnQuote(boolean expectedValue) throws ConnectionException {
        var quote = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id, Enabled_LBO__c " +
                        "FROM Quote " +
                        "WHERE Id = '" + wizardPage.getSelectedQuoteId() + "'",
                Quote.class);
        assertThat(quote.getEnabled_LBO__c())
                .as("Quote.Enabled_LBO__c value")
                .isEqualTo(expectedValue);
    }
}