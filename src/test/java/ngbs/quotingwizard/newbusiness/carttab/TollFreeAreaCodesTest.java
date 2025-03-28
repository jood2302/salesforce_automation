package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.internal.proxy.SelenideProxyMocksDisabledExtension;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static base.Pages.*;
import static com.codeborne.selenide.CollectionCondition.allMatch;
import static com.codeborne.selenide.CollectionCondition.textsInAnyOrder;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("NGBS")
@ExtendWith(SelenideProxyMocksDisabledExtension.class)
public class TollFreeAreaCodesTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final Product mainLocalNumber;
    private final Product mainTollFreeNumber;
    private final List<String> countries;

    public TollFreeAreaCodesTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_NoPhones.json",
                Dataset.class);
        steps = new Steps(data);

        mainLocalNumber = data.getProductByDataName("LC_MLN_31");
        mainTollFreeNumber = data.getProductByDataName("LC_MTN_32");
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
    @TmsLink("CRM-13318")
    @TmsLink("CRM-13317")
    @DisplayName("CRM-13318 - Area Code selector shows Toll Free Area Codes on Toll-Free Numbers. \n" +
            "CRM-13317 - Area Code selector shows countries According to Package. Toll-Free Number.")
    @Description("CRM-13318 - Verify that Area Code Selector shows appropriate countries for the package. \n" +
            "CRM-13317 - Verify that Area Code Selector shows appropriate countries for the package.")
    public void test() {
        step("1. Open the Quote Wizard for the New Business Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes, " +
                "remove Main Local Number and add Main Toll Free Number on the Add Products tab, " +
                "save changes on the Price tab", () -> {
            steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());

            productsPage.openTab();
            productsPage.removeProduct(mainLocalNumber);
            productsPage.addProduct(mainTollFreeNumber);

            cartPage.openTab();
            cartPage.saveChanges();
        });

        //  CRM-13318, CRM-13317
        step("2. Click on Area code selector for 'Main Toll-Free Number', " +
                "check available countries, and check available codes for the US", () -> {
            cartPage.getQliFromCartByDisplayName(mainTollFreeNumber.name)
                    .getAreaCodeButton()
                    .click();
            areaCodePage.addAreaCodeButton.click();
            var areaCodeLineItem = areaCodePage.getFirstAreaCodeItem();
            areaCodeLineItem.getAreaCodeSelector().getInputElement().click();

            //  CRM-13317
            areaCodeLineItem.getAreaCodeSelector().getSearchResults().shouldHave(textsInAnyOrder(countries));
            areaCodeLineItem.getAreaCodeSelector().selectOption(steps.quoteWizard.tollFreeAreaCode.country);

            //  CRM-13318
            //  Toll-Free Number should have 3-digit codes for US, Canada, Puerto Rico (888, 808, etc...)
            areaCodeLineItem.getAreaCodeSelector().getSearchResults()
                    .should(allMatch("All toll-free numbers should have 3-digit codes",
                            webElement -> webElement.getText().matches("^\\d{3}$")));
            areaCodePage.cancelButton.click();
        });
    }
}
