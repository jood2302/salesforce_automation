package ngbs.quotingwizard.newbusiness.dqtab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.Map;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("DealQualificationTab")
@Tag("Multiproduct-Lite")
public class DisplayLicenseRequirementsMultiProductTest extends BaseTest {
    private final Steps steps;
    private final DealQualificationSteps dqSteps;

    //  Test data
    private final Package officePackage;
    private final Package engageDigitalPackage;
    private final Package rcCcPackage;

    private final Map<String, Package> packageFolderNameToPackageMap;

    private final Product engageDigitalProduct;
    private final Product rcCcProduct;

    private final String engageProductNewQuantity;
    private final String engageProductNewDiscount;
    private final String rcCcProductNewQuantity;
    private final String rcCcProductNewDiscount;

    public DisplayLicenseRequirementsMultiProductTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_Annual_Contract.json",
                Dataset.class);
        steps = new Steps(data);
        dqSteps = new DealQualificationSteps(data);

        officePackage = data.packageFolders[0].packages[0];
        engageDigitalPackage = data.packageFolders[2].packages[0];
        rcCcPackage = data.packageFolders[3].packages[0];

        packageFolderNameToPackageMap = Map.of(
                data.packageFolders[0].name, officePackage,
                data.packageFolders[2].name, engageDigitalPackage,
                data.packageFolders[3].name, rcCcPackage
        );

        engageDigitalProduct = data.getProductByDataName("SA_SEAT_5", engageDigitalPackage);
        rcCcProduct = data.getProductByDataName("CC_RCCCSEAT_1", rcCcPackage);

        dqSteps.dlUnlimited.quantity = 2;

        //  these values should exceed a 'ceiling' configured in the custom metadata types:
        //  DiscountCeilingCBox__mdt, DiscountCeilingCategory__mdt, DiscountCeilingLicense__mdt
        engageDigitalProduct.discount = 82;
        rcCcProduct.discount = 83;

        engageProductNewQuantity = "91";
        engageProductNewDiscount = "28";
        rcCcProductNewQuantity = "92";
        rcCcProductNewDiscount = "29";
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-30050")
    @DisplayName("CRM-30050 - Multiple services, creating new DQ")
    @Description("Verify that License Requirements section is displayed correctly when items from different services require approval. \n" +
            "Verify that Quantity and Ceiling Discount fields can be updated")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select MVP, Engage Digital and RC Contact Center packages for it, and save changes", () -> {
            steps.quoteWizard.prepareOpportunityForMultiProduct(steps.quoteWizard.opportunity.getId(), packageFolderNameToPackageMap);
        });

        step("2. Open the Quote Details tab, populate Discount Justification and Main Area Code fields, and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.discountJustificationTextArea.setValue(TEST_STRING);
            quotePage.saveChanges();
        });

        step("3. Open the Price tab, set quantities and discounts on at least 1 item on each service and save changes", () -> {
            cartPage.openTab();
            steps.cartTab.setUpQuantities(dqSteps.dlUnlimited, engageDigitalProduct, rcCcProduct);
            steps.cartTab.setUpDiscounts(dqSteps.dlUnlimited, engageDigitalProduct, rcCcProduct);
            cartPage.saveChanges();
        });

        step("4. Submit the quote for approval via 'Submit for Approval' button", () -> {
            cartPage.submitForApproval();
        });

        step("5. Open the Deal Qualification tab " +
                "and verify items in the 'License Requirements' section on 'Deal Qualification' tab", () -> {
            dealQualificationPage.openTab();
            dealQualificationPage.licenseRequirementsSection.shouldBe(visible);

            dqSteps.checkLicenseRequirementsItem(dqSteps.dlUnlimited, officePackage.name);
            dqSteps.checkLicenseRequirementsItem(engageDigitalProduct, engageDigitalPackage.name);
            dqSteps.checkLicenseRequirementsItem(rcCcProduct, rcCcPackage.name);
        });

        step("6. Open the Price tab, click 'Submit for Approval' button and click 'Revise' button via Revise DQ modal, " +
                "check that the user is redirected to the Deal Qualification tab, " +
                "that Approval Status = 'Revision Pending', " +
                "and that Quantity and Ceiling Discount fields are editable in the License Requirements section", () ->
                dqSteps.checkDqApprovalStatusAndLicenseRequirementsFieldsAfterRevise()
        );

        step("7. Set new values in 'Quantity' and 'Ceiling Discount' fields in the 'License Requirements' section, " +
                "and click 'Submit For Approval' button", () -> {
            var dlUnlimitedItem = dealQualificationPage.getLicenseRequirementsItem(dqSteps.dlUnlimited.productName);
            dlUnlimitedItem.getQuantityInput().setValue(dqSteps.dlUnlimitedNewQuantity);
            dlUnlimitedItem.getCeilingDiscountInput().setValue(dqSteps.dlUnlimitedNewDiscount);

            var engageDigitalItem = dealQualificationPage.getLicenseRequirementsItem(engageDigitalProduct.productName);
            engageDigitalItem.getQuantityInput().setValue(engageProductNewQuantity);
            engageDigitalItem.getCeilingDiscountInput().setValue(engageProductNewDiscount);

            var rcCcItem = dealQualificationPage.getLicenseRequirementsItem(rcCcProduct.productName);
            rcCcItem.getQuantityInput().setValue(rcCcProductNewQuantity);
            rcCcItem.getCeilingDiscountInput().setValue(rcCcProductNewDiscount);

            dealQualificationPage.submitForApproval();
        });

        step("8. Check the updated Quantity__c and Discount__c values on DQ_Deal_Qualification_Discounts__c records", () -> {
            dqSteps.checkUpdatedDqDiscountsInDB(dqSteps.dlUnlimited, dqSteps.dlUnlimitedNewQuantity, dqSteps.dlUnlimitedNewDiscount);
            dqSteps.checkUpdatedDqDiscountsInDB(engageDigitalProduct, engageProductNewQuantity, engageProductNewDiscount);
            dqSteps.checkUpdatedDqDiscountsInDB(rcCcProduct, rcCcProductNewQuantity, rcCcProductNewDiscount);
        });
    }
}
