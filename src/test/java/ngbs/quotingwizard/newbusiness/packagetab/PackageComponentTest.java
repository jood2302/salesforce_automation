package ngbs.quotingwizard.newbusiness.packagetab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.packagePage;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonList;

@Tag("P0")
@Tag("PackageTab")
@Tag("UQT")
public class PackageComponentTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    //  Test data
    private final String officeServiceName;
    private final Package packageWithContract;
    private final String monthlyChargeTerm;
    private final String annualChargeTerm;

    private final Integer newQuantity;
    private final String priceWithContractMonthlyDefault;
    private final String priceWithContractMonthlyNewQuantity;
    private final String priceWithoutContractMonthlyNewQuantity;
    private final String priceWithContractAnnualNewQuantity;
    private final String priceValueRegex;

    public PackageComponentTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Annual_Contract_NonContract_DLsOnly.json",
                Dataset.class);
        steps = new Steps(data);

        officeServiceName = data.packageFolders[0].name;
        packageWithContract = data.packageFolders[0].packages[0];
        monthlyChargeTerm = data.packageFolders[0].chargeTerm;
        annualChargeTerm = data.packageFolders[1].chargeTerm;

        var dlUnlimitedContract = data.getProductByDataName("LC_DL-UNL_50",
                singletonList(data.packageFolders[0].packages[0].productsDefault));
        var dlUnlimitedNoContract = data.getProductByDataName("LC_DL-UNL_50",
                singletonList(data.packageFolders[0].packages[1].productsDefault));
        var dlUnlimitedContractAnnual = data.getProductByDataName("LC_DL-UNL_50",
                singletonList(data.packageFolders[1].packages[0].productsDefault));

        newQuantity = dlUnlimitedContract.priceRater[1].minimumBorder;
        priceWithContractMonthlyDefault = dlUnlimitedContract.priceRater[0].raterPrice.toString();
        priceWithContractMonthlyNewQuantity = dlUnlimitedContract.priceRater[1].raterPrice.toString();
        priceWithoutContractMonthlyNewQuantity = dlUnlimitedNoContract.priceRater[1].raterPrice.toString();
        priceWithContractAnnualNewQuantity = dlUnlimitedContractAnnual.priceRater[1].raterPrice.toString();

        //  e.g. "30.99", "525.00", etc.
        priceValueRegex = "^\\d+\\.\\d{2}$";
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-26996")
    @DisplayName("CRM-26996 - Redesign QT 2.0. Package Selection. Package Component for New Business")
    @Description("Verify that the User can select a Package on Select Package tab of UQT, " +
            "specify a number of licenses, charge term, contract, and view the prices for the digital line")
    public void test() {
        step("1. Open the Quote Wizard for the New Business Opportunity to add a new Sales Quote, " +
                "and select Service = '" + officeServiceName + "'", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            packagePage.packageSelector.packageFilter.servicePicklist.selectOption(officeServiceName);
        });

        step("2. Select a new package and check its elements", () -> {
            packagePage.packageSelector.selectPackage(data.chargeTerm, officeServiceName, packageWithContract);

            var selectedPackage = packagePage.packageSelector.getSelectedPackage();
            selectedPackage.getName().shouldHave(exactTextCaseSensitive(packageWithContract.getFullName()), ofSeconds(20));
            selectedPackage.getPriceValue().shouldHave(exactText(priceWithContractMonthlyDefault));
            selectedPackage.getPriceCurrency().shouldHave(exactText(data.getCurrencyIsoCode()));
        });

        step("3. Check the price and currency elements on the other packages", () -> {
            packagePage.packageSelector.packagesPriceValues.asDynamicIterable()
                    .forEach(packagePriceValue -> packagePriceValue.should(matchText(priceValueRegex)));
            packagePage.packageSelector.packagesPriceCurrencies.asDynamicIterable()
                    .forEach(packagePriceCurrency -> packagePriceCurrency.shouldHave(exactText(data.getCurrencyIsoCode())));
        });

        step("4. Set 'Number of Licenses' = 100, and check the price on the preselected package", () -> {
            packagePage.packageSelector.getPackageFolderByName(officeServiceName).setNumberOfLicenses(newQuantity);
            packagePage.packageSelector.getSelectedPackage().getPriceValue()
                    .shouldHave(exactText(priceWithContractMonthlyNewQuantity));
        });

        step("5. Deselect the Contract checkbox, and check the price on the preselected package", () -> {
            packagePage.packageSelector.setContractSelected(false);
            packagePage.packageSelector.getSelectedPackage().getPriceValue()
                    .shouldHave(exactText(priceWithoutContractMonthlyNewQuantity));
        });

        step("6. Select the Contract checkbox, and check the price on the preselected package", () -> {
            packagePage.packageSelector.setContractSelected(true);
            packagePage.packageSelector.getSelectedPackage().getPriceValue()
                    .shouldHave(exactText(priceWithContractMonthlyNewQuantity));
        });

        step("7. Select Charge Term = 'Annual', and check the price on the preselected package", () -> {
            packagePage.packageSelector.packageFilter.selectChargeTerm(annualChargeTerm);
            packagePage.packageSelector.getSelectedPackage().getPriceValue()
                    .shouldHave(exactText(priceWithContractAnnualNewQuantity));
        });

        step("8. Select Charge Term = 'Monthly', and check the price on the preselected package", () -> {
            packagePage.packageSelector.packageFilter.selectChargeTerm(monthlyChargeTerm);
            packagePage.packageSelector.getSelectedPackage().getPriceValue()
                    .shouldHave(exactText(priceWithContractMonthlyNewQuantity));
        });
    }
}
