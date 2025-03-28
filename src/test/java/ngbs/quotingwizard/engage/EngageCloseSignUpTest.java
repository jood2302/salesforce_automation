package ngbs.quotingwizard.engage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.INVOICE_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToInteger;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.CALIFORNIA_STATE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.CLOSED_WON_STAGE;
import static com.codeborne.selenide.CollectionCondition.*;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.closeWindow;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("Engage")
@Tag("SignUp")
@Tag("OpportunityClose")
public class EngageCloseSignUpTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User dealDeskUser;
    private Account account;
    private Opportunity opportunity;

    //  Test data
    private final Product concurrentSeatOmniChannel;
    private final Product seatsOnDemand;
    private final String initialTerm;
    private final int legacyOffset;
    private final int blockId;
    private final int sqRatio;
    private final int sandboxRatio;

    public EngageCloseSignUpTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_EngageDSAndMVP_Monthly_Contract_WithProducts.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        concurrentSeatOmniChannel = data.packageFolders[0].packages[0].productsDefault[0];
        seatsOnDemand = data.packageFolders[0].packages[0].productsOther[0];
        initialTerm = data.packageFolders[0].packages[0].contractTerms.initialTerm[0];
        legacyOffset = 130_000_000;
        blockId = 990;
        sqRatio = 10_000;
        sandboxRatio = 1_000;
    }

    @BeforeEach
    public void setUpTest() {
        dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        account = steps.salesFlow.account;

        steps.quoteWizard.createOpportunity(account, steps.salesFlow.contact, dealDeskUser);
        opportunity = steps.quoteWizard.opportunity;

        //  replace a default 'CA' with 'California' to easily check this value on the 'Process Order' modal
        step("Set Account.BillingState = 'California' via API", () -> {
            account.setBillingState(CALIFORNIA_STATE);
            enterpriseConnectionUtils.update(account);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-20660")
    @TmsLink("CRM-20058")
    @DisplayName("CRM-20660 - Close Engage Digital w/o Master - can be possible. \n" +
            "CRM-20058 - 'Enterprise Account ID' field population with generated ID")
    @Description("CRM-20660 - Verify that Close Engage Digital w/o linked Existing Business Master Account " +
            "or New Business Master Account with linked Quotes - can be possible. \n" +
            "CRM-20058 - Verify that with clicking 'Process Order' button on the Engage Opportunity record page " +
            "Account's 'Internal Enterprise Account ID' field will be populated with generated ID")
    public void test() {
        step("1. Open the Engage Opportunity, switch to the Quote Wizard, add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunity(opportunity.getId())
        );

        step("2. Open the Price tab and check that two products were added, " +
                "change quantity for " + concurrentSeatOmniChannel.name, () -> {
            cartPage.openTab();
            cartPage.cartItemNames
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(concurrentSeatOmniChannel.name, seatsOnDemand.name));
            cartPage.setQuantityForQLItem(concurrentSeatOmniChannel.name, concurrentSeatOmniChannel.quantity);

            cartPage.getQliFromCartByDisplayName(seatsOnDemand.name).getQuantityInput().shouldBe(disabled);
        });

        step("3. Open the Quote Details tab, populate Initial Term, Start Date, Payment method, " +
                "save changes, and verify that End Date is populated", () -> {
            quotePage.openTab();
            quotePage.initialTermPicklist.selectOption(initialTerm);
            quotePage.selectPaymentMethod(INVOICE_PAYMENT_METHOD);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();

            quotePage.endDateInput.shouldNotBe(empty);

            closeWindow();
        });

        step("4. Update the Quote to Active Agreement via API", () ->
                steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(opportunity)
        );

        step("5. Create new Invoice Request Approval for Engage Account " +
                "with related 'Accounts Payable' AccountContactRole record, " +
                "and set Approval__c.Status = 'Approved' (all via API)", () ->
                createInvoiceApprovalApproved(opportunity, account, steps.salesFlow.contact, dealDeskUser.getId(), false)
        );

        step("6. Check that new fields are populated on the Account", () -> {
            var updatedAccount = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Monthly_Credit_Limit__c, Payment_Method__c, Sign_up_Purchase_Limit__c, " +
                            "Payment_Terms__c, Active_Invoice_Approval__c, Invoice_Terms__c " +
                            "FROM Account " +
                            "WHERE Id = '" + account.getId() + "'",
                    Account.class);
            assertThat(updatedAccount.getMonthly_Credit_Limit__c())
                    .as("Account.Monthly_Credit_Limit__c value")
                    .isNotNull();

            assertThat(updatedAccount.getPayment_Method__c())
                    .as("Account.Payment_Method__c value")
                    .isEqualTo(INVOICE_PAYMENT_METHOD);

            assertThat(updatedAccount.getSign_up_Purchase_Limit__c())
                    .as("Account.Sign_up_Purchase_Limit__c value")
                    .isNotNull();

            assertThat(updatedAccount.getPayment_Terms__c())
                    .as("Account.Payment_Terms__c value")
                    .isNotNull();

            assertThat(updatedAccount.getActive_Invoice_Approval__c())
                    .as("Account.Active_Invoice_Approval__c value")
                    .isNotNull();

            assertThat(updatedAccount.getInvoice_Terms__c())
                    .as("Account.Invoice_Terms__c value")
                    .isNotNull();
        });

        //  For CRM-20660
        //  'Deal Desk Lightning' user can close the Opportunity immediately 
        //  without the Close Wizard and additional validations related to Stage changing
        //  see Opportunity_Close_Setup__mdt.ProfileNames__c for the full list of profiles under validation
        step("7. Click 'Close' button on the Opportunity's record page, " +
                "and check that Opportunity.StageName = '7. Closed Won'", () -> {
            opportunityPage.clickCloseButton();
            opportunityPage.spinner.shouldBe(visible, ofSeconds(10));
            opportunityPage.spinner.shouldBe(hidden, ofSeconds(30));
            opportunityPage.alertNotificationBlock.shouldNot(exist);

            var updatedOpportunity = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, StageName " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + opportunity.getId() + "'",
                    Opportunity.class);
            assertThat(updatedOpportunity.getStageName())
                    .as("Opportunity.StageName value")
                    .isEqualTo(CLOSED_WON_STAGE);
        });

        //  For CRM-20058
        step("8. Click 'Process Order' button on the Opportunity record page, " +
                "and check the 'Add Address Validation' section on the modal window", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.engageDigitalSignUpCompletedStepNames.shouldHave(size(1), ofSeconds(60));
            opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(60));

            //  'Add Address Validation' fields should be disabled and populated from the related Account's Billing Address
            opportunityPage.processOrderModal.engageDigitalAddress1Input
                    .should(be(disabled), have(exactValue(account.getBillingStreet())));
            opportunityPage.processOrderModal.engageDigitalAddress2Input.shouldBe(disabled)
                    .shouldBe(disabled, empty);
            opportunityPage.processOrderModal.engageDigitalCountrySelect.getInput()
                    .should(be(disabled), have(exactTextCaseSensitive(account.getBillingCountry())));
            opportunityPage.processOrderModal.engageDigitalStateSelect.getInput()
                    .should(be(disabled), have(exactText(account.getBillingState())));
            opportunityPage.processOrderModal.engageDigitalCityInput
                    .should(be(disabled), have(exactValue(account.getBillingCity())));
            opportunityPage.processOrderModal.engageDigitalZipInput
                    .should(be(disabled), have(exactValue(account.getBillingPostalCode())));

            opportunityPage.processOrderModal.engageDigitalValidateAddressButton.shouldBe(disabled);
        });

        //  these checks are here because Account.Internal_Enterprise_Account_ID__c and RCSequence__c.SubSequence__c are updated
        //  right after the "pre sign-up checks" are finished after the user clicks "Process Order"
        step("9. Check that 'Account.Internal_Enterprise_Account_ID__c' is populated correctly", () -> {
            var updatedAccount = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Internal_Enterprise_Account_ID__c " +
                            "FROM Account " +
                            "WHERE Id = '" + account.getId() + "'",
                    Account.class);

            var latestRCSequence = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, SubSequence__c, SandboxNumber__c " +
                            "FROM RCSequence__c " +
                            "LIMIT 1",
                    RCSequence__c.class);
            var latestSubSequence = doubleToInteger(latestRCSequence.getSubSequence__c());
            assertThat(latestSubSequence)
                    .as("RCSequence__c.SubSequence__c value")
                    .isNotNull();

            var sandboxNumber = doubleToInteger(latestRCSequence.getSandboxNumber__c());
            assertThat(sandboxNumber)
                    .as("RCSequence__c.SandboxNumber__c value")
                    .isNotNull();

            var expectedEnterpriseAccountId = String.valueOf(legacyOffset +
                    (blockId + (sandboxNumber * sandboxRatio)) +
                    (latestSubSequence * sqRatio));

            assertThat(updatedAccount.getInternal_Enterprise_Account_ID__c())
                    .as("Account.Internal_Enterprise_Account_ID__c value")
                    .isEqualTo(expectedEnterpriseAccountId);
        });

        step("10. Select Engage Digital Platform Location, RC Engage Digital Platform, Language and Timezone " +
                "and populate RC Engage Digital Domain field", () -> {
            opportunityPage.processOrderModal.engageDigitalPlatformLocationSelect.selectOption(US1_PLATFORM_LOCATION);
            opportunityPage.processOrderModal.selectRcEngagePlatformFirstOption(ENGAGE_DIGITAL_SERVICE);
            opportunityPage.processOrderModal.engageDigitalLanguageSelect.selectOption(EN_US_LANGUAGE);
            opportunityPage.processOrderModal.engageDigitalTimezoneSelect.selectOption(ANCHORAGE_TIME_ZONE);

            //  Engage Digital Domain should contain up to 32 symbols
            var randomEngageDomainValue = UUID.randomUUID().toString().substring(0, 31);
            opportunityPage.processOrderModal.engageDigitalDomainInput.setValue(randomEngageDomainValue);
        });

        step("11. Check that address fields are enabled and click 'Validate' button", () -> {
            opportunityPage.processOrderModal.engageDigitalAddress1Input.shouldBe(enabled);
            opportunityPage.processOrderModal.engageDigitalAddress2Input.shouldBe(enabled);
            opportunityPage.processOrderModal.engageDigitalCountrySelect.getInput().shouldBe(enabled);
            opportunityPage.processOrderModal.engageDigitalStateSelect.getInput().shouldBe(enabled);
            opportunityPage.processOrderModal.engageDigitalCityInput.shouldBe(enabled);
            opportunityPage.processOrderModal.engageDigitalZipInput.shouldBe(enabled);

            opportunityPage.processOrderModal.engageDigitalValidateAddressButton.click();
            opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(10));
        });

        step("12. Click 'Sign Up Engage Digital' button and check notification that Engage Digital submitted successfully", () -> {
            opportunityPage.processOrderModal.signUpButton.click();
            opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(150));
            opportunityPage.processOrderModal.successNotifications
                    .shouldHave(exactTexts(format(SERVICE_SUBMITTED_SUCCESSFULLY, ENGAGE_DIGITAL_SERVICE)), ofSeconds(60));
        });
    }
}
