package ngbs.quotingwizard.newbusiness.signup;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.getBillingInfoSummaryLicenses;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.searchAccountsByContactLastNameInNGBS;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("P1")
@Tag("SignUp")
@Tag("Phoenix")
public class RcMeetingsSingleProductSignUpTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    //  Test data
    private final Product videoProPlus;
    private final Product rcVideoProPlusSubscription;
    private final Product rcWebinar3000;
    private final Product rcWebinar5000;
    private final Product rcRooms;

    public RcMeetingsSingleProductSignUpTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_Meetings_Phoenix_Annual_Contract_with_Additional_Products.json",
                Dataset.class);
        steps = new Steps(data);

        videoProPlus = data.getProductByDataName("LC_SM_405");
        rcVideoProPlusSubscription = data.getProductByDataName("LC_SC_404");
        rcWebinar3000 = data.getProductByDataName("LC_WB_413");
        rcWebinar5000 = data.getProductByDataName("LC_WB_414");
        rcRooms = data.getProductByDataName("LC_RCRM_416");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-36168")
    @DisplayName("CRM-36168 - RC Meetings Single Product Sign Up")
    @Description("Verify that RingCentral US Meetings Account can be signed up with enabled Enable UI-Less Process Order Meetings")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, select a package for it, " +
                "open the Add Products tab and add some Products to the Cart", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectDefaultPackageFromTestData();
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());
        });

        step("2. Open the Price tab, set up quantities and save changes", () -> {
            cartPage.openTab();
            steps.cartTab.setUpQuantities(videoProPlus);
            steps.cartTab.setUpQuantities(data.getNewProductsToAdd());
            cartPage.saveChanges();
        });

        step("3. Open the Quote Details tab, set Payment Method = 'Credit Card', set Start Date field and save changes", () -> {
            quotePage.openTab();
            quotePage.selectPaymentMethod(QuotePage.CREDIT_CARD_PAYMENT_METHOD);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();
        });

        step("4. Update Quote to Active Agreement via API", () -> {
            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity);
        });

        step("5. Open the Opportunity record page, open Process Order modal, select value in TimeZone picklist, " +
                "click 'Sign Up' button and check that the account is processed for signing up", () -> {
            opportunityPage.openPage(steps.quoteWizard.opportunity.getId());
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.meetingsPreparingDataActiveStep.shouldHave(exactText(READY_TO_REQUEST_FUNNEL_STEP), ofSeconds(120));
            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(hidden);

            opportunityPage.processOrderModal.selectDefaultTimezone();
            opportunityPage.processOrderModal.signUpButton.click();

            opportunityPage.processOrderModal.signUpMeetingsStatus
                    .shouldHave(exactTextCaseSensitive(format(YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE, MEETINGS_SERVICE)), ofSeconds(60));
        });

        step("6. Wait until Account is signed up and verify it in Billing", () -> {
            var accountNgbsDTO = step("Check that the account is created in NGBS via NGBS API", () -> {
                return assertWithTimeout(() -> {
                    var accounts = searchAccountsByContactLastNameInNGBS(steps.salesFlow.contact.getLastName());
                    assertEquals(1, accounts.size(),
                            "Number of NGBS accounts found by the related Contact's Last Name");
                    return accounts.get(0);
                }, ofSeconds(180));
            });

            var includedProductLicenses = List.of(videoProPlus, rcVideoProPlusSubscription, rcWebinar3000, rcWebinar5000, rcRooms);

            step("Check that the account has the included product licenses in NGBS", () -> {
                var billingID = accountNgbsDTO.id;
                var packageID = accountNgbsDTO.packages[0].id;

                assertWithTimeout(() -> {
                    var billingInfoLicenses = getBillingInfoSummaryLicenses(billingID, packageID);

                    for (var product : includedProductLicenses) {
                        step("Check product data for '" + product.name + "'");
                        var licenseActual = Arrays.stream(billingInfoLicenses)
                                .filter(license -> license.catalogId.equals(product.dataName))
                                .findFirst();

                        assertTrue(licenseActual.isPresent(),
                                format("The license from NGBS for the product '%s' (should exist)", product.name));
                        assertEquals(product.quantity, licenseActual.get().qty,
                                format("The 'quantity' value on the license from NGBS for the product '%s'", product.name));
                    }
                }, ofSeconds(60));
            });
        });
    }
}