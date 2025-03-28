package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.internal.proxy.SelenideProxyMocksDisabledExtension;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.page.components.AreaCodeSelector;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Area_Code_Line_Item__c;
import com.sforce.soap.enterprise.sobject.QuoteLineItem;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToInteger;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.codeborne.selenide.CollectionCondition.allMatch;
import static com.codeborne.selenide.CollectionCondition.textsInAnyOrder;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.text;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("PDV")
@Tag("NGBS")
@ExtendWith(SelenideProxyMocksDisabledExtension.class)
public class AreaCodeAssignToPhoneTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String statesPattern;
    private final String citiesPattern;
    private final Product polycomPhone;
    private final Product ciscoPhone;
    private final Product digitalLineUnlimited;
    private final Product mainLocalNumber;
    private final int quantityDLs;
    private final AreaCode puertoRicoAreaCode;
    private final List<String> countries;

    public AreaCodeAssignToPhoneTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_AnnualAndMonthly_Contract_PhonesAndDLs_Promos.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        //  to check states, like "New York", "California", "Empire State-With-Dash", etc...
        statesPattern = "^[a-zA-Z -]+$";
        //  to check locations with codes, like "Dublin-San Ramon (925)", "Anaheim (657)", "Compton, Gardena (454)", "Beverly Hills (310)", etc...
        citiesPattern = "^[a-zA-Z-() ,]+ \\(\\d{2,4}\\)";
        polycomPhone = data.getProductByDataName("LC_HD_936");
        ciscoPhone = data.getProductByDataName("LC_HD_523");
        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        mainLocalNumber = data.getProductByDataName("LC_MLFN_45");
        quantityDLs = digitalLineUnlimited.quantity;
        puertoRicoAreaCode = new AreaCode("Local", "Puerto Rico", EMPTY_STRING, EMPTY_STRING, "787");
        countries = List.of("United States", "Canada", "Puerto Rico");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-13289")
    @TmsLink("CRM-20576")
    @TmsLink("CRM-13294")
    @TmsLink("CRM-13314")
    @TmsLink("CRM-13313")
    @DisplayName("CRM-13289 - Area Code can be assigned to a Phone. \n" +
            "CRM-20576 - Save button saves changes in cart. New Business. \n" +
            "CRM-13294 - Area Code selector shows states if selected country has states. \n" +
            "CRM-13314 - Area Code selector shows states if selected country has states. \n" +
            "CRM-13313 - Area Code selector shows countries According to Package. Local Number.")
    @Description("CRM-13289 - Verify that Area Code can be assigned to a Phone. \n" +
            "CRM-20576 - Verify that pressing Save button on Price tab saves changes made on the said tab and on Add Products tab. \n" +
            "CRM-13294 - Verify that Area Code selector shows states if selected Country has them. \n" +
            "CRM-13314 - Verify that Area Code selector shows states if selected Country has them. \n" +
            "CRM-13313 - Verify that Area Code Selector shows appropriate countries for the package.")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, add some products on the Add Products tab", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectDefaultPackageFromTestData();
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());
        });

        step("2. Open the Price tab, and increase the quantity of DLs and Phones", () -> {
            cartPage.openTab();
            cartPage.setQuantityForQLItem(digitalLineUnlimited.name, quantityDLs);
            cartPage.setQuantityForQLItem(polycomPhone.name, polycomPhone.quantity);
            cartPage.setQuantityForQLItem(ciscoPhone.name, ciscoPhone.quantity);
        });

        //  CRM-20576
        step("3. Check the Price tab's content", () -> {
            steps.cartTab.checkProductsInCartNewBusiness(data.getNewProductsToAdd());
            steps.cartTab.checkProductsInCartNewBusiness(data.getProductsDefault());
        });

        //  CRM-13294
        step("4. Click on the Area code selector for 'DigitalLine Unlimited' " +
                "and check available states for the US and locations for Puerto Rico", () -> {
            stepCheckAreaCodeSelectorOptionsOnDeviceAssignmentPage(steps.quoteWizard.localAreaCode);
            stepCheckAreaCodeSelectorOptionsOnDeviceAssignmentPage(puertoRicoAreaCode);
        });

        //  CRM-13314, CRM-13313
        step("5. Click on the Area code selector for 'Main Local Number', check available Countries then " +
                "check available states for the US and locations for Puerto Rico", () -> {
            stepCheckAreaCodeSelectorOptionsOnAreaCodePage(steps.quoteWizard.localAreaCode);
            stepCheckAreaCodeSelectorOptionsOnAreaCodePage(puertoRicoAreaCode);
        });

        step("6. Assign the Area Code(s) of Digital Line to added phone(s)", () -> {
            steps.cartTab.assignDevicesToDL(polycomPhone.name, digitalLineUnlimited.name, steps.quoteWizard.localAreaCode,
                    polycomPhone.quantity);
            steps.cartTab.assignDevicesToDL(ciscoPhone.name, digitalLineUnlimited.name, steps.quoteWizard.localAreaCode,
                    ciscoPhone.quantity);
            cartPage.saveChanges();

            //  Each phone gets 1 AssignmentLineItem and DigitalLine Unlimited gets 2 (for every assigned phone)
            cartPage.getQliFromCartByDisplayName(polycomPhone.name).getNumberAssignmentLineItems()
                    .shouldHave(text("1"));
            cartPage.getQliFromCartByDisplayName(ciscoPhone.name).getNumberAssignmentLineItems()
                    .shouldHave(text("1"));
            cartPage.getQliFromCartByDisplayName(digitalLineUnlimited.name).getNumberAssignmentLineItems()
                    .shouldHave(text("2"));
        });

        //  CRM-20576
        step("7. Check that the quantities on QuoteLineItem(s) (Phone(s) and DL(s)) and Area Code Line Item(s) are equals", () -> {
            stepCheckUpdatedQuoteLineItemsAndAreaCodeLineItems(polycomPhone);
            stepCheckUpdatedQuoteLineItemsAndAreaCodeLineItems(ciscoPhone);
        });
    }

    @Step("Check updated quantities for QuoteLineItems (such as DL Unlimited and One-Time Phones) " +
            "and the related Area Code Line Items")
    private void stepCheckUpdatedQuoteLineItemsAndAreaCodeLineItems(Product phoneToCheck) throws Exception {
        //  QLI for Phone is present and has correct quantity
        var qliList = enterpriseConnectionUtils.query(
                "SELECT Id, Product2.ExtId__c, Quantity " +
                        "FROM QuoteLineItem " +
                        "WHERE Quote.OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "'",
                QuoteLineItem.class);

        var phoneToAddQLIActual = qliList.stream()
                .filter(qli -> qli.getProduct2().getExtID__c().equals(phoneToCheck.dataName))
                .findFirst();

        assertThat(phoneToAddQLIActual)
                .as("QLI for %s", phoneToCheck.name)
                .isPresent();
        assertThat(doubleToInteger(phoneToAddQLIActual.get().getQuantity()))
                .as(String.format("QuoteLineItem.Quantity for %s", phoneToCheck.name))
                .isEqualTo(phoneToCheck.quantity);

        //  DL quantity is present and correct
        var digitalLineQLIActual = qliList.stream()
                .filter(qli -> qli.getProduct2().getExtID__c().equals(digitalLineUnlimited.dataName))
                .findFirst();

        assertThat(digitalLineQLIActual)
                .as("QLI for %s", digitalLineUnlimited.name)
                .isPresent();
        assertThat(doubleToInteger(digitalLineQLIActual.get().getQuantity()))
                .as(String.format("QuoteLineItem.Quantity for %s", digitalLineUnlimited.name))
                .isEqualTo(quantityDLs);

        //  Area Code Line item is present with correct data
        var areaCodeLineItem = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id, Quantity__c " +
                        "FROM Area_Code_Line_Item__c " +
                        "WHERE Quote_Line_Item__r.Quote.OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                        "AND Quote_Line_Item__r.Display_Name__c = '" + phoneToCheck.name + "'",
                Area_Code_Line_Item__c.class);

        assertThat(doubleToInteger(areaCodeLineItem.getQuantity__c()))
                .as(String.format("Area_Code_Line_Item__c.Quantity__c for %s", phoneToCheck.name))
                .isEqualTo(phoneToCheck.quantity);
    }

    /**
     * Launch the Area Code Selector on Device Assignment page for the DL Unlimited,
     * switch to the Polycom phone section, select the Country, and check that the Area Code selector shows States if the selected Country has them.
     *
     * @param areaCode area code entity with the name of the country, state, city, and the code
     */
    @Step("Open Device Assignment page for a DL Unlimited, switch to the Polycom phone section, " +
            "and check that its Area Code Selector shows appropriate States/Cities for the selected Country")
    private void stepCheckAreaCodeSelectorOptionsOnDeviceAssignmentPage(AreaCode areaCode) {
        var pattern = areaCode.state.isBlank() ? citiesPattern : statesPattern;
        var locations = areaCode.state.isBlank() ? "Cities" : "States";

        cartPage.getQliFromCartByDisplayName(digitalLineUnlimited.name).getDeviceAssignmentButton().click();
        deviceAssignmentPage.getProductItemByName(polycomPhone.name)
                .getNameElement()
                .shouldHave(exactText(polycomPhone.name), ofSeconds(60));
        deviceAssignmentPage.getProductItemByName(polycomPhone.name)
                .getAddAreaCodeButton()
                .click();
        {
            var areaCodeInputLocator = deviceAssignmentPage.getFirstAreaCodeItem(polycomPhone.name).getAreaCodeSelector().getSelf();
            var areaCodeSelector = new AreaCodeSelector(areaCodeInputLocator);
            areaCodeSelector.selectOption(areaCode.country);
        }

        //  This block of code below is a workaround for StaleElementReferenceException
        //  that occurs after clear of AreaCodeSelector (need to re-initialize web element for area code input again)
        {
            var areaCodeInputLocator = deviceAssignmentPage.getFirstAreaCodeItem(polycomPhone.name).getAreaCodeSelector().getSelf();
            var areaCodeSelector = new AreaCodeSelector(areaCodeInputLocator);
            areaCodeSelector.getSearchResults()
                    .should(allMatch("All " + locations + " names match the RegExp " + pattern,
                            el -> el.getText().matches(pattern)));

            deviceAssignmentPage.cancelButton.click();
        }
    }

    /**
     * Launch the Area Code Selector on Area Code page for the Main Local Number, check available Countries,
     * select the Country, and check that the Area Code selector shows States if the selected Country has them.
     *
     * @param areaCode area code entity with the name of the country, state, city, and the code
     */
    @Step("Open Area Code assignment page for a Main Local Number, " +
            "check that Area Code Selector shows countries for the selected Package, " +
            "select a country and check available States/Cities")
    private void stepCheckAreaCodeSelectorOptionsOnAreaCodePage(AreaCode areaCode) {
        var pattern = areaCode.state.isBlank() ? citiesPattern : statesPattern;
        var locations = areaCode.state.isBlank() ? "Cities" : "States";

        cartPage.getQliFromCartByDisplayName(mainLocalNumber.name).getAreaCodeButton().click();
        areaCodePage.addAreaCodeButton.click();

        var areaCodeLineItem = areaCodePage.getFirstAreaCodeItem();
        areaCodeLineItem.getAreaCodeSelector().getInputElement().click();
        areaCodeLineItem.getAreaCodeSelector().getSearchResults()
                .shouldHave(textsInAnyOrder(countries));

        areaCodeLineItem.getAreaCodeSelector().selectOption(areaCode.country);
        areaCodeLineItem.getAreaCodeSelector().getSearchResults()
                .should(allMatch("All " + locations + " names match the RegExp " + pattern,
                        el -> el.getText().matches(pattern)));

        areaCodePage.cancelButton.click();
    }
}
