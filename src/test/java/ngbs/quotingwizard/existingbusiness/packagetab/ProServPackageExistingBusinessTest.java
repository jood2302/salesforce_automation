package ngbs.quotingwizard.existingbusiness.packagetab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.PackageFactory.createBillingAccountPackage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.INVOICE_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.PAID_RC_ACCOUNT_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("P1")
@Tag("LTR-121")
@Tag("ProServInNGBS")
public class ProServPackageExistingBusinessTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    private User salesRepUserWithProServInNgbsFT;

    //  Test data
    private final String officeServiceName;
    private final Package officePackage;
    private final String engageDigitalServiceName;
    private final Package engageDigitalPackage;
    private final String proServServiceName;
    private final Package proServPackage;
    private final Product proServProductWithDiscount;

    public ProServPackageExistingBusinessTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_ProServ_Monthly_Contract_212407013.json",
                Dataset.class);

        steps = new Steps(data);
        officeServiceName = data.packageFolders[0].name;
        officePackage = data.packageFolders[0].packages[0];
        engageDigitalServiceName = data.packageFolders[1].name;
        engageDigitalPackage = data.packageFolders[1].packages[0];
        proServServiceName = data.packageFolders[2].name;
        proServPackage = data.packageFolders[2].packages[0];

        proServProductWithDiscount = data.getProductByDataName("PS_UCOT_1431005", proServPackage);

        //  because only 1 Package__c object is allowed in SFDC for each Package ID in NGBS
        steps.ngbs.isGenerateAccountsForSingleTest = true;
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateMultiProductUnifiedBillingAccount();
        steps.ngbs.stepCreateDiscountsInNGBS(data.billingId, proServPackage.ngbsPackageId, proServProductWithDiscount);

        step("Find a user with 'Sales Rep - Lightning' profile, 'ProServ in NGBS' feature toggle " +
                "and 'Enable Super User ProServ In UQT' permission set", () -> {
            salesRepUserWithProServInNgbsFT = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withFeatureToggles(List.of(PROSERV_IN_NGBS_FT))
                    //  to add ProServ Products (as a Sales Rep user)
                    .withPermissionSet(ENABLE_SUPER_USER_PROSERV_IN_UQT_PS)
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserWithProServInNgbsFT);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUserWithProServInNgbsFT);
        steps.syncWithNgbs.stepCreateAdditionalActiveSalesAgreement(steps.salesFlow.account,
                steps.salesFlow.contact, salesRepUserWithProServInNgbsFT);

        step("Create new Billing Account Package objects (Package__c) for the Account " +
                "for the Office, Engage Digital and Professional Services NGBS packages via SFDC API", () -> {
            createBillingAccountPackage(steps.salesFlow.account.getId(), data.packageId, officePackage.id,
                    data.brandName, officeServiceName, INVOICE_PAYMENT_METHOD, PAID_RC_ACCOUNT_STATUS);

            createBillingAccountPackage(steps.salesFlow.account.getId(), engageDigitalPackage.ngbsPackageId, engageDigitalPackage.id,
                    data.brandName, engageDigitalServiceName, INVOICE_PAYMENT_METHOD, PAID_RC_ACCOUNT_STATUS);

            createBillingAccountPackage(steps.salesFlow.account.getId(), proServPackage.ngbsPackageId, proServPackage.id,
                    data.brandName, proServServiceName, INVOICE_PAYMENT_METHOD, PAID_RC_ACCOUNT_STATUS);
        });

        step("Log in as a user with 'Sales Rep - Lightning' profile, 'ProServ in NGBS' feature toggle, " +
                "and 'Enable Super User ProServ In UQT' permission set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(salesRepUserWithProServInNgbsFT);
        });
    }

    @Test
    @TmsLink("CRM-37904")
    @TmsLink("CRM-38721")
    @DisplayName("CRM-37904 - ProServ Package on Existing Business Quote. \n" +
            "CRM-38721 - PS package availability on the 'Select Package' tab for MVP+ED+PS Account")
    @Description("CRM-37904 - Verify that: \n" +
            "- By default, the ProServ package is not selected in the Quote. \n" +
            "- The process for ProServ behaves the same as a new business opportunity when ProServ package is selected. \n" +
            "CRM-38721 - Verify that the ProServ package can be selected on the 'Select Package' tab " +
            "of the change order Opportunity of the MVP+ED+PS EB Account")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
        });

        //  CRM-37904
        step("2. Check that none of the ProServ packages are pre-selected on the Select Packages tab", () -> {
            var proServPackageFolder = packagePage.packageSelector.getPackageFolderByName(proServServiceName);
            proServPackageFolder.expandFolder().getPackagesElements().shouldHave(sizeGreaterThan(0));

            proServPackageFolder.getAllPackages().forEach(pkg -> {
                step("Check the ProServ package = " + pkg.getName().getText(), () -> {
                    pkg.getSelectButton().shouldBe(visible);
                    pkg.getUnselectButton().shouldBe(hidden);
                });
            });
        });

        //  CRM-38721
        step("3. Select the ProServ package from the NGBS Account, save changes " +
                "and make sure that the quote is saved successfully with ProServ, MVP and ED packages selected", () -> {
            //  MVP and ED packages are already preselected
            packagePage.packageSelector.selectPackage(data.chargeTerm, proServServiceName, proServPackage);

            packagePage.saveChanges();
        });

        //  CRM-37904
        step("4. Open the Price tab, and check that the ProServ products are not added automatically", () -> {
            cartPage.openTab();

            Arrays.stream(proServPackage.productsFromBilling).forEach(product -> {
                step("Check the ProServ product = " + product.name, () -> {
                    cartPage.getQliFromCartByDisplayName(product.name).getCartItemElement().shouldBe(hidden);
                });
            });
        });

        //  CRM-37904
        step("5. Open the Add Products tab, add the ProServ product that has a discount in NGBS, " +
                "open the Price tab, and check that the discount for it = 0, and its 'Existing Quantity' field is hidden", () -> {
            productsPage.openTab();
            productsPage.addProduct(proServProductWithDiscount);

            cartPage.openTab();
            var cartItem = cartPage.getQliFromCartByDisplayName(proServProductWithDiscount.name);
            cartItem.getDiscountInput().shouldHave(exactValue("0"));
            cartItem.getExistingQuantityInput().shouldBe(hidden);
        });
    }
}
