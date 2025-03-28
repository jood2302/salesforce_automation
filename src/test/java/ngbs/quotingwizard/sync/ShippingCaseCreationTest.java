package ngbs.quotingwizard.sync;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.PackageFactory.createBillingAccountPackage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.CREDIT_CARD_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.PAID_RC_ACCOUNT_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.CaseHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.CLOSED_WON_STAGE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.EXECUTED_QUOTE_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("P1")
@Tag("CaseManagement")
@Tag("DaaS")
@Tag("ShippingTab")
public class ShippingCaseCreationTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesUserWithLboQuoteEnablePermissionSet;
    private Quote additionalActiveSalesAgreement;

    //  Test data
    private final String officeService;
    private final String ngbsPackageCatalogId;
    private final Product ciscoPhone;
    private final Product polyHeadsetPhone;

    private final ShippingGroupAddress shippingGroupAddress;
    private final String shippingAddressFormatted;

    public ShippingCaseCreationTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/sync/RC_MVP_Monthly_Contract_Manual_Shipping_163110013.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        //  because only 1 Package__c object is allowed in SFDC for each Package ID in NGBS
        steps.ngbs.isGenerateAccountsForSingleTest = true;

        officeService = data.packageFolders[0].name;
        ngbsPackageCatalogId = data.packageFolders[0].packages[0].id;
        ciscoPhone = data.getProductByDataName("LC_HDRF_590");
        polyHeadsetPhone = data.getProductByDataName("LC_HDHS_2");

        shippingGroupAddress = new ShippingGroupAddress(
                "United States", "Foster City",
                new ShippingGroupAddress.State("California", true),
                "App.129 13 Elm Street", "94404", "QA Automation");
        shippingAddressFormatted = shippingGroupAddress.getAddressFormatted();
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();
        steps.ngbs.stepCreateContractInNGBS();

        step("Find a user with 'Sales Rep - Lightning' profile and with 'LBO Quote Enable' permission set", () -> {
            salesUserWithLboQuoteEnablePermissionSet = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withPermissionSet(LBO_QUOTE_ENABLE_PS)
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(salesUserWithLboQuoteEnablePermissionSet);

        step("Create a new Billing Account Package object (Package__c) with EnabledLBO__c = true for the Account via API", () -> {
            var accountPackage = createBillingAccountPackage(steps.salesFlow.account.getId(),
                    data.packageId, ngbsPackageCatalogId,
                    data.getBrandName(), officeService, CREDIT_CARD_PAYMENT_METHOD, PAID_RC_ACCOUNT_STATUS);
            accountPackage.setEnabledLBO__c(true);
            enterpriseConnectionUtils.update(accountPackage);
        });

        step("Create an additional Existing Business Opportunity with the related Active Sales Agreement, " +
                "and and close this Opportunity (all via API)", () -> {
            additionalActiveSalesAgreement = steps.syncWithNgbs.stepCreateAdditionalActiveSalesAgreement(
                    steps.salesFlow.account, steps.salesFlow.contact, salesUserWithLboQuoteEnablePermissionSet);

            var bufferOpportunity = new Opportunity();
            bufferOpportunity.setId(additionalActiveSalesAgreement.getOpportunityId());
            steps.quoteWizard.stepCloseOpportunity(bufferOpportunity);
        });

        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesUserWithLboQuoteEnablePermissionSet);

        step("Login as the user with 'Sales Rep - Lightning' profile and with 'LBO Quote Enable' permission set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(salesUserWithLboQuoteEnablePermissionSet);
        });
    }

    @Test
    @TmsLink("CRM-32781")
    @DisplayName("CRM-32781 - Case Creation logic for products with different labels. Existing Business")
    @Description("Verify that Sales Order Except Desk Case record is created only for Quotes " +
            "with items that have 'shippingReq': 'manual' label in NGBS Catalog")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Add Products tab and add products to cart", () -> {
            steps.quoteWizard.addProductsOnProductsTab(ciscoPhone, polyHeadsetPhone);
        });

        step("3. Open the Shipping tab, create a new Shipping Group, " +
                "and assign products to the new Shipping Group", () -> {
            shippingPage.openTab();
            shippingPage.addNewShippingGroup(shippingGroupAddress);
            shippingPage.assignDeviceToShippingGroup(ciscoPhone.productName, shippingAddressFormatted, shippingGroupAddress.shipAttentionTo);
            shippingPage.assignDeviceToShippingGroup(polyHeadsetPhone.productName, shippingAddressFormatted, shippingGroupAddress.shipAttentionTo);

            shippingPage.getShippingGroup(shippingAddressFormatted, shippingGroupAddress.shipAttentionTo)
                    .getAllShippingDevicesNames()
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(ciscoPhone.name, polyHeadsetPhone.name));

            shippingPage.shippingGroupDropLists.shouldHave(sizeGreaterThan(0), ofSeconds(10));
        });

        step("4. Open the Quote Details tab, and save changes", () -> {
            quotePage.openTab();
            quotePage.saveChanges();
        });

        step("5. Set Status = 'Executed' for the existing Active Sales Agreement, " +
                "and update the currently created Quote to Active Agreement (all via API)", () -> {
            additionalActiveSalesAgreement.setStatus(EXECUTED_QUOTE_STATUS);
            enterpriseConnectionUtils.update(additionalActiveSalesAgreement);

            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity);
        });

        step("6. Click 'Close' button on the Opportunity record page, reload the page after the spinner is hidden " +
                "and verify that the Opportunity's Stage = '7. Closed Won'", () -> {
            opportunityPage.openPage(steps.quoteWizard.opportunity.getId());
            opportunityPage.clickCloseButton();
            opportunityPage.spinner.shouldBe(visible, ofSeconds(10));
            opportunityPage.spinner.shouldBe(hidden, ofSeconds(60));
            refresh();
            opportunityPage.waitUntilLoaded();
            opportunityPage.stagePicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(CLOSED_WON_STAGE), ofSeconds(10));
        });

        step("7. Check that a new Case record is created and that Case record fields are populated with proper values", () -> {
            var shippingCase = step("Wait until the required Case is created", () ->
                    assertWithTimeout(() -> {
                        var caseSubject = getProductsForManualShippingCaseSubject(steps.quoteWizard.opportunity.getName());
                        var shippingCases = enterpriseConnectionUtils.query(
                                "SELECT Id, RecordType.Name, Opportunity_Reference__c, " +
                                        "Status, Origin, Description " +
                                        "FROM Case " +
                                        "WHERE AccountId = '" + steps.salesFlow.account.getId() + "' " +
                                        "AND Subject = '" + caseSubject + "'",
                                Case.class);
                        assertEquals(1, shippingCases.size(),
                                "Number of Cases with Subject = '" + caseSubject + "' for the Account");
                        return shippingCases.get(0);
                    }, ofSeconds(150))
            );

            assertThat(shippingCase.getRecordType().getName())
                    .as("Case.RecordType.Name value")
                    .isEqualTo(SALES_ORDER_EXCEPT_DESK_RECORD_TYPE);

            assertThat(shippingCase.getOpportunity_Reference__c())
                    .as("Case.Opportunity_Reference__c value")
                    .isEqualTo(steps.quoteWizard.opportunity.getId());

            assertThat(shippingCase.getStatus())
                    .as("Case.Status value")
                    .isEqualTo(NEW_STATUS);

            assertThat(shippingCase.getOrigin())
                    .as("Case.Origin value")
                    .isEqualTo(AUTO_EMAIL_ORIGIN);

            var caseDescription = formatProductsForManualShippingInCaseDescription(
                    steps.salesFlow.account.getId(), steps.quoteWizard.opportunity.getId(),
                    shippingAddressFormatted,
                    polyHeadsetPhone.name, polyHeadsetPhone.quantity);
            assertThat(shippingCase.getDescription())
                    .as("Case.Description value")
                    .isEqualTo(caseDescription);

            //  Case.Subject and Case.AccountId are checked implicitly by the fact that the Case is found by the query
        });
    }
}