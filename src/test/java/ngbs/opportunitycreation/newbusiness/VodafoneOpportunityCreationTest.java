package ngbs.opportunitycreation.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Opportunity;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("NGBS")
@Tag("QOP")
@Tag("Vodafone")
public class VodafoneOpportunityCreationTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String officePackageFolderName;

    public VodafoneOpportunityCreationTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/newbusiness/Vodafone_Office_Monthly_QOP.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        officePackageFolderName = data.packageFolders[0].name;
    }

    @BeforeEach
    public void setUpTest() {
        var salesUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesUser);
    }

    @Test
    @TmsLink("CRM-22834")
    @DisplayName("CRM-22834 - QOP for Vodafone (New Business)")
    @Description("Verify that Opportunity can be created for 'Vodafone Business with RingCentral' Brand " +
            "on the Quick Opportunity page")
    public void test() {
        step("1. Open QOP for the test Account", () -> {
            opportunityCreationPage.openPage(steps.salesFlow.account.getId());
        });

        step("2. Populate 'Close Date' and 'Provisioning Details' fields, " +
                "check Business Identity picklist, and click 'Continue to Opportunity' button", () -> {
            opportunityCreationPage.populateCloseDate();
            opportunityCreationPage.provisioningDetailsTextArea.setValue(TEST_STRING);

            opportunityCreationPage.businessIdentityPicklist.getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(data.getBusinessIdentityName()));

            //  these 2 checks help to press "Continue to Opportunity" button at the right time
            opportunityCreationPage.currencyPicklist.getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(data.getCurrencyIsoCode()));
            opportunityCreationPage.servicePicklist.getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(officePackageFolderName));
            
            steps.opportunityCreation.pressContinueToOpp();
        });

        step("3. Check that Opportunity is created with expected Currency", () -> {
            var createdOpportunity = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, CurrencyIsoCode " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + opportunityPage.getCurrentRecordId() + "'",
                    Opportunity.class);
            assertThat(createdOpportunity.getCurrencyIsoCode())
                    .as("Opportunity.CurrencyIsoCode value")
                    .isEqualTo(data.getCurrencyIsoCode());
        });

        step("4. Switch to the Quote Selection page on the Opportunity record page, " +
                "check that 'Initiate ProServ' and 'Initiate CC ProServ' buttons are displayed, " +
                "and the 'Sales Quote' and 'POC Quote' sections and ProServ Quote sub-tab are hidden", () -> {
            opportunityPage.switchToNGBSQWIframeWithoutQuote();
            quoteSelectionWizardPage.initiateProServButton.shouldBe(visible, ofSeconds(60));
            quoteSelectionWizardPage.initiateCcProServButton.shouldBe(visible);

            quoteSelectionWizardPage.salesQuotesSection.shouldBe(hidden);
            quoteSelectionWizardPage.pocQuotesSection.shouldBe(hidden);
            wizardBodyPage.proServTab.shouldBe(hidden);
        });
    }
}
