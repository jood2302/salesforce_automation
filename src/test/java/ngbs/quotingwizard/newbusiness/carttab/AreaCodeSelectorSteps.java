package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.page.components.AreaCodeSelector;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.DeviceAssignmentPage;
import ngbs.quotingwizard.QuoteWizardSteps;

import java.util.List;
import java.util.Map;

import static base.Pages.cartPage;
import static base.Pages.deviceAssignmentPage;
import static com.codeborne.selenide.CollectionCondition.textsInAnyOrder;
import static com.codeborne.selenide.Condition.exactText;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

/**
 * Test methods related to the Area Code Selector checks for different Digital Lines.
 */
public class AreaCodeSelectorSteps extends BaseTest {
    private final QuoteWizardSteps quoteWizardSteps;

    //  Test data
    private final Product[] newProductsToAdd;
    private final Product polycomPhone;

    /**
     * New instance for the class with the test methods/steps related to
     * the Area Code Selector checks for different Digital Lines.
     *
     * @param data object parsed from the JSON files with the test data
     */
    public AreaCodeSelectorSteps(Dataset data) {
        quoteWizardSteps = new QuoteWizardSteps(data);

        newProductsToAdd = data.getNewProductsToAdd();
        polycomPhone = data.getProductByDataName("LC_HD_687");
    }

    /**
     * Add a new Sales Quote in the Quote Wizard with Digital Line products, and check available countries
     * in Area Code selector on the {@link DeviceAssignmentPage} for different DL licenses.
     *
     * @param opportunityId          ID of the Opportunity for which the Quote is created
     * @param countriesOnLicensesMap expected list of countries mapped to different DL products
     *                               (using product's name)
     */
    public void checkAreaCodeSelectorDifferentDigitalLinesTestSteps(String opportunityId,
                                                                    Map<String, List<String>> countriesOnLicensesMap) {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales quote, " +
                "select a package for it, add some products, and open the Price tab", () -> {
            quoteWizardSteps.openQuoteWizardForNewSalesQuoteDirect(opportunityId);
            quoteWizardSteps.selectDefaultPackageFromTestData();
            quoteWizardSteps.addProductsOnProductsTab(newProductsToAdd);

            cartPage.openTab();
        });

        step("2. Check available Countries in Area Code Selector for given licenses", () ->
                countriesOnLicensesMap.forEach((licenseName, expectedCountries) ->
                        step("Check available countries for '" + licenseName + "' license", () -> {
                            cartPage.getQliFromCartByDisplayName(licenseName)
                                    .getDeviceAssignmentButton()
                                    .click();
                            deviceAssignmentPage.getProductItemByName(polycomPhone.name)
                                    .getNameElement()
                                    .shouldHave(exactText(polycomPhone.name), ofSeconds(60));
                            deviceAssignmentPage.getProductItemByName(polycomPhone.name)
                                    .getAddAreaCodeButton()
                                    .click();

                            var areaCodeSelector = new AreaCodeSelector(
                                    deviceAssignmentPage.getFirstAreaCodeItem(polycomPhone.name).getAreaCodeSelector().getSelf()
                            );
                            areaCodeSelector.getInputElement().click();
                            areaCodeSelector.getSearchResults().shouldHave(textsInAnyOrder(expectedCountries));
                            deviceAssignmentPage.cancelButton.click();
                        }))
        );
    }
}
