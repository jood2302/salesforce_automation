package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.DQ_Deal_Qualification__c;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.DqDealQualificationHelper.APPROVED_STATUS;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("FVT")
@Tag("Sunrisers")
@Tag("QuantityUpdate")
@Tag("PBC-8710")
public class ApprovalStatusOnQuantityUpdateTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Product digitalLineUnlimited;
    private final Product yealinkPhone;

    private final int aboveThresholdPositive;
    private final int aboveThresholdNegative;
    private final int belowThreshold;

    public ApprovalStatusOnQuantityUpdateTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_NonContract_1TypeOfDL.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        yealinkPhone = data.getProductByDataName("LC_HD_959");

        //  this value should exceed a 'ceiling' configured in the custom metadata types: 
        //  DiscountCeilingCBox__mdt, DiscountCeilingCategory__mdt, DiscountCeilingLicense__mdt
        yealinkPhone.discount = 90;

        yealinkPhone.quantity = 50;
        digitalLineUnlimited.quantity = 50;

        //  See the value in the Custom Setting RC_UnifiedQuotingTool_Quantity_Threshold__c.Threshold__c (by default, 20%)
        aboveThresholdPositive = 25;
        aboveThresholdNegative = -25;
        belowThreshold = 15;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-27307")
    @TmsLink("CRM-27301")
    @TmsLink("CRM-27305")
    @DisplayName("CRM-27307 - Verify approval status for quantity update +15%\n" +
            "CRM-27301 - Verify approval status for quantity update > +20%\n" +
            "CRM-27305 - Verify approval status for quantity update < -20%")
    @Description("CRM-27307 - To validate Quote approval status when updating quantity to +15% after DQ approval. \n" +
            "CRM-27301 - To validate Quote approval status when updating quantity to greater than +20% after DQ approval. \n" +
            "CRM-27305 - To validate Quote approval status when updating quantity to less than -20% after DQ approval")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity, add a new Sales quote, " +
                "select a package for it, and save changes", () -> {
            steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());
        });

        step("2. Open the Add Products tab, add a phone, " +
                "open the Price tab, set up quantities, add discount above the threshold to the phone, " +
                "save changes, and check that the Approval Status = 'Required'", () -> {
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());

            cartPage.openTab();
            steps.cartTab.setUpQuantities(digitalLineUnlimited, yealinkPhone);
            steps.cartTab.setUpDiscounts(yealinkPhone);
            cartPage.saveChanges();

            cartPage.approvalStatus.shouldHave(exactTextCaseSensitive(REQUIRED_APPROVAL_STATUS));
        });

        step("3. Open the Quote Details tab, populate Main Area Code, Discount Justification, and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.discountJustificationTextArea.setValue(TEST_STRING);
            quotePage.saveChanges();
        });

        step("4. Open the Price tab, and submit the quote for approval via 'Submit for Approval' button", () -> {
            cartPage.openTab();
            cartPage.submitForApproval();
        });

        //  shortcut to avoid approving DQ via Deal Desk user on the Deal Qualification tab
        step("5. Set the related DQ_Deal_Qualification__c.Status__c = 'Approved' via API", () -> {
            var dealQualification = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM DQ_Deal_Qualification__c " +
                            "WHERE Opportunity__c = '" + steps.quoteWizard.opportunity.getId() + "'",
                    DQ_Deal_Qualification__c.class);
            dealQualification.setStatus__c(APPROVED_STATUS);
            enterpriseConnectionUtils.update(dealQualification);
        });

        step("6. Refresh the page, open the Price tab, and check that the Approval Status = 'Approved'", () -> {
            refresh();
            packagePage.waitUntilLoaded();
            cartPage.openTab();
            cartPage.approvalStatus.shouldHave(exactTextCaseSensitive(APPROVED_APPROVAL_STATUS));
        });

        //  CRM-27301
        step("7. Update the quantity of DL Unlimited and Phone to > +20% of its original quantity, " +
                "save changes, and check that the Approval Status = 'Required'", () -> {
            editItemQuantityByPercentage(aboveThresholdPositive);

            cartPage.approvalStatus.shouldHave(exactTextCaseSensitive(REQUIRED_APPROVAL_STATUS));
        });

        //  CRM-27307
        step("8. Update the quantity of DL Unlimited and Phone to +15% of its original quantity, " +
                "save changes, and check that the Approval Status = 'Not Required'", () -> {
            editItemQuantityByPercentage(belowThreshold);

            cartPage.approvalStatus.shouldHave(exactTextCaseSensitive(NOT_REQUIRED_APPROVAL_STATUS));
        });

        //  CRM-27305
        step("9. Update the quantity of DL Unlimited and Phone to < -20% of its original quantity, " +
                "save changes, and check that the Approval Status = 'Required'", () -> {
            editItemQuantityByPercentage(aboveThresholdNegative);

            cartPage.approvalStatus.shouldHave(exactTextCaseSensitive(REQUIRED_APPROVAL_STATUS));
        });
    }

    /**
     * Update the quantity of DL Unlimited and Phone by specific percentage, and save changes.
     *
     * @param percentage percentage of the product's original quantity value, to increase or decrease the current quantity
     *                   (e.g. 20, -10, etc.)
     */
    private void editItemQuantityByPercentage(int percentage) {
        var dlQuantity = getNewQuantityByPercentage(digitalLineUnlimited.quantity, percentage);
        cartPage.setQuantityForQLItem(digitalLineUnlimited.name, dlQuantity);

        var phoneQuantity = getNewQuantityByPercentage(yealinkPhone.quantity, percentage);
        cartPage.setQuantityForQLItem(yealinkPhone.name, phoneQuantity);

        cartPage.saveChanges();
    }

    /**
     * Get a product's new quantity value based on the percentage of its original value.
     * E.g.
     * <p> for original quantity = 50, percentage 25, the new quantity is 62.</p>
     * <p> for original quantity = 50, percentage -25, the new quantity is 37.</p>
     *
     * @param originalQuantity original quantity value of the product
     * @param percentage       percentage of the original quantity value, to increase/decrease
     * @return integer value of the product's new quantity
     */
    private int getNewQuantityByPercentage(int originalQuantity, int percentage) {
        return originalQuantity + (originalQuantity * percentage / 100);
    }
}
