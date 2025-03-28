package ngbs.quotingwizard.existingbusiness.carttab;

import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage;
import ngbs.quotingwizard.CartTabSteps;
import ngbs.quotingwizard.QuoteWizardSteps;

import static base.Pages.cartPage;
import static base.Pages.packagePage;
import static io.qameta.allure.Allure.step;

/**
 * Test methods for test cases related to {@link CartPage} functionality with changing Recurring Charges.
 */
public class RecurringChargesChangeSteps {
    private final QuoteWizardSteps quoteWizardSteps;
    private final CartTabSteps cartTabSteps;

    //  Test data
    private final Product dlUnlimited;
    private final Product rentalPhoneToAdd;

    /**
     * New instance for test methods related to {@link CartPage} functionality with changing Recurring Charges.
     *
     * @param data test data object in the form of {@link Dataset} parsed from JSON file.
     */
    public RecurringChargesChangeSteps(Dataset data) {
        quoteWizardSteps = new QuoteWizardSteps(data);
        cartTabSteps = new CartTabSteps(data);

        dlUnlimited = data.getProductByDataNameFromUpgradeData("LC_DL-UNL_50");
        rentalPhoneToAdd = data.getProductByDataNameFromUpgradeData("LC_HDR_619");
    }

    /**
     * Preparation test steps related to test cases that check changes in Recurring Charges.
     *
     * @param opportunityId ID of the Opportunity that the new Sales Quote relates to
     */
    public void createNewContractedQuoteWithAssignedRentalPhones(String opportunityId) {
        step("Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "and select the same package with a contract", () -> {
            quoteWizardSteps.openQuoteWizardForNewSalesQuoteDirect(opportunityId);

            packagePage.packageSelector.setContractSelected(true);
        });

        step("Add Rental phones on the Add Products tab", () ->
                quoteWizardSteps.addProductsOnProductsTab(rentalPhoneToAdd)
        );

        step("Change quantities for new Products on the Price tab, assign devices, and save changes", () -> {
            cartPage.openTab();
            cartTabSteps.setUpQuantities(dlUnlimited, rentalPhoneToAdd);
            cartTabSteps.assignDevicesToDLAndSave(rentalPhoneToAdd.name, dlUnlimited.name, quoteWizardSteps.localAreaCode,
                    rentalPhoneToAdd.quantity);
        });
    }
}
