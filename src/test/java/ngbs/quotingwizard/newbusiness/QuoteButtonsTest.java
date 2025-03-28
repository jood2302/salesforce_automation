package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.POC_QUOTE_RECORD_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.PROSERV_QUOTE_RECORD_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.PROFESSIONAL_SERVICES_LIGHTNING_PROFILE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.getUser;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("P1")
@Tag("QuoteTab")
@Tag("POC")
@Tag("ProServ")
@Tag("Multiproduct-Lite")
@Tag("NGBS")
public class QuoteButtonsTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Quote proServQuote;

    //  Test data
    private final String packageFolder;
    private final Package pocPackage;
    private final Product polycomPhone;
    private final Product digitalLineUnlimited;
    private final AreaCode euAreaCode;

    public QuoteButtonsTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_EU_MVP_Monthly_Contract_RegularAndPOC.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        packageFolder = data.packageFolders[0].name;
        pocPackage = data.packageFolders[0].packages[1];
        polycomPhone = data.getProductByDataName("LC_HD_611");
        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        euAreaCode = new AreaCode("Local", "France", EMPTY_STRING, EMPTY_STRING, "1");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @Tag("KnownIssue")
    @Issue("PBC-26306")
    @TmsLink("CRM-9303")
    @TmsLink("CRM-9331")
    @TmsLink("CRM-9332")
    @TmsLink("CRM-9304")
    @TmsLink("CRM-13061")
    @DisplayName("CRM-9303 - Initiate ProServ and Initiate CC ProServ buttons availability. \n" +
            "CRM-9331 - Create POC Approval button is shown on POC Quote. \n" +
            "CRM-9332 - Create POC Approval button is not shown on Sales/CC/PS/CC PS Quotes. \n" +
            "CRM-9304 - Engage Legal button availability - POC Quote. \n" +
            "CRM-13061 - 'Send with DocuSign' button is shown on Opportunities for brands != 'Avaya Cloud Office'.")
    @Description("CRM-9303 - Verify that 'Initiate ProServ' button is shown only on Sales Quote " +
            "and isn't shown on POC, CC Quotes and that 'Initiate CC ProServ' button is shown on Sales Quote only. \n" +
            "CRM-9331 - Verify that Create POC Approval button is shown in the Quoting Wizard on Quotes with Record Type == 'POC Quote'. \n" +
            "CRM-9332 - Verify that Create POC Approval button is not shown in the Quoting Wizard on Quotes " +
            "with Record Type == 'Sales Quote v2', 'Contact Center Quote', 'ProServ Quote' and 'CC ProServ Quote'. \n" +
            "CRM-9304 - Verify that Engage Legal button is shown on Sales Quote and not shown on POC, Contact Center, ProServ and CC ProServ Quotes. \n" +
            "CRM-13061 - Verify if Opportunity has Brand that does not equal to 'Avaya Cloud Office' (for example 'RingCentral') " +
            "then 'Send with DocuSign' button is shown.")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity, add a new Sales Quote, " +
                "select package for it, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
            steps.quoteWizard.selectPackageFromTestDataAndCreateQuote();
        });

        step("2. Check buttons on the different tabs of the Quote Wizard for the Sales Quote", () -> {
            //  CRM-9303
            wizardPage.initiateProServButton.shouldBe(visible).shouldBe(enabled, ofSeconds(20));
            wizardPage.initiateCcProServButton.shouldBe(visible).shouldBe(disabled, ofSeconds(10));

            //  CRM-9304
            quotePage.openTab();
            //  TODO Known Issue PBC-26306 ('Engage Legal' button is not shown on the Quote Details tab, but it should be)
            quotePage.engageLegalButton.shouldBe(visible);

            //  CRM-13061
            quotePage.sendWithDocuSignButton.shouldBe(visible);

            //  CRM-9332
            quotePage.createPocApprovalButton.shouldBe(hidden);
        });

        step("3. Create a new POC Quote, add a phone, " +
                "assign it to DigitalLines on the Price tab, and save changes", () -> {
            switchTo().window(0);
            quoteSelectionWizardPage.addNewPocQuoteButton.click();
            switchTo().window(2);
            wizardPage.waitUntilLoaded();

            packagePage.packageSelector.selectPackage(data.chargeTerm, packageFolder, pocPackage);
            packagePage.saveChanges();

            var quoteListAfterPocCreated = enterpriseConnectionUtils.query(
                    "SELECT Id, Name " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND RecordType.Name = '" + POC_QUOTE_RECORD_TYPE + "'",
                    Quote.class);

            assertThat(quoteListAfterPocCreated)
                    .as("List of created POC quotes")
                    .hasSize(1);

            steps.quoteWizard.addProductsOnProductsTab(polycomPhone);
            cartPage.openTab();
            steps.cartTab.assignDevicesToDLAndSave(polycomPhone.name, digitalLineUnlimited.name, euAreaCode, polycomPhone.quantity);
        });

        step("4. Check buttons on the different tabs of the Quote Wizard for the POC Quote", () -> {
            //  CRM-9303
            wizardPage.initiateProServButton.shouldBe(hidden);
            wizardPage.initiateCcProServButton.shouldBe(hidden);

            //  CRM-9331
            quotePage.openTab();
            quotePage.createPocApprovalButton.shouldBe(visible, enabled);

            //  CRM-9304
            quotePage.engageLegalButton.shouldBe(hidden);
        });

        step("5. On the Sales quote, click 'Initiate ProServ' button and submit data for it", () -> {
            switchTo().window(1);

            wizardPage.initiateProServ();

            var quoteListAfterProServCreated = enterpriseConnectionUtils.query(
                    "SELECT Id, Name, RecordType.Name " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND RecordType.Name = '" + PROSERV_QUOTE_RECORD_TYPE + "'",
                    Quote.class);

            assertThat(quoteListAfterProServCreated)
                    .as("List of created ProServ quotes")
                    .hasSize(1);

            proServQuote = quoteListAfterProServCreated.get(0);
        });

        step("6. Re-login as a user with profile = 'Professional Services Lightning', " +
                "transfer the ownership of the Account, Contact, and Opportunity to this user, " +
                "and open the Quote Wizard for the same opportunity again", () -> {
            var proServUser = getUser().withProfile(PROFESSIONAL_SERVICES_LIGHTNING_PROFILE).execute();

            steps.salesFlow.account.setOwnerId(proServUser.getId());
            steps.salesFlow.contact.setOwnerId(proServUser.getId());
            steps.quoteWizard.opportunity.setOwnerId(proServUser.getId());
            enterpriseConnectionUtils.update(steps.salesFlow.account, steps.salesFlow.contact, steps.quoteWizard.opportunity);

            steps.sfdc.reLoginAsUser(proServUser);
            steps.quoteWizard.openQuoteWizardDirect(steps.quoteWizard.opportunity.getId());
        });

        step("7. Open the 'ProServ Quote' tab, select the ProServ quote and check Quote Wizard buttons for Quotes", () -> {
            wizardBodyPage.proServTab.click();
            proServWizardPage.quotesPicklist.shouldBe(visible, ofSeconds(30))
                    .selectOptionContainingText(proServQuote.getName());

            //  CRM-9332
            proServWizardPage.createPocApprovalButton.shouldBe(hidden);

            //  CRM-9304
            proServWizardPage.engageLegalButton.shouldBe(hidden);
        });
    }
}