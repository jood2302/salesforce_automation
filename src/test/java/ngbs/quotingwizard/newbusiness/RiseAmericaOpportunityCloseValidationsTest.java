package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import com.sforce.ws.ConnectionException;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal.EngageProServModal.*;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.CaseHelper.UC_QUOTE_REQUESTED_FOR_SUBJECT;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.CLOSED_WON_STAGE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("P1")
@Tag("OpportunityClose")
@Tag("RiseBrands")
@Tag("Validations")
public class RiseAmericaOpportunityCloseValidationsTest extends BaseTest {
    private final RiseOpportunitySteps riseOpportunitySteps;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User dealDeskUserFromBrightspeedGroup;

    //  Test data
    private final Product proServProduct;
    private final List<String> pdfTemplatesToSelect;

    public RiseAmericaOpportunityCloseValidationsTest() {
        riseOpportunitySteps = new RiseOpportunitySteps(0);
        steps = new Steps(riseOpportunitySteps.data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        proServProduct = riseOpportunitySteps.data.packageFolders[0].packages[0].products[0];
        pdfTemplatesToSelect = List.of(
                IMPLEMENTATION_SERVICES_QUOTE_TEMPLATE,
                MANAGED_RECURRING_SERVICES_ADVANCED_SUPPORT_TEMPLATE
        );
    }

    @BeforeEach
    public void setUpTest() {
        riseOpportunitySteps.loginAndSetUpRiseOpportunitySteps(BRIGHTSPEED_USERS_GROUP);
    }

    @Test
    @Tag("KnownIssue")
    @Issue("PBC-25279")
    @Issue("PBC-25784")
    @TmsLink("CRM-35504")
    @TmsLink("CRM-35339")
    @TmsLink("CRM-37971")
    @DisplayName("CRM-35504 - Opportunity for Rise America brand can't be closed without ProServ Quote in Status = Sold. \n" +
            "CRM-35339 - Contact Center Legacy Tab is hidden for Rise brands. \n" +
            "CRM-37971 - GSP: Initiate ProServ modal window and case creation")
    @Description("CRM-35504 - Verify that Opportunity for Rise America brand can't be closed:\n" +
            " - without created ProServ Quote\n" +
            " - with ProServ Quote with ProServ Status != Sold. \n\n" +
            "CRM-35339 - Verify that Contact Center Legacy Tab is hidden from Opportunity page " +
            "and in UQT for Rise America and Rise International brands. \n\n" +
            "CRM-37971 - Verify that by 'Initiate ProServ' button on GSP brand will be shown Initiate ProServ modal window\n" +
            "and after submitting will be created a case with all provided options from this model window")
    public void test() {
        //  CRM-35339
        step("1. Open the test Opportunity record page " +
                "and check that Contact Center tab is hidden on the Quote Selection page", () -> {
            opportunityPage.openPage(riseOpportunitySteps.customerOpportunity.getId());
            opportunityPage.switchToNGBSQWIframeWithoutQuote();
            quoteSelectionWizardPage.initiateProServButton.shouldBe(visible, ofSeconds(60));

            wizardBodyPage.contactCenterTab.shouldBe(hidden);
        });

        //  CRM-35504
        step("2. Click 'Close' button, " +
                "and check the error message and that the Opportunity is not closed", () -> {
            //  TODO Known Issue PBC-25279 (There's no error message about missing ProServ Quote, but it should be displayed)
            stepCheckValidationMessagesOnOpportunityClose(List.of(PLEASE_COMPLETE_PROSERV_QUOTE_TO_CLOSE_OPPORTUNITY_ERROR));

            stepCheckOpportunityIsNotClosed();
        });

        //  CRM-37971
        step("3. Switch to the Quote tab of Opportunity record page, click 'Initiate ProServ' button " +
                "and check that there are 4 optional and unselected checkboxes are displayed in the modal window, " +
                "and 'Cancel' and 'Submit' buttons are displayed", () -> {
            opportunityPage.switchToNGBSQWIframeWithoutQuote();
            quoteSelectionWizardPage.initiateProServButton.shouldBe(visible, ofSeconds(20)).click();

            quoteSelectionWizardPage.engageProServDialog.header
                    .shouldHave(exactTextCaseSensitive(INITIATE_PROFESSIONAL_SERVICES_HEADER));
            quoteSelectionWizardPage.engageProServDialog.proServOptionsLegend
                    .shouldHave(exactTextCaseSensitive(PLEASE_SELECT_ALL_FOLLOWING_OPTIONS_MESSAGE));
            quoteSelectionWizardPage.engageProServDialog.proServOptions.shouldHave(exactTextsCaseSensitiveInAnyOrder(List.of(
                    IMPLEMENTATION_SERVICES_QUOTE_TEMPLATE,
                    MANAGED_RECURRING_SERVICES_ADVANCED_SUPPORT_TEMPLATE,
                    AFTERMARKET_PS_SUPPORT_TEMPLATE,
                    VIDEO_ROOMS_IN_A_BOX_TEMPLATE
            )));

            quoteSelectionWizardPage.engageProServDialog.proServOptionCheckboxes
                    .forEach(checkbox -> checkbox.shouldNotHave(attribute("required")));
            quoteSelectionWizardPage.engageProServDialog.proServOptionCheckboxes
                    .forEach(checkbox -> checkbox.shouldNotBe(checked));

            quoteSelectionWizardPage.engageProServDialog.cancelButton.shouldBe(visible);
            quoteSelectionWizardPage.engageProServDialog.submitButton.shouldBe(visible);
        });

        //  CRM-35504
        step("4. Select 2 case reasons in the modal window, click 'Submit' button, " +
                "and check that ProServ Quote is created with ProServ_Status__c = 'Created'", () -> {
            pdfTemplatesToSelect.forEach(pdfTemplateName ->
                    quoteSelectionWizardPage.engageProServDialog.selectPdfTemplate(pdfTemplateName)
            );
            quoteSelectionWizardPage.engageProServDialog.submitButton.click();
            quoteSelectionWizardPage.spinner.shouldBe(visible);
            quoteSelectionWizardPage.spinner.shouldBe(hidden, ofSeconds(60));

            steps.proServ.checkProServQuoteStatus(riseOpportunitySteps.customerOpportunity.getId(), CREATED_PROSERV_STATUS);
        });

        //  CRM-35504, CRM-37971
        step("5. Check that created ProServ Quote is displayed on the Quotes list of the Quote Selection page " +
                "and 'Make Primary', 'Copy' and 'Delete' buttons are disabled", () -> {
            var proServQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + riseOpportunitySteps.customerOpportunity.getId() + "' " +
                            "AND RecordType.Name = '" + PROSERV_QUOTE_RECORD_TYPE + "'",
                    Quote.class);
            quoteSelectionWizardPage.getQuoteName(proServQuote.getId()).shouldBe(visible);
            quoteSelectionWizardPage.proServQuotes.shouldHave(size(1));

            quoteSelectionWizardPage.getMakePrimaryButton(proServQuote.getId()).shouldBe(disabled);
            quoteSelectionWizardPage.getCopyButton(proServQuote.getId()).shouldBe(disabled);
            quoteSelectionWizardPage.getDeleteButton(proServQuote.getId()).shouldBe(disabled);
        });

        //  CRM-37971
        step("6. Check that Case is created with the correct Subject with Account's Name " +
                "and Category__c field value that contains the selected PDF templates", () -> {
            var proServCase = assertWithTimeout(() -> {
                var proServCases = enterpriseConnectionUtils.query(
                        "SELECT Id, Subject, Category__c, Account_Name__c " +
                                "FROM Case " +
                                "WHERE Opportunity_Reference__c = '" + riseOpportunitySteps.customerOpportunity.getId() + "' ",
                        Case.class);
                assertEquals(1, proServCases.size(), "Number of ProServ Cases for the test Opportunity");

                return proServCases.get(0);
            }, ofSeconds(30), ofSeconds(3));

            assertThat(proServCase.getSubject())
                    .as("Case.Subject value")
                    .isEqualTo(format(UC_QUOTE_REQUESTED_FOR_SUBJECT, riseOpportunitySteps.customerAccount.getName()));
            assertThat(asList(proServCase.getCategory__c().split(";")))
                    .as("List of PDF Templates in the Case.Category__c field value")
                    .containsExactlyInAnyOrderElementsOf(pdfTemplatesToSelect);
        });

        //  CRM-35504
        step("7. Click 'Close' button on the Opportunity record page, " +
                "and check the validation messages and that the Opportunity is not closed", () -> {
            //  TODO Known Issue PBC-25784 (There's no error message '0 items in the cart...', but it should be displayed)
            stepCheckValidationMessagesOnOpportunityClose(List.of(ZERO_ITEMS_IN_CART_ERROR, PROSERV_QUOTES_NOT_MARKED_AS_SOLD_ERROR));

            stepCheckOpportunityIsNotClosed();
        });

        step("8. Transfer the test Customer's Account, Opportunity and the Contact records ownership " +
                "to the user with 'Deal Desk Lightning' profile and membership in the 'Brightspeed Users' Public Group, " +
                "and re-login as this user", () -> {
            step("Find a user with 'Deal Desk Lightning' profile and a membership in the 'Brightspeed Users' Public Group (GSP User)", () -> {
                dealDeskUserFromBrightspeedGroup = getUser()
                        .withProfile(DEAL_DESK_LIGHTNING_PROFILE)
                        .withGroupMembership(BRIGHTSPEED_USERS_GROUP)
                        .withGspUserValue(true)
                        .execute();
            });

            step("Transfer the test Customer's Account, Opportunity and the Contact records ownership " +
                    "to the user with 'Deal Desk Lightning' profile and a membership in the 'Brightspeed Users' Public Group (GSP User) via API", () -> {
                riseOpportunitySteps.customerAccount.setOwnerId(dealDeskUserFromBrightspeedGroup.getId());
                riseOpportunitySteps.customerContact.setOwnerId(dealDeskUserFromBrightspeedGroup.getId());
                riseOpportunitySteps.customerOpportunity.setOwnerId(dealDeskUserFromBrightspeedGroup.getId());
                enterpriseConnectionUtils.update(riseOpportunitySteps.customerAccount, riseOpportunitySteps.customerContact, riseOpportunitySteps.customerOpportunity);
            });

            steps.sfdc.reLoginAsUserWithSessionReset(dealDeskUserFromBrightspeedGroup);
        });

        //  CRM-35504
        step("9. Open the Opportunity record page, switch to the Quote tab, open the ProServ Quote tab, " +
                "add products and assign it to Phases, open the Quote Tab, populate required fields with any values, " +
                "save changes and check that ProServ Quote.ProServ_Status__c = 'In Progress'", () -> {
            opportunityPage.openPage(riseOpportunitySteps.customerOpportunity.getId());
            opportunityPage.waitUntilLoaded();
            opportunityPage.switchToNGBSQWIframeWithoutQuote();

            steps.proServ.addProductOnProServQuoteTab(proServProduct.name);

            step("Open the Phase tab, click 'Add phase' button, move added product to the added phase and save changes", () -> {
                proServWizardPage.phaseTabButton.click();
                phasePage.addAndSavePhase();
            });

            step("Open the Quote tab, populate necessary fields, and save changes", () -> {
                steps.proServ.populateMandatoryFieldsOnQuoteTabAndSave(false);
            });

            step("Check that ProServ Quote.ProServ_Status__c = 'In Progress'", () -> {
                steps.proServ.checkProServQuoteStatus(riseOpportunitySteps.customerOpportunity.getId(), IN_PROGRESS_PROSERV_STATUS);
            });
        });

        //  CRM-35504
        step("10. Click 'Close' button on the Opportunity record page, " +
                "and check the validation message and that the Opportunity is not closed", () -> {
            stepCheckValidationMessagesOnOpportunityClose(List.of(PROSERV_QUOTES_NOT_MARKED_AS_SOLD_ERROR));

            stepCheckOpportunityIsNotClosed();
        });

        //  CRM-35504
        step("11. Check that 'Sync to Primary Quote' button is not displayed on the ProServ tab in the Quote Wizard", () -> {
            wizardBodyPage.switchToIFrame();
            proServWizardPage.syncToPrimaryQuoteButton.shouldBe(hidden);
        });

        //  CRM-35504
        step("12. Click 'ProServ is Out for Signature' button on the ProServ tab, " +
                "and check that ProServ Quote.ProServ_Status__c = 'Out for Signature'", () -> {
            proServWizardPage.markProServIsOutForSignature();

            steps.proServ.checkProServQuoteStatus(riseOpportunitySteps.customerOpportunity.getId(), OUT_FOR_SIGNATURE_PROSERV_STATUS);
        });

        //  CRM-35504
        step("13. Click 'Close' button on the Opportunity record page, " +
                "check the validation message and that the Opportunity is not closed", () -> {
            stepCheckValidationMessagesOnOpportunityClose(List.of(PROSERV_QUOTES_NOT_MARKED_AS_SOLD_ERROR));

            stepCheckOpportunityIsNotClosed();
        });

        //  CRM-35504
        step("14. Click 'Mark as Sold' button on the ProServ tab of the Quote Wizard, " +
                "and check that ProServ Quote.ProServ_Status__c = 'Sold'", () -> {
            wizardBodyPage.switchToIFrame();
            proServWizardPage.markProServAsSold();

            steps.proServ.checkProServQuoteStatus(riseOpportunitySteps.customerOpportunity.getId(), SOLD_PROSERV_STATUS);
        });

        //  CRM-35504
        step("15. Click 'Close' button on the Opportunity record page, populate required fields in the Close Wizard, " +
                        "submit the form and verify that Opportunity.StageName = '7. Closed Won'",
                riseOpportunitySteps::stepCloseOpportunityAndCheckItsStatus
        );
    }

    /**
     * Check that the Rise America Opportunity's StageName != "7. Closed Won"
     * and IsClosed = false.
     *
     * @throws ConnectionException in case of malformed DB queries or network failures
     */
    private void stepCheckOpportunityIsNotClosed() throws ConnectionException {
        var opportunityUpdated = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id, IsClosed, StageName " +
                        "FROM Opportunity " +
                        "WHERE Id = '" + riseOpportunitySteps.customerOpportunity.getId() + "'",
                Opportunity.class);
        assertThat(opportunityUpdated.getIsClosed())
                .as("Opportunity.IsClosed value")
                .isFalse();
        assertThat(opportunityUpdated.getStageName())
                .as("Opportunity.StageName value")
                .isNotEqualTo(CLOSED_WON_STAGE);
    }

    /**
     * Click 'Close' button on the Opportunity record page, check the validation messages on the Opportunity record page
     * and close the Opportunity modal window.
     *
     * @param errorMessages validation messages to check
     */
    private void stepCheckValidationMessagesOnOpportunityClose(List<String> errorMessages) {
        opportunityPage.clickCloseButton();
        opportunityPage.alertNotificationBlock.shouldBe(visible, ofSeconds(60));

        opportunityPage.notifications.shouldHave(exactTextsCaseSensitiveInAnyOrder(errorMessages));
        opportunityPage.closeErrorAlertNotifications();
        opportunityPage.closeOpportunityModal.closeWindow();
    }
}
