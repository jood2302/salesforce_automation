package ngbs.quotingwizard.newbusiness.signup;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.funnel.SignUpBodyFunnelDTO;
import com.aquiva.autotests.rc.model.funnel.SignUpBodyFunnelDTO.SubscriptionInfo.LicenseCategory;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.CREDIT_CARD_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.Condition.exactText;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("SignUp")
public class SalesLboQuoteWithDevicesFromSameLicenseCategorySignUpTest extends BaseTest {
    private final Steps steps;

    private LicenseCategory phonesLicenseCategoryItem;

    //  Test data
    private final Product dlUnlimited;
    private final Product polycomPhone;
    private final Product ciscoPhone;

    private final int numberOfPolycomPhonesToAssignToDl;
    private final int numberOfCiscoPhonesToAssignToDl;

    private final AreaCode localAreaCode;
    private final ShippingGroupAddress shippingAddress;
    private final String shippingAddressFormatted;

    private final String dlLicenseCategoryName;
    private final String phonesLicenseCategoryName;

    public SalesLboQuoteWithDevicesFromSameLicenseCategorySignUpTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_AnnualAndMonthly_Contract_PhonesAndDLs_Promos.json",
                Dataset.class);

        steps = new Steps(data);

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        polycomPhone = data.getProductByDataName("LC_HD_936");
        ciscoPhone = data.getProductByDataName("LC_HD_523");

        dlUnlimited.quantity = 15;
        polycomPhone.quantity = 10;
        ciscoPhone.quantity = 20;

        numberOfPolycomPhonesToAssignToDl = 6;
        numberOfCiscoPhonesToAssignToDl = 4;

        localAreaCode = steps.quoteWizard.localAreaCode;
        shippingAddress = new ShippingGroupAddress("United States", "Findlay",
                new ShippingGroupAddress.State("Ohio", true),
                "3644 Cedarstone Drive", "45840", "QA Automation");
        shippingAddressFormatted = shippingAddress.getAddressFormatted();

        dlLicenseCategoryName = "DL";
        phonesLicenseCategoryName = "HD";
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @Tag("KnownIssue")
    @Issue("PBC-25430")
    @TmsLink("CRM-34903")
    @DisplayName("CRM-34903 - New Business Sign Up with LBO Quote and devices from the same licenseCategory")
    @Description("Verify that Sign Up funnel body for a New Business LBO Quote with devices from the same licenseCategory " +
            "contains only one licenseCategory for all added devices")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote and select a package for it", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectDefaultPackageFromTestData();
        });

        step("2. Open the Add Products tab and add phones to the Cart", () -> {
            steps.quoteWizard.addProductsOnProductsTab(polycomPhone, ciscoPhone);
        });

        step("3. Open the Price tab, set up quantities, assign phones to the DL Unlimited and save changes", () -> {
            cartPage.openTab();
            steps.cartTab.setUpQuantities(dlUnlimited);
            steps.cartTab.setUpQuantities(ciscoPhone);
            steps.cartTab.setUpQuantities(polycomPhone);

            steps.cartTab.assignDevicesToDL(polycomPhone.name, dlUnlimited.name, localAreaCode, numberOfPolycomPhonesToAssignToDl);
            steps.cartTab.assignDevicesToDL(ciscoPhone.name, dlUnlimited.name, localAreaCode, numberOfCiscoPhonesToAssignToDl);

            cartPage.saveChanges();
        });

        step("4. Open the Quote Details tab, turn off Provisioning toggle, set Main Area Code and Start Date, " +
                "select Payment Method, save changes and check that Quote.Enabled_LBO__c = true", () -> {
            quotePage.openTab();
            quotePage.provisionToggle.click();

            quotePage.setMainAreaCode(localAreaCode);
            quotePage.setDefaultStartDate();
            quotePage.selectPaymentMethod(CREDIT_CARD_PAYMENT_METHOD);
            quotePage.saveChanges();

            steps.lbo.checkEnableLboOnQuote(true);
        });

        step("5. Open the Shipping tab, add a new shipping group, assign phones to the default and added shipping groups " +
                "and save changes", () -> {
            shippingPage.openTab();

            shippingPage.addNewShippingGroup(shippingAddress);

            var defaultShippingGroup = shippingPage.getFirstShippingGroup();
            var polycomPhoneShippingDevice = shippingPage.getShippingDevice(polycomPhone.name);
            shippingPage.assignDeviceToShippingGroup(polycomPhoneShippingDevice, defaultShippingGroup);

            var addedShippingGroup = shippingPage.getShippingGroup(shippingAddressFormatted, shippingAddress.shipAttentionTo);
            var ciscoPhoneShippingDevice = shippingPage.getShippingDevice(ciscoPhone.name);
            shippingPage.assignDeviceToShippingGroup(ciscoPhoneShippingDevice, addedShippingGroup);

            shippingPage.saveChanges();
        });

        step("6. Set the Quote to Active Agreement via API", () -> {
            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity);
        });

        step("7. Close the Opportunity via API", () -> {
            steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity);
        });

        step("8. Re-login as a user with 'System Administrator' profile and 'UI-less Sign Up Preview for Admins' permission set " +
                "and open the Opportunity record page", () -> {
            var adminWithUiLessSignUpPreviewPS = getUser()
                    .withProfile(SYSTEM_ADMINISTRATOR_PROFILE)
                    .withPermissionSet(UI_LESS_SIGN_UP_PREVIEW_FOR_ADMINS_PS)
                    .execute();

            steps.sfdc.reLoginAsUserWithSessionReset(adminWithUiLessSignUpPreviewPS);

            opportunityPage.openPage(steps.quoteWizard.opportunity.getId());
        });

        step("9. Press 'Process Order' button on the Opportunity record page, " +
                "verify that 'Preparing Data' step is completed, and no errors are displayed, " +
                "select the default Timezone, and open the 'Admin Preview' tab", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();

            opportunityPage.processOrderModal.selectDefaultTimezone();

            opportunityPage.processOrderModal.adminPreviewTab.click();
            opportunityPage.processOrderModal.signUpBodyContents.shouldNotHave(exactText(EMPTY_STRING), ofSeconds(20));
        });

        step("10. Check that the 'Sign Up body' text contains only 1 DL license category, " +
                "there is only 1 license category for both added phones, and DL and added phones have correct quantities", () -> {
            var signUpBodyText = opportunityPage.processOrderModal.signUpBodyContents.getText();
            var signUpBodyObj = JsonUtils.readJson(signUpBodyText, SignUpBodyFunnelDTO.class);

            assertThat(signUpBodyObj.subscriptionInfo)
                    .as(format("SignUpBody's 'SubscriptionInfo' object's value " +
                            "(SignUpBody contents: %s)", signUpBodyText))
                    .isNotNull();
            assertThat(signUpBodyObj.subscriptionInfo.licenseCategories)
                    .as(format("SignUpBody's 'SubscriptionInfo.LicenseCategories[]' collection " +
                            "(SubscriptionInfo contents: %s)", signUpBodyObj.subscriptionInfo))
                    .isNotNull();

            var dlLicenseCategories = signUpBodyObj.subscriptionInfo.licenseCategories
                    .stream()
                    .filter(lc -> lc.licenseCategory.equals(dlLicenseCategoryName))
                    .toList();
            assertThat(dlLicenseCategories)
                    .as(format("SignUpBody's 'SubscriptionInfo.LicenseCategories[]' collection for DLs category " +
                            "(SubscriptionInfo.LicenseCategories contents: %s)", signUpBodyObj.subscriptionInfo.licenseCategories))
                    .hasSize(1);
            assertThat(dlLicenseCategories.get(0).licenses)
                    .as(format("SignUpBody's 'SubscriptionInfo.LicenseCategories.licenses[]' collection for DLs category " +
                            "(SubscriptionInfo.LicenseCategories contents: %s)", dlLicenseCategories.get(0)))
                    .hasSize(1);
            assertThat(dlLicenseCategories.get(0).licenses.get(0).qty)
                    .as(format("SignUpBody's 'SubscriptionInfo.LicenseCategories.licenses.qty' value for DLs category's license " +
                            "(SubscriptionInfo.LicenseCategories contents for DL: %s)", dlLicenseCategories.get(0)))
                    .isEqualTo(dlUnlimited.quantity);

            var phonesLicenseCategories = signUpBodyObj.subscriptionInfo.licenseCategories
                    .stream()
                    .filter(lc -> lc.licenseCategory.equals(phonesLicenseCategoryName))
                    .toList();
            assertThat(phonesLicenseCategories)
                    .as(format("SignUpBody's 'SubscriptionInfo.LicenseCategories[]' collection for Phones category " +
                            "(SubscriptionInfo.LicenseCategories contents: %s)", signUpBodyObj.subscriptionInfo.licenseCategories))
                    .hasSize(1);

            phonesLicenseCategoryItem = phonesLicenseCategories.get(0);
            assertThat(phonesLicenseCategoryItem.licenses)
                    .as(format("SignUpBody's 'SubscriptionInfo.LicenseCategories.licenses[]' collection for Phones category " +
                            "(SubscriptionInfo.LicenseCategories contents for Phones: %s)", phonesLicenseCategoryItem))
                    .hasSize(2);

            //  TODO Known Issue PBC-25430 (The quantity of the phone = 1 due to resetting the quantity on the Price tab, but it should be 10/20)
            checkPhoneLicenseItemInSignUpBody(polycomPhone);
            checkPhoneLicenseItemInSignUpBody(ciscoPhone);
        });
    }

    /**
     * Check that there is only one license item for the provided phone,
     * and it has the correct quantity in the SignUp body.
     *
     * @param phone the phone data to check the SignUp body against
     */
    private void checkPhoneLicenseItemInSignUpBody(Product phone) {
        var phoneLicenseItems = phonesLicenseCategoryItem.licenses.stream()
                .filter(li -> li.catalogId.equals(phone.dataName))
                .toList();
        assertThat(phoneLicenseItems)
                .as(format("SignUpBody's 'SubscriptionInfo.LicenseCategories.licenses[]' collection for " + phone.name +
                        "(licenses[] contents: %s)", phonesLicenseCategoryItem.licenses))
                .hasSize(1);
        assertThat(phoneLicenseItems.get(0).qty)
                .as(format("SignUpBody's 'SubscriptionInfo.LicenseCategories.licenses.qty' value for " + phone.name +
                        "(licenses[] contents: %s)", phonesLicenseCategoryItem.licenses))
                .isEqualTo(phone.quantity);
    }
}
