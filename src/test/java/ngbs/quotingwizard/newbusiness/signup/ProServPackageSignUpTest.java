package ngbs.quotingwizard.newbusiness.signup;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import ngbs.quotingwizard.ProServInNgbsSteps;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static base.Pages.opportunityPage;
import static base.Pages.processOrderModal;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.activateAccountInNGBS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("P0")
@Tag("ProServInNGBS")
@Tag("LTR-121")
@Tag("SignUp")
public class ProServPackageSignUpTest extends BaseTest {
    private final Steps steps;
    private final MultiProductSignUpSteps multiProductSignUpSteps;
    private final ProServInNgbsSteps proServInNgbsSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User dealDeskUserWithProServInNgbsFT;
    private String billingId;

    //  Test data
    private final Map<String, Package> packageFolderNameToPackageMap;
    private final String proServServiceName;
    private final Product[] proServProductsToAdd;
    private final List<String> expectedProServSteps;
    private final int proServPhasesQuantity;

    public ProServPackageSignUpTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_ProServ_Monthly_Contract.json",
                Dataset.class);
        steps = new Steps(data);
        multiProductSignUpSteps = new MultiProductSignUpSteps();
        proServInNgbsSteps = new ProServInNgbsSteps(data, data.packageFolders[4]);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        packageFolderNameToPackageMap = Map.of(
                data.packageFolders[0].name, data.packageFolders[0].packages[0],
                data.packageFolders[4].name, data.packageFolders[4].packages[0]
        );
        proServServiceName = data.packageFolders[4].packages[0].name;
        proServProductsToAdd = data.packageFolders[4].packages[0].products;

        expectedProServSteps = List.of(CHECK_FOR_MAIN_COST_CENTER_STEP, CREATION_OF_A_MAIN_COST_CENTER_STEP, SIGN_UP_PRO_SERV_STEP);
        proServPhasesQuantity = 1;
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with 'Deal Desk Lightning' profile and 'ProServ in NGBS' feature toggle", () -> {
            dealDeskUserWithProServInNgbsFT = getUser()
                    .withProfile(DEAL_DESK_LIGHTNING_PROFILE)
                    .withFeatureToggles(List.of(PROSERV_IN_NGBS_FT))
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUserWithProServInNgbsFT);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUserWithProServInNgbsFT);

        step("Log in as a user with 'Deal Desk Lightning' profile and 'ProServ in NGBS' feature toggle", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUserWithProServInNgbsFT);
        });
    }

    @Test
    @TmsLink("CRM-37516")
    @DisplayName("CRM-37516 - Sign Up for ProServ package")
    @Description("Verify that: \n" +
            "- Steps are shown in the Process Order modal window for Professional Services; \n" +
            "- 'Process Professional Services' button is shown and disabled until MVP is activated; \n" +
            "- 'Process Professional Services' button is displayed for processing steps; \n" +
            "- 'SignUp Professional Services' button is displayed for final activation; \n" +
            "- The ProServ package is linked to the correct NGBS billing account after Sign Up")
    public void test() {
        step("1. Open the Quote Wizard to add a new Sales Quote, select MVP and ProServ packages, and save changes", () -> {
            proServInNgbsSteps.prepareOpportunityWithProServForSignUp(steps.salesFlow.account, steps.salesFlow.contact,
                    steps.quoteWizard.opportunity, dealDeskUserWithProServInNgbsFT.getId(),
                    packageFolderNameToPackageMap, proServProductsToAdd, proServPhasesQuantity);
        });

        step("2. Press 'Process Order' button on the Opportunity's record page, " +
                "expand 'Professional Services' section, check that 'Process Professional Services' button is disabled, " +
                "and that all steps are correct and not completed", () -> {
            opportunityPage.openPage(steps.quoteWizard.opportunity.getId());
            opportunityPage.clickProcessOrderButton();
            processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();

            processOrderModal.professionalServicesExpandButton.click();
            processOrderModal.proServAllSignUpStepNames.shouldHave(exactTexts(expectedProServSteps), ofSeconds(90));
            processOrderModal.proServCompletedSignUpStepNames.shouldHave(size(0));
            processOrderModal.processProfessionalServicesButton.shouldBe(visible, disabled);
        });

        step("3. Expand MVP section, select the timezone, click 'Sign Up MVP', " +
                "check that the account is processed for signing up, " +
                "Billing_ID__c and RC_User_ID__c fields are populated on the Account, " +
                "activate the Account in the NGBS via API, and close Process Order modal window", () -> {
            processOrderModal.mvpExpandButton.click();

            processOrderModal.selectDefaultTimezone();
            processOrderModal.signUpButton.click();

            processOrderModal.signUpMvpStatus
                    .shouldHave(exactTextCaseSensitive(format(YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE, MVP_SERVICE)), ofSeconds(60));

            step("Check that Billing_ID__c and RC_User_ID__c fields are populated on the Master Account, " +
                    "activate the Account in the NGBS via API, and close Process Order modal window", () -> {
                billingId = step("Wait until Account's Billing_ID__c and RC_User_ID__c will get the values from NGBS", () -> {
                    return assertWithTimeout(() -> {
                        var accountUpdated = enterpriseConnectionUtils.querySingleRecord(
                                "SELECT Id, Billing_ID__c, RC_User_ID__c " +
                                        "FROM Account " +
                                        "WHERE Id = '" + steps.salesFlow.account.getId() + "'",
                                Account.class);
                        assertNotNull(accountUpdated.getBilling_ID__c(), "Account.Billing_ID__c field");
                        assertNotNull(accountUpdated.getRC_User_ID__c(), "Account.RC_User_ID__c field");

                        step("Account.Billing_ID__c = " + accountUpdated.getBilling_ID__c());
                        step("Account.RC_User_ID__c = " + accountUpdated.getRC_User_ID__c());

                        return accountUpdated.getBilling_ID__c();
                    }, ofSeconds(120), ofSeconds(5));
                });

                activateAccountInNGBS(billingId);

                processOrderModal.closeWindow();
            });
        });

        step("4. Press 'Process Order' button on the Opportunity's record page, " +
                "check that MVP service is successfully signed up, expand 'Professional Services' section, " +
                "check that 'Process Professional Services' button is visible and enabled, " +
                "and all the steps are incomplete", () -> {
            opportunityPage.clickProcessOrderButton();
            processOrderModal.mvpTierStatus.shouldHave(exactTextCaseSensitive(SIGNED_UP_STATUS), ofSeconds(60));
            processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(60));

            processOrderModal.professionalServicesExpandButton.click();
            processOrderModal.processProfessionalServicesButton.shouldBe(visible, enabled);
            processOrderModal.proServCompletedSignUpStepNames.shouldHave(size(0));
        });

        step("5. Click 'Process Professional Services' button, check the completed steps, " +
                "and that only 'Process Professional Services' and 'Close' buttons are visible and enabled", () -> {
            processOrderModal.processProfessionalServicesButton.click();
            processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(60));

            processOrderModal.proServCompletedSignUpStepNames
                    .shouldHave(exactTexts(CHECK_FOR_MAIN_COST_CENTER_STEP), ofSeconds(60));
            processOrderModal.processProfessionalServicesButton.shouldBe(visible, enabled);
            processOrderModal.closeButton.shouldBe(visible, enabled);
        });

        step("6. Click 'Process Professional Services' button, check the completed steps, " +
                "check that only 'Sign Up Professional Services' and 'Close' buttons are visible and enabled" +
                "and close Process Order modal window", () -> {
            processOrderModal.processProfessionalServicesButton.click();
            processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(60));

            processOrderModal.proServCompletedSignUpStepNames
                    .shouldHave(exactTexts(CHECK_FOR_MAIN_COST_CENTER_STEP, CREATION_OF_A_MAIN_COST_CENTER_STEP), ofSeconds(60));
            processOrderModal.signUpProfessionalServicesButton.shouldBe(visible, enabled);
            processOrderModal.closeButton.shouldBe(visible, enabled);
            processOrderModal.processProfessionalServicesButton.shouldBe(hidden);

            processOrderModal.closeWindow();
        });

        step("7. Click 'Process Order' button, expand 'Professional Services' section, " +
                "check that all steps are marked as incomplete, " +
                "and that 'Process Professional Services' and 'Close' buttons are visible and enabled", () -> {
            opportunityPage.clickProcessOrderButton();
            processOrderModal.mvpTierStatus.shouldHave(exactTextCaseSensitive(SIGNED_UP_STATUS), ofSeconds(60));
            processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(60));

            processOrderModal.professionalServicesExpandButton.click();
            processOrderModal.proServAllSignUpStepNames.shouldHave(exactTexts(expectedProServSteps));
            processOrderModal.proServCompletedSignUpStepNames.shouldHave(size(0));
            processOrderModal.processProfessionalServicesButton.shouldBe(visible, enabled);
            processOrderModal.closeButton.shouldBe(visible, enabled);
        });

        step("8. Click 'Process Professional Services' button, check the completed steps, " +
                "check that only 'Sign Up Professional Services' and 'Close' buttons are visible and enabled", () -> {
            processOrderModal.processProfessionalServicesButton.click();
            processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(60));

            processOrderModal.proServCompletedSignUpStepNames
                    .shouldHave(exactTexts(CHECK_FOR_MAIN_COST_CENTER_STEP, CREATION_OF_A_MAIN_COST_CENTER_STEP), ofSeconds(60));
            processOrderModal.signUpProfessionalServicesButton.shouldBe(visible, enabled);
            processOrderModal.closeButton.shouldBe(visible, enabled);
            processOrderModal.processProfessionalServicesButton.shouldBe(hidden);
        });

        step("9. Click 'Sign Up Professional Services' button, check the account is processed for signing up, " +
                "'Professional Services' section is displayed with 'Signed Up' status, " +
                "only 'Close' and 'Check Professional Services Status' buttons are visible and enabled, " +
                "and close Process Order modal window", () -> {
            processOrderModal.signUpProfessionalServicesButton.click();
            processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(60));

            opportunityPage.processOrderModal.signUpProServStatus
                    .shouldHave(exactTextCaseSensitive(format(YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE, PRO_SERV_SERVICE)), ofSeconds(60));

            processOrderModal.checkProfessionalServicesStatusButton.shouldBe(visible, enabled);
            processOrderModal.closeButton.shouldBe(visible, enabled);
            processOrderModal.signUpProfessionalServicesButton.shouldBe(hidden);

            processOrderModal.closeWindow();

            //  To make sure that on the next step the ProServ package is added in the NGBS (signed up)
            step("Wait until the Package__c record is created for the Account's ProServ package in SFDC", () ->
                    assertWithTimeout(() -> {
                        var proServBillingPackages = enterpriseConnectionUtils.query(
                                "SELECT Id " +
                                        "FROM Package__c " +
                                        "WHERE Account__c = '" + steps.salesFlow.account.getId() + "' " +
                                        "AND Service_Type__c = '" + proServServiceName + "'",
                                Package__c.class);
                        assertEquals(1, proServBillingPackages.size(),
                                "Number of created Package__c records in SFDC for the ProServ package in NGBS");
                        return proServBillingPackages.get(0);
                    }, ofSeconds(60), ofSeconds(5))
            );
        });

        step("10. Click 'Process Order' button, expand 'Professional Services' section, " +
                "check that all steps are completed, " +
                "'Professional Services' section is displayed with 'Signed Up' status, " +
                "only 'Close' button is visible and enabled, " +
                "and that Professional Services package is added to the account in NGBS", () -> {
            opportunityPage.clickProcessOrderButton();
            processOrderModal.mvpTierStatus.shouldHave(exactTextCaseSensitive(SIGNED_UP_STATUS), ofSeconds(60));
            processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(60));

            processOrderModal.professionalServicesExpandButton.click();

            processOrderModal.proServTierStatus.shouldHave(exactTextCaseSensitive(SIGNED_UP_STATUS), ofSeconds(60));
            processOrderModal.proServCompletedSignUpStepNames
                    .shouldHave(exactTexts(
                            CHECK_FOR_MAIN_COST_CENTER_STEP,
                            CREATION_OF_A_MAIN_COST_CENTER_STEP,
                            SIGN_UP_PRO_SERV_STEP
                    ));
            processOrderModal.closeButton.shouldBe(visible, enabled);
            processOrderModal.signUpProfessionalServicesButton.shouldBe(hidden);
            processOrderModal.processProfessionalServicesButton.shouldBe(hidden);
            processOrderModal.checkProfessionalServicesStatusButton.shouldBe(hidden);
        });

        step("11. Check that Professional Services package is added to the account in NGBS", () -> {
            multiProductSignUpSteps.checkAddedPackageAfterSignUp(billingId, proServServiceName);
        });
    }
}
