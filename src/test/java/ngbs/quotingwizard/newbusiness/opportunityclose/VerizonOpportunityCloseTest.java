package ngbs.quotingwizard.newbusiness.opportunityclose;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.CLOSED_WON_FOR_PROSERV_STAGE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.setRequiredFieldsForOpportunityStageChange;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.refresh;
import static com.codeborne.selenide.Selenide.sleep;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P0")
@Tag("OpportunityClose")
public class VerizonOpportunityCloseTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User dealDeskUserFromVerizonGroup;

    //  Test data
    private final String proServProductName;

    public VerizonOpportunityCloseTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/newbusiness/RC_Verizon_Monthly_NonContract_QOP.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        proServProductName = data.packageFolders[0].packages[0].productsOther[0].name;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-25650")
    @DisplayName("CRM-25650 - Opportunity Close for 'RingCentral with Verizon'")
    @Description("Verify that Opportunity for 'RingCentral with Verizon' can be closed only to the new stage: " +
            "'7.1. Closed Won for Proserv'")
    public void test() {
        step("1. Open the test Opportunity, switch to the Quote tab, click 'Initiate ProServ' button, " +
                "submit the form, and wait until the ProServ Quote is shown on the list", () -> {
            opportunityPage.openPage(steps.quoteWizard.opportunity.getId());
            opportunityPage.switchToNGBSQWIframeWithoutQuote();
            quoteSelectionWizardPage.initiateProServ();

            var proServQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND RecordType.Name = '" + PROSERV_QUOTE_RECORD_TYPE + "'",
                    Quote.class);
            quoteSelectionWizardPage.getQuoteName(proServQuote.getId()).shouldBe(visible);
            quoteSelectionWizardPage.proServQuotes.shouldHave(size(1));
        });

        step("2. Transfer the Account, Opportunity and the Contact records ownership " +
                "to the user with 'Deal Desk Lightning' profile and membership in the 'Verizon Users Group' Public Group, " +
                "and re-login as this user", () -> {
            step("Find a user with 'Deal Desk Lightning' profile and a membership in the 'Verizon Users Group' Public Group (GSP User)", () -> {
                dealDeskUserFromVerizonGroup = getUser()
                        .withProfile(DEAL_DESK_LIGHTNING_PROFILE)
                        .withGroupMembership(VERIZON_USERS_GROUP)
                        .withGspUserValue(true)
                        .execute();
            });

            steps.salesFlow.account.setOwnerId(dealDeskUserFromVerizonGroup.getId());
            steps.salesFlow.contact.setOwnerId(dealDeskUserFromVerizonGroup.getId());
            steps.quoteWizard.opportunity.setOwnerId(dealDeskUserFromVerizonGroup.getId());
            enterpriseConnectionUtils.update(steps.salesFlow.account, steps.salesFlow.contact, steps.quoteWizard.opportunity);

            steps.sfdc.reLoginAsUserWithSessionReset(dealDeskUserFromVerizonGroup);
        });

        step("3. Open the Opportunity record page, switch to the Quote tab, open the ProServ Quote tab, " +
                "add products and assign it to Phases, open the Quote Tab, populate required fields with any values, and save changes", () -> {
            opportunityPage.openPage(steps.quoteWizard.opportunity.getId());
            opportunityPage.waitUntilLoaded();
            opportunityPage.switchToNGBSQWIframeWithoutQuote();

            step("Open the ProServ Quote tab, switch to the Products tab, select a product by 'Add to Cart' button, " +
                    "and check that the product is added on the Cart tab", () -> {
                steps.proServ.addProductOnProServQuoteTab(proServProductName);
            });

            step("Open the Phase tab, click 'Add phase' button, move added product to the added phase and save changes", () -> {
                proServWizardPage.phaseTabButton.click();
                phasePage.addAndSavePhase();
            });

            step("Open the Quote tab, populate necessary fields, and save changes", () -> {
                steps.proServ.populateMandatoryFieldsOnQuoteTabAndSave(false);
            });

            step("Click 'ProServ is Out for Signature' button " +
                    "and click 'Mark as 'Out for Signature' and Lock Quote' button in modal window", () -> {
                proServWizardPage.markProServIsOutForSignature();
                steps.proServ.checkProServQuoteStatus(steps.quoteWizard.opportunity.getId(), OUT_FOR_SIGNATURE_PROSERV_STATUS);
            });

            step("Populate 'UID from .biz' field, save changes, " +
                    "click 'ProServ is Sold' button, and click 'Mark as Sold' button in the opened pop-up window", () -> {
                legacyQuotePage.uidFromBizInput.setValue(TEST_STRING);
                legacyQuotePage.saveQuote();

                proServWizardPage.markProServAsSold();
                steps.proServ.checkProServQuoteStatus(steps.quoteWizard.opportunity.getId(), SOLD_PROSERV_STATUS);
            });
        });

        step("4. Set the required fields to be able to close the Opportunity via API", () -> {
            setRequiredFieldsForOpportunityStageChange(steps.quoteWizard.opportunity);
            enterpriseConnectionUtils.update(steps.quoteWizard.opportunity);
        });

        step("5. Click 'Close' button on the Opportunity record page, reload the page after the spinner is hidden " +
                "and verify that the Opportunity.StageName = '" + CLOSED_WON_FOR_PROSERV_STAGE + "'", () -> {
            opportunityPage.clickCloseButton();
            opportunityPage.spinner.shouldBe(visible, ofSeconds(10));
            opportunityPage.spinner.shouldBe(hidden, ofSeconds(30));
            sleep(2_000);   //  the error message might appear a little later
            opportunityPage.alertNotificationBlock.shouldBe(hidden);

            refresh();
            opportunityPage.waitUntilLoaded();

            opportunityPage.stagePicklist.getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(CLOSED_WON_FOR_PROSERV_STAGE), ofSeconds(60));
        });
    }
}
