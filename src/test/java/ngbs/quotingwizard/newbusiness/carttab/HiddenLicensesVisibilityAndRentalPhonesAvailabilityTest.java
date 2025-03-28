package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.QuoteLineItem;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.producttab.ProductsPage.RENTAL_PHONES_ARE_AVAILABLE_ONLY_UNDER_CONTRACT;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThanOrEqual;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Tag("P1")
@Tag("ProductsTab")
@Tag("NGBS")
public class HiddenLicensesVisibilityAndRentalPhonesAvailabilityTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final List<String> hiddenLicensesDisplayNames;
    private final String hiddenDIDLicenseExtId;
    private final String hiddenMobileUserLicenseExtId;
    private final Product rentalPhone;

    public HiddenLicensesVisibilityAndRentalPhonesAvailabilityTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_NonContract_1TypeOfDL.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        hiddenLicensesDisplayNames = List.of("DID", "Mobile User");
        hiddenDIDLicenseExtId = "LC_DID_53";
        hiddenMobileUserLicenseExtId = "LC_MU_281";
        rentalPhone = data.packageFolders[0].packages[2].products[0];
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-11369")
    @TmsLink("CRM-12384")
    @DisplayName("CRM-11369 - To check that hidden licenses are not visible for user on Price tab. \n" +
            "CRM-12384 - Rental Phones Unavailable without Contract")
    @Description("CRM-11369 - To check that hidden licenses are not visible for user on Price tab. \n" +
            "CRM-12384 - Verify that Rental Phones cannot be added to cart if no Contract is selected")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                        "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        //  CRM-12384
        step("2. Open the Add Products tab, check that all Rental Phones are unavailable for adding, " +
                "and that the tooltip message for the 'Add' button is correct", () -> {
            productsPage.openTab();

            productsPage.openGroup(rentalPhone.group);
            productsPage.openSubgroup(rentalPhone.subgroup);

            productsPage.addToCartButtons.shouldHave(sizeGreaterThanOrEqual(1));
            var numberOfProducts = productsPage.products.size();
            productsPage.addToCartButtons
                    .filter(and("visible and disabled", visible, disabled))
                    .shouldHave(size(numberOfProducts));
            
            productsPage.addToCartButtons.first().hover();
            productsPage.tooltip.shouldHave(exactTextCaseSensitive(RENTAL_PHONES_ARE_AVAILABLE_ONLY_UNDER_CONTRACT));
        });

        step("3. Open the Select Package tab, select a contract, and save changes", () -> {
            packagePage.openTab();
            packagePage.packageSelector.setContractSelected(true);
            packagePage.saveChanges();
        });

        step("4. Open the Add Products tab and add Rental Phone to the cart", () -> {
            steps.quoteWizard.addProductsOnProductsTab(rentalPhone);

            var rentalPhoneProductItem = productsPage.getProductItem(rentalPhone);
            rentalPhoneProductItem.getAddButtonElement().shouldBe(hidden);
            rentalPhoneProductItem.getRemoveButtonElement().shouldBe(visible);
        });

        step("5. Open the Price tab, verify that the rental phone is visible, " +
                "and 'DID' license is added to the quote and not visible on the Price tab, " +
                "and 'Mobile User' license is not added to the quote, nor visible on the Price tab", () -> {
            cartPage.openTab();
            //  CRM-12384
            cartPage.getQliFromCartByDisplayName(rentalPhone.name).getDisplayName().shouldBe(visible);

            //  CRM-11369
            hiddenLicensesDisplayNames.forEach(hiddenLicenseDisplayName ->
                    step("Check that the license '" + hiddenLicenseDisplayName + "' is hidden on the Price tab", () -> {
                        cartPage.getQliFromCartByDisplayName(hiddenLicenseDisplayName)
                                .getDisplayName()
                                .shouldBe(hidden);
                    })
            );

            var qliForDID = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM QuoteLineItem " +
                            "WHERE Quote.OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND Product2.ExtId__c = '" + hiddenDIDLicenseExtId + "'",
                    QuoteLineItem.class);
            assertThat(qliForDID)
                    .as("Number of QuoteLineItem records for a 'DID' license in DB")
                    .asList().hasSize(1);

            var qliForMobileUser = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM QuoteLineItem " +
                            "WHERE Quote.OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND Product2.ExtId__c = '" + hiddenMobileUserLicenseExtId + "'",
                    QuoteLineItem.class);
            assertThat(qliForMobileUser)
                    .as("Number of QuoteLineItem records for a 'Mobile User' license in DB")
                    .asList().hasSize(0);
        });
    }
}
