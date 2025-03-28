package ngbs.quotingwizard.newbusiness.dqtab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("DealQualificationTab")
@Tag("Multiproduct-Lite")
public class DisplayLicenseRequirementsSingleProductTest extends BaseTest {
    private final Steps steps;
    private final DealQualificationSteps dqSteps;

    //  Test data
    private final String packageName;

    private final Product phone;

    private final String phoneNewQuantity;
    private final String phoneNewDiscount;

    public DisplayLicenseRequirementsSingleProductTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_PhonesAndDLs.json",
                Dataset.class);
        steps = new Steps(data);
        dqSteps = new DealQualificationSteps(data);

        packageName = data.packageFolders[0].packages[0].name;

        phone = data.getProductByDataName("LC_HD_523");

        //  this value should exceed a 'ceiling' configured in the custom metadata types:
        //  DiscountCeilingCBox__mdt, DiscountCeilingCategory__mdt, DiscountCeilingLicense__mdt
        phone.discount = 85;

        phoneNewDiscount = "92";
        phoneNewQuantity = "42";
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-30049")
    @DisplayName("CRM-30049 - Single service, creating new DQ")
    @Description("Verify that License Requirements section is displayed correctly when items from single service require approval. \n" +
            "Verify that Quantity and Ceiling Discount fields can be updated.")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, select a package for it, " +
                "add Products to the Cart, and save changes on the Price tab", () ->
                steps.cartTab.prepareCartTabViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Quote Details tab, populate Discount Justification and Main Area Code fields, and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.discountJustificationTextArea.setValue(TEST_STRING);
            quotePage.saveChanges();
        });

        step("3. Open the Price tab, set quantities and discounts for the DL Unlimited and the phone, " +
                "and save changes", () -> {
            cartPage.openTab();
            steps.cartTab.setUpQuantities(dqSteps.dlUnlimited, phone);
            steps.cartTab.setUpDiscounts(dqSteps.dlUnlimited, phone);
            cartPage.saveChanges();
        });

        step("4. Submit the quote for approval via 'Submit for Approval' button", () -> {
            cartPage.submitForApproval();
        });

        step("5. Open the Deal Qualification tab, " +
                "verify items in the 'License Requirements' section on 'Deal Qualification' tab", () -> {
            dealQualificationPage.openTab();
            dealQualificationPage.licenseRequirementsSection.shouldBe(visible);

            dqSteps.checkLicenseRequirementsItem(dqSteps.dlUnlimited, packageName);
            dqSteps.checkLicenseRequirementsItem(phone, packageName);
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

            var phoneItem = dealQualificationPage.getLicenseRequirementsItem(phone.name);
            phoneItem.getQuantityInput().setValue(phoneNewQuantity);
            phoneItem.getCeilingDiscountInput().setValue(phoneNewDiscount);

            dealQualificationPage.submitForApproval();
        });

        step("8. Check the updated Quantity__c and Discount__c values on DQ_Deal_Qualification_Discounts__c records", () -> {
            dqSteps.checkUpdatedDqDiscountsInDB(dqSteps.dlUnlimited, dqSteps.dlUnlimitedNewQuantity, dqSteps.dlUnlimitedNewDiscount);
            dqSteps.checkUpdatedDqDiscountsInDB(phone, phoneNewQuantity, phoneNewDiscount);
        });
    }
}
