package ngbs.quotingwizard.newbusiness.signup;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityShareFactory;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.Map;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.ALASKA_TIME_ZONE;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.CONTACT_CENTER_SERVICE;
import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomPositiveInteger;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.CaseHelper.INCONTACT_COMPLETED_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.CaseHelper.INCONTACT_ORDER_RECORD_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.EXECUTED_QUOTE_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.setQuoteToApprovedActiveAgreement;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.PROFESSIONAL_SERVICES_LIGHTNING_PROFILE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.getUser;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThanOrEqual;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("MultiProduct")
@Tag("SignUp")
@Tag("ContactCenter")
public class RcContactCenterSignUpTest extends BaseTest {
    private final Dataset data;
    private final MultiProductSignUpSteps multiProductSignUpSteps;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User dealDeskUser;
    private Quote additionalActiveSalesAgreement;
    private String quoteId;

    //  Test data
    private final String rcCcServiceName;
    private final Product proServProductToAdd;
    private final Map<String, Package> packageFolderNameToPackageMap;

    private final String rcMainNumberValue;
    private final String usGeoRegionOption;
    private final String ringCentralTeam;
    private final String inContactSegmentOption;
    private final String inContactBuId;

    public RcContactCenterSignUpTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Annual_Contract_196116013_ED_Standalone_CC_EV_NB.json",
                Dataset.class);

        steps = new Steps(data);
        multiProductSignUpSteps = new MultiProductSignUpSteps();
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        steps.ngbs.isGenerateAccountsForSingleTest = true;

        rcCcServiceName = data.packageFolders[2].name;
        proServProductToAdd = data.packageFolders[2].packages[0].productsOther[0];
        packageFolderNameToPackageMap = Map.of(
                data.packageFolders[0].name, data.packageFolders[0].packages[0],
                data.packageFolders[2].name, data.packageFolders[2].packages[0]
        );

        rcMainNumberValue = getRandomPositiveInteger();
        usGeoRegionOption = "US";
        ringCentralTeam = "RingCentral";
        inContactSegmentOption = "1-50 Seats";
        inContactBuId = getRandomPositiveInteger();
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();
        steps.ngbs.stepCreateContractInNGBS();

        dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);

        step("Create an additional Existing Business Opportunity with the related Active Sales Agreement, " +
                "and and close this Opportunity (all via API)", () -> {
            additionalActiveSalesAgreement = steps.syncWithNgbs.stepCreateAdditionalActiveSalesAgreement(
                    steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);

            var bufferOpportunity = new Opportunity();
            bufferOpportunity.setId(additionalActiveSalesAgreement.getOpportunityId());
            steps.quoteWizard.stepCloseOpportunity(bufferOpportunity);
        });

        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-35212")
    @DisplayName("CRM-35212 - E2E Scenario of Sign Up RC CC")
    @Description("Check that RingCentral Contact Center service can be signed up through Multi-Product sales flow")
    public void test() {
        step("1. Open the the Quote Wizard to add a new Sales Quote, select RC CC package and save changes", () -> {
            steps.quoteWizard.prepareOpportunityForMultiProduct(steps.quoteWizard.opportunity.getId(), packageFolderNameToPackageMap);
        });

        step("2. Open the Price tab, click 'Initiate CC ProServ' button, click 'Submit' button in popup window " +
                "and check that 'Initiate CC ProServ' button is hidden and 'CC ProServ Created' button is visible and disabled", () -> {
            cartPage.openTab();
            cartPage.initiateCcProServ();

            cartPage.initiateCcProServButton.shouldBe(hidden);
            cartPage.ccProServCreatedButton.shouldBe(disabled);
        });

        step("3. Re-login as a user with 'Professional Services Lightning' profile, " +
                "manually share the Opportunity with this user via API, " +
                "and re-open the Sales Quote in the Quote Wizard for the test Opportunity", () -> {
            quoteId = wizardPage.getSelectedQuoteId();

            var proServUser = getUser().withProfile(PROFESSIONAL_SERVICES_LIGHTNING_PROFILE).execute();
            steps.sfdc.reLoginAsUser(proServUser);

            OpportunityShareFactory.shareOpportunity(steps.quoteWizard.opportunity.getId(), proServUser.getId());

            wizardPage.openPage(steps.quoteWizard.opportunity.getId(), quoteId);
            wizardPage.waitUntilLoaded();
        });

        step("4. Open the ProServ Quote tab and prepare the CC ProServ Quote for the sign-up flow", () -> {
            steps.proServ.prepareCcProServQuoteForSignUp(proServProductToAdd.name);
        });

        step("5. Create approved Invoicing Approval Request for the test Account via API", () -> {
            createInvoiceApprovalApproved(steps.quoteWizard.opportunity, steps.salesFlow.account, steps.salesFlow.contact,
                    dealDeskUser.getId(), true);
        });

        step("6. Re-login as a user with 'Deal Desk Lightning' profile, " +
                "and re-open the Sales Quote in the Quote Wizard for the test Opportunity,", () -> {
            steps.sfdc.reLoginAsUser(dealDeskUser);

            wizardPage.openPage(steps.quoteWizard.opportunity.getId(), quoteId);
            cartPage.waitUntilLoaded();
        });

        step("7. Open the Quote Details tab, populate RingCentral Main Number, save changes, " +
                "and check Opportunity's NumberToBeCCNumber__c and NumberToBeRCMainNumber__c field values", () -> {
            quotePage.openTab();
            quotePage.rcMainNumberInput.setValue(rcMainNumberValue);
            quotePage.saveChanges();

            var opportunityUpdated = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, NumberToBeCCNumber__c, NumberToBeRCMainNumber__c " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Opportunity.class);
            assertThat(opportunityUpdated.getNumberToBeCCNumber__c())
                    .as("Opportunity.NumberToBeCCNumber__c value")
                    .isEqualTo(rcMainNumberValue);
            assertThat(opportunityUpdated.getNumberToBeRCMainNumber__c())
                    .as("Opportunity.NumberToBeRCMainNumber__c value")
                    .isEqualTo(rcMainNumberValue);
        });

        step("8. Set Status = 'Executed' on the existing additional Active Sales Agreement, " +
                "and update the current Quote to the Active Sales Agreement (all via API)", () -> {
            additionalActiveSalesAgreement.setStatus(EXECUTED_QUOTE_STATUS);
            enterpriseConnectionUtils.update(additionalActiveSalesAgreement);

            var quoteToUpdate = new Quote();
            quoteToUpdate.setId(quoteId);
            setQuoteToApprovedActiveAgreement(quoteToUpdate);
            enterpriseConnectionUtils.update(quoteToUpdate);
        });

        step("9. Close the test Opportunity via API", () ->
                steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity)
        );

        step("10. Open the Opportunity record page, click 'Process Order' button, " +
                "expand RingCentral Contact Center section " +
                "and populate necessary fields in 'Add General Information' section", () -> {
            opportunityPage.openPage(steps.quoteWizard.opportunity.getId());
            opportunityPage.clickProcessOrderButton();
            processOrderModal.mvpAllSyncStepNames.shouldHave(sizeGreaterThanOrEqual(1), ofSeconds(120));
            opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(120));
            opportunityPage.processOrderModal.errorNotifications.shouldHave(size(0));

            opportunityPage.processOrderModal.expandContactCenterSection();
            opportunityPage.processOrderModal.clickProcessContactCenter();

            opportunityPage.processOrderModal.rcCcTimezoneSelect.selectOption(ALASKA_TIME_ZONE);
            opportunityPage.processOrderModal.selectGeoRegionPicklist.selectOption(usGeoRegionOption);
            opportunityPage.processOrderModal.selectImplementationTeamPicklist.selectOption(ringCentralTeam);
            opportunityPage.processOrderModal.selectInContactSegmentPicklist.selectOption(inContactSegmentOption);
            opportunityPage.processOrderModal.ccNumberInput.shouldHave(exactValue(rcMainNumberValue));
        });

        step("11. Click 'Sign Up Contact Center' button, " +
                "and check that success notification is shown in the Process Order modal window", () -> {
            multiProductSignUpSteps.signUpFinalStep(CONTACT_CENTER_SERVICE);
        });

        step("12. Populate 'inContact_BU_ID__c' and 'inContact_Status__c' fields " +
                "of the related Case with Record Type = 'inContact_Order' via API, " +
                "and check that RC CC package is added to the account in NGBS", () -> {
            var inContactOrderCase = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Case " +
                            "WHERE Opportunity_Reference__c = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND RecordTypeName__c = '" + INCONTACT_ORDER_RECORD_TYPE + "'",
                    Case.class);
            inContactOrderCase.setInContact_BU_ID__c(Double.valueOf(inContactBuId));
            inContactOrderCase.setInContact_Status__c(INCONTACT_COMPLETED_STATUS);
            enterpriseConnectionUtils.update(inContactOrderCase);

            multiProductSignUpSteps.checkAddedPackageAfterSignUp(data.billingId, rcCcServiceName);
        });
    }
}
