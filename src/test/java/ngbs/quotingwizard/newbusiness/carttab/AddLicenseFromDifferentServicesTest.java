package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import com.sforce.soap.enterprise.sobject.QuoteLineItem;
import com.sforce.ws.ConnectionException;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.Map;

import static base.Pages.cartPage;
import static base.Pages.wizardPage;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToInteger;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.StringHelper.PERCENT;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteLineItemHelper.DISCOUNT_TYPE_CURRENCY;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteLineItemHelper.DISCOUNT_TYPE_PERCENTAGE;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.String.valueOf;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("Multiproduct-Lite")
@Tag("PriceTab")
@Tag("ProductsTab")
@Tag("Quote")
@Tag("Opportunity")
@Tag("Account")
public class AddLicenseFromDifferentServicesTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Map<String, Package> packageFolderNameToPackageMap;

    public AddLicenseFromDifferentServicesTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_Annual_Contract.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        var officePackage = data.packageFolders[0].packages[0];
        var engageDigitalPackage = data.packageFolders[2].packages[0];
        var rcCcPackage = data.packageFolders[3].packages[0];

        packageFolderNameToPackageMap = Map.of(
                data.packageFolders[0].name, officePackage,
                data.packageFolders[2].name, engageDigitalPackage,
                data.packageFolders[3].name, rcCcPackage
        );

        var officeProduct = officePackage.products[0];
        officeProduct.quantity = 4;
        officeProduct.discount = 10;

        var engageDigitalProduct = engageDigitalPackage.products[0];
        engageDigitalProduct.quantity = 5;
        engageDigitalProduct.discount = 9;
        engageDigitalProduct.discountType = "USD";

        var rcCcProduct = rcCcPackage.products[0];
        rcCcProduct.quantity = 3;
        rcCcProduct.discount = 8;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-29587")
    @TmsLink("CRM-32738")
    @DisplayName("CRM-29587 - Adding licenses from different Services. \n" +
            "CRM-32738 - Technical Objects are created only for chosen Services")
    @Description("CRM-29587 - Verify that licenses from different Services can be added and saved to Multiproduct Quote. \n" +
            "CRM-32738 - Verify that Technical Objects (Quotes, QLIs) are created only for chosen Services")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select Office, Engage Digital, and RingCentral Contact Center packages for it", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectPackagesForMultiProductQuote(packageFolderNameToPackageMap);
        });

        step("2. Open the Add Products tab and add Office, ED, and RC CC products to the cart", () -> {
            packageFolderNameToPackageMap.values().forEach(pkg -> {
                step("Add products from package " + pkg.name, () -> {
                    steps.quoteWizard.addProductsOnProductsTab(pkg.products);
                });
            });
        });


        step("3. Open the Price tab, set up quantities and discounts, and save changes", () -> {
            cartPage.openTab();
            packageFolderNameToPackageMap.values().forEach(pkg -> {
                step("Set up quantities and discounts for products from package " + pkg.name, () -> {
                    steps.cartTab.setUpQuantities(pkg.products);
                    steps.cartTab.setUpDiscounts(pkg.products);
                });
            });
            cartPage.saveChanges();
        });

        //  CRM-29587
        step("4. Collapse all services on the Price tab, open each one " +
                "and check that added licenses are displayed for corresponding service in the Price tab", () -> {
            //  all section should be collapsed first, so we could then open them one by one
            //  to be able to check that corresponding licenses are displayed only `inside` the given sections
            step("Collapse all Service sections", () -> {
                packageFolderNameToPackageMap.keySet().forEach(cartPage::clickCartGroup);
            });

            packageFolderNameToPackageMap.forEach((packageFolderName, pkg) -> {
                step("Expand the section = " + packageFolderName + ", check the products in it, and collapse the section", () -> {
                    cartPage.clickCartGroup(packageFolderName);
                    stream(pkg.products).forEach(product ->
                            step("Check " + product.name + " is displayed " +
                                    "under section " + packageFolderName, () -> {
                                cartPage.getQliFromCartByDisplayName(product.name)
                                        .getCartItemElement()
                                        .shouldBe(visible);
                            }));
                    cartPage.clickCartGroup(packageFolderName);
                });
            });

            step("Expand all Service sections", () ->
                    packageFolderNameToPackageMap.keySet().forEach(cartPage::clickCartGroup)
            );
        });

        //  CRM-29587
        step("5. Check Quantity, Discount, and Discount Type fields for all the added items on the Price tab", () ->
                packageFolderNameToPackageMap.forEach((packageFolderName, pkg) ->
                        step("Check Quantity, Discount, and Discount Type fields for added products in " + pkg.name, () ->
                                stream(pkg.products).forEach(product -> {
                                    var currentCartItem = cartPage.getQliFromCartByDisplayName(product.name);
                                    currentCartItem.getQuantityInput().shouldHave(exactValue(valueOf(product.quantity)));
                                    currentCartItem.getDiscountInput().shouldHave(exactValue(valueOf(product.discount)));
                                    currentCartItem.getDiscountTypeSelect()
                                            .getSelectedOption()
                                            .shouldHave(exactTextCaseSensitive(product.discountType));
                                }))));

        //  CRM-29587
        step("6. Check that all corresponding Master QuoteLineItems records are added, " +
                "and that Quantity, Discount_number__c, Discount_Type__c fields on them are correct", () -> {
            checkQuantitiesAndDiscountsOnQlis(wizardPage.getSelectedQuoteId(), EMPTY_STRING);
        });

        //  CRM-32738
        step("7. Check that all corresponding Technical Quotes and QuoteLineItems are created correctly", () -> {
            var techQuotes = enterpriseConnectionUtils.query(
                    "SELECT Id, Name, AccountId, OpportunityId, " +
                            "IsMultiProductTechnicalQuote__c, ServiceName__c, Opportunity.Tier_Name__c " +
                            "FROM Quote " +
                            "WHERE MasterQuote__c = '" + wizardPage.getSelectedQuoteId() + "' ",
                    Quote.class);

            assertThat(techQuotes.size())
                    .as("Number of Technical Quotes")
                    //  number of selected services/packages
                    .isEqualTo(packageFolderNameToPackageMap.keySet().size());

            techQuotes.forEach(techQuote -> {
                step("Check the Tech Quote with Name = '" + techQuote.getName() + "' and its QuoteLineItems", () -> {
                    //  CRM-32738
                    step("Check that Tech Quote is created, " +
                            "and that its IsMultiProductTechnicalQuote__c, ServiceName__c, AccountId, OpportunityId values are correct", () -> {
                        assertThat(techQuote.getIsMultiProductTechnicalQuote__c())
                                .as("Tech Quote.IsMultiProductTechnicalQuote__c value ")
                                .isTrue();

                        assertThat(techQuote.getServiceName__c())
                                .as("Tech Quote.ServiceName__c value")
                                .isIn(packageFolderNameToPackageMap.keySet());

                        assertThat(techQuote.getAccountId())
                                .as("Tech Quote.AccountId value")
                                .isEqualTo(steps.salesFlow.account.getId());

                        assertThat(techQuote.getOpportunityId())
                                .as("Tech Quote.OpportunityId value")
                                .isEqualTo(steps.quoteWizard.opportunity.getId());
                    });

                    step("Check that all corresponding Tech QuoteLineItems for the Tech Quote are added, " +
                            "and that Quantity, Discount_number__c, Discount_Type__c, QuoteId values on them are correct", () -> {
                        //  CRM-29587
                        checkQuantitiesAndDiscountsOnQlis(techQuote.getId(), techQuote.getServiceName__c());
                        //  CRM-32738
                        checkQuoteIdOnTechQuoteLineItems(techQuote.getId(), techQuote.getServiceName__c());
                    });
                });
            });
        });
    }

    /**
     * Check 'Quantity', 'Discount_number__c' and 'Discount_type__c' fields values
     * on the QuoteLineItem records against the given test data.
     *
     * @param quoteId     ID of provided Quote
     * @param serviceName service name for the quote (e.g. "Office", "RingCentral Contact Center")
     * @throws ConnectionException in case of malformed DB queries or network failures
     */
    private void checkQuantitiesAndDiscountsOnQlis(String quoteId, String serviceName) throws ConnectionException {
        var quoteLineItems = enterpriseConnectionUtils.query(
                "SELECT Id, Product2.ExtID__c, " +
                        "Quantity, Discount_number__c, Discount_Type__c  " +
                        "FROM QuoteLineItem " +
                        "WHERE QuoteId = '" + quoteId + "'",
                QuoteLineItem.class);

        var packageByService = packageFolderNameToPackageMap.get(serviceName) == null
                ? packageFolderNameToPackageMap.values()   //  for Master Quote (all packages, all added products)
                : singletonList(packageFolderNameToPackageMap.get(serviceName));   //  for Tech Quotes (single package, only service-specific products)

        packageByService.forEach(pkg ->
                stream(pkg.products).forEach(product ->
                        step("Check Quantity, Discount_number__c, Discount_type__c fields for " + product.name, () -> {
                            var quoteLineItem = quoteLineItems.stream()
                                    .filter(qli -> qli.getProduct2().getExtID__c().equals(product.dataName))
                                    .findFirst().orElseThrow(() -> new AssertionError("There's no QuoteLineItem for " + product.dataName));

                            assertThat(doubleToInteger(quoteLineItem.getQuantity()))
                                    .as("QuoteLineItem.Quantity value for " + product.name)
                                    .isEqualTo(product.quantity);

                            assertThat(doubleToInteger(quoteLineItem.getDiscount_number__c()))
                                    .as("QuoteLineItem.Discount_number__c value for " + product.name)
                                    .isEqualTo(product.discount);

                            var expectedDiscountType = product.discountType.equals(PERCENT) ?
                                    DISCOUNT_TYPE_PERCENTAGE :
                                    DISCOUNT_TYPE_CURRENCY;
                            assertThat(quoteLineItem.getDiscount_type__c())
                                    .as("QuoteLineItem.Discount_type__c value for " + product.name)
                                    .isEqualTo(expectedDiscountType);
                        })));
    }

    /**
     * Check 'QuoteId' for all Technical QuoteLineItem records in the context of their service.
     *
     * @param techQuoteId expected ID of the Technical Quote on QLIs
     * @param serviceName Service name for the quote (e.g. "Office", "RingCentral Contact Center")
     * @throws ConnectionException in case of errors while accessing API
     */
    private void checkQuoteIdOnTechQuoteLineItems(String techQuoteId, String serviceName) throws ConnectionException {
        var quoteLineItems = enterpriseConnectionUtils.query(
                "SELECT Id, Display_Name__c, QuoteId " +
                        "FROM QuoteLineItem " +
                        "WHERE Quote.MasterQuote__c = '" + wizardPage.getSelectedQuoteId() + "' " +
                        "AND ServiceName__c = '" + serviceName + "'",
                QuoteLineItem.class);

        quoteLineItems.forEach(quoteLineItem -> {
            assertThat(quoteLineItem.getQuoteId())
                    .as("Tech QuoteLineItem.QuoteId value for " + quoteLineItem.getDisplay_Name__c())
                    .isEqualTo(techQuoteId);
        });
    }
}
