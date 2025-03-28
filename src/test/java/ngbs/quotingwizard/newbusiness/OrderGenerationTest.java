package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.OrderItem;
import com.sforce.soap.enterprise.sobject.Quote;
import com.sforce.ws.ConnectionException;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import ngbs.quotingwizard.newbusiness.signup.SalesQuoteSignUpSteps;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OrderHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.Product2Helper.AUTO_WITH_CASE_INITIAL_ORDER_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.Product2Helper.MANUAL_INITIAL_ORDER_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.setQuoteToApprovedActiveAgreement;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("SignUp")
@Tag("MultiProduct-UB")
@Tag("LTR-569")
@Tag("Order")
public class OrderGenerationTest extends BaseTest {
    private final Steps steps;
    private final SalesQuoteSignUpSteps salesQuoteSignUpSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Map<String, Package> packageFolderNameToPackageMap;
    private final Product dlUnlimited;
    private final Product officePhoneWithInitialOrderTypeEmpty;
    private final Product productWithInitialOrderTypeAutoWithCase;
    private final Product productWithInitialOrderTypeManual;

    public OrderGenerationTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_Annual_Contract.json",
                Dataset.class);
        steps = new Steps(data);
        salesQuoteSignUpSteps = new SalesQuoteSignUpSteps();
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        packageFolderNameToPackageMap = Map.of(
                data.packageFolders[0].name, data.packageFolders[0].packages[0],
                data.packageFolders[2].name, data.packageFolders[2].packages[0]
        );

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        officePhoneWithInitialOrderTypeEmpty = data.getProductByDataName("LC_HD_523");
        productWithInitialOrderTypeManual = data.getProductByDataName("LC_SCN_582");
        productWithInitialOrderTypeAutoWithCase = data.getProductByDataName("LC_EVNT_1350005");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUserWithPermissionSet = salesQuoteSignUpSteps.getSalesRepUserWithAllowedProcessOrderWithoutShipping();

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserWithPermissionSet);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUserWithPermissionSet);
        salesQuoteSignUpSteps.loginAsSalesRepUserWithAllowedProcessOrderWithoutShipping();

        step("Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select MVP and Engage Digital packages for it", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectPackagesForMultiProductQuote(packageFolderNameToPackageMap);
        });

        step("Add necessary products on the Add Products tab, " +
                "open the Price tab, assign phone to DL, and save changes", () -> {
            steps.quoteWizard.addProductsOnProductsTab(officePhoneWithInitialOrderTypeEmpty,
                    productWithInitialOrderTypeAutoWithCase, productWithInitialOrderTypeManual);

            cartPage.openTab();
            steps.cartTab.assignDevicesToDLAndSave(officePhoneWithInitialOrderTypeEmpty.name, dlUnlimited.name, steps.quoteWizard.localAreaCode,
                    officePhoneWithInitialOrderTypeEmpty.quantity);
        });

        step("Open the Quote Details tab, set Start Date and Main Area Code, save changes", () -> {
            quotePage.openTab();
            quotePage.setDefaultStartDate();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.saveChanges();
        });

        step("Update the Master Quote to Active Agreement via API", () -> {
            var masterQuoteToUpdate = new Quote();
            masterQuoteToUpdate.setId(wizardPage.getSelectedQuoteId());
            setQuoteToApprovedActiveAgreement(masterQuoteToUpdate);
            enterpriseConnectionUtils.update(masterQuoteToUpdate);
        });

        step("Create Invoice Request Approval for the test Account " +
                "with related 'Accounts Payable' AccountContactRole record, " +
                "set Approval__c.Status = 'Approved' (all via API)", () ->
                createInvoiceApprovalApproved(steps.quoteWizard.opportunity, steps.salesFlow.account,
                        steps.salesFlow.contact, salesRepUserWithPermissionSet.getId(), true)
        );
    }

    @Test
    @TmsLink("CRM-36339")
    @DisplayName("CRM-36339 - Order Generation for New Business")
    @Description("Verify that Orders in 'New' status are generated for Multiproduct Unified Billing Opportunity " +
            "as the first step in Process Order for New Business. \n" +
            "- If there are QuoteLineItems with Product2.InitialOrderType__c = 'autoWithCase' or empty, " +
            "an order for auto provisioning should be created with Provisioning__c = 'auto'; \n" +
            "- If there are QuoteLineItems without Product2.InitialOrderType__c = 'autoWithCase' or empty " +
            "(e.g. Product2.InitialOrderType__c = 'manual'), " +
            "an order for manual provisioning should be created with Provisioning__c = 'manual'")
    public void test() {
        step("1. Open the Opportunity record page, click 'Process Order' button, " +
                "verify that 'Preparing Data' step is completed, and no errors are displayed on the modal window", () -> {
            opportunityPage.openPage(steps.quoteWizard.opportunity.getId());
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();
        });

        step("2. Check that there are different Order records " +
                "for products with InitialOrderType__c = 'autoWithCase'/empty and all the others, " +
                "that all Orders have Status = 'New', " +
                "and that each OrderItem.Order.Provisioning__c is correct " +
                "depending on the related OrderItem.Product2.InitialOrderType__c", () -> {
            var orderItemAutoWithCase = getOrderItem(productWithInitialOrderTypeAutoWithCase.dataName);
            var orderItemManual = getOrderItem(productWithInitialOrderTypeManual.dataName);
            var orderItemEmpty = getOrderItem(officePhoneWithInitialOrderTypeEmpty.dataName);

            var allOrderItems = List.of(orderItemAutoWithCase, orderItemManual, orderItemEmpty);
            allOrderItems.forEach(orderItem ->
                    assertThat(orderItem.getOrder().getStatus())
                            .as("OrderItem.Order.Status value for OrderId = " + orderItem.getOrderId())
                            .isEqualTo(NEW_STATUS)
            );

            checkInitialOrderTypeAndProvisioning(orderItemAutoWithCase, AUTO_WITH_CASE_INITIAL_ORDER_TYPE, AUTO_PROVISIONING);
            checkInitialOrderTypeAndProvisioning(orderItemEmpty, null, AUTO_PROVISIONING);
            checkInitialOrderTypeAndProvisioning(orderItemManual, MANUAL_INITIAL_ORDER_TYPE, MANUAL_PROVISIONING);

            step("Check that there are different orders for products with InitialOrderType__c = 'autoWithCase'/empty, and all the others", () -> {
                assertThat(orderItemManual.getOrderId())
                        .as("OrderItem.OrderId value for Product = " + orderItemManual.getProduct2().getName())
                        .isNotEqualTo(orderItemAutoWithCase.getOrderId())
                        .isNotEqualTo(orderItemEmpty.getOrderId());
            });
        });
    }

    /**
     * Get data for the OrderItem record using its external ID.
     *
     * @param dataName external ID from NGBS catalog for the product to check
     *                 (e.g. 'LC_EVNT_1350005')
     * @return OrderItem record with data for the related Product2 and Order data
     * @throws ConnectionException in case of errors while accessing API
     */
    private OrderItem getOrderItem(String dataName) throws ConnectionException {
        return enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id, OrderId, Order.Status, Order.Provisioning__c, " +
                        "Product2.InitialOrderType__c, Product2.Name " +
                        "FROM OrderItem " +
                        "WHERE Order.OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                        "AND QuoteLineItem.Product2.ExtID__c = '" + dataName + "'",
                OrderItem.class);
    }

    /**
     * Check that the OrderItem.Product2.InitialOrderType__c and OrderItem.Order.Provisioning__c values are correct.
     *
     * @param orderItem                OrderItem record with data for the related Product2 and Order data
     * @param expectedInitialOrderType expected value for Product2.InitialOrderType__c (e.g. 'autoWithCase')
     * @param expectedProvisioning     expected value for Order.Provisioning__c (e.g. 'auto')
     */
    private void checkInitialOrderTypeAndProvisioning(OrderItem orderItem, String expectedInitialOrderType, String expectedProvisioning) {
        step("Check Product2.InitialOrderType__c and Order.Provisioning__c for OrderItem with ID = " + orderItem.getId(), () -> {
            assertThat(orderItem.getProduct2().getInitialOrderType__c())
                    .as("OrderItem.Product2.InitialOrderType__c value for Product = " + orderItem.getProduct2().getName())
                    .isEqualTo(expectedInitialOrderType);
            assertThat(orderItem.getOrder().getProvisioning__c())
                    .as("OrderItem.Order.Provisioning__c value for OrderId = " + orderItem.getOrderId())
                    .isEqualTo(expectedProvisioning);
        });
    }
}
