package e2e;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.leadConvert.Datasets;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import com.sforce.soap.enterprise.sobject.Lead;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.NEW_ACCOUNT_WILL_BE_CREATED_MESSAGE;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.MVP_SERVICE;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.CREDIT_CARD_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToInteger;
import static com.aquiva.autotests.rc.utilities.StringHelper.PERCENT;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ContactHelper.setRequiredFieldsRandomly;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.sleep;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("P1")
@Tag("LeadConvert")
@Tag("SignUp")
@Tag("E2E")
public class RcRingExSignUpTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Lead createdLead;
    private Lead convertedLead;
    private Opportunity opportunityFromLead;
    private Contact contactFromLead;
    private String billingID;
    private String packageID;

    //  Test data
    private final String rcUsCountry;
    private final String serviceName;
    private final String rcUsBusinessIdentity;
    private final int numberOfPhonesToAssignToDl;
    private final Product dlUnlimited;
    private final Product addlLocalNumber;
    private final Product polycomPhone;
    private final Product polyEncorePro;
    private final Product polyDaas;
    private final int numberOfDevicesToAssignToShippingGroup;
    private final int numberOfDevicesToAssignToThirdShippingGroup;
    private final ShippingGroupAddress firstShippingAddress;
    private final String firstShippingAddressFormatted;
    private final ShippingGroupAddress secondShippingAddress;
    private final String secondShippingAddressFormatted;
    private final String specialTerms;
    private final String initialTerm;
    private final String renewalTerm;
    private final String mobileUserLicenseName;
    private final String contractExtId;

    public RcRingExSignUpTest() {
        var datasets = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_USAndCanada_NB.json",
                Datasets.class);

        data = datasets.dataSets[0];
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        rcUsCountry = "United States";
        serviceName = data.packageFolders[0].name;
        rcUsBusinessIdentity = data.getBusinessIdentityName();
        numberOfPhonesToAssignToDl = 10;
        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        addlLocalNumber = data.getProductByDataName("LC_ALN_38");
        polycomPhone = data.getProductByDataName("LC_HD_611");
        polyEncorePro = data.getProductByDataName("LC_HDRO_1068");
        polyDaas = data.getProductByDataName("LC_HDRO_1075");
        numberOfDevicesToAssignToShippingGroup = 5;
        numberOfDevicesToAssignToThirdShippingGroup = 5;

        firstShippingAddress = new ShippingGroupAddress("United States", "Findlay",
                new ShippingGroupAddress.State("Ohio", true),
                "3644 Cedarstone Drive", "45840", "QA Automation");
        firstShippingAddressFormatted = firstShippingAddress.getAddressFormatted();
        secondShippingAddress = new ShippingGroupAddress("United States", "Los Angeles",
                new ShippingGroupAddress.State("California", true),
                "Sunbeam Lane 78", "90022", "QA Automation");
        secondShippingAddressFormatted = secondShippingAddress.getAddressFormatted();
        specialTerms = "1 Free Month of Service";
        initialTerm = data.packageFolders[0].packages[0].contractTerms.initialTerm[0];
        renewalTerm = data.packageFolders[0].packages[0].contractTerms.renewalTerm;
        mobileUserLicenseName = "Mobile User";
        contractExtId = data.packageFolders[0].packages[0].contractExtId;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);

        createdLead = steps.leadConvert.createSalesLeadViaLeadCreationPage();

        step("Click 'Edit' on the Lead Record page, " +
                "populate Address fields, Lead Qualification fields, and save changes", () -> {
            leadRecordPage.clickEditButton();
            leadRecordPage.populateAddressFields();
            leadRecordPage.populateLeadQualificationFields();
            leadRecordPage.saveChanges();
        });
    }

    @Test
    @Tag("KnownIssue")
    @Issue("PBC-20908")
    @Issue("PBC-25888")
    @TmsLink("CRM-36068")
    @DisplayName("CRM-36068 - RC US RingEX Single Product Sign Up")
    @Description("Verify that RingCentral US RingEX Account can be signed up in a flow " +
            "that includes Credit Card payment method and Free Service Credit. " +
            "This does not include verifications of DocuSign CLM integration")
    public void test() {
        step("1. Click 'Convert' button on the Lead Record page", () -> {
            leadRecordPage.clickConvertButton();

            leadConvertPage.switchToIFrame();
            leadConvertPage.newExistingAccountToggle.shouldBe(visible, ofSeconds(60));
            leadConvertPage.existingAccountSearchInput.getSelf().shouldBe(visible);
        });

        step("2. Switch the toggle into 'Create New Account' position in Account Info section", () -> {
            leadConvertPage.newExistingAccountToggle.click();

            leadConvertPage.accountInfoLabel.shouldHave(exactTextCaseSensitive(NEW_ACCOUNT_WILL_BE_CREATED_MESSAGE));
            leadConvertPage.accountInfoApplyButton.shouldBe(hidden);
        });

        step("3. Click 'Edit' button on the Opportunity section, check Opportunity section field values, " +
                "populate Close Date field and click 'Apply' button", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.countryPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(rcUsCountry));
            leadConvertPage.servicePickList.getSelectedOption().shouldHave(exactTextCaseSensitive(serviceName));
            leadConvertPage.businessIdentityPicklist
                    .getSelectedOption().shouldHave(exactTextCaseSensitive(rcUsBusinessIdentity));

            leadConvertPage.closeDateDatepicker.setTomorrowDate();

            leadConvertPage.opportunityInfoApplyButton.click();
        });

        step("4. Press 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        step("5. Check that Lead is converted correctly", () -> {
            steps.leadConvert.checkLeadConversion(createdLead);

            convertedLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ConvertedAccountId, ConvertedOpportunityId, ConvertedContactId " +
                            "FROM Lead " +
                            "WHERE Id = '" + createdLead.getId() + "'",
                    Lead.class);
        });

        step("6. Populate required fields on the created Contact record via API", () -> {
            contactFromLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, LastName " +
                            "FROM Contact " +
                            "WHERE Id = '" + convertedLead.getConvertedContactId() + "'",
                    Contact.class);

            setRequiredFieldsRandomly(contactFromLead);
            enterpriseConnectionUtils.update(contactFromLead);
        });

        step("7. Open the Quote Wizard for the created Opportunity to create a new Quote, select a package and save changes", () -> {
            opportunityFromLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Name " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + convertedLead.getConvertedOpportunityId() + "'",
                    Opportunity.class);

            steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(opportunityFromLead.getId());
        });

        step("8. Open the Add Products tab and add necessary products to the Cart", () ->
                steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd())
        );

        step("9. Open the Price tab and set up quantities and discounts", () -> {
            cartPage.openTab();
            steps.cartTab.setUpQuantities(data.getNewProductsToAdd());
            steps.cartTab.setUpQuantities(dlUnlimited);
            steps.cartTab.setUpDiscounts(data.getNewProductsToAdd());
            steps.cartTab.setUpDiscounts(dlUnlimited);
        });

        step("10. Assign some phones to the DigitalLine Unlimited", () ->
                steps.cartTab.assignDevicesToDL(polycomPhone.name, dlUnlimited.name, steps.quoteWizard.localAreaCode,
                        numberOfPhonesToAssignToDl)
        );

        step("11. Open the Shipping tab, create 2 additional shipping groups " +
                "and assign some devices to the default and added shipping groups", () -> {
            shippingPage.openTab();

            shippingPage.addNewShippingGroup(firstShippingAddress);
            shippingPage.addNewShippingGroup(secondShippingAddress);

            var defaultShippingGroup = shippingPage.getFirstShippingGroup();
            var polycomPhoneShippingDevice = shippingPage.getShippingDevice(polycomPhone.productName);
            var polyEncoreProShippingDevice = shippingPage.getShippingDevice(polyEncorePro.productName);
            shippingPage.assignDeviceToShippingGroup(polycomPhoneShippingDevice, defaultShippingGroup);
            shippingPage.assignDeviceToShippingGroup(polyEncoreProShippingDevice, defaultShippingGroup);
            defaultShippingGroup.setQuantityForAssignedDevice(polycomPhone.name, numberOfDevicesToAssignToShippingGroup);
            defaultShippingGroup.setQuantityForAssignedDevice(polyEncorePro.name, numberOfDevicesToAssignToShippingGroup);

            shippingPage.assignDeviceToShippingGroup(polycomPhone.productName, firstShippingAddressFormatted,
                    firstShippingAddress.shipAttentionTo);
            shippingPage.assignDeviceToShippingGroup(polyEncorePro.productName, firstShippingAddressFormatted,
                    firstShippingAddress.shipAttentionTo);
            var firstAddedShippingGroup = shippingPage.getShippingGroup(firstShippingAddressFormatted,
                    firstShippingAddress.shipAttentionTo);
            firstAddedShippingGroup.setQuantityForAssignedDevice(polycomPhone.name, numberOfDevicesToAssignToShippingGroup);
            firstAddedShippingGroup.setQuantityForAssignedDevice(polyEncorePro.name, numberOfDevicesToAssignToShippingGroup);

            shippingPage.assignDeviceToShippingGroup(polyDaas.productName, secondShippingAddressFormatted,
                    secondShippingAddress.shipAttentionTo);
            var secondAddedShippingGroup = shippingPage.getShippingGroup(secondShippingAddressFormatted,
                    secondShippingAddress.shipAttentionTo);
            secondAddedShippingGroup.setQuantityForAssignedDevice(polyDaas.name, numberOfDevicesToAssignToThirdShippingGroup);
        });

        step("12. Open the Quote Details tab, open Billing Details and Terms Modal, add 1 Free Month of Service Special Terms, " +
                "select values in Initial Term and Renewal Term picklists and apply changes", () -> {
            quotePage.openTab();
            quotePage.footer.billingDetailsAndTermsButton.click();

            quotePage.billingDetailsAndTermsModal.specialTermsPicklist.selectOption(specialTerms);
            quotePage.billingDetailsAndTermsModal.initialTermPicklist.selectOption(initialTerm);
            quotePage.billingDetailsAndTermsModal.renewalTermPicklist.selectOption(renewalTerm);
            quotePage.applyChangesInBillingDetailsAndTermsModal();
        });

        step("13. Populate Main Area Code, Start Date, and Discount Justification fields, set Payment Method = 'Credit Card' " +
                "and save changes on the Quote Details tab", () -> {
            sleep(5_000);   //  this wait should help avoid the Known Issue below; to be removed when the issue is resolved
            //  TODO Known Issue PBC-20908 (Area Code dropdown list disappears when selecting it on the Quote Details tab, but it shouldn't)
            quotePage.setDefaultStartDate();
            quotePage.selectPaymentMethod(CREDIT_CARD_PAYMENT_METHOD);
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.discountJustificationTextArea.setValue(TEST_STRING);
            quotePage.saveChanges();
        });

        step("14. Update the Quote to Active Agreement via API", () ->
                steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(opportunityFromLead)
        );

        step("15. Open the Opportunity record page, open Process Order modal, select value in TimeZone picklist " +
                "and click Sign Up MVP button", () -> {
            opportunityPage.openPage(opportunityFromLead.getId());
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();
            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(hidden);

            opportunityPage.processOrderModal.selectDefaultTimezone();
            opportunityPage.processOrderModal.signUpButton.click();

            opportunityPage.processOrderModal.signUpMvpStatus
                    .shouldHave(exactTextCaseSensitive(format(YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE, MVP_SERVICE)), ofSeconds(60));
        });

        step("16. Wait until the Account is signed up and verify it in NGBS", () -> {
            var accountNgbsDTO = step("Check that the account is created in NGBS via NGBS API", () -> {
                return assertWithTimeout(() -> {
                    var accounts = searchAccountsByContactLastNameInNGBS(contactFromLead.getLastName());
                    assertEquals(1, accounts.size(),
                            "Number of NGBS accounts found by the related Contact's Last Name");
                    return accounts.get(0);
                }, ofSeconds(180));
            });

            billingID = accountNgbsDTO.id;
            packageID = accountNgbsDTO.packages[0].id;

            var includedProductLicenses = List.of(dlUnlimited, addlLocalNumber, polycomPhone);
            var excludedProductLicenses = List.of(polyDaas, polyEncorePro);

            step("Check that the account has the included product licenses and doesn't have the excluded ones in NGBS", () -> {
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
                                format("The 'quantity' value on the license from NGBS for the product '%s'", product.name)
                        );
                    }

                    //  TODO Known Issue PBC-25888 (All licenses from excludedProductLicenses should not exist in NGBS, but they do exist)
                    for (var product : excludedProductLicenses) {
                        step("Check product data for '" + product.name + "'");
                        var licenseActual = Arrays.stream(billingInfoLicenses)
                                .filter(license -> license.catalogId.equals(product.dataName))
                                .findFirst();

                        assertTrue(licenseActual.isEmpty(),
                                format("The license from NGBS for the product '%s' (should not exist)", product.name));
                    }
                }, ofSeconds(60));
            });

            step("Check that the account has the expected discounts with expected types and values in NGBS", () -> {
                var allDiscounts = getDiscountsFromNGBS(billingID, packageID);
                assertThat(allDiscounts.size())
                        .as("Number of discounts on the NGBS account")
                        .isGreaterThan(0);

                //  All discounts should be in the 1st Discount Template Group, and we should filter out the Mobile User discount from the checks
                var discounts = Arrays.stream(allDiscounts.get(0).discountTemplates)
                        .filter(discount -> !discount.description.equals(mobileUserLicenseName))
                        .toList();

                var productsWithDiscounts = List.of(dlUnlimited, addlLocalNumber, polyEncorePro);
                assertThat(discounts.size())
                        .as("Number of discounts on the NGBS account")
                        .isEqualTo(productsWithDiscounts.size());

                step("Check that the account has the expected discounts for non-DL licenses in NGBS", () -> {
                    var productsWithDiscountsWithoutDL = List.of(addlLocalNumber, polyEncorePro);
                    productsWithDiscountsWithoutDL.forEach(productWithDiscount -> {
                        var discountTemplate = discounts.stream()
                                .filter(discount -> discount.description.equals(productWithDiscount.name))
                                .findFirst();
                        assertThat(discountTemplate)
                                .as("The discount for the product '" + productWithDiscount.name + "' (should exist)")
                                .isPresent();

                        assertThat(doubleToInteger(discountTemplate.get().values.monthly.value))
                                .as("Discount value for the product '" + productWithDiscount.name + "'")
                                .isEqualTo(productWithDiscount.discount);
                        var actualDiscountType = discountTemplate.get().values.monthly.unit.equals("Percent") ?
                                PERCENT :
                                data.getCurrencyIsoCode();
                        assertThat(actualDiscountType)
                                .as("Discount type for the product '" + productWithDiscount.name + "'")
                                .isEqualTo(productWithDiscount.discountType);
                    });
                });

                step("Check that the account has the expected discount for the DigitalLine Unlimited in NGBS", () -> {
                    var discountTemplateForDL = discounts.stream()
                            .filter(discount -> discount.description.equals(dlUnlimited.name))
                            .findFirst();
                    assertThat(discountTemplateForDL)
                            .as("The discount for the product '" + dlUnlimited.name + "' (should exist)")
                            .isPresent();

                    var expectedDlDiscount = steps.syncWithNgbs.getTotalExpectedDiscount(dlUnlimited, contractExtId);
                    assertThat(discountTemplateForDL.get().values.monthly.value)
                            .as("Discount value for the product '" + dlUnlimited.name + "'")
                            .isEqualTo(expectedDlDiscount);

                    var actualDiscountType = discountTemplateForDL.get().values.monthly.unit.equals("Percent") ?
                            PERCENT :
                            data.getCurrencyIsoCode();
                    assertThat(actualDiscountType)
                            .as("Discount type for the product '" + dlUnlimited.name + "'")
                            .isEqualTo(dlUnlimited.discountType);
                });
            });
        });
    }
}
