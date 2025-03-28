package ngbs.quotingwizard.newbusiness.shippingtab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.CustomAddressAssignments__c;
import com.sforce.ws.ConnectionException;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactValue;
import static io.qameta.allure.Allure.step;
import static java.lang.String.valueOf;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("ShippingTab")
public class RemoveShippingGroupAssignmentTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Product dlUnlimited;
    private final Product dlBasic;
    private final Product phoneToAdd;
    private final int numberOfPhonesToAssignInitial;
    private final int numberOfPhonesToAssignDifferent;
    private final AreaCode differentAreaCode;
    private final ShippingGroupAddress shippingGroupAddress;
    private final String shippingAddressFormatted;

    public RemoveShippingGroupAssignmentTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_PhonesAndDLs.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        dlBasic = data.getProductByDataName("LC_DL-BAS_178");
        phoneToAdd = data.getProductByDataName("LC_HD_523");
        numberOfPhonesToAssignInitial = phoneToAdd.quantity;
        numberOfPhonesToAssignDifferent = phoneToAdd.quantity - 1;

        differentAreaCode = new AreaCode("Local", "United States", "California", EMPTY_STRING, "323");

        shippingGroupAddress = new ShippingGroupAddress(
                "United States", "Foster City",
                new ShippingGroupAddress.State("California", true),
                "App.129 13 Elm Street", "94404", "QA Automation");
        shippingAddressFormatted = shippingGroupAddress.getAddressFormatted();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-34787")
    @DisplayName("CRM-34787 - Removing phone assigned to DL from the shipping group if DL, " +
            "area code or quantity of the assigned phones have changed")
    @Description("Verify that phone is removed from the shipping group if DL, area code or quantity of the assigned phones have changed")
    public void test() {
        step("1. Open the test Opportunity, switch to the Quote Wizard, " +
                "select a package for it, and add a phone and DL Basic on the Add Products tab", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
            steps.quoteWizard.selectDefaultPackageFromTestData();

            steps.quoteWizard.addProductsOnProductsTab(dlBasic, phoneToAdd);
        });

        step("2. Open the Price tab, set up products' quantities, assign the phones to the DLs, and save changes", () -> {
            cartPage.openTab();
            steps.cartTab.setUpQuantities(dlUnlimited, dlBasic, phoneToAdd);
            steps.cartTab.assignDevicesToDLAndSave(phoneToAdd.name, dlUnlimited.name, steps.quoteWizard.localAreaCode, numberOfPhonesToAssignInitial);
        });

        step("3. Open the Shipping tab, create a new Shipping Group, assign the phone to it, " +
                "check that CustomAddressAssignments__c record is created, " +
                "and that its Quantity and Product Name are aligned with the assigned Device", () -> {
            assignDeviceToShippingGroupAndCheckCustomAddressAssignments(numberOfPhonesToAssignInitial);
        });

        step("4. Open the Price tab, change the area code for the assigned phone, save changes, " +
                "open the Shipping tab, and check that the phone assignment to the Shipping Group does not exist, " +
                "and that CustomAddressAssignments__c record does not exist anymore in DB", () -> {
            cartPage.openTab();
            steps.cartTab.assignDevicesToDL(phoneToAdd.name, dlUnlimited.name, differentAreaCode, numberOfPhonesToAssignInitial, true);
            cartPage.saveChanges();

            checkThatPhoneAssignmentOnShippingGroupDoesNotExist();
        });

        step("5. Open the Shipping tab, create a new Shipping Group, assign the phone to it, " +
                "check that CustomAddressAssignments__c record is created, " +
                "and that its Quantity and Product Name are aligned with the assigned Device", () ->
                assignDeviceToShippingGroupAndCheckCustomAddressAssignments(numberOfPhonesToAssignInitial)
        );

        step("6. Open the Price tab, assign all the phones to another DL (" + dlBasic.name + "), save changes," +
                "open the Shipping tab, and check that the phone assignment to the Shipping Group does not exist, " +
                "and that CustomAddressAssignments__c record does not exist anymore", () -> {
            cartPage.openTab();

            cartPage.getQliFromCartByDisplayName(dlUnlimited.name)
                    .getDeviceAssignmentButton()
                    .click();
            deviceAssignmentPage.getFirstAreaCodeItem(phoneToAdd.name)
                    .getDeleteButton()
                    .click();
            deviceAssignmentPage.applyButton.click();

            steps.cartTab.assignDevicesToDLAndSave(phoneToAdd.name, dlBasic.name, steps.quoteWizard.localAreaCode, numberOfPhonesToAssignInitial);

            checkThatPhoneAssignmentOnShippingGroupDoesNotExist();
        });

        step("7. Open the Price tab, change the number of phones assigned to the DL, save changes," +
                "open the Shipping tab, and check that the phone assignment to the Shipping Group does not exist, " +
                "and that CustomAddressAssignments__c record does not exist anymore in DB", () -> {
            cartPage.openTab();
            steps.cartTab.assignDevicesToDLWithoutSettingAreaCode(phoneToAdd.name, dlBasic.name, numberOfPhonesToAssignDifferent);
            cartPage.setQuantityForQLItem(phoneToAdd.name, numberOfPhonesToAssignDifferent);
            cartPage.saveChanges();

            checkThatPhoneAssignmentOnShippingGroupDoesNotExist();
        });
    }

    /**
     * <p> - Open the Shipping tab, add a new Shipping Group, and assign the phone to it </p>
     * <p> - Check that the assignment to the Shipping Group is displayed correctly </p>
     * <p> - Check that CustomAddressAssignments__c is created
     * and its Quantity__c and QuoteLineItem__r.Product2.Name fields have expected values </p>
     *
     * @param numberOfPhonesToAssign expected number of the phones to be assigned to the Shipping Group
     * @throws ConnectionException in case of malformed DB queries or network failures
     */
    private void assignDeviceToShippingGroupAndCheckCustomAddressAssignments(int numberOfPhonesToAssign) throws ConnectionException {
        shippingPage.openTab();
        shippingPage.addNewShippingGroup(shippingGroupAddress);
        shippingPage.assignDeviceToShippingGroup(phoneToAdd.productName, shippingAddressFormatted, shippingGroupAddress.shipAttentionTo);
        shippingPage.saveChanges();

        shippingPage.getShippingGroup(shippingAddressFormatted, shippingGroupAddress.shipAttentionTo)
                .getAllShippingDevicesNames()
                .shouldHave(exactTextsCaseSensitiveInAnyOrder(phoneToAdd.name), ofSeconds(30));

        shippingPage.getShippingGroup(shippingAddressFormatted, shippingGroupAddress.shipAttentionTo)
                .getAssignedDevice(phoneToAdd.name)
                .getQuantityInput()
                .shouldHave(exactValue(valueOf(numberOfPhonesToAssign)), ofSeconds(30));

        var customAddressAssignments = enterpriseConnectionUtils.query(
                "SELECT Id, Quantity__c, QuoteLineItem__r.Product2.Name " +
                        "FROM CustomAddressAssignments__c " +
                        "WHERE QuoteLineItem__r.Quote.OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "'",
                CustomAddressAssignments__c.class);

        assertThat(customAddressAssignments)
                .as("Quantity of CustomAddressAssignments__c records in DB")
                .hasSizeGreaterThan(0);
        assertThat(customAddressAssignments.get(0).getQuantity__c())
                .as("CustomAddressAssignments__c.Quantity__c value")
                .isEqualTo(numberOfPhonesToAssign);
        assertThat(customAddressAssignments.get(0).getQuoteLineItem__r().getProduct2().getName())
                .as("CustomAddressAssignments__c.QuoteLineItem__r.Product2.Name value")
                .isEqualTo(phoneToAdd.productName);
    }

    /**
     * <p> - Open the Shipping tab and check that the assignment to the Shipping Group does not exist </p>
     * <p> - Check that CustomAddressAssignments__c records do not exist anymore
     **/
    private void checkThatPhoneAssignmentOnShippingGroupDoesNotExist() throws ConnectionException {
        shippingPage.openTab();

        shippingPage.getShippingGroup(shippingAddressFormatted, shippingGroupAddress.shipAttentionTo)
                .getAllShippingDevicesNames()
                .shouldHave(size(0), ofSeconds(30));

        var customAddressAssignments = enterpriseConnectionUtils.query(
                "SELECT Id " +
                        "FROM CustomAddressAssignments__c " +
                        "WHERE QuoteLineItem__r.Quote.OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "'",
                CustomAddressAssignments__c.class);

        assertThat(customAddressAssignments)
                .as("Quantity of CustomAddressAssignments__c records in DB")
                .hasSize(0);
    }
}
