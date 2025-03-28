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
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static java.lang.String.valueOf;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("DealQualificationTab")
@Tag("Multiproduct-Lite")
public class AddNewLicenseToPendingApprovalDqTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final DealQualificationSteps dqSteps;

    //  Test data
    private final Product phoneToAdd;
    private final Integer phoneNewQuantity;
    private final Integer phoneNewDiscount;

    public AddNewLicenseToPendingApprovalDqTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_NonContract_1TypeOfDL.json",
                Dataset.class);
        steps = new Steps(data);
        dqSteps = new DealQualificationSteps(data);

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
    @TmsLink("CRM-30043")
    @DisplayName("CRM-30043 - Adding new license to pending approval DQ")
    @Description("Verify that License Requirements section is displayed correctly when DQ is revised with the new license. \n" +
            "Verify that no duplicate rows are created when the same license is submitted for approval multiple times")
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

        step("5. Add a > 80% Discount to the phone license, and save changes", () -> {
            cartPage.setDiscountForQLItem(phoneToAdd.name, phoneToAdd.discount);
            cartPage.saveChanges();
        });

        step("6. Click 'Submit for Approval' button and click 'Revise' button via Revise DQ modal", () -> {
            cartPage.submitForApprovalButton.click();
            cartPage.reviseDealQualificationModal.reviseButton.click();
        });

        step("7. Check that the user is redirected to the Deal Qualification tab, " +
                "that Approval Status = 'Revision Pending', " +
                "that License Requirements item for the phone is shown, " +
                "and that corresponding DQ_Deal_Qualification_Discounts__c record is created", () -> {
            dqSteps.checkDqApprovalStatusAndNewLicenseReqItem(APPROVAL_STATUS_REVISION_PENDING, phoneToAdd);
        });

        step("8. Click 'Submit For Approval' button, " +
                "and check that Approval Status has changed to 'Pending Approval'", () -> {
            dealQualificationPage.submitForApproval();

            dealQualificationPage.dqApprovalStatus.shouldHave(exactTextCaseSensitive(APPROVAL_STATUS_PENDING_APPROVAL), ofSeconds(10));
        });

        step("9. Open the Price tab, increase the discount and quantity for the phone license, and save changes", () -> {
            cartPage.openTab();
            cartPage.setQuantityForQLItem(phoneToAdd.name, phoneNewQuantity);
            cartPage.setDiscountForQLItem(phoneToAdd.name, phoneNewDiscount);
            cartPage.saveChanges();
        });

        step("10. Click 'Submit for Approval' button and click 'Revise' button via Revise DQ modal", () -> {
            cartPage.submitForApprovalButton.click();
            cartPage.reviseDealQualificationModal.reviseButton.click();
        });

        step("11. Check that the user is redirected to the Deal Qualification tab, " +
                "that Approval Status = 'Revision Pending', " +
                "and that License Requirements item for the phone is shown with the updated quantity and discount", () -> {
            dqSteps.checkDqApprovalStatusAndUpdatedLicenseReqItem(APPROVAL_STATUS_REVISION_PENDING,
                    phoneToAdd.productName, valueOf(phoneNewQuantity), valueOf(phoneNewDiscount));
        });

        step("12. Check the updated Quantity__c and Discount__c values on DQ_Deal_Qualification_Discounts__c record for the phone", () -> {
            dqSteps.checkUpdatedDqDiscountsInDB(phoneToAdd, valueOf(phoneNewQuantity), valueOf(phoneNewDiscount));
        });
    }
}
