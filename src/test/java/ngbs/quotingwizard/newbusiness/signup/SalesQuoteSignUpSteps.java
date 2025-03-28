package ngbs.quotingwizard.newbusiness.signup;

import base.SfdcSteps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.DeviceAssignmentPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.packagetab.PackagePage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.producttab.ProductsPage;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils;
import com.sforce.soap.enterprise.sobject.User;
import ngbs.quotingwizard.CartTabSteps;
import ngbs.quotingwizard.QuoteWizardSteps;

import static base.Pages.cartPage;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.ALLOW_PROCESS_ORDER_WITHOUT_SHIPPING_PS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.SALES_REP_LIGHTNING_PROFILE;
import static io.qameta.allure.Allure.step;

/**
 * Test methods related to the test cases which check Sign Up flow for Sales Quotes.
 */
public class SalesQuoteSignUpSteps {
    public final Dataset data;
    private final SfdcSteps sfdcSteps;
    private final QuoteWizardSteps quoteWizardSteps;
    private final CartTabSteps cartTabSteps;

    private User salesRepUserWithAllowProcessOrderWithoutShippingPS;

    //  Test data
    private final Product yealinkPhone;
    private final Product polycomRentalPhone;
    private final Product digitalLineUnlimited;
    private final Product commonPhone;
    private final AreaCode localAreaCode;

    /**
     * New instance of test methods related to the test cases which check Sign Up flow for Sales Quotes.
     */
    public SalesQuoteSignUpSteps() {
        this.data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_2TypesOfDLs_RegularAndPOC.json",
                Dataset.class);
        sfdcSteps = new SfdcSteps();
        quoteWizardSteps = new QuoteWizardSteps(data);
        cartTabSteps = new CartTabSteps(data);

        yealinkPhone = data.getProductByDataName("LC_HD_959");
        polycomRentalPhone = data.getProductByDataName("LC_HDR_619");
        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        commonPhone = data.getProductByDataName("LC_DL-HDSK_177");
        localAreaCode = new AreaCode("Local", "United States", "California", EMPTY_STRING, "619");
    }

    /**
     * Find a user with 'Sales Rep - Lightning' profile and 'Allow Process Order Without Shipping' Permission Set via SFDC API.
     */
    public User getSalesRepUserWithAllowedProcessOrderWithoutShipping() {
        return step("Find a user with 'Sales Rep - Lightning' profile and 'Allow Process Order Without Shipping' Permission Set", () -> {
            salesRepUserWithAllowProcessOrderWithoutShippingPS = UserUtils.getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withPermissionSet(ALLOW_PROCESS_ORDER_WITHOUT_SHIPPING_PS)
                    .execute();
            return salesRepUserWithAllowProcessOrderWithoutShippingPS;
        });
    }

    /**
     * Initial login as a user 'Sales Rep - Lightning' profile and 'Allow Process Order Without Shipping' Permission Set
     * via Salesforce login page.
     */
    public void loginAsSalesRepUserWithAllowedProcessOrderWithoutShipping() {
        step("Login as a user with 'Sales Rep - Lightning' profile and 'Allow Process Order Without Shipping' Permission Set", () -> {
            sfdcSteps.initLoginToSfdcAsTestUser(salesRepUserWithAllowProcessOrderWithoutShippingPS);
        });
    }

    /**
     * <p>1. Open a new Opportunity record page and switch to the Quote Wizard.</p>
     * <p>2. Add a new Sales quote, select a package from test data on the {@link PackagePage}.</p>
     * <p>3. Add necessary products on the {@link ProductsPage}.</p>
     * <p>4. On the {@link CartPage} assign Devices to DLs via {@link DeviceAssignmentPage} and save changes.</p>
     *
     * @param opportunityId ID of the related Opportunity
     */
    public void prepareSalesQuoteWithAssignedDevicesSteps(String opportunityId) {
        step("Open the New Business Opportunity, switch to the Quote Wizard, add a new Sales quote, " +
                "select a package for it, and add some Products on the Add Products tab", () -> {
            quoteWizardSteps.openQuoteWizardOnOpportunityRecordPage(opportunityId);
            quoteWizardSteps.addNewSalesQuote();
            quoteWizardSteps.selectDefaultPackageFromTestData();

            quoteWizardSteps.addProductsOnProductsTab(data.getNewProductsToAdd());
        });

        step("Open the Price tab, assign devices to DLs, and save changes", () -> {
            cartPage.openTab();
            cartTabSteps.assignDevicesToDL(yealinkPhone.name, digitalLineUnlimited.name, localAreaCode, yealinkPhone.quantity);
            cartTabSteps.assignDevicesToDL(polycomRentalPhone.name, commonPhone.name, localAreaCode, polycomRentalPhone.quantity);
            cartPage.saveChanges();
        });
    }
}
