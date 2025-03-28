package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage.PROVISIONING_AND_SHIPPING_WILL_BE_DISABLED_MESSAGE;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.PROVISIONING_NOT_ALLOWED_WHEN_PROSERV_INITIATED_MESSAGE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.containExactTextsCaseSensitive;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("LTR-121")
@Tag("ProServInNGBS")
@Tag("ShippingTab")
public class ShippingTabIsHiddenIfProServPackageSelectedTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final InitiateAndCancelProServSteps initiateAndCancelProServSteps;

    private User salesRepUserWithProServInNgbsFT;

    //  Test data
    private final Product dlUnlimited;
    private final Product phoneToAdd;
    private final int expectedMvpQuotesQuantityMultiProduct;
    private final int expectedMvpQuotesQuantitySingleProduct;
    private final ShippingGroupAddress shippingAddress;
    private final String shippingAddressFormatted;
    private final String proServServiceName;
    private final Package proServPackage;

    public ShippingTabIsHiddenIfProServPackageSelectedTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_ProServ_Monthly_Contract.json",
                Dataset.class);

        steps = new Steps(data);
        initiateAndCancelProServSteps = new InitiateAndCancelProServSteps();

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        phoneToAdd = data.getProductByDataName("LC_HDR_619");
        //  1 Master MVP + 1 Tech MVP Quotes
        expectedMvpQuotesQuantityMultiProduct = 2;
        //  1 Master MVP Quote
        expectedMvpQuotesQuantitySingleProduct = 1;

        shippingAddress = new ShippingGroupAddress(
                "United States", "Findlay",
                new ShippingGroupAddress.State("Ohio", true),
                "3644 Cedarstone Drive", "45840", "QA Automation");
        shippingAddressFormatted = shippingAddress.getAddressFormatted();

        proServServiceName = data.packageFolders[4].name;
        proServPackage = data.packageFolders[4].packages[0];
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with 'Sales Rep - Lightning' profile and 'ProServ in NGBS' feature toggle", () -> {
            salesRepUserWithProServInNgbsFT = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withFeatureToggles(List.of(PROSERV_IN_NGBS_FT))
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserWithProServInNgbsFT);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUserWithProServInNgbsFT);

        step("Log in as a user with 'Sales Rep - Lightning' profile, 'ProServ in NGBS' feature toggle, " +
                "and 'EnableSuperUserProServ In UQT' permission set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(salesRepUserWithProServInNgbsFT);
        });
    }

    @Test
    @TmsLink("CRM-37803")
    @DisplayName("CRM-37803 - The Shipping tab is hidden if the ProServ package was selected in the Quote")
    @Description("Verify that: \n" +
            " - The Shipping tab is hidden if the ProServ package was added to the Quote \n" +
            " - A notification message appears in the 'Price' tab to inform user that Provisioning and Shipping are disabled \n" +
            " - The 'Provision' toggle is set as 'OFF' automatically \n" +
            " - Quote.EnableLBO__c = true if the ProServ package was added to the Quote")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, select a package for it, " +
                "open the Price Tab, assign all devices to the DLs, and check that the Shipping tab is visible", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectDefaultPackageFromTestData();
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());

            cartPage.openTab();
            steps.cartTab.assignDevicesToDL(phoneToAdd.name, dlUnlimited.name, steps.quoteWizard.localAreaCode, phoneToAdd.quantity);

            wizardPage.shippingTabButton.shouldBe(visible);
        });

        step("2. Open the Shipping tab, add a new Shipping Group, assign any number of phones to it, save changes, " +
                "and check that custom address assignments records related to the quote are created", () -> {
            shippingPage.openTab();
            shippingPage.addNewShippingGroup(shippingAddress);
            shippingPage.assignDeviceToShippingGroup(phoneToAdd.productName, shippingAddressFormatted, shippingAddress.shipAttentionTo);
            shippingPage.saveChanges();

            initiateAndCancelProServSteps.checkIfCustomAssignmentRecordsArePresent(true);
        });

        step("3. Open the Quote Details tab, set the Main Area Code, and check that Provisioning toggle is turned on", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            steps.lbo.checkProvisionToggleOn(true);
        });

        step("4. Open the Select Package tab, select ProServ package with 'UC' and 'CC' selected checkboxes, and save changes", () -> {
            packagePage.openTab();
            packagePage.packageSelector.selectPackage(data.chargeTerm, proServServiceName, proServPackage);
            packagePage.saveChanges();
        });

        step("5. Open the Quote Details tab, check that Shipping tab is hidden, Provisioning toggle is turned on and disabled, " +
                "Sales Quotes have Enabled_LBO__c = 'true', and all CustomAddressAssignments__c records related to the quote are deleted", () -> {
            quotePage.openTab();
            initiateAndCancelProServSteps.checkShippingTabAndProvisioning(hidden, false,
                    steps.quoteWizard.opportunity.getId(), expectedMvpQuotesQuantityMultiProduct);
            initiateAndCancelProServSteps.checkIfCustomAssignmentRecordsArePresent(false);
        });

        step("6. Hover over tooltip icon near Provisioning toggle and check the text message near the tooltip", () -> {
            quotePage.provisionToggleInfoIcon.hover();
            quotePage.tooltip.shouldHave(exactTextCaseSensitive(PROVISIONING_NOT_ALLOWED_WHEN_PROSERV_INITIATED_MESSAGE));
        });

        step("7. Open the Price tab, and check the info notification message", () -> {
            cartPage.openTab();
            cartPage.notificationBar.click();
            cartPage.notifications.should(containExactTextsCaseSensitive(PROVISIONING_AND_SHIPPING_WILL_BE_DISABLED_MESSAGE));
        });

        step("8. Open the Select Package tab, unselect ProServ package, save changes, " +
                "open the Quote Details tab, check that Shipping tab is displayed, " +
                "Provisioning toggle is turned off and enabled, and Sales Quote have Enabled_LBO__c = 'true'", () -> {
            packagePage.openTab();
            packagePage.packageSelector.getPackageFolderByName(proServServiceName).expandFolder()
                    .getChildPackageByName(proServPackage.getFullName())
                    .getUnselectButton()
                    .click();
            packagePage.saveChanges();

            quotePage.openTab();
            initiateAndCancelProServSteps.checkShippingTabAndProvisioning(visible, true,
                    steps.quoteWizard.opportunity.getId(), expectedMvpQuotesQuantitySingleProduct);
        });
    }
}
