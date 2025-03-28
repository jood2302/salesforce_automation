package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal.AccountManagerModal.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.USD_CURRENCY_ISO_CODE;
import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomPositiveInteger;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewCustomerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityFactory.createOpportunity;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.PackageFactory.createBillingAccountPackage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.ACTIVE_QUOTE_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.DRAFT_QUOTE_STATUS;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("IndiaMVP")
public class IndianAccountManagerValidationsTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Opportunity indianOpportunity;
    private Account masterAccount;
    private Opportunity masterOpportunity;
    private Quote masterQuote;
    private Package__c masterAccountBillingPackage;

    //  Test data
    private final String billingIdRandomValue;
    private final String officeServiceType;
    private final String rcIndiaCurrencyIsoCode;
    private final String rcIndiaBrandName;
    private final Package rcUsOfficePackage;

    public IndianAccountManagerValidationsTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_IndiaAndUS_MVP_Monthly_Contract.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        billingIdRandomValue = getRandomPositiveInteger();
        officeServiceType = data.packageFolders[0].name;
        rcIndiaCurrencyIsoCode = data.getCurrencyIsoCode();
        rcIndiaBrandName = data.getBrandName();
        rcUsOfficePackage = data.packageFolders[0].packages[1];
    }

    @BeforeEach
    public void setUpTest() {
        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);

        indianOpportunity = steps.quoteWizard.opportunity;

        step("Create 'Master Account' as New Business Office (RC US) account " +
                "with related Contact and AccountContactRole records via API", () -> {
            masterAccount = createNewCustomerAccountInSFDC(dealDeskUser,
                    new AccountData().withCurrencyIsoCode(USD_CURRENCY_ISO_CODE).withBillingCountry(US_BILLING_COUNTRY));
        });

        step("Create New Business 'Master' Opportunity via API", () -> {
            var masterAccountContact = getPrimaryContactOnAccount(masterAccount);
            masterOpportunity = createOpportunity(masterAccount, masterAccountContact, true,
                    RC_US_BRAND_NAME, RC_US_BI_ID, dealDeskUser, USD_CURRENCY_ISO_CODE, officeServiceType);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);

        step("Open the Quote Wizard for the Master Account's Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(masterOpportunity.getId());

            packagePage.packageSelector.selectPackage(data.chargeTerm, officeServiceType, rcUsOfficePackage);
            packagePage.saveChanges();
        });

        step("Open the Quote Details tab, populate Main Area Code, Start Date, save changes, " +
                "and update it to the Active Agreement via API", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();

            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(masterOpportunity);
        });
    }

    @Test
    @TmsLink("CRM-23995")
    @DisplayName("CRM-23995 - Validations in the Account Manager")
    @Description("Verify that only following brands can be linked to the Indian accounts:\n" +
            "- RingCentral EU\n" +
            "- RingCentral\n" +
            "- RingCentral Canada\n" +
            "- RingCentral UK\n" +
            "\n" +
            "Verify that only contracted accounts can be linked to Indian accounts.\n" +
            "Verify that only Existing business can be linked to Indian accounts")
    public void test() {
        step("1. Set Master Account's RC_Brand__c = 'Avaya Cloud Office', Billing_ID__c and RC_User_ID__c = random value, " +
                "Service_Type__c and RC_Service_name__c = 'Office' and RC_Account_Status__c = 'Paid', " +
                "and create a new Billing Account Package object (Package__c) for the Office Account (all via API)", () -> {
            masterAccount.setRC_Brand__c(AVAYA_US_BRAND_NAME);
            masterAccount.setBilling_ID__c(billingIdRandomValue);
            masterAccount.setService_Type__c(officeServiceType);
            masterAccount.setRC_Service_name__c(officeServiceType);
            masterAccount.setRC_Account_Status__c(PAID_RC_ACCOUNT_STATUS);
            setRandomEnterpriseAccountId(masterAccount);
            enterpriseConnectionUtils.update(masterAccount);

            masterAccountBillingPackage = createBillingAccountPackage(masterAccount.getId(), masterAccount.getRC_User_ID__c(),
                    rcUsOfficePackage.id, AVAYA_US_BRAND_NAME, officeServiceType, CREDIT_CARD_PAYMENT_METHOD, PAID_RC_ACCOUNT_STATUS);
        });

        //  For appearance of 'Manage Account bindings' button on the next steps
        step("2. Open the Quote Wizard for the Indian Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(indianOpportunity.getId())
        );

        step("3. Open Quote Wizard for the Indian Opportunity, open Account Manager modal window, " +
                "select Master Account and check error validation message", () ->
                checkValidationErrorMessageTestSteps(MASTER_ACCOUNT_BRAND_SHOULD_BE_RC_ERROR)
        );

        step("4. Set Master Account's RC_Brand__c = 'RingCentral', " +
                "clear RC_Account_Status__c and Billing_ID__c fields, " +
                "set RC_Brand__c = 'RingCentral', " +
                "and clear RC_Account_Status__c fields on the related Package__c record (all via API)", () -> {
            masterAccount.setRC_Brand__c(RINGCENTRAL_RC_BRAND);
            enterpriseConnectionUtils.update(masterAccount);

            var officeAccountToUpdate = new Account();
            officeAccountToUpdate.setId(masterAccount.getId());
            officeAccountToUpdate.setFieldsToNull(new String[]{"Billing_ID__c", "RC_Account_Status__c"});
            enterpriseConnectionUtils.update(officeAccountToUpdate);

            masterAccountBillingPackage.setRC_Brand__c(RINGCENTRAL_RC_BRAND);
            enterpriseConnectionUtils.update(masterAccountBillingPackage);

            var masterAccountBillingPackageToUpdate = new Package__c();
            masterAccountBillingPackageToUpdate.setId(masterAccountBillingPackage.getId());
            masterAccountBillingPackageToUpdate.setFieldsToNull(new String[]{"RC_Account_Status__c"});
            enterpriseConnectionUtils.update(masterAccountBillingPackageToUpdate);
        });

        step("5. Open Quote Wizard for the Indian Opportunity, open Account Manager modal window, " +
                "select Master Account and check error validation message", () ->
                checkValidationErrorMessageTestSteps(MASTER_ACCOUNT_SHOULD_BE_EXISTING_BUSINESS_ERROR)
        );

        step("6. Set RC_Account_Status__c = 'Paid' on the Master Account and its related Package__c, " +
                "set Master Account.Billing_ID__c = random value, " +
                "and change Office Quote's Status = 'Draft' (all via API)", () -> {
            masterAccount.setBilling_ID__c(billingIdRandomValue);
            masterAccount.setRC_Account_Status__c(PAID_RC_ACCOUNT_STATUS);
            enterpriseConnectionUtils.update(masterAccount);

            masterAccountBillingPackage.setRC_Account_Status__c(PAID_RC_ACCOUNT_STATUS);
            enterpriseConnectionUtils.update(masterAccountBillingPackage);

            masterQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + masterOpportunity.getId() + "'",
                    Quote.class);
            masterQuote.setStatus(DRAFT_QUOTE_STATUS);
            enterpriseConnectionUtils.update(masterQuote);
        });

        step("7. Open Quote Wizard for the Indian Opportunity, open Account Manager modal window, " +
                "select Master Account and check error validation message", () ->
                checkValidationErrorMessageTestSteps(MASTER_ACCOUNT_IS_NOT_CONTRACTED_ERROR)
        );

        step("8. Clear Master Account's Billing_ID__c, and set Office Quote's Status = 'Active' via API", () -> {
            var officeAccountToUpdate = new Account();
            officeAccountToUpdate.setId(masterAccount.getId());
            officeAccountToUpdate.setFieldsToNull(new String[]{"Billing_ID__c"});
            enterpriseConnectionUtils.update(officeAccountToUpdate);

            masterQuote.setStatus(ACTIVE_QUOTE_STATUS);
            enterpriseConnectionUtils.update(masterQuote);
        });

        step("9. Open Quote Wizard for the Indian Opportunity, open Account Manager modal window, " +
                "select Master Account and check error validation message", () ->
                checkValidationErrorMessageTestSteps(MASTER_ACCOUNT_SHOULD_BE_EXISTING_BUSINESS_ERROR)
        );

        step("10. Set Service_Type__c and RC_Service_name__c = 'Fax' on the Master Account and its related Package__c, " +
                "and Master Account.Billing_ID__c = random value (all via API)", () -> {
            masterAccount.setBilling_ID__c(billingIdRandomValue);
            masterAccount.setService_Type__c(FAX_SERVICE_TYPE);
            masterAccount.setRC_Service_name__c(FAX_SERVICE_TYPE);
            enterpriseConnectionUtils.update(masterAccount);

            masterAccountBillingPackage.setRC_Service_name__c(FAX_SERVICE_TYPE);
            masterAccountBillingPackage.setService_Type__c(FAX_SERVICE_TYPE);
            enterpriseConnectionUtils.update(masterAccountBillingPackage);
        });

        step("11. Open Quote Wizard for the Indian Opportunity, open Account Manager modal window, " +
                "select Master Account and check error validation message", () ->
                checkValidationErrorMessageTestSteps(MASTER_ACCOUNT_SERVICE_TYPE_SHOULD_BE_OFFICE_ERROR)
        );

        step("12. Set RC_Account_Status__c = 'Pending', Service_Type__c  " +
                "and RC_Service_name__c = 'Office' on the Master Account and its related Package__c (all via API)", () -> {
            masterAccount.setService_Type__c(OFFICE_SERVICE);
            masterAccount.setRC_Service_name__c(OFFICE_SERVICE);
            masterAccount.setRC_Account_Status__c(PENDING_RC_ACCOUNT_STATUS);
            enterpriseConnectionUtils.update(masterAccount);

            masterAccountBillingPackage.setRC_Service_name__c(OFFICE_SERVICE);
            masterAccountBillingPackage.setService_Type__c(OFFICE_SERVICE);
            masterAccountBillingPackage.setRC_Account_Status__c(PENDING_RC_ACCOUNT_STATUS);
            enterpriseConnectionUtils.update(masterAccountBillingPackage);
        });

        step("13. Open Quote Wizard for the Indian Opportunity, open Account Manager modal window, " +
                "select Master Account and check error validation message", () ->
                checkValidationErrorMessageTestSteps(MASTER_ACCOUNT_STATUS_SHOULD_BE_PAID_ERROR)
        );

        step("14. Set RC_Brand__c = 'RingCentral EU', CurrencyIsoCode = 'INR', " +
                "and RC_Account_Status__c = 'Paid' on the Master Account and its related Package__c (all via API)", () -> {
            masterAccount.setRC_Brand__c(rcIndiaBrandName);
            masterAccount.setCurrencyIsoCode(rcIndiaCurrencyIsoCode);
            masterAccount.setRC_Account_Status__c(PAID_RC_ACCOUNT_STATUS);
            enterpriseConnectionUtils.update(masterAccount);

            masterAccountBillingPackage.setRC_Brand__c(rcIndiaBrandName);
            masterAccountBillingPackage.setCurrencyIsoCode(rcIndiaCurrencyIsoCode);
            masterAccountBillingPackage.setRC_Account_Status__c(PAID_RC_ACCOUNT_STATUS);
            enterpriseConnectionUtils.update(masterAccountBillingPackage);
        });

        step("15. Open Quote Wizard for the Indian Opportunity, open Account Manager modal window, " +
                "select Master Account and check error validation message", () ->
                checkValidationErrorMessageTestSteps(MASTER_ACCOUNT_SHOULD_NOT_BE_INDIAN_ERROR)
        );
    }

    /**
     * Reload the Quote Wizard for the Indian Opportunity, open Account Manager modal window on the Quote Details tab,
     * select Master Account and check error validation message.
     *
     * @param expectedMessage message that is expected to appear after selecting Master Account
     */
    private void checkValidationErrorMessageTestSteps(String expectedMessage) {
        step("Reload the Quote Wizard for the Indian Opportunity", () -> {
            //  reopen the Quote Wizard to obtain actual values on the Master Account
            refresh();
            wizardPage.waitUntilLoaded();
            packagePage.packageSelector.waitUntilLoaded();
        });

        step("Open the Quote Details tab, open Account Manager modal window, " +
                "select Master Account and check the error notification message", () -> {
            quotePage.openTab();
            quotePage.manageAccountBindingsButton.click();
            quotePage.manageAccountBindings.accountSearchInput.selectItemInCombobox(masterAccount.getName());

            quotePage.manageAccountBindings.notifications.shouldHave(itemWithText(expectedMessage));
        });
    }
}
