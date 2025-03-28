package ngbs.quotingwizard.newbusiness.signup;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Account;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.MVP_SERVICE;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.hidden;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("P1")
@Tag("LBO")
@Tag("ShippingTab")
public class SalesLboQuoteWithShippingGroupsSignUpTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Product dlUnlimited;
    private final Product dlBasic;
    private final Product firstPhone;
    private final Product secondPhone;

    private final ShippingGroupAddress firstShippingAddress;
    private final String firstShippingAddressFormatted;
    private final ShippingGroupAddress secondShippingAddress;
    private final String secondShippingAddressFormatted;

    public SalesLboQuoteWithShippingGroupsSignUpTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_PhonesAndDLsAndGlobalMVP.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        dlBasic = data.getProductByDataName("LC_DL-BAS_178");
        firstPhone = data.getProductByDataName("LC_HD_523");
        secondPhone = data.getProductByDataName("LC_HDR_619");

        dlUnlimited.quantity = 10;
        firstPhone.quantity = 10;
        dlBasic.quantity = 15;
        secondPhone.quantity = 15;

        firstShippingAddress = new ShippingGroupAddress("United States", "Los Angeles",
                new ShippingGroupAddress.State("California", true),
                "Sunbeam Lane 78", "90022", "QA Automation");
        firstShippingAddressFormatted = firstShippingAddress.getAddressFormatted();
        secondShippingAddress = new ShippingGroupAddress("United States", "Brooklyn",
                new ShippingGroupAddress.State("New York", true),
                "Linden 34", "12212", "QA Automation_2");
        secondShippingAddressFormatted = secondShippingAddress.getAddressFormatted();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-34800")
    @DisplayName("CRM-34800 - LBO quote for New Business signs up successfully if devices are added to the shipping group")
    @Description("Verify that LBO quote for New Business signs up successfully if devices are not assigned to DL, " +
            "but added to the shipping group")
    public void test() {
        step("1. Open the New Business Opportunity, switch to the Quote Wizard, add a new Sales Quote, " +
                "select a package for it, and add DL Basic and some phones on the Add Products tab", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
            steps.quoteWizard.selectDefaultPackageFromTestData();

            steps.quoteWizard.addProductsOnProductsTab(dlBasic, firstPhone, secondPhone);
        });

        step("2. Open the Price tab, set the quantity for the DL Unlimited, DL Basic and added phones, " +
                "and save changes", () -> {
            cartPage.openTab();
            steps.cartTab.setUpQuantities(dlUnlimited, dlBasic, firstPhone, secondPhone);
            cartPage.saveChanges();
        });

        step("3. Open the Quote Details tab, set Start Date, Main Area Code, Payment Method, " +
                "turn off the Provision toggle, and save changes", () -> {
            quotePage.openTab();
            quotePage.setDefaultStartDate();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.selectPaymentMethod(QuotePage.CREDIT_CARD_PAYMENT_METHOD);

            quotePage.provisionToggle.click();
            steps.lbo.checkProvisionToggleOn(false);
            quotePage.saveChanges();
        });

        step("4. Open the Shipping tab, add 2 Shipping groups, assign each phone to each Shipping group, and save changes", () -> {
            shippingPage.openTab();

            shippingPage.addNewShippingGroup(firstShippingAddress);
            shippingPage.addNewShippingGroup(secondShippingAddress);

            shippingPage.assignDeviceToShippingGroup(firstPhone.productName, firstShippingAddressFormatted,
                    firstShippingAddress.shipAttentionTo);
            shippingPage.assignDeviceToShippingGroup(secondPhone.productName, secondShippingAddressFormatted,
                    secondShippingAddress.shipAttentionTo);
            shippingPage.saveChanges();

            shippingPage.getShippingGroup(firstShippingAddressFormatted, firstShippingAddress.shipAttentionTo)
                    .getAllShippingDevicesNames()
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(firstPhone.name), ofSeconds(30));
            shippingPage.getShippingGroup(secondShippingAddressFormatted, secondShippingAddress.shipAttentionTo)
                    .getAllShippingDevicesNames()
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(secondPhone.name), ofSeconds(30));
        });

        step("5. Update the current Quote to Active Agreement via API", () ->
                steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity)
        );

        step("6. Check that the current Quote.Enabled_LBO__c field = true", () ->
                steps.lbo.checkEnableLboOnQuote(true)
        );

        step("7. Press 'Process Order' button on the Opportunity's record page, " +
                "verify that 'Preparing Data' step is completed, " +
                "select the timezone, click 'Sign Up MVP', check that the account is processed for signing up, " +
                "and check that Billing_ID__c and RC_User_ID__c fields are populated on the Account", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();
            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(hidden);

            opportunityPage.processOrderModal.selectDefaultTimezone();
            opportunityPage.processOrderModal.signUpButton.click();

            opportunityPage.processOrderModal.signUpMvpStatus
                    .shouldHave(exactTextCaseSensitive(format(YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE, MVP_SERVICE)), ofSeconds(150));

            step("Wait until Account's Billing_ID__c and RC_User_ID__c will get the values from NGBS", () -> {
                assertWithTimeout(() -> {
                    var accountUpdated = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Id, Billing_ID__c, RC_User_ID__c " +
                                    "FROM Account " +
                                    "WHERE Id = '" + steps.salesFlow.account.getId() + "'",
                            Account.class);
                    assertNotNull(accountUpdated.getBilling_ID__c(), "Account.Billing_ID__c field");
                    assertNotNull(accountUpdated.getRC_User_ID__c(), "Account.RC_User_ID__c field");

                    step("Account.Billing_ID__c = " + accountUpdated.getBilling_ID__c());
                    step("Account.RC_User_ID__c = " + accountUpdated.getRC_User_ID__c());
                }, ofSeconds(120));
            });
        });
    }
}
