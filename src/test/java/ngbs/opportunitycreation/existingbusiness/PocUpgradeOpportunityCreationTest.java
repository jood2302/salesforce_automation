package ngbs.opportunitycreation.existingbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.SALES_QUOTE_RECORD_TYPE;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("PDV")
@Tag("NGBS")
@Tag("QOP")
public class PocUpgradeOpportunityCreationTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String chargeTerm;
    private final Package upgradePackage;
    private final Product digitalLineUnlimited;
    private final String upgradePackageFolderName;
    private final String officeService;

    public PocUpgradeOpportunityCreationTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/existingbusiness/RC_MVP_POC_Monthly_NonContract_QOP_145148013.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        chargeTerm = data.packageFoldersUpgrade[0].chargeTerm;
        upgradePackage = data.packageFoldersUpgrade[0].packages[0];
        digitalLineUnlimited = data.getProductByDataNameFromUpgradeData("LC_DL-UNL_50");
        upgradePackageFolderName = data.packageFoldersUpgrade[0].name;
        officeService = upgradePackageFolderName;
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();

        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
        steps.opportunityCreation.openQopAndPopulateRequiredFields(steps.salesFlow.account.getId());
    }

    @Test
    @TmsLink("CRM-20497")
    @DisplayName("CRM-20497 - POC Account NGBS Opportunity creation. QOP")
    @Description("Verify that NGBS Opportunity can be created from Quick Oppty Page for POC Accounts")
    public void test() {
        step("1. Check fields in 'Service Plan' section on QOP for a POC Account", () -> {
            opportunityCreationPage.businessIdentityPicklist.getOptions()
                    .shouldHave(sizeGreaterThan(0), ofSeconds(30));
            opportunityCreationPage.businessIdentityPicklist.shouldBe(disabled)
                    .getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(data.getBusinessIdentityName()));
            opportunityCreationPage.currencyPicklist.shouldBe(disabled)
                    .getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(data.getCurrencyIsoCode()));
            opportunityCreationPage.servicePicklist.getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(officeService));
        });

        step("2. Set new number of DLs and click 'Continue to Opportunity' button", () -> {
            opportunityCreationPage.newNumberOfDLsInput.setValue(digitalLineUnlimited.quantity.toString());
            steps.opportunityCreation.pressContinueToOpp();
        });

        step("3. On created Opportunity record page switch to the Quote Wizard, " +
                "add new Sales Quote, select a package for it and save changes", () -> {
            opportunityPage.switchToNGBSQW();
            steps.quoteWizard.addNewSalesQuote();
            packagePage.packageSelector.selectPackage(chargeTerm, upgradePackageFolderName, upgradePackage);
            packagePage.saveChanges();
        });

        step("4. Check that the Quote is created with Record Type = 'Sales Quote v2' in SFDC", () -> {
            var quote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, RecordType.Name " +
                            "FROM Quote " +
                            "WHERE Id = '" + wizardPage.getSelectedQuoteId() + "'",
                    Quote.class);

            assertThat(quote.getRecordType().getName())
                    .as("Quote.RecordType.Name value")
                    .isEqualTo(SALES_QUOTE_RECORD_TYPE);
        });
    }
}
