package ngbs.opportunitycreation.existingbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.components.packageselector.PackageSelector.PACKAGE_FROM_ACCOUNT_BADGE;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NgbsQuoteSelectionQuoteWizardPage.NO_SALES_QUOTES_MESSAGE;
import static com.aquiva.autotests.rc.page.salesforce.RecordPage.NEW_BUTTON_LABEL;
import static com.aquiva.autotests.rc.page.salesforce.account.AccountRecordPage.OPPORTUNITIES_RELATED_LIST;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.NEW_SALES_OPPORTUNITY_RECORD_TYPE;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.String.valueOf;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("PDV")
@Tag("NGBS")
@Tag("QOP")
public class NgbsExistingBusinessOpportunityCreationViaAccountUpsellTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private String opportunityId;

    //  Test data
    private final String businessIdentity;
    private final String serviceName;
    private final Package testPackage;
    private final String digitalLineUnlimitedNewQuantity;

    public NgbsExistingBusinessOpportunityCreationViaAccountUpsellTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_NonContract_163074013.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        businessIdentity = data.getBusinessIdentityName();
        serviceName = data.packageFolders[0].name;
        testPackage = data.packageFolders[0].packages[0];

        digitalLineUnlimitedNewQuantity = valueOf(data.getProductByDataName("LC_DL-UNL_50").quantity);
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();

        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-13043")
    @TmsLink("CRM-4454")
    @TmsLink("CRM-21362")
    @DisplayName("CRM-13043 - Opening QOP from Account preselects that Account. NGBS. \n" +
            "CRM-4454 - Creating upsell opportunity. \n" +
            "CRM-21362 - Existing Business | QOP | No buttons are enabled after Upsell Quote creation.")
    @Description("CRM-13043 - Verify that Opening QOP from Account page opens QOP with that Account preselected. \n" +
            "CRM-4454 - To verify that upsell oppty is created correctly from QOP. \n" +
            "CRM-21362 - Verify that Save/Discard/Cancel Upgrade buttons behave correctly. No buttons are enabled after QOP Upsell.")
    public void test() {
        step("1. Open the Account record page and create 'New Sales Opportunity'", () -> {
            accountRecordPage.openPage(steps.salesFlow.account.getId());
            accountRecordPage.opportunitiesTab.shouldBe(visible, ofSeconds(120)).click();
            accountRecordPage.clickVisibleListButtonOnRelatedList(OPPORTUNITIES_RELATED_LIST, NEW_BUTTON_LABEL);
            opportunityRecordTypeSelectionModal.selectRecordType(NEW_SALES_OPPORTUNITY_RECORD_TYPE);
            opportunityRecordTypeSelectionModal.nextButton.click();
        });

        //  CRM-13043
        step("2. Verify that the correct Account is selected in the lookup on the QOP", () -> {
            opportunityCreationPage.switchToIFrame();
            opportunityCreationPage.accountInputWithSelectedValue
                    .shouldBe(visible, ofSeconds(60))
                    .shouldHave(attribute("title", steps.salesFlow.account.getName()));
        });

        //  CRM-13043
        step("3. Verify that Business Identity and Service picklists values are taken from account in NGBS", () -> {
            opportunityCreationPage.businessIdentityPicklist.shouldBe(disabled, ofSeconds(30))
                    .getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(businessIdentity));
            opportunityCreationPage.servicePicklist.shouldBe(disabled)
                    .getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(serviceName));
        });

        step("4. Populate 'Close date' and 'Provisioning details' fields", () -> {
            opportunityCreationPage.populateCloseDate();
            opportunityCreationPage.provisioningDetailsTextArea.setValue(TEST_STRING);
        });

        step("5. Update the field 'New Number of DLs'", () -> {
            opportunityCreationPage.newNumberOfDLsInput.setValue(digitalLineUnlimitedNewQuantity);
        });

        step("6. Click 'Continue To Opportunity' button", () ->
                steps.opportunityCreation.pressContinueToOpp()
        );

        //  CRM-4454
        step("7. Open the Quote Wizard and check that there are no created quotes on the Opportunity", () -> {
            opportunityPage.switchToNGBSQW();
            opportunityId = opportunityPage.getCurrentRecordId();
            quoteSelectionWizardPage.waitUntilLoaded();

            quoteSelectionWizardPage.salesQuotes.shouldHave(size(0));
            quoteSelectionWizardPage.noSalesQuotesNotification.shouldHave(exactTextCaseSensitive(NO_SALES_QUOTES_MESSAGE));
            quoteSelectionWizardPage.pocQuotesSection.shouldBe(hidden);
        });

        step("8. Add new Sales Quote in the Quote Selection Wizard, " +
                "and check that the package from NGBS account is preselected", () -> {
            steps.quoteWizard.addNewSalesQuote();

            packagePage.packageSelector.getSelectedPackage().getName()
                    .shouldHave(exactTextCaseSensitive(testPackage.getFullName()));
            packagePage.packageSelector.getSelectedPackage().getBadge()
                    .shouldHave(exactTextCaseSensitive(PACKAGE_FROM_ACCOUNT_BADGE));
        });

        //  CRM-4454
        step("9. Click 'Save and Continue' button, and check that Quote is created with preselected package", () -> {
            packagePage.saveChanges();

            packagePage.openTab();
            packagePage.packageSelector.getSelectedPackage().getName()
                    .shouldHave(exactTextCaseSensitive(testPackage.getFullName()));
            packagePage.packageSelector.getSelectedPackage().getBadge()
                    .shouldHave(exactTextCaseSensitive(PACKAGE_FROM_ACCOUNT_BADGE));

            var createdQuotes = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + opportunityId + "'",
                    Quote.class);
            assertThat(createdQuotes)
                    .as("Number of Quotes on the Opportunity")
                    .hasSize(1);
        });

        //  CRM-21362
        step("10. Open the Price and Quote Details tabs, and verify that they are opened without any confirmation windows, " +
                "and 'Save Changes' button is disabled on each tab", () -> {
            cartPage.openTab();
            cartPage.saveButton.shouldBe(disabled);

            quotePage.openTab();
            quotePage.saveButton.shouldBe(disabled);
        });
    }
}
