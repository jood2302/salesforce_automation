package ngbs.quotingwizard.newbusiness.signup;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.funnel.SignUpBodyFunnelDTO;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Account;
import com.sforce.soap.enterprise.sobject.Opportunity;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.model.funnel.SignUpBodyFunnelDTO.BILLING_ELA_TYPE;
import static com.aquiva.autotests.rc.model.funnel.SignUpBodyFunnelDTO.SERVICE_ELA_TYPE;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.MVP_SERVICE;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.CREDIT_CARD_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.ELA_BILLING_ACCOUNT_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.ELA_SERVICE_ACCOUNT_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("P0")
@Tag("P1")
@Tag("ELALeads")
@Tag("SignUp")
public class ElaTypeAndLicensesSignUpTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Account account;
    private Opportunity opportunity;

    //  Test data
    private final Product[] productsToAdd;
    private final List<Product> productsThatShouldBeInElaSettingsLicenses;

    public ElaTypeAndLicensesSignUpTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_GlobalMVP.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        var digitalLine = data.getProductByDataName("LC_DL_75");
        var complianceRecoveryFee = data.getProductByDataName("LC_CRF_51");
        var e911Fee = data.getProductByDataName("LC_E911_52");
        var dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        var dlBasic = data.getProductByDataName("LC_DL-BAS_178");

        var globalRingExDigitalLine = data.getProductByDataName("LC_DLI_282");
        var globalRingExEMEA = data.getProductByDataName("LC_IBO_284");
        var globalRingExUK = data.getProductByDataName("LC_IBO_286");
        var globalRingExAPAC = data.getProductByDataName("LC_IBO_288");
        var globalRingExLATAM = data.getProductByDataName("LC_IBO_290");
        var dlBasicInternational = data.getProductByDataName("LC_IBO-BAS_496");

        //  1 DL Unlimited + 1 DL Basic = 2 (for parent DL and fees)
        digitalLine.quantity = 2;
        complianceRecoveryFee.quantity = 2;
        e911Fee.quantity = 2;
        dlUnlimited.quantity = 1;
        dlBasic.quantity = 1;

        //  1 Global RingEX EMEA + 1 Global RingEX UK + 1 Global RingEX APAC + 1 Global RingEX LATAM + 1 DL Basic International = 5 (for parent Global DL)
        globalRingExDigitalLine.quantity = 5;
        globalRingExEMEA.quantity = 1;
        globalRingExUK.quantity = 1;
        globalRingExAPAC.quantity = 1;
        globalRingExLATAM.quantity = 1;
        dlBasicInternational.quantity = 1;

        productsToAdd = new Product[]{
                dlBasic, dlBasicInternational,
                globalRingExEMEA, globalRingExUK, globalRingExAPAC, globalRingExLATAM
        };
        productsThatShouldBeInElaSettingsLicenses = List.of(
                digitalLine, complianceRecoveryFee, e911Fee, dlUnlimited, dlBasic,
                globalRingExDigitalLine, globalRingExEMEA, globalRingExUK, globalRingExAPAC, globalRingExLATAM, dlBasicInternational
        );
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        account = steps.salesFlow.account;
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        opportunity = steps.quoteWizard.opportunity;

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-21707")
    @TmsLink("CRM-24786")
    @DisplayName("CRM-21707 - Send ELAType parameter to Funnel. \n" +
            "CRM-24786 - Sign Up Licenses with 'Enable ELA Phase 2' Feature Toggle")
    @Description("CRM-21707 - Verify that during the Sign Up of the ELA Service or ELA Billing Account, " +
            "ELAType will be sent to the Sales Funnel with the following logic: \n" +
            "- ELAType = 'Billing' for ELA Billing Account \n" +
            "- ELAType = 'Service' for ELA Service Account \n\n" +
            "CRM-24786 - Verify that when Feature Toggle 'Enable ELA Phase 2' = true (org-wide setting): \n" +
            "- Sign Up Body includes ELA licenses as a separate list\n" +
            "- Sign Up flow can be completed successfully")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, add products on the Add Products tab, " +
                "and save changes on the Price tab", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(opportunity.getId());
            steps.quoteWizard.selectDefaultPackageFromTestData();

            //  Add these products to check CRM-24786 later
            steps.quoteWizard.addProductsOnProductsTab(productsToAdd);

            cartPage.openTab();
            cartPage.saveChanges();
        });

        step("2. Open the Quote Details tab, populate Main Area Code, Start Date, Payment Method, and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.selectPaymentMethod(CREDIT_CARD_PAYMENT_METHOD);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();
        });

        step("3. Update the current quote to the Active Agreement status via API", () -> {
            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(opportunity);
        });

        step("4. Re-login as a user with 'System Administrator' profile " +
                "and 'UI-less Sign Up Preview for Admins' Permission Set, " +
                "and open the Opportunity record page", () -> {
            var adminWithUiLessSignUpPreviewPS = getUser()
                    .withProfile(SYSTEM_ADMINISTRATOR_PROFILE)
                    .withPermissionSet(UI_LESS_SIGN_UP_PREVIEW_FOR_ADMINS_PS)
                    .execute();

            steps.sfdc.reLoginAsUserWithSessionReset(adminWithUiLessSignUpPreviewPS);

            opportunityPage.openPage(opportunity.getId());
        });

        step("5. Set Account.ELA_Account_Type__c = 'ELA Billing' via API", () -> {
            account.setELA_Account_Type__c(ELA_BILLING_ACCOUNT_TYPE);
            enterpriseConnectionUtils.update(account);
        });

        //  CRM-21707
        step("6. Press 'Process Order' button on the Opportunity record page, " +
                "select the default Timezone, open the 'Admin Preview' tab, " +
                "check that the 'Sign Up body' text includes 'ELAType': 'Billing', " +
                "and close the modal window", () -> {
            checkElaTypeInSignUpBodyInProcessOrderModal("includes 'ELAType': 'Billing'", BILLING_ELA_TYPE);
            opportunityPage.processOrderModal.closeWindow();
        });

        step("7. Set Account.ELA_Account_Type__c = null via API", () -> {
            var accountToUpdate = new Account();
            accountToUpdate.setId(account.getId());
            accountToUpdate.setFieldsToNull(new String[]{"ELA_Account_Type__c"});
            enterpriseConnectionUtils.update(accountToUpdate);
        });

        //  CRM-21707
        step("8. Press 'Process Order' button on the Opportunity record page, " +
                "select the default Timezone, open the 'Admin Preview' tab, " +
                "check that the 'Sign Up body' text does not include 'ELAType', " +
                "and close the modal window", () -> {
            checkElaTypeInSignUpBodyInProcessOrderModal("does not include 'ELAType'", null);
            opportunityPage.processOrderModal.closeWindow();
        });

        step("9. Set Account.ELA_Account_Type__c = 'ELA Service' via API", () -> {
            account.setELA_Account_Type__c(ELA_SERVICE_ACCOUNT_TYPE);
            enterpriseConnectionUtils.update(account);
        });

        //  CRM-21707
        step("10. Press 'Process Order' button on the Opportunity record page, " +
                "select the default Timezone, open the 'Admin Preview' tab, " +
                "check that the 'Sign Up body' text includes 'ELAType': 'Service'", () -> {
            checkElaTypeInSignUpBodyInProcessOrderModal("includes 'ELAType': 'Service'", SERVICE_ELA_TYPE);
        });

        //  CRM-24786
        step("11. Check that the 'Sign Up body' text includes the correct 'SubscriptionInfo.ELAContract' content", () -> {
            var signUpBodyText = opportunityPage.processOrderModal.signUpBodyContents.getText();
            var signUpBodyObj = JsonUtils.readJson(signUpBodyText, SignUpBodyFunnelDTO.class);

            assertThat(signUpBodyObj.subscriptionInfo)
                    .as(format("SignUpBody's 'SubscriptionInfo' object's value (SignUpBody contents: %s)", signUpBodyText))
                    .isNotNull();
            assertThat(signUpBodyObj.subscriptionInfo.elaContract)
                    .as(format("SignUpBody's 'SubscriptionInfo.ELAContract' object's value (SignUpBody contents: %s)", signUpBodyText))
                    .isNotNull();
            assertThat(signUpBodyObj.subscriptionInfo.elaContract.licenses)
                    .as(format("SignUpBody's 'SubscriptionInfo.ELAContract.licenses[]' value (SignUpBody contents: %s)", signUpBodyText))
                    .isNull();
            assertThat(signUpBodyObj.subscriptionInfo.elaContract.elaSettings)
                    .as(format("SignUpBody's 'SubscriptionInfo.ELAContract.elaSettings' object's value (SignUpBody contents: %s)", signUpBodyText))
                    .isNotNull();

            var elaSettingsLicenses = signUpBodyObj.subscriptionInfo.elaContract.elaSettings.licenses;
            assertThat(elaSettingsLicenses)
                    .as(format("SignUpBody's 'SubscriptionInfo.ELAContract.elaSettings.licenses[]' size (licenses[] contents: %s)", elaSettingsLicenses))
                    .hasSize(productsThatShouldBeInElaSettingsLicenses.size());

            productsThatShouldBeInElaSettingsLicenses.forEach(product -> {
                step(format("Check that data for the product '%s' (%s) is correct", product.name, product.dataName), () -> {
                    var productInElaSettings = elaSettingsLicenses.stream()
                            .filter(elaSettingsLicense -> elaSettingsLicense.catalogId.equals(product.dataName))
                            .findFirst();

                    assertThat(productInElaSettings)
                            .as(format("SignUpBody's 'SubscriptionInfo.ELAContract.elaSettings.licenses[]' value " +
                                    "for the product '%s' (licenses[] contents: %s)", product.dataName, elaSettingsLicenses))
                            .isPresent();
                    assertThat(productInElaSettings.get().maxQty)
                            .as(format("SignUpBody's 'SubscriptionInfo.ELAContract.elaSettings.licenses[].maxQty' value " +
                                    "for the product '%s' (licenses[] contents: %s)", product.dataName, elaSettingsLicenses))
                            .isEqualTo(product.quantity);
                });
            });
        });

        //  CRM-24786
        step("12. Switch back to the 'Sign Up' tab in the 'Process Order' modal window, " +
                "click the 'Sign Up MVP' button, and check that the account is processed for signing up without any errors", () -> {
            opportunityPage.processOrderModal.signUpTab.click();
            opportunityPage.processOrderModal.signUpButton.click();

            opportunityPage.processOrderModal.signUpMvpStatus.shouldHave(exactTextCaseSensitive(format(YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE, MVP_SERVICE)), ofSeconds(60));
            opportunityPage.processOrderModal.errorNotifications.shouldHave(size(0));
        });

        //  CRM-24786
        step("13. Check that Billing_ID__c and RC_User_ID__c fields are populated on the Account in SFDC from NGBS", () -> {
            assertWithTimeout(() -> {
                var accountUpdated = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id, Billing_ID__c, RC_User_ID__c " +
                                "FROM Account " +
                                "WHERE Id = '" + account.getId() + "'",
                        Account.class);
                assertNotNull(accountUpdated.getBilling_ID__c(), "Account.Billing_ID__c field");
                assertNotNull(accountUpdated.getRC_User_ID__c(), "Account.RC_User_ID__c field");

                step("Account.Billing_ID__c = " + accountUpdated.getBilling_ID__c());
                step("Account.RC_User_ID__c = " + accountUpdated.getRC_User_ID__c());

                return accountUpdated;
            }, ofSeconds(120), ofSeconds(5));
        });
    }

    /**
     * Open the 'Process Order' button on the Opportunity record page,
     * get the SignUpBody contents on the 'Admin Preview' tab,
     * and check 'ELAType' in it.
     *
     * @param additionalAssertStepDescription additional step's description for the assertion step
     * @param elaTypeExpectedResult           expected value of the 'ELAType' parameter in the SignUpBody
     *                                        ("stringValue" or {@code null})
     */
    private void checkElaTypeInSignUpBodyInProcessOrderModal(String additionalAssertStepDescription, String elaTypeExpectedResult) {
        step("Press 'Process Order' button on the Opportunity record page, " +
                "verify that 'Preparing Data' step is completed, and no errors are displayed, " +
                "select the default Timezone, and open the 'Admin Preview' tab", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();

            opportunityPage.processOrderModal.selectDefaultTimezone();

            opportunityPage.processOrderModal.adminPreviewTab.click();
            opportunityPage.processOrderModal.signUpBodyContents.shouldNotHave(exactText(EMPTY_STRING), ofSeconds(20));
        });

        step("Check that the 'Sign Up body' text " + additionalAssertStepDescription, () -> {
            var signUpBodyText = opportunityPage.processOrderModal.signUpBodyContents.getText();
            var signUpBodyObj = JsonUtils.readJson(signUpBodyText, SignUpBodyFunnelDTO.class);

            assertThat(signUpBodyObj.elaType)
                    .as(format("SignUpBody's 'ELAType' value (SignUpBody contents: %s)", signUpBodyText))
                    .isEqualTo(elaTypeExpectedResult);
        });
    }
}
