package ngbs.quotingwizard.newbusiness.signup;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Contact;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.MVP_SERVICE;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.DocuSignStatusFactory.createDocuSignStatus;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.DocuSignStatusHelper.COMPLETED_ENVELOPE_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.POC_QUOTE_RECORD_TYPE;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.closeWindow;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Tag("P0")
@Tag("P1")
@Tag("PDV")
@Tag("NGBS")
@Tag("SignUp")
@Tag("POC")
public class PocSignUpTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final PocSignUpSteps pocSignUpSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Contact contact;
    private Quote pocQuote;

    //  Test data
    private final Product ciscoPhone;
    private final Product polycomPhone;
    private final Product commonPhone;
    private final Product digitalLineUnlimited;
    private final String pocInitialTerm;
    private final int numberOfDevicesToAssignToShippingGroup;

    public PocSignUpTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_Annual_RegularAndPOC.json",
                Dataset.class);

        steps = new Steps(data);
        pocSignUpSteps = new PocSignUpSteps();
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        ciscoPhone = data.getProductByDataName("LC_HD_523");
        polycomPhone = data.getProductByDataName("LC_HD_687");
        commonPhone = data.getProductByDataName("LC_DL-HDSK_177");
        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        pocInitialTerm = data.getInitialTerm();

        numberOfDevicesToAssignToShippingGroup = 2;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        contact = steps.salesFlow.contact;
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, contact, salesRepUser);

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-12157")
    @TmsLink("CRM-22995")
    @DisplayName("CRM-12157 - Sign Up POC with two different Types of DL. \n" +
            "CRM-22995 - Validation for Sign Up with a None payment method. POC")
    @Description("CRM-12157 - Verify that POC Sign Up sends User to the funnel with data from Salesforce. \n" +
            "CRM-22995 - Verify that validation isn't triggered if the payment method sent as None for POC Quote")
    public void test() {
        step("1. Open the test Opportunity, switch to the Quote Wizard, and create a new POC Quote", () ->
                steps.quoteWizard.preparePocQuote(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Add Products tab and add some products", () ->
                steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd())
        );

        step("3. Open the Price Tab, set up products' quantities, assign devices to DLs", () -> {
            cartPage.openTab();

            steps.cartTab.setUpQuantities(data.getNewProductsToAdd());
            steps.cartTab.setUpQuantities(digitalLineUnlimited);

            steps.cartTab.assignDevicesToDL(ciscoPhone.name, digitalLineUnlimited.name, steps.quoteWizard.localAreaCode,
                    ciscoPhone.quantity);
            steps.cartTab.assignDevicesToDL(polycomPhone.name, commonPhone.name, steps.quoteWizard.localAreaCode,
                    polycomPhone.quantity);
        });

        step("4. Open the Shipping tab and assign all the devices to the default Shipping group", () -> {
            shippingPage.openTab();

            var defaultShippingGroup = shippingPage.getFirstShippingGroup();
            var polycomPhoneShippingDevice = shippingPage.getShippingDevice(polycomPhone.productName);
            var ciscoPhoneShippingDevice = shippingPage.getShippingDevice(ciscoPhone.productName);
            shippingPage.assignDeviceToShippingGroup(polycomPhoneShippingDevice, defaultShippingGroup);
            shippingPage.assignDeviceToShippingGroup(ciscoPhoneShippingDevice, defaultShippingGroup);
            defaultShippingGroup.setQuantityForAssignedDevice(polycomPhone.name, numberOfDevicesToAssignToShippingGroup);
            defaultShippingGroup.setQuantityForAssignedDevice(ciscoPhone.name, numberOfDevicesToAssignToShippingGroup);
        });

        step("5. Open the Quote Details tab, set Start Date, Initial Term, Main Area Code, and save changes", () -> {
            quotePage.openTab();
            quotePage.setDefaultStartDate();
            quotePage.initialTermPicklist.selectOption(pocInitialTerm);
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.saveChanges();
        });

        //  CRM-22995
        step("6. Make sure that Quote.PaymentMethod__c is null " +
                "and Payment Method picklist is hidden on the Quote Details tab", () -> {
            pocQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, IsQuoteHasErrors__c, PaymentMethod__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND RecordType.Name = '" + POC_QUOTE_RECORD_TYPE + "'",
                    Quote.class);

            assertThat(pocQuote.getPaymentMethod__c())
                    .as("POC Quote.PaymentMethod__c value")
                    .isNull();

            quotePage.paymentMethodPicklist.shouldNot(exist);
        });

        step("7. Create POC Approval via Quote Wizard", () -> {
            quotePage.createPocApproval(pocSignUpSteps.linkToSignedEvaluationAgreement);
            closeWindow();
        });

        step("8. Change POC Approval status to 'Approved' via SFDC API", () ->
                pocSignUpSteps.stepSetPocApprovalStatusToApproved(pocQuote.getId())
        );

        step("9. Create a new DocuSign Status object for the POC Quote with Envelope Status = 'Completed' via SFDC API", () -> {
            createDocuSignStatus(steps.salesFlow.account.getId(), steps.quoteWizard.opportunity.getId(),
                    pocQuote.getId(), COMPLETED_ENVELOPE_STATUS);
        });

        //  CRM-22995
        step("10. Check that the Quote has no errors", () -> {
            assertThat(pocQuote.getIsQuoteHasErrors__c())
                    .as("Quote.IsQuoteHasErrors__c value")
                    .isFalse();
        });

        //  CRM-12157 and CRM-22995
        step("11. Press 'Process Order' button on the Opportunity's record page, " +
                "verify that 'Preparing Data' step is completed, " +
                "select the timezone, click 'Sign Up MVP', and check that the account is processed for signing up", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();

            opportunityPage.processOrderModal.selectDefaultTimezone();
            opportunityPage.processOrderModal.signUpButton.click();

            opportunityPage.processOrderModal.signUpMvpStatus
                    .shouldHave(exactTextCaseSensitive(format(YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE, MVP_SERVICE)), ofSeconds(60));
        });

        //  CRM-12157
        step("12. Check that all data was transmitted to NGBS correctly", () -> {
            //  polling is used here because the created NGBS account might be obtained a bit later
            var accountNgbsDTO = step("Check that the account is created in NGBS via NGBS API", () -> {
                return assertWithTimeout(() -> {
                    var accounts = searchAccountsByContactLastNameInNGBS(contact.getLastName());
                    assertEquals(1, accounts.size(),
                            "Number of NGBS accounts found by the related Contact's Last Name");
                    return accounts.get(0);
                }, ofSeconds(90));
            });

            //  polling is used here because initially license info might not be correct right away
            assertWithTimeout(() -> {
                var billingID = accountNgbsDTO.id;
                var packageID = accountNgbsDTO.packages[0].id;

                //  Check the expected licenses are in NGBS
                var billingInfoLicenses = getBillingInfoSummaryLicenses(billingID, packageID);
                var expectedProductLicenses = List.of(digitalLineUnlimited, commonPhone);

                for (var product : expectedProductLicenses) {
                    step("Check product data for '" + product.name + "'");
                    var licenseActual = Arrays.stream(billingInfoLicenses)
                            .filter(license -> license.catalogId.equals(product.dataName))
                            .findFirst();

                    assertTrue(licenseActual.isPresent(),
                            format("The license from NGBS for the product '%s' (should exist)", product.name));
                    assertEquals(product.quantity, licenseActual.get().qty,
                            format("The 'quantity' value on the license from NGBS for the product '%s'", product.name)
                    );
                }

                step("Check that there's no intended payment method on the NGBS account");
                var intendedPaymentMethod = getIntendedPaymentMethodFromNGBS(billingID);
                assertNull(intendedPaymentMethod, "Intended payment method on the NGBS account (should not exist)");
            }, ofSeconds(60));
        });
    }
}
