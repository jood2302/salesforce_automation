package ngbs.quotingwizard.newbusiness.dqtab;

import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.DQ_Deal_Qualification_Discounts__c;
import com.sforce.ws.ConnectionException;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.dealqualificationtab.DealQualificationPage.APPROVAL_STATUS_REVISION_PENDING;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToIntToString;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToInteger;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test methods related to the tests that check License Requirements section content
 * on the Deal Qualification tab.
 */
public class DealQualificationSteps {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    public final Product dlUnlimited;
    public final String dlUnlimitedNewQuantity;
    public final String dlUnlimitedNewDiscount;

    /**
     * New instance for the class with the test methods related
     * to the tests that check License Requirements section content on the Deal Qualification tab.
     *
     * @param data object parsed from the JSON files with the test data
     */
    public DealQualificationSteps(Dataset data) {
        this.enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");

        //  this value should exceed a 'ceiling' configured in the custom metadata types:
        //  DiscountCeilingCBox__mdt, DiscountCeilingCategory__mdt, DiscountCeilingLicense__mdt
        dlUnlimited.discount = 81;

        dlUnlimitedNewDiscount = "90";
        dlUnlimitedNewQuantity = "27";
    }

    /**
     * Check the 'License Requirements' item for the selected discounted product on the Deal Qualification tab,
     * and the values on the corresponding DQ_Deal_Qualification_Discounts__c record in DB.
     *
     * @param product     expected test data for the product with a discount
     * @param packageName name of the package with the product with a discount
     */
    protected void checkLicenseRequirementsItem(Product product, String packageName) {
        var productName = product.productName != null ? product.productName : product.name;

        step("Check 'License Requirements' item on the Deal Qualification tab for the " + productName, () -> {
            var licenseRequirementsItem = dealQualificationPage.getLicenseRequirementsItem(productName);
            licenseRequirementsItem.getService().shouldHave(exactTextCaseSensitive(product.serviceName));
            licenseRequirementsItem.getPackage().shouldHave(exactTextCaseSensitive(packageName));
            licenseRequirementsItem.getGroup().shouldHave(exactTextCaseSensitive(product.group));
            licenseRequirementsItem.getSubGroup().shouldHave(exactTextCaseSensitive(product.subgroup));
            licenseRequirementsItem.getQuantityInput().shouldHave(exactValue(valueOf(product.quantity)));
            licenseRequirementsItem.getCeilingDiscountInput().shouldHave(exactValue(valueOf(product.discount)));
        });

        step("Check the corresponding DQ_Deal_Qualification_Discounts__c record in DB for the " + productName, () -> {
            var dealQualificationDiscount = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Service__c, Group__c, Subgroup__c, " +
                            "Product__r.Name, Quantity__c, Discount__c " +
                            "FROM DQ_Deal_Qualification_Discounts__c " +
                            "WHERE Deal_Qualification__r.Quote__c = '" + wizardPage.getSelectedQuoteId() + "' " +
                            "AND Product__r.ExtID__c = '" + product.dataName + "'",
                    DQ_Deal_Qualification_Discounts__c.class);
            assertThat(dealQualificationDiscount.getService__c())
                    .as("DQ_Deal_Qualification_Discounts__c.Service__c value of ", productName)
                    .isEqualTo(product.serviceName);
            assertThat(dealQualificationDiscount.getGroup__c())
                    .as("DQ_Deal_Qualification_Discounts__c.Group__c value of ", productName)
                    .contains(product.group); //    Group__c might contain ordering prefix (e.g. "[123] Services")
            assertThat(dealQualificationDiscount.getSubgroup__c())
                    .as("DQ_Deal_Qualification_Discounts__c.Subgroup__c value of ", productName)
                    .contains(product.subgroup); //    Subgroup__c might contain ordering prefix (e.g. "[456] Other")
            assertThat(dealQualificationDiscount.getProduct__r().getName())
                    .as("DQ_Deal_Qualification_Discounts__c.Product__r.Name value of ", productName)
                    .isEqualTo(productName);
            assertThat(doubleToInteger(dealQualificationDiscount.getQuantity__c()))
                    .as("DQ_Deal_Qualification_Discounts__c.Quantity__c value of ", productName)
                    .isEqualTo(product.quantity);
            assertThat(doubleToInteger(dealQualificationDiscount.getDiscount__c()))
                    .as("DQ_Deal_Qualification_Discounts__c.Discount__c value of ", productName)
                    .isEqualTo(product.discount);
        });
    }

    /**
     * Check that 'Quantity' and 'Ceiling Discount' fields are editable,
     * and 'Approval Status' = 'Revision Pending'
     * after clicking 'Submit for Approval' -> 'Revise' on the Revise DQ modal on the Price tab.
     */
    protected void checkDqApprovalStatusAndLicenseRequirementsFieldsAfterRevise() {
        step("Open the Price tab, click 'Submit for Approval' button, and click 'Revise' button via Revise DQ modal", () -> {
            cartPage.openTab();
            cartPage.submitForApprovalButton.click();
            cartPage.reviseDealQualificationModal.reviseButton.click();
        });

        step("Check that the user is redirected to the Deal Qualification tab, " +
                "check 'Approval Status' value, and check that 'Quantity' and 'Ceiling Discount' fields are editable", () -> {
            dealQualificationPage.waitUntilLoaded();
            dealQualificationPage.dqApprovalStatus.shouldHave(exactTextCaseSensitive(APPROVAL_STATUS_REVISION_PENDING));

            dealQualificationPage.licenseRequirementsQuantities.asDynamicIterable().forEach(quantity -> quantity.shouldBe(editable));
            dealQualificationPage.licenseRequirementsDiscounts.asDynamicIterable().forEach(discount -> discount.shouldBe(editable));
        });
    }

    /**
     * Check the updated values on the DQ_Deal_Qualification_Discounts__c record for the selected product.
     *
     * @param product     expected test data for the updated product
     * @param newQuantity new quantity for License Requirements item
     * @param newDiscount new discount value for License Requirements item
     */
    protected void checkUpdatedDqDiscountsInDB(Product product, String newQuantity, String newDiscount) {
        step("Check the updated Quantity__c and Discount__c values on DQ_Deal_Qualification_Discounts__c record " +
                "for " + product.name, () -> {
            var dealQualificationItem = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Quantity__c, Discount__c " +
                            "FROM DQ_Deal_Qualification_Discounts__c " +
                            "WHERE Deal_Qualification__r.Quote__c = '" + wizardPage.getSelectedQuoteId() + "' " +
                            "AND Product__r.ExtID__c = '" + product.dataName + "'",
                    DQ_Deal_Qualification_Discounts__c.class);
            assertThat(doubleToIntToString(dealQualificationItem.getQuantity__c()))
                    .as("DQ_Deal_Qualification_Discounts__c.Quantity__c value of ", product.name)
                    .isEqualTo(newQuantity);
            assertThat(doubleToIntToString(dealQualificationItem.getDiscount__c()))
                    .as("DQ_Deal_Qualification_Discounts__c.Discount__c value of ", product.name)
                    .isEqualTo(newDiscount);
        });
    }

    /**
     * Check 'Approval Status' and the visibility of a new License Requirements item on the Deal Qualification tab.
     * Also check that the single corresponding DQ_Deal_Qualification_Discounts__c record exists in DB.
     *
     * @param dqApprovalStatus expected approval status (e.g. 'Approval Status: Revision Pending')
     * @param product          test data with the product that the License Requirements item should be checked against
     * @throws ConnectionException in case of errors while accessing API
     */
    protected void checkDqApprovalStatusAndNewLicenseReqItem(String dqApprovalStatus, Product product) throws ConnectionException {
        dealQualificationPage.waitUntilLoaded();
        dealQualificationPage.dqApprovalStatus.shouldHave(exactTextCaseSensitive(dqApprovalStatus));

        dealQualificationPage.getLicenseRequirementsItem(product.productName).getSelf().shouldBe(visible);

        var productDqDiscounts = enterpriseConnectionUtils.query(
                "SELECT Id " +
                        "FROM DQ_Deal_Qualification_Discounts__c " +
                        "WHERE Deal_Qualification__r.Quote__c = '" + wizardPage.getSelectedQuoteId() + "' " +
                        "AND Product__r.ExtID__c = '" + product.dataName + "'",
                DQ_Deal_Qualification_Discounts__c.class);
        assertThat(productDqDiscounts.size())
                .as("Number of DQ_Deal_Qualification_Discounts__c records for " + product.name)
                .isEqualTo(1);
    }

    /**
     * Check 'Approval Status' and the updated 'Quantity' and 'Ceiling Discount' fields
     * on the existing License Requirements item on the Deal Qualification tab.
     *
     * @param dqApprovalStatus   expected approval status (e.g. 'Approval Status: Revision Pending')
     * @param productName        product name to identify the corresponding License Requirements item to check
     * @param productNewQuantity new quantity on the corresponding License Requirements item to check
     * @param productNewDiscount new discount on the corresponding License Requirements item to check
     */
    protected void checkDqApprovalStatusAndUpdatedLicenseReqItem(String dqApprovalStatus, String productName,
                                                                 String productNewQuantity, String productNewDiscount) {
        dealQualificationPage.waitUntilLoaded();
        dealQualificationPage.dqApprovalStatus.shouldHave(exactTextCaseSensitive(dqApprovalStatus));

        var updatedLicenseReqItem = dealQualificationPage.getLicenseRequirementsItem(productName);
        updatedLicenseReqItem.getQuantityInput().shouldHave(exactValue(productNewQuantity));
        updatedLicenseReqItem.getCeilingDiscountInput().shouldHave(exactValue(productNewDiscount));
    }
}
