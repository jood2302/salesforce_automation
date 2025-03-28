package ngbs.quotingwizard.newbusiness.signup;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.MVP_SERVICE;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.getBillingInfoSummaryLicenses;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.searchAccountsByContactLastNameInNGBS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Selenide.closeWindow;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

@Tag("P0")
@Tag("NGBS")
@Tag("SignUp")
@Tag("LBO")
public class SalesLboQuoteSignUpTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    private User salesRepUser;
    private Account account;
    private Contact contact;

    //  Test data
    private final Product dlUnlimited;
    private final Product globalMvpDL;
    private final Product globalMvpEMEA;
    private final Product ciscoPhone;
    private final Product polycomRentalPhone;
    private final List<String> excludedLicenseNames;
    private final ShippingGroupAddress shippingGroupAddress;
    private final String shippingAddressFormatted;

    public SalesLboQuoteSignUpTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_PhonesAndDLsAndGlobalMVP.json",
                Dataset.class);
        steps = new Steps(data);

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        globalMvpDL = data.getProductByDataName("LC_DLI_282");
        globalMvpEMEA = data.getProductByDataName("LC_IBO_284");
        ciscoPhone = data.getProductByDataName("LC_HD_523");
        polycomRentalPhone = data.getProductByDataName("LC_HDR_619");

        //  There should be no "DigitalLine", "Main Local Number", "Main Local Fax Number" licenses in NGBS.
        //  "Global MVP DigitalLine" and related licenses (e.g. "Global MVP - EMEA") are added manually, so they should not be in NGBS either.
        var mainLocalNumber = data.getProductByDataName("LC_MLN_31");
        var mainLocalFaxNumber = data.getProductByDataName("LC_MLFN_45");
        var digitalLine = data.getProductByDataName("LC_DL_75");
        excludedLicenseNames = List.of(mainLocalNumber.name, mainLocalFaxNumber.name, digitalLine.name,
                globalMvpDL.name, globalMvpEMEA.name);

        shippingGroupAddress = new ShippingGroupAddress(
                "United States", "Foster City",
                new ShippingGroupAddress.State("California", true),
                "App.129 13 Elm Street", "94404", "QA Automation");
        shippingAddressFormatted = shippingGroupAddress.getAddressFormatted();
    }

    @BeforeEach
    public void setUpTest() {
        salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        account = steps.salesFlow.account;
        contact = steps.salesFlow.contact;

        steps.quoteWizard.createOpportunity(account, contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-21869")
    @DisplayName("CRM-21869 - LBO Changes to Signup funnel")
    @Description("Verify that Signup with LBO transmitted correct data in correct format")
    public void test() {
        step("1. Open the New Business Opportunity, switch to the Quote Wizard, add a new Sales Quote, " +
                "select a package for it, " +
                "add some products, set up their discounts and quantities, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
            steps.quoteWizard.selectDefaultPackageFromTestData();

            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());

            cartPage.openTab();
            steps.cartTab.setUpQuantities(data.getNewProductsToAdd());
            steps.cartTab.setUpQuantities(dlUnlimited);
            steps.cartTab.setUpDiscounts(data.getNewProductsToAdd());
            steps.cartTab.setUpDiscounts(dlUnlimited);
            cartPage.saveChanges();
        });

        step("2. Open the Quote Details tab, switch off the Provision toggle, set Main Area Code, " +
                "populate payment method, set default Start Date, and save changes", () -> {
            quotePage.openTab();
            quotePage.provisionToggle.click();
            steps.lbo.checkProvisionToggleOn(false);
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.selectPaymentMethod(QuotePage.INVOICE_PAYMENT_METHOD);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();
        });

        step("3. Open the Shipping tab, create a new Shipping Group, " +
                "assign " + ciscoPhone.name + " and " + polycomRentalPhone.name + " to it, and save changes", () -> {
            shippingPage.openTab();
            shippingPage.addNewShippingGroup(shippingGroupAddress);
            shippingPage.assignDeviceToShippingGroup(ciscoPhone.productName, shippingAddressFormatted,
                    shippingGroupAddress.shipAttentionTo);
            shippingPage.assignDeviceToShippingGroup(polycomRentalPhone.productName, shippingAddressFormatted,
                    shippingGroupAddress.shipAttentionTo);
            shippingPage.saveChanges();
        });

        step("4. Update Quote to Approved Active Agreement and create Approved Invoice Approval for Account via API", () -> {
            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity);

            createInvoiceApprovalApproved(steps.quoteWizard.opportunity, account, contact, salesRepUser.getId(), false);
            closeWindow();
        });

        step("5. Press 'Process Order' button on the Opportunity record page " +
                "verify that 'Preparing Data' step is completed, " +
                "select the timezone, click 'Sign Up MVP', and check that the account is processed for signing up", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();
            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(hidden);

            opportunityPage.processOrderModal.selectDefaultTimezone();
            opportunityPage.processOrderModal.signUpButton.click();
            opportunityPage.processOrderModal.signUpMvpStatus
                    .shouldHave(exactTextCaseSensitive(format(YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE, MVP_SERVICE)), ofSeconds(60));
        });

        step("6. Check that all data was transmitted to NGBS correctly", () -> {
            var accountNgbsDTO = step("Check that the account is created in NGBS via NGBS API", () -> {
                return assertWithTimeout(() -> {
                    var accounts = searchAccountsByContactLastNameInNGBS(contact.getLastName());
                    assertEquals(1, accounts.size(),
                            "Number of NGBS accounts found by the related Contact's Last Name");
                    return accounts.get(0);
                }, ofSeconds(60));
            });

            assertWithTimeout(() -> {
                var billingID = accountNgbsDTO.id;
                var packageID = accountNgbsDTO.packages[0].id;

                var billingInfoLicenses = getBillingInfoSummaryLicenses(billingID, packageID);

                var allAddedLicenses = new ArrayList<Product>();
                allAddedLicenses.addAll(asList(data.getNewProductsToAdd()));
                allAddedLicenses.addAll(asList(data.getProductsDefault()));
                var expectedProductLicenses = allAddedLicenses.stream()
                        .filter(product -> !excludedLicenseNames.contains(product.name))
                        .toList();

                for (var product : expectedProductLicenses) {
                    step("Check product data for '" + product.name + "'");
                    var licenseActual = Arrays.stream(billingInfoLicenses)
                            .filter(license -> license.catalogId.equals(product.dataName))
                            .findFirst();

                    assertTrue(licenseActual.isPresent(),
                            format("The license from NGBS for the product '%s' (should exist)", product.name));
                    assertEquals(product.quantity, licenseActual.get().qty,
                            format("The 'quantity' value on the license from NGBS for the product '%s'", product.name));
                }

                step("Check that there are no Global Office licenses on the account in NGBS");
                for (var licenseActual : billingInfoLicenses) {
                    assertNotEquals(globalMvpDL.dataName, licenseActual.catalogId,
                            "'Global MVP DigitalLine' license catalogId");
                    assertNotEquals(globalMvpEMEA.dataName, licenseActual.catalogId,
                            "'Global MVP - EMEA' license catalogId");
                }
            }, ofSeconds(30));
        });
    }
}
