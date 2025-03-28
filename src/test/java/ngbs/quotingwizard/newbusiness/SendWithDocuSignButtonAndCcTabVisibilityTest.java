package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.Opportunity;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.quotePage;
import static base.Pages.wizardBodyPage;
import static com.aquiva.autotests.rc.utilities.StringHelper.CHF_CURRENCY_ISO_CODE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityFactory.createOpportunity;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.SWITZERLAND_BILLING_COUNTRY;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.getPrimaryContactOnAccount;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.*;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("PDV")
@Tag("Avaya")
@Tag("ContactCenter")
public class SendWithDocuSignButtonAndCcTabVisibilityTest extends BaseTest {
    private final Steps steps;

    private User dealDeskUser;
    private Opportunity avayaUsOpportunity;
    private Opportunity unifySwissOpportunity;
    private Opportunity rcSwissOpportunity;

    //  Test data
    private final String tierName;

    public SendWithDocuSignButtonAndCcTabVisibilityTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/Avaya_Office_Monthly_Contract_1TypeOfDLs.json",
                Dataset.class);
        steps = new Steps(data);

        tierName = data.packageFolders[0].name;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        dealDeskUser = steps.salesFlow.getDealDeskUser();

        step("Create an Opportunity (along with related Account/Contact/AccountContactRole records) " +
                "with Brand = 'Avaya Cloud Office' via API", () -> {
            steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
            steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);
            avayaUsOpportunity = steps.quoteWizard.opportunity;
        });

        var swissAccountData = new AccountData()
                .withBillingCountry(SWITZERLAND_BILLING_COUNTRY)
                .withCurrencyIsoCode(CHF_CURRENCY_ISO_CODE);

        step("Create an Opportunity (along with related Account/Contact/AccountContactRole records) " +
                "with Business Identity = 'Unify Office CH' via API", () -> {
            var unifySwissAccount = createAccountInSFDC(dealDeskUser, swissAccountData.withRcBrand(UNIFY_OFFICE_BRAND_NAME));
            var unifySwissContact = getPrimaryContactOnAccount(unifySwissAccount);
            unifySwissOpportunity = createOpportunity(unifySwissAccount, unifySwissContact, true,
                    UNIFY_OFFICE_BRAND_NAME, UNIFY_CH_BI_ID, dealDeskUser, CHF_CURRENCY_ISO_CODE, tierName);
        });

        step("Create an Opportunity (along with related Account/Contact/AccountContactRole records) " +
                "with Business Identity = 'RingCentral CH GmbH' via API", () -> {
            var rcSwissAccount = createAccountInSFDC(salesRepUser, swissAccountData.withRcBrand(RC_EU_BRAND_NAME));
            var rcSwissContact = getPrimaryContactOnAccount(rcSwissAccount);
            rcSwissOpportunity = createOpportunity(rcSwissAccount, rcSwissContact, true,
                    RC_EU_BRAND_NAME, RC_CH_BI_ID, salesRepUser, CHF_CURRENCY_ISO_CODE, tierName);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-21651")
    @TmsLink("CRM-21650")
    @DisplayName("CRM-21651 - 'Send with DocuSign' button is hidden for Avaya Cloud Office \n" +
            "CRM-21650 - Contact Center tab is hidden for Avaya Cloud Office brand, " +
            "RingCentral CH GmbH and Unify Office CH Business Identities")
    @Description("CRM-21651 - Verify if Opportunity has Brand = 'Avaya Cloud Office' then 'Send with DocuSign' button is hidden. \n" +
            "CRM-21650 - Verify if Opportunity has Brand equal to 'Avaya Cloud Office' " +
            "or Business Identity = 'Unify Office CH' or 'RingCentral CH GmbH' then 'Contact Center' tab button is hidden")
    public void test() {
        step("1. Open the Quote Wizard for the New Business Opportunity with Business Identity = 'RingCentral CH GmbH'", () ->
                steps.quoteWizard.openQuoteWizardDirect(rcSwissOpportunity.getId())
        );

        //  CRM-21650
        step("2. Verify that Contact Center tab is hidden", () -> {
            wizardBodyPage.contactCenterTab.shouldBe(hidden);
        });

        step("3. Re-login as a user with 'Deal Desk Lightning' profile", () ->
                steps.sfdc.reLoginAsUser(dealDeskUser)
        );

        step("4. Open the Quote Wizard for the New Business Opportunity with Brand_Name__c = 'Avaya Cloud Office' " +
                "to add a new Sales Quote, select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(avayaUsOpportunity.getId())
        );

        step("5. Open the Quote Details tab, and verify that 'Send with DocuSign' button is hidden, " +
                "and that the Contact Center tab is hidden as well", () -> {
            quotePage.openTab();

            //  CRM-21651
            quotePage.sendWithDocuSignButton.shouldNot(exist);
            //  CRM-21650
            wizardBodyPage.contactCenterTab.shouldBe(hidden);
        });

        step("6. Open the Quote Wizard for the New Business Opportunity with Business Identity = 'Unify Office CH'", () ->
                steps.quoteWizard.openQuoteWizardDirect(unifySwissOpportunity.getId())
        );

        //  CRM-21650
        step("7. Verify that Contact Center tab is hidden", () -> {
            wizardBodyPage.contactCenterTab.shouldBe(hidden);
        });
    }
}
