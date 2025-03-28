package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.MVP_SERVICE;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.PRIMARY_QUOTE_HAS_ERRORS_ERROR;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage.PACKAGE_CAN_ONLY_HAVE_DL_UNLIMITED_IN_TOTAL;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitive;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

@Tag("P0")
@Tag("P1")
@Tag("PDV")
@Tag("NGBS")
@Tag("PriceTab")
public class DefaultCartItemsTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    //  Test data
    private final Product[] defaultProducts;
    private final String packageFolderName;
    private final Package advancedPackage;
    private final Product dlUnlimitedAdvanced;

    public DefaultCartItemsTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_NonContract_1TypeOfDL.json",
                Dataset.class);
        steps = new Steps(data);

        defaultProducts = new Product[]{data.getProductByDataName("LC_MLN_31"),
                data.getProductByDataName("LC_MLFN_45")};
        packageFolderName = data.packageFolders[0].name;
        advancedPackage = data.packageFolders[0].packages[1];
        dlUnlimitedAdvanced = data.getProductByDataName("LC_DL-UNL_50", advancedPackage);
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-19887")
    @TmsLink("CRM-19899")
    @TmsLink("CRM-24931")
    @DisplayName("CRM-19887 - New Business. New Quotes get Main Local, Main Local Fax and Additional Local Numbers automatically added to Cart. \n" +
            "CRM-19899 - New Business. Main Local, Main Local Fax and Additional Local Numbers are added after selecting a different package. \n" +
            "CRM-24931 - Error message in UQT and Process Order for missing DLs")
    @Description("CRM-19887 - Verify that new Quotes get Main Local Number, Main Local Fax Number and Additional Local Number " +
            "added to Cart upon selection of a new Package. \n" +
            "CRM-19899 - Verify that Quotes get Main Local, Main Local Fax and Additional Local Numbers " +
            "added to Cart upon selection of a new Package. \n" +
            "CRM-24931 - Verify that the error message appears in UQT and in Process Order modal " +
            "if the Quote doesn't contain DL Unlimited in the Price tab")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                        "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        //  CRM-19887
        step("2. Open the Price tab and check Main Local Number, Main Local Fax Number and Additional Local Number", () -> {
            cartPage.openTab();
            steps.cartTab.checkProductsInCartNewBusiness(defaultProducts);
        });

        //  CRM-19899
        step("3. Open the Select Package tab, and select different package", () -> {
            packagePage.openTab();
            packagePage.packageSelector.selectPackage(data.chargeTerm, packageFolderName, advancedPackage);
        });

        //  CRM-19899
        step("4. Open the Price tab again and check Main Local Number, Main Local Fax Number and Additional Local Number", () -> {
            cartPage.openTab();
            steps.cartTab.checkProductsInCartNewBusiness(defaultProducts);
        });

        //  CRM-24931
        step("5. Remove DigitalLine Unlimited item from the Cart, " +
                "check the error message on the Price tab", () -> {
            cartPage.getQliFromCartByDisplayName(dlUnlimitedAdvanced.name).getDeleteButton().click();

            cartPage.notificationBar.click();
            cartPage.notifications.shouldHave(itemWithText(format(PACKAGE_CAN_ONLY_HAVE_DL_UNLIMITED_IN_TOTAL,
                    advancedPackage.name, packageFolderName)));
        });

        step("6. Open the Quote Details tab, set Main Area Code, and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.saveChanges();
        });

        //  CRM-24931
        step("7. Open the Opportunity record page, click the 'Process Order' button, and check error message in the modal", () -> {
            opportunityPage.openPage(steps.quoteWizard.opportunity.getId());
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60));
            opportunityPage.processOrderModal.errorNotifications
                    .shouldHave(exactTextsCaseSensitive(format(PRIMARY_QUOTE_HAS_ERRORS_ERROR, MVP_SERVICE)));
        });
    }
}
