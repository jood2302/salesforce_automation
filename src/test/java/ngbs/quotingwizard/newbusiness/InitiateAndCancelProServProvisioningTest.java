package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage.PROVISIONING_AND_SHIPPING_WILL_BE_DISABLED_MESSAGE;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.PROVISIONING_NOT_ALLOWED_WHEN_PROSERV_INITIATED_MESSAGE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.containExactTextsCaseSensitive;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

// TODO remove testclass after LTR-121 OrgWide will be enabled
@Tag("P1")
@Tag("ProServ")
@Tag("ShippingTab")
public class InitiateAndCancelProServProvisioningTest extends BaseTest {
    private final Steps steps;
    private final InitiateAndCancelProServSteps initiateAndCancelProServSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesRepUserWithLboEnabledPS;
    private String firstQuoteId;
    private String secondQuoteId;

    //  Test data
    private final Product dlUnlimited;
    private final Product phoneToAdd;
    private final int expectedQuotesQuantity;
    private final ShippingGroupAddress shippingAddress;
    private final String shippingAddressFormatted;

    public InitiateAndCancelProServProvisioningTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_1PhoneAnd1DL_RegularAndPOC.json",
                Dataset.class);

        steps = new Steps(data);
        initiateAndCancelProServSteps = new InitiateAndCancelProServSteps();
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        phoneToAdd = data.getProductByDataName("LC_HDR_619");
        // 2 Single Product MVP Quotes (Primary + Non-Primary)
        expectedQuotesQuantity = 2;

        shippingAddress = new ShippingGroupAddress(
                "United States", "Findlay",
                new ShippingGroupAddress.State("Ohio", true),
                "3644 Cedarstone Drive", "45840", "QA Automation");
        shippingAddressFormatted = shippingAddress.getAddressFormatted();
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with 'Sales Rep - Lightning' profile and 'LBOQuoteEnable' Permission Set", () -> {
            salesRepUserWithLboEnabledPS = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withPermissionSet(LBO_QUOTE_ENABLE_PS)
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserWithLboEnabledPS);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUserWithLboEnabledPS);

        step("Login as a user with 'Sales Rep - Lightning' profile and 'LBOQuoteEnable' Permission Set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(salesRepUserWithLboEnabledPS);
        });

        step("Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select MVP package for it, add a phone, and save changes on the Price tab", () -> {
            steps.cartTab.prepareCartTabViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());
            firstQuoteId = wizardPage.getSelectedQuoteId();
        });

        step("Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select MVP package for it, add a phone, and save changes on the Price tab", () -> {
            steps.cartTab.prepareCartTabViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());
            secondQuoteId = wizardPage.getSelectedQuoteId();
        });

        step("Verify that all created Quotes have Enabled_LBO__c = 'false'", () -> {
            var quotes = enterpriseConnectionUtils.query(
                    "SELECT Id, Enabled_LBO__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Quote.class);
            assertThat(quotes.size())
                    .as("Created quotes' size")
                    .isEqualTo(expectedQuotesQuantity);
            for (var quote : quotes) {
                assertThat(quote.getEnabled_LBO__c())
                        .as("Quote.Enabled_LBO__c value for the Quote with Id = " + quote.getId())
                        .isFalse();
            }
        });
    }

    @Test
    @TmsLink("CRM-33510")
    @DisplayName("CRM-33510 - Hide shipping tab, disable Provisioning and it's toggle, clear CustomAddress assignments " +
            "and set Quotes as LBO, when ProServ is initiated")
    @Description("When a ProServ quote exists under the opportunity with the ProServ_Status__c != 'Cancelled', then \n" +
            " - the Shipping tab is hidden \n" +
            " - Provisioning is disabled as LBO flag is true \n" +
            " - Provision toggle on Quote Details tab is disabled \n" +
            " - Shipping Address is hidden from quote tab \n\n" +
            "When a ProServ quote exists under the opportunity with the ProServ_Status__c = 'Cancelled', then \n" +
            " - the Shipping tab is shown \n" +
            " - Provisioning is disabled as LBO flag is still true \n" +
            " - Provision toggle on Quote Details tab is enabled if there are no other conditions blocking the toggle \n\n" +
            "This test case covers flow to test both CC ProServ and ProServ Quotes creation and cancellation, with all the consequences")
    public void test() {
        step("1. Open the Quote Wizard for the Primary quote, open the Price tab, " +
                "assign all devices to the DLs, and check that the Shipping tab is present", () -> {
            wizardPage.openPage(steps.quoteWizard.opportunity.getId(), firstQuoteId);

            cartPage.openTab();
            steps.cartTab.assignDevicesToDL(phoneToAdd.name, dlUnlimited.name, steps.quoteWizard.localAreaCode, phoneToAdd.quantity);

            wizardPage.shippingTabButton.shouldBe(visible);
        });

        step("2. Open the Shipping tab, add a new Shipping Group, assign any number of phones to it, save changes, " +
                "and check that custom address assignments records related to the quote are created", () -> {
            shippingPage.openTab();
            shippingPage.addNewShippingGroup(shippingAddress);
            shippingPage.assignDeviceToShippingGroup(phoneToAdd.productName, shippingAddressFormatted, shippingAddress.shipAttentionTo);
            shippingPage.saveChanges();

            initiateAndCancelProServSteps.checkIfCustomAssignmentRecordsArePresent(true);
        });

        step("3. Open the Quote Details tab and check that Provisioning toggle is turned on", () -> {
            quotePage.openTab();
            steps.lbo.checkProvisionToggleOn(true);
        });

        step("4. Click 'Initiate ProServ' button, click 'Submit' button in popup window, " +
                "and check that ProServ is initiated, Shipping tab is hidden, Provisioning toggle is turned on and disabled, " +
                "all Sales Quotes have Enabled_LBO__c = 'true'," +
                "and all CustomAddressAssignments__c records related to the quote are deleted", () -> {
            quotePage.initiateProServ();
            quotePage.waitUntilLoaded();

            var proServQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ProServ_Status__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND RecordType.Name = '" + PROSERV_QUOTE_RECORD_TYPE + "'",
                    Quote.class);
            assertThat(proServQuote.getProServ_Status__c())
                    .as("ProServ Quote.ProServ_Status__c value")
                    .isEqualTo(CREATED_PROSERV_STATUS);

            initiateAndCancelProServSteps.checkShippingTabAndProvisioning(hidden, false,
                    steps.quoteWizard.opportunity.getId(), expectedQuotesQuantity);
            initiateAndCancelProServSteps.checkIfCustomAssignmentRecordsArePresent(false);
        });

        step("5. Hover over tooltip icon near Provisioning toggle and check the text message near the tooltip", () -> {
            quotePage.provisionToggleInfoIcon.hover();
            quotePage.tooltip.shouldHave(exactTextCaseSensitive(PROVISIONING_NOT_ALLOWED_WHEN_PROSERV_INITIATED_MESSAGE));
        });

        step("6. Refresh the Quote Wizard, open the Price tab, and check the info notification message", () -> {
            refresh();
            wizardPage.waitUntilLoaded();
            cartPage.openTab();
            cartPage.notificationBar.click();
            cartPage.notifications.should(containExactTextsCaseSensitive(PROVISIONING_AND_SHIPPING_WILL_BE_DISABLED_MESSAGE));
        });

        step("7. Open the Quote Wizard for the non-primary Quote, open the Quote Details tab, " +
                "and check that the Shipping tab is not displayed, and Provisioning toggle is turned off and disabled", () -> {
            wizardPage.openPage(steps.quoteWizard.opportunity.getId(), secondQuoteId);
            quotePage.openTab();

            wizardPage.shippingTabButton.shouldBe(hidden);

            quotePage.provisionToggle.shouldBe(visible, ofSeconds(20));
            steps.lbo.checkProvisionToggleOn(false);
            steps.lbo.checkProvisionToggleEnabled(false);
        });

        step("8. Click 'X' button for ProServ engagement, click 'Submit' button in popup window, " +
                "check that ProServ Quote.ProServ_Status__c = 'Cancelled', Shipping tab is displayed, " +
                "Provisioning toggle is turned off and enabled, and all Sales Quotes have Enabled_LBO__c = 'true'", () -> {
            wizardPage.cancelProServ();
            quotePage.waitUntilLoaded();

            var proServQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ProServ_Status__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND RecordType.Name = '" + PROSERV_QUOTE_RECORD_TYPE + "'",
                    Quote.class);
            assertThat(proServQuote.getProServ_Status__c())
                    .as("ProServ Quote.ProServ_Status__c value")
                    .isEqualTo(CANCELLED_PROSERV_STATUS);

            initiateAndCancelProServSteps.checkShippingTabAndProvisioning(visible, true,
                    steps.quoteWizard.opportunity.getId(), expectedQuotesQuantity);
        });
    }
}
