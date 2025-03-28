package ngbs.quotingwizard.newbusiness.quotetab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.dto.partner.PartnerNgbsDTO;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import ngbs.quotingwizard.PartnerPaymentMethodSteps;
import org.junit.jupiter.api.*;

import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.INVOICE_WHOLESALE_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.createPartnerInNGBS;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.createPartnerPackageInNGBS;
import static com.aquiva.autotests.rc.utilities.ngbs.PartnerNgbsFactory.createWholesalePartner;
import static com.aquiva.autotests.rc.utilities.ngbs.PartnerPackageNgbsFactory.createPartnerPackage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.WHOLESALE_RESELLER_PARTNER_TYPE;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("WholesalePayment")
public class InvoiceWholesalePaymentMethodTest extends BaseTest {
    private final Steps steps;
    private final PartnerPaymentMethodSteps partnerPaymentMethodSteps;

    private PartnerNgbsDTO ngbsPartner;

    //  Test data
    private final Package mvpPackage;

    public InvoiceWholesalePaymentMethodTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_NonContract_1TypeOfDL.json",
                Dataset.class);

        steps = new Steps(data);
        partnerPaymentMethodSteps = new PartnerPaymentMethodSteps(data);

        mvpPackage = data.packageFolders[0].packages[0];
    }

    @BeforeEach
    public void setUpTest() {
        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);

        step("Create a Wholesale partner with a package in NGBS", () -> {
            var partnerDTO = createWholesalePartner();
            ngbsPartner = createPartnerInNGBS(partnerDTO);

            var partnerPackageDTO = createPartnerPackage(mvpPackage);
            createPartnerPackageInNGBS(ngbsPartner.id, partnerPackageDTO);
        });

        partnerPaymentMethodSteps.setUpPartnerAccountForCustomerAccountTestSteps((double) ngbsPartner.id,
                WHOLESALE_RESELLER_PARTNER_TYPE, steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-24609")
    @DisplayName("CRM-24609 - Invoice Wholesale Payment Method Support")
    @Description("Verify that 'Invoice Wholesale' payment method is available for Wholesale Customers " +
            "and all other payment methods are disabled for all Wholesale deals")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Quote Details tab, " +
                "and verify that only the 'Invoice Wholesale' option is available in the 'Payment Method' picklist", () -> {
            quotePage.openTab();
            quotePage.paymentMethodPicklist.getOptions().shouldHave(size(1));
            quotePage.paymentMethodPicklist.getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(INVOICE_WHOLESALE_PAYMENT_METHOD));
        });
    }
}
