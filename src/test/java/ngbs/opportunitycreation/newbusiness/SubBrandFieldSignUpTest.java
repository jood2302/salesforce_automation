package ngbs.opportunitycreation.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.funnel.SignUpBodyFunnelDTO;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.Account;
import com.sforce.soap.enterprise.sobject.SubBrandsMapping__c;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.opportunityPage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.CREDIT_CARD_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewPartnerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.SubBrandsMappingFactory.createNewSubBrandsMapping;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.Condition.exactText;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("SignUp")
@Tag("Ignite")
public class SubBrandFieldSignUpTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Account partnerAccount;
    private SubBrandsMapping__c testSubBrandsMapping;

    public SubBrandFieldSignUpTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_PhonesAndDLs.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);

        var customerAccount = steps.salesFlow.account;

        step("Create a new Partner Account with RC_Brand__c = '" + data.brandName + "' via API", () -> {
            partnerAccount = createNewPartnerAccountInSFDC(salesRepUser, new AccountData(data));
        });

        step("Create a new SubBrandsMapping__c Custom Setting record for the Partner Account via API", () -> {
            testSubBrandsMapping = createNewSubBrandsMapping(partnerAccount);
        });

        step("Populate Customer Account's Partner_Account__c, Partner_ID__c fields " +
                "with Partner Account field values (all via API)", () -> {
            customerAccount.setPartner_Account__c(partnerAccount.getId());
            customerAccount.setPartner_ID__c(partnerAccount.getPartner_ID__c());
            enterpriseConnectionUtils.update(customerAccount);
        });

        step("Set Sub_Brand__c = '" + testSubBrandsMapping.getSub_Brand__c() + "' via API", () -> {
            //  Sub_Brand__c is only set automatically on the Lead Conversion and when creating Opportunity via QOP.
            //  We are setting it here manually so that Quoting will be fully valid (e.g. 'Add New' button is enabled on the QW/UQT Landing page)
            steps.quoteWizard.opportunity.setSub_Brand__c(testSubBrandsMapping.getSub_Brand__c());
            enterpriseConnectionUtils.update(steps.quoteWizard.opportunity);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-33260")
    @DisplayName("CRM-33260 - Sub_brand__c field value is sent to Sales Funnel upon SignUp")
    @Description("Verify that Sub-brand field value from Opportunity is sent to Sales Funnel upon SignUp")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () -> {
            steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());
        });

        step("2. Open the Quote Details tab, populate Main Area Code, Start Date, set Payment Method = 'Credit Card' " +
                "and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.setDefaultStartDate();
            quotePage.selectPaymentMethod(CREDIT_CARD_PAYMENT_METHOD);
            quotePage.saveChanges();
        });

        step("3. Update the current quote to the Active Agreement status via API", () -> {
            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity);
        });

        step("4. Re-login as a user with 'System Administrator' profile " +
                "and 'UI-less Sign Up Preview for Admins' Permission Set, " +
                "and open the Opportunity record page", () -> {
            var adminWithUiLessSignUpPreviewPS = getUser()
                    .withProfile(SYSTEM_ADMINISTRATOR_PROFILE)
                    .withPermissionSet(UI_LESS_SIGN_UP_PREVIEW_FOR_ADMINS_PS)
                    .execute();

            steps.sfdc.reLoginAsUserWithSessionReset(adminWithUiLessSignUpPreviewPS);

            opportunityPage.openPage(steps.quoteWizard.opportunity.getId());
        });

        step("5. Press 'Process Order' button on the Opportunity record page, " +
                "verify that 'Preparing Data' step is completed, and no errors are displayed, " +
                "select the default Timezone, and open the 'Admin Preview' tab", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();

            opportunityPage.processOrderModal.selectDefaultTimezone();

            opportunityPage.processOrderModal.adminPreviewTab.click();
            opportunityPage.processOrderModal.signUpBodyContents.shouldNotHave(exactText(EMPTY_STRING), ofSeconds(20));
        });

        step("6. Check that the 'Sign Up body' text contains 'uBrandId' = " + testSubBrandsMapping.getSub_Brand__c(), () -> {
            var signUpBodyText = opportunityPage.processOrderModal.signUpBodyContents.getText();
            var signUpBodyObj = JsonUtils.readJson(signUpBodyText, SignUpBodyFunnelDTO.class);

            assertThat(signUpBodyObj.uBrandId)
                    .as(format("SignUpBody's 'uBrandId' value (SignUpBody contents: %s)", signUpBodyText))
                    .isEqualTo(testSubBrandsMapping.getSub_Brand__c());
        });
    }
}
