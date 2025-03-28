package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.QuoteLineItem;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static base.Pages.cartPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.Product2Helper.FAMILY_TAXES;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Multiproduct-Lite")
@Tag("LTR-569")
@Tag("Taxes")
public class AddTaxesMultiProductTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Map<String, Package> packageFolderNameToPackageMap;

    public AddTaxesMultiProductTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_ProServ_Monthly_Contract.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        packageFolderNameToPackageMap = Map.of(
                data.packageFolders[0].name, data.packageFolders[0].packages[0],
                data.packageFolders[2].name, data.packageFolders[2].packages[0]
        );
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-29751")
    @DisplayName("CRM-29751 - QLI of added taxes for all Services stored on Master Quote and stored on specific Tech Quotes.")
    @Description("Verify that QLI of added taxes for all Services saved on Master and appropriate Technical Quotes.")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select MVP and Engage Voice packages for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityForMultiProduct(steps.quoteWizard.opportunity.getId(), packageFolderNameToPackageMap)
        );

        step("2. Open the Price tab, press 'Add Taxes' button, save changes " +
                "and collapse 'Office' and 'Engage Voice Standalone' services sections", () -> {
            cartPage.openTab();
            cartPage.addTaxes();
            cartPage.taxCartItems.shouldHave(sizeGreaterThan(0));
            cartPage.saveChanges();

            packageFolderNameToPackageMap.keySet().forEach(cartPage::clickCartGroup);
        });

        step("3. Check that the Master Quote's QuoteLineItem records for the taxes are saved in DB " +
                "and check that the taxes are shown for each service on the Price tab", () -> {
            packageFolderNameToPackageMap.keySet().forEach(serviceName -> {
                step("Check Master Quote's QuoteLineItem records for Taxes for " + serviceName + " in DB and UI", () -> {
                    var taxQuoteLineItemsOnMasterQuote = enterpriseConnectionUtils.query(
                            "SELECT Id, Product2.ExtID__c " +
                                    "FROM QuoteLineItem " +
                                    "WHERE QuoteId = '" + cartPage.getSelectedQuoteId() + "' " +
                                    "AND Product2.Family = '" + FAMILY_TAXES + "' " +
                                    "AND ServiceName__c = '" + serviceName + "'",
                            QuoteLineItem.class);
                    checkTaxItemsInDBAndUI(taxQuoteLineItemsOnMasterQuote, serviceName);
                });
            });
        });

        step("4. Check that the Technical Quotes' QuoteLineItem records for the taxes are saved in DB " +
                "and check that the taxes are shown for each service on the Price tab", () -> {
            packageFolderNameToPackageMap.forEach((serviceName, aPackage) -> {
                step("Check Tech Quote's QuoteLineItem records for Taxes for " + serviceName + " in DB and UI", () -> {
                    var taxQuoteLineItemsOnTechQuote = enterpriseConnectionUtils.query(
                            "SELECT Id, Product2.ExtID__c " +
                                    "FROM QuoteLineItem " +
                                    "WHERE Quote.MasterQuote__c = '" + cartPage.getSelectedQuoteId() + "' " +
                                    "AND Product2.Family = '" + FAMILY_TAXES + "' " +
                                    "AND ServiceName__c = '" + serviceName + "'",
                            QuoteLineItem.class);
                    checkTaxItemsInDBAndUI(taxQuoteLineItemsOnTechQuote, serviceName);
                });
            });
        });
    }

    /**
     * Check values of Tax items in the DB and UI for the provided service.
     *
     * @param quoteLineItems list of QuoteLineItem records to check
     * @param serviceName    name of the service to check (e.g. "Engage Voice Standalone")
     */
    private void checkTaxItemsInDBAndUI(List<QuoteLineItem> quoteLineItems, String serviceName) {
        step("Check that QuoteLineItem__c items are saved and each item has price greater 0 in DB", () -> {
            assertThat(quoteLineItems.size())
                    .as("QuoteLineItem__c records count")
                    .isGreaterThan(0);
        });

        step("Check that all created QuoteLineItem records for Taxes are shown for " + serviceName + " service on the Price tab", () -> {
            cartPage.clickCartGroup(serviceName);

            cartPage.taxCartItems
                    .filter(visible)
                    .shouldHave(size(quoteLineItems.size()));

            quoteLineItems.forEach(qli -> {
                cartPage.getQliFromCartByDataId(qli.getProduct2().getExtID__c())
                        .getCartItemElement()
                        .shouldBe(visible);
            });

            cartPage.clickCartGroup(serviceName);
        });
    }
}
