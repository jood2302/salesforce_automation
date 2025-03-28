package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.CLOSED_WON_STAGE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.setRequiredFieldsForOpportunityStageChange;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.ACTIVE_QUOTE_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.AGREEMENT_QUOTE_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("ShippingTab")
public class ProcessOrderForQuoteWithPopulatedShippingTabTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesRepUserWithoutPS;
    private String opportunityId;

    //  Test data
    private final Product polycomPhone;
    private final Product yealinkPhone;
    private final Product ciscoPhone;
    private final Product dlUnlimited;

    private final ShippingGroupAddress initialAddress;
    private final String initialAddressFormatted;
    private final ShippingGroupAddress firstEditAddress;
    private final String firstEditAddressFormatted;
    private final ShippingGroupAddress secondEditAddress;
    private final String secondEditAddressFormatted;

    public ProcessOrderForQuoteWithPopulatedShippingTabTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_2TypesOfDLs_RegularAndPOC.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        polycomPhone = data.getProductByDataName("LC_HDR_619");
        yealinkPhone = data.getProductByDataName("LC_HD_959");
        ciscoPhone = data.getProductByDataName("LC_HD_523");
        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");

        dlUnlimited.quantity = polycomPhone.quantity + yealinkPhone.quantity + ciscoPhone.quantity;

        initialAddress = new ShippingGroupAddress(
                "United States", "Findlay",
                new ShippingGroupAddress.State("Ohio", true),
                "3644 Cedarstone Drive", "45840", "QA Automation");
        initialAddressFormatted = initialAddress.getAddressFormatted();

        firstEditAddress = new ShippingGroupAddress(
                "Belgium", "Leerbeek",
                new ShippingGroupAddress.State("Flemish Brabant", false),
                "Schietboompleinstraat 421", "1755", "TestFName1 TestLName1");
        firstEditAddressFormatted = firstEditAddress.getAddressFormatted();

        secondEditAddress = new ShippingGroupAddress(
                "Germany", "Bruschied",
                new ShippingGroupAddress.State("Rheinland-Pfalz", false),
                "Ansbacher Strasse 6", "55606", "TestFName2 TestLName2");
        secondEditAddressFormatted = secondEditAddress.getAddressFormatted();
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with 'Sales Rep - Lightning' profile and without 'AllowProcessOrderWithoutShipping' permission set", () -> {
            salesRepUserWithoutPS = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withoutPermissionSet(ALLOW_PROCESS_ORDER_WITHOUT_SHIPPING_PS)
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserWithoutPS);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUserWithoutPS);

        opportunityId = steps.quoteWizard.opportunity.getId();

        step("Login as a user with 'Sales Rep - Lightning' profile and without 'AllowProcessOrderWithoutShipping' permission set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(salesRepUserWithoutPS);
        });
    }

    @Test
    @TmsLink("CRM-33521")
    @DisplayName("CRM-33521 - Process Order Validation and Permission set to bypass it. Shipping tab, New Business")
    @Description("Verify that shipping tab is not locked upon quote reaches agreement stage, also while Opportunity gets closed won. " +
            "Also check validation preventing user from Processing Order on a Oppty which Quote does not have all phones " +
            "assigned to shipping groups, then check a Permission Set that allows to bypass a validation. " +
            "This test-case covers whole flow of interacting with Shipping tab in case of sign up quote lock.")
    public void test() {
        step("1. Open the New Business Opportunity, switch to the Quote Wizard, add a new Sales Quote, " +
                "select a package for it, and add some phones on the Add Products tab", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(opportunityId);
            steps.quoteWizard.addNewSalesQuote();
            steps.quoteWizard.selectDefaultPackageFromTestData();

            steps.quoteWizard.addProductsOnProductsTab(polycomPhone, yealinkPhone, ciscoPhone);
        });

        step("2. Open the Price tab, set the quantity for the DL Unlimited, " +
                "assign devices to the digital lines, and save changes", () -> {
            cartPage.openTab();
            cartPage.setQuantityForQLItem(dlUnlimited.name, dlUnlimited.quantity);
            steps.cartTab.assignDevicesToDL(polycomPhone.name, dlUnlimited.name, steps.quoteWizard.localAreaCode, polycomPhone.quantity);
            steps.cartTab.assignDevicesToDL(yealinkPhone.name, dlUnlimited.name, steps.quoteWizard.localAreaCode, yealinkPhone.quantity);
            steps.cartTab.assignDevicesToDL(ciscoPhone.name, dlUnlimited.name, steps.quoteWizard.localAreaCode, ciscoPhone.quantity);

            cartPage.saveChanges();
        });

        step("3. Open the Quote Details tab, populate Main Area Code and Start Date fields, " +
                "set Payment Method = 'Credit Card', and save changes", () -> {
            quotePage.openTab();
            quotePage.setDefaultStartDate();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.selectPaymentMethod(QuotePage.CREDIT_CARD_PAYMENT_METHOD);
            quotePage.saveChanges();
        });

        step("4. Open the Shipping tab, create a new Shipping Group, " +
                "assign " + yealinkPhone.name + " and " + polycomPhone.name + " to it, save changes, " +
                "and check that changes are successfully saved on the tab", () -> {
            shippingPage.openTab();

            shippingPage.addNewShippingGroup(initialAddress);
            shippingPage.assignDeviceToShippingGroup(yealinkPhone.productName, initialAddressFormatted, initialAddress.shipAttentionTo);
            shippingPage.assignDeviceToShippingGroup(polycomPhone.productName, initialAddressFormatted, initialAddress.shipAttentionTo);
            shippingPage.saveChanges();

            shippingPage.getShippingGroup(initialAddressFormatted, initialAddress.shipAttentionTo)
                    .getAllShippingDevicesNames()
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(yealinkPhone.name, polycomPhone.name), ofSeconds(30));

            shippingPage.listNameOfDevices.shouldHave(exactTextsCaseSensitiveInAnyOrder(ciscoPhone.productName), ofSeconds(30));
        });

        step("5. Open the Quote Details tab, set Stage = 'Agreement', and save changes", () -> {
            quotePage.openTab();
            quotePage.stagePicklist.selectOption(AGREEMENT_QUOTE_TYPE);
            quotePage.saveChanges();
        });

        step("6. Open the Shipping tab, remove " + yealinkPhone.name + " from the Shipping Group, " +
                "assign " + ciscoPhone.name + " to it, save changes, " +
                "and check that changes are successfully saved on the tab", () -> {
            shippingPage.openTab();
            var shippingGroup = shippingPage.getShippingGroup(initialAddressFormatted, initialAddress.shipAttentionTo);

            shippingGroup
                    .getAssignedDevice(yealinkPhone.name)
                    .getDeleteButton()
                    .click();
            shippingPage.assignDeviceToShippingGroup(ciscoPhone.productName, initialAddressFormatted, initialAddress.shipAttentionTo);
            shippingPage.saveChanges();

            shippingGroup.getAllShippingDevicesNames()
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(polycomPhone.name, ciscoPhone.name), ofSeconds(30));

            shippingPage.listNameOfDevices.shouldHave(exactTextsCaseSensitiveInAnyOrder(yealinkPhone.productName), ofSeconds(30));
        });

        step("7. Open the 'Shipping Group Details' modal for the created Shipping Group, " +
                "change all the available fields, click 'Apply' button on the modal, and save changes on the Shipping tab", () -> {
            shippingPage.getShippingGroup(initialAddressFormatted, initialAddress.shipAttentionTo)
                    .getEditButton()
                    .click();
            shippingPage.shippingGroupDetailsModal.submitChanges(firstEditAddress);
            shippingPage.saveChanges();

            shippingPage.getShippingGroup(firstEditAddressFormatted, firstEditAddress.shipAttentionTo)
                    .getSelf()
                    .shouldBe(visible, ofSeconds(60));
        });

        step("8. Set Quote.Status = 'Active' via API", () -> {
            var quoteToUpdate = new Quote();
            quoteToUpdate.setId(wizardPage.getSelectedQuoteId());
            quoteToUpdate.setStatus(ACTIVE_QUOTE_STATUS);
            enterpriseConnectionUtils.update(quoteToUpdate);
        });

        step("9. Click 'Process Order' button on the Opportunity's record page " +
                "and verify that error notification is shown", () -> {
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60));
            opportunityPage.processOrderModal.errorNotifications
                    .shouldHave(exactTexts(format(MOVE_ALL_PRODUCT_SETS_TO_THE_SHIPPING_GROUPS_ERROR, MVP_SERVICE)), ofSeconds(1));
            opportunityPage.processOrderModal.closeWindow();
        });

        step("10. Set the required fields to be able to close the Opportunity via API", () -> {
            setRequiredFieldsForOpportunityStageChange(steps.quoteWizard.opportunity);
            enterpriseConnectionUtils.update(steps.quoteWizard.opportunity);
        });

        step("11. Click 'Close' button on the Opportunity's record page, " +
                "and check that Opportunity.StageName = '7. Closed Won'", () -> {
            opportunityPage.clickCloseButton();
            opportunityPage.spinner.shouldBe(visible, ofSeconds(10));
            opportunityPage.spinner.shouldBe(hidden, ofSeconds(30));
            opportunityPage.alertNotificationBlock.shouldNot(exist);

            var updatedOpportunity = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, StageName " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + opportunityId + "'",
                    Opportunity.class);
            assertThat(updatedOpportunity.getStageName())
                    .as("Opportunity.StageName value")
                    .isEqualTo(CLOSED_WON_STAGE);
        });

        step("12. Switch to the Quote Wizard, refresh the QW, open the Shipping tab, " +
                "remove " + polycomPhone.name + " from the Shipping Group, assign " + yealinkPhone.name + " to it, " +
                "save changes, and check that changes are successfully saved on the tab", () -> {
            switchTo().window(1);
            //  to retrieve actual Quote's Status picklist value
            refresh();
            wizardPage.waitUntilLoaded();

            shippingPage.openTab();
            var shippingGroup = shippingPage.getShippingGroup(firstEditAddressFormatted, firstEditAddress.shipAttentionTo);
            shippingGroup
                    .getAssignedDevice(polycomPhone.name)
                    .getDeleteButton()
                    .click();
            shippingPage.assignDeviceToShippingGroup(yealinkPhone.productName, firstEditAddressFormatted, firstEditAddress.shipAttentionTo);

            shippingPage.saveChanges();

            shippingGroup
                    .getAllShippingDevicesNames()
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(yealinkPhone.name, ciscoPhone.name), ofSeconds(30));

            shippingPage.listNameOfDevices.shouldHave(exactTextsCaseSensitiveInAnyOrder(polycomPhone.productName), ofSeconds(30));
        });

        step("13. Open the 'Shipping Group Details' modal for the created Shipping Group, " +
                "change all the available fields, click 'Apply' button on the modal, and save changes on the Shipping tab", () -> {
            shippingPage.getShippingGroup(firstEditAddressFormatted, firstEditAddress.shipAttentionTo)
                    .getEditButton()
                    .click();
            shippingPage.shippingGroupDetailsModal.submitChanges(secondEditAddress);
            shippingPage.saveChanges();

            shippingPage.getShippingGroup(secondEditAddressFormatted, secondEditAddress.shipAttentionTo)
                    .getSelf()
                    .shouldBe(visible, ofSeconds(60));

            closeWindow();
        });

        step("14. Transfer the ownership of the test Account, Contact, and Opportunity " +
                "to the user with 'Sales Rep - Lightning' profile and 'Allow Process Order Without Shipping' Permission Set via API, " +
                "and re-login as this user", () -> {
            var salesUserWithAllowProcessOrderWithoutShippingPS = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withPermissionSet(ALLOW_PROCESS_ORDER_WITHOUT_SHIPPING_PS)
                    .execute();

            steps.salesFlow.account.setOwnerId(salesUserWithAllowProcessOrderWithoutShippingPS.getId());
            steps.salesFlow.contact.setOwnerId(salesUserWithAllowProcessOrderWithoutShippingPS.getId());
            steps.quoteWizard.opportunity.setOwnerId(salesUserWithAllowProcessOrderWithoutShippingPS.getId());
            enterpriseConnectionUtils.update(steps.salesFlow.account, steps.salesFlow.contact, steps.quoteWizard.opportunity);

            switchTo().window(0);
            steps.sfdc.reLoginAsUser(salesUserWithAllowProcessOrderWithoutShippingPS);
        });

        step("15. Open the Opportunity's record page, click 'Process Order' button, " +
                "verify that 'Preparing Data' step is completed, and Timezone selector is enabled for a user, " +
                "and that warning notification is shown,", () -> {
            opportunityPage.openPage(opportunityId);
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();
            opportunityPage.processOrderModal.selectTimeZonePicklist.getInput().shouldBe(enabled, ofSeconds(30));

            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60));
            opportunityPage.processOrderModal.warningNotifications
                    .shouldHave(exactTexts(format(YOU_HAVE_AN_UNSHIPPED_PRODUCT_SET_WARNING, MVP_SERVICE)), ofSeconds(1));
        });
    }
}

