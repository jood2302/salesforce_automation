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
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.dealqualificationtab.DealQualificationPage.APPROVAL_STATUS_PENDING_APPROVAL;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.dealqualificationtab.DealQualificationPage.APPROVAL_STATUS_REVISION_PENDING;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.lang.String.valueOf;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("DealQualificationTab")
@Tag("Multiproduct-Lite")
public class AddNewLicenseToRevisionPendingDqTest extends BaseTest {
    private final Steps steps;
    private final DealQualificationSteps dqSteps;

    //  Test data
    private final Product firstPhoneToAdd;
    private final Product secondPhoneToAdd;
    private final Integer firstPhoneNewQuantity;
    private final Integer firstPhoneNewDiscount;

    public AddNewLicenseToRevisionPendingDqTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_1PhoneAnd1DL_RegularAndPOC.json",
                Dataset.class);
        steps = new Steps(data);
        dqSteps = new DealQualificationSteps(data);

        firstPhoneToAdd = data.getProductByDataName("LC_HDR_619");
        secondPhoneToAdd = data.getProductByDataName("LC_HD_959");

        //  this value should exceed a 'ceiling' configured in the custom metadata types:
        //  DiscountCeilingCBox__mdt, DiscountCeilingCategory__mdt, DiscountCeilingLicense__mdt
        firstPhoneToAdd.discount = 81;
        secondPhoneToAdd.discount = 85;

        firstPhoneNewQuantity = firstPhoneToAdd.quantity + 1;
        firstPhoneNewDiscount = firstPhoneToAdd.discount + 1;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-30047")
    @DisplayName("CRM-30047 - Adding new license to revision pending DQ")
    @Description("Verify that License Requirements are updated correctly on Deal Qualification tab " +
            "when DQ is resubmitted after new licenses are added to DQ in Revision Pending status")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () -> {
            steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());
        });

        step("2. Add phones on the Add Products tab, " +
                "open the Price tab, add a > 80% Discount to DigitalLine Unlimited, and save changes", () -> {
            steps.quoteWizard.addProductsOnProductsTab(firstPhoneToAdd, secondPhoneToAdd);

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

        //  to set DQ_Deal_Qualification__c.Status__c = 'Revision Pending'
        step("5. Click 'Submit for Approval' button and click 'Revise' button via Revise DQ modal", () -> {
            cartPage.submitForApprovalButton.click();
            cartPage.reviseDealQualificationModal.reviseButton.click();
            //  the user is redirected automatically to the DQ tab => need to wait for it to be loaded before switching
            dealQualificationPage.waitUntilLoaded();
        });

        step("6. Open the Price tab, add a > 80% Discount to the 1st phone license, and save changes", () -> {
            cartPage.openTab();
            cartPage.setDiscountForQLItem(firstPhoneToAdd.name, firstPhoneToAdd.discount);
            cartPage.saveChanges();
        });

        step("7. Click 'Submit for Approval' button and click 'Review' button via Revise DQ modal", () -> {
            cartPage.submitForApprovalButton.click();
            cartPage.reviseDealQualificationModal.reviewButton.click();
        });

        step("8. Check that the user is redirected to the Deal Qualification tab, " +
                "that Approval Status = 'Revision Pending', " +
                "that License Requirements item for the 1st phone is shown, " +
                "and that corresponding DQ_Deal_Qualification_Discounts__c record is created", () -> {
            dqSteps.checkDqApprovalStatusAndNewLicenseReqItem(APPROVAL_STATUS_REVISION_PENDING, firstPhoneToAdd);
        });

        step("9. Open the Price tab, increase the discount and quantity for the 1st phone license, and save changes", () -> {
            cartPage.openTab();
            cartPage.setQuantityForQLItem(firstPhoneToAdd.name, firstPhoneNewQuantity);
            cartPage.setDiscountForQLItem(firstPhoneToAdd.name, firstPhoneNewDiscount);
            cartPage.saveChanges();
        });

        step("10. Click 'Submit for Approval' button and click 'Review' button via Revise DQ modal", () -> {
            cartPage.submitForApprovalButton.click();
            cartPage.reviseDealQualificationModal.reviewButton.click();
        });

        step("11. Check that the user is redirected to the Deal Qualification tab, " +
                "that Approval Status = 'Revision Pending', " +
                "and that License Requirements item for the 1st phone is shown with the updated quantity and discount", () -> {
            dqSteps.checkDqApprovalStatusAndUpdatedLicenseReqItem(APPROVAL_STATUS_REVISION_PENDING,
                    firstPhoneToAdd.productName, valueOf(firstPhoneNewQuantity), valueOf(firstPhoneNewDiscount));
        });

        step("12. Check the updated Quantity__c and Discount__c values on DQ_Deal_Qualification_Discounts__c record for the phone", () -> {
            dqSteps.checkUpdatedDqDiscountsInDB(firstPhoneToAdd, valueOf(firstPhoneNewQuantity), valueOf(firstPhoneNewDiscount));
        });

        step("13. Open the Price tab, add a > 80% Discount to the 2nd phone license, and save changes", () -> {
            cartPage.openTab();
            cartPage.setDiscountForQLItem(secondPhoneToAdd.name, secondPhoneToAdd.discount);
            cartPage.saveChanges();
        });

        step("14. Click 'Submit for Approval' button and click 'Submit Immediately' button via Revise DQ modal", () -> {
            cartPage.submitForApprovalButton.click();
            cartPage.reviseDealQualificationModal.submitImmediatelyButton.click();
            cartPage.progressBar.shouldBe(visible, ofSeconds(10));
            cartPage.waitUntilLoaded();
        });

        step("15. Open the Deal Qualification tab, and check that Approval Status = 'Pending Approval', " +
                "that License Requirements item for the 2nd phone is shown, " +
                "and that corresponding DQ_Deal_Qualification_Discounts__c record is created", () -> {
            dealQualificationPage.openTab();
            dqSteps.checkDqApprovalStatusAndNewLicenseReqItem(APPROVAL_STATUS_PENDING_APPROVAL, secondPhoneToAdd);
        });
    }
}
