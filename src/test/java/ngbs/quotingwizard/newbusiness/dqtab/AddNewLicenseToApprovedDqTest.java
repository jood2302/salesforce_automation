package ngbs.quotingwizard.newbusiness.dqtab;

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
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.dealqualificationtab.DealQualificationPage.APPROVAL_STATUS_REVISION_PENDING;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.DqDealQualificationHelper.APPROVED_STATUS;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;
import static java.lang.String.valueOf;

@Tag("P1")
@Tag("DealQualificationTab")
@Tag("Multiproduct-Lite")
public class AddNewLicenseToApprovedDqTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final DealQualificationSteps dqSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Product phoneToAdd;
    private final Integer phoneNewQuantity;
    private final Integer phoneNewDiscount;

    public AddNewLicenseToApprovedDqTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_NonContract_1TypeOfDL.json",
                Dataset.class);
        steps = new Steps(data);
        dqSteps = new DealQualificationSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        phoneToAdd = data.getProductByDataName("LC_HD_959");

        //  this value should exceed a 'ceiling' configured in the custom metadata types:
        //  DiscountCeilingCBox__mdt, DiscountCeilingCategory__mdt, DiscountCeilingLicense__mdt
        phoneToAdd.discount = 81;

        phoneNewQuantity = phoneToAdd.quantity + 1;
        phoneNewDiscount = phoneToAdd.discount + 1;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-30044")
    @DisplayName("CRM-30044 - Adding new license to approved DQ")
    @Description("Verify that License Requirements are updated correctly on Deal Qualification tab " +
            "when DQ is resubmitted after new licenses are added to DQ in Approved status")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () -> {
            steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());
        });

        step("2. Add a phone on the Add Products tab, " +
                "open the Price tab, add a > 80% Discount to DigitalLine Unlimited, and save changes", () -> {
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());

            cartPage.openTab();
            cartPage.setDiscountForQLItem(dqSteps.dlUnlimited.name, dqSteps.dlUnlimited.discount);
            cartPage.saveChanges();
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
                    "SELECT Id, Status__c " +
                            "FROM DQ_Deal_Qualification__c " +
                            "WHERE Opportunity__c = '" + steps.quoteWizard.opportunity.getId() + "'",
                    DQ_Deal_Qualification__c.class);
            dealQualification.setStatus__c(APPROVED_STATUS);
            enterpriseConnectionUtils.update(dealQualification);
        });

        step("6. Refresh the Quote Wizard, open the Price tab, " +
                "add a > 80% Discount to the phone license, and save changes", () -> {
            //  to update the DQ/Quote statuses in the QW
            refresh();
            packagePage.waitUntilLoaded();

            cartPage.openTab();
            cartPage.setDiscountForQLItem(phoneToAdd.name, phoneToAdd.discount);
            cartPage.saveChanges();
        });

        step("7. Click 'Submit for Approval' button and click 'Revise' button via Revise DQ modal", () -> {
            cartPage.submitForApprovalButton.click();
            cartPage.reviseDealQualificationModal.reviseButton.click();
        });

        step("8. Check that the user is redirected to the Deal Qualification tab, " +
                "that Approval Status = 'Revision Pending', " +
                "that License Requirements item for the phone is shown, " +
                "and that corresponding DQ_Deal_Qualification_Discounts__c record is created", () -> {
            dqSteps.checkDqApprovalStatusAndNewLicenseReqItem(APPROVAL_STATUS_REVISION_PENDING, phoneToAdd);
        });

        //  shortcut to avoid approving DQ via Deal Desk user on the Deal Qualification tab
        step("9. Set the related DQ_Deal_Qualification__c.Status__c = 'Approved' via API", () -> {
            var dealQualification = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Status__c " +
                            "FROM DQ_Deal_Qualification__c " +
                            "WHERE Opportunity__c = '" + steps.quoteWizard.opportunity.getId() + "'",
                    DQ_Deal_Qualification__c.class);
            dealQualification.setStatus__c(APPROVED_STATUS);
            enterpriseConnectionUtils.update(dealQualification);
        });

        step("10. Refresh the Quote Wizard, open the Price tab, " +
                "increase the discount and quantity for the phone license, and save changes", () -> {
            //  to update the DQ/Quote statuses in the QW
            refresh();
            packagePage.waitUntilLoaded();

            cartPage.openTab();
            cartPage.setQuantityForQLItem(phoneToAdd.name, phoneNewQuantity);
            cartPage.setDiscountForQLItem(phoneToAdd.name, phoneNewDiscount);
            cartPage.saveChanges();
        });

        step("11. Click 'Submit for Approval' button and click 'Revise' button via Revise DQ modal", () -> {
            cartPage.submitForApprovalButton.click();
            cartPage.reviseDealQualificationModal.reviseButton.click();
        });

        step("12. Check that the user is redirected to the Deal Qualification tab, " +
                "that Approval Status = 'Revision Pending', " +
                "and that License Requirements item for the phone is shown with the updated quantity and discount", () -> {
            dqSteps.checkDqApprovalStatusAndUpdatedLicenseReqItem(APPROVAL_STATUS_REVISION_PENDING,
                    phoneToAdd.productName, valueOf(phoneNewQuantity), valueOf(phoneNewDiscount));
        });

        step("13. Check the updated Quantity__c and Discount__c values on DQ_Deal_Qualification_Discounts__c record for the phone", () -> {
            dqSteps.checkUpdatedDqDiscountsInDB(phoneToAdd, valueOf(phoneNewQuantity), valueOf(phoneNewDiscount));
        });
    }
}
