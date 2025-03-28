package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityShareFactory;
import com.sforce.soap.enterprise.sobject.Quote;
import com.sforce.soap.enterprise.sobject.QuoteLineItem;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal.EngageProServModal.AFTERMARKET_PS_SUPPORT_TEMPLATE;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal.EngageProServModal.IMPLEMENTATION_SERVICES_QUOTE_TEMPLATE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.CC_PROSERV_QUOTE_RECORD_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.SOLD_PROSERV_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("LTR-1772")
@Tag("CCProServ")
public class RiseAmericaCcProServQuoteTest extends BaseTest {
    private final RiseOpportunitySteps riseOpportunitySteps;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private String ccProServQuoteId;

    //  Test data
    private final List<Product> ccProServProducts;
    private final String ccProServUniversalPriceBookName;

    public RiseAmericaCcProServQuoteTest() {
        riseOpportunitySteps = new RiseOpportunitySteps(0);
        steps = new Steps(riseOpportunitySteps.data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        ccProServProducts = Arrays.asList(riseOpportunitySteps.data.packageFolders[0].packages[0].productsOther);
        ccProServUniversalPriceBookName = "CC ProServ Universal " + riseOpportunitySteps.data.getCurrencyIsoCode();
    }

    @BeforeEach
    public void setUpTest() {
        riseOpportunitySteps.loginAndSetUpRiseOpportunitySteps(BRIGHTSPEED_USERS_GROUP);
    }

    @Test
    @TmsLink("CRM-38093")
    @DisplayName("CRM-38093 - Verify the CC ProServ Quote can be created and edited. New Business")
    @Description("Verify that CC ProServ Quote is created after clicking the 'Initiate CC ProServ' button " +
            "and can be edited in the 'ProServ Quote' sub-tab by the ProServ Users")
    public void test() {
        step("1. Open the Opportunity record page, switch to the Quote Selection Wizard, " +
                "click the 'Initiate CC ProServ' button, choose PDF templates in the modal window, submit the form, " +
                "and check that the 'Initiate CC ProServ' button is disappeared, and CC ProServ quote is created", () -> {
            opportunityPage.openPage(riseOpportunitySteps.customerOpportunity.getId());
            opportunityPage.switchToNGBSQWIframeWithoutQuote();

            quoteSelectionWizardPage.initiateCcProServ(List.of(IMPLEMENTATION_SERVICES_QUOTE_TEMPLATE, AFTERMARKET_PS_SUPPORT_TEMPLATE));
            quoteSelectionWizardPage.initiateCcProServButton.shouldBe(hidden);

            var ccProServQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + riseOpportunitySteps.customerOpportunity.getId() + "' " +
                            "AND RecordType.Name = '" + CC_PROSERV_QUOTE_RECORD_TYPE + "'",
                    Quote.class);
            ccProServQuoteId = ccProServQuote.getId();

            quoteSelectionWizardPage.getQuoteName(ccProServQuoteId).shouldBe(visible);
            quoteSelectionWizardPage.proServQuotes.shouldHave(size(1));
        });

        step("2. Re-login as a user with 'Professional Services Lightning' profile, " +
                "and manually share the Opportunity with this user via API", () -> {
            var proServUser = getUser().withProfile(PROFESSIONAL_SERVICES_LIGHTNING_PROFILE).execute();

            OpportunityShareFactory.shareOpportunity(riseOpportunitySteps.customerOpportunity.getId(), proServUser.getId());

            steps.sfdc.reLoginAsUserWithSessionReset(proServUser);
        });

        step("3. Open the Opportunity record page, open the 'ProServ Quote' tab in the Quote Wizard, " +
                "and check that CC ProServ Quote is displayed in the Quote picklist", () -> {
            opportunityPage.openPage(riseOpportunitySteps.customerOpportunity.getId());
            opportunityPage.switchToNGBSQWIframeWithoutQuote();
            wizardBodyPage.proServTab.click();
            proServWizardPage.waitUntilLoaded();

            proServWizardPage.quotesPicklist.getSelectedOption().shouldHave(exactValue(ccProServQuoteId));
        });

        step("4. Open the Products tab, add some products, " +
                "and check that the products are added on the Cart tab", () -> {
            legacyProductsPage.openTab();
            legacyProductsPage.addProducts(ccProServProducts);

            legacyCartPage.openTab();
            ccProServProducts.forEach(product -> {
                legacyCartPage.getQliFromCart(product.name).getNewQuantityInput()
                        .shouldHave(value(String.valueOf(product.quantity)), ofSeconds(30));
            });
        });

        step("5. Check that PricebookEntry.Pricebook2.Name = 'CC ProServ Universal USD' " +
                "for the added QuoteLineItem records", () -> {
            var addedQuoteLineItems = enterpriseConnectionUtils.query(
                    "SELECT Id, PricebookEntry.Pricebook2.Name, Product2.Name " +
                            "FROM QuoteLineItem " +
                            "WHERE QuoteId = '" + ccProServQuoteId + "'",
                    QuoteLineItem.class);

            assertThat(addedQuoteLineItems.size())
                    .as("Number of added QuoteLineItem records")
                    .isEqualTo(ccProServProducts.size());
            addedQuoteLineItems.forEach(qli ->
                    assertThat(qli.getPricebookEntry().getPricebook2().getName())
                            .as("QuoteLineItem.PricebookEntry.Pricebook2.Name value for " + qli.getProduct2().getName())
                            .isEqualTo(ccProServUniversalPriceBookName)
            );
        });

        step("6. Open the Phase tab, click 'Add phase' button, move added product to the added phase, and save changes", () -> {
            proServWizardPage.phaseTabButton.click();
            phasePage.addAndSavePhase();
        });

        step("7. Open the Quote tab, populate necessary fields, and save changes", () ->
                steps.proServ.populateMandatoryFieldsOnQuoteTabAndSave(true)
        );

        step("8. Click 'ProServ is Out for Signature' button", () -> {
            proServWizardPage.markProServIsOutForSignature();
            proServWizardPage.proServIsSoldButton.shouldBe(visible, ofSeconds(10));
        });

        step("9. Click 'ProServ is Sold' button, click 'Mark as Sold' button in the opened pop-up window, " +
                "and check that CC ProServ Quote.ProServ_Status__c = 'Sold'", () -> {
            proServWizardPage.markProServAsSold();
            steps.proServ.checkCcProServQuoteStatus(riseOpportunitySteps.customerOpportunity.getId(), SOLD_PROSERV_STATUS);
        });
    }
}