package ngbs.quotingwizard.existingbusiness.carttab;

import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Area_Code_Line_Item__c;
import com.sforce.soap.enterprise.sobject.QuoteLineItem;
import ngbs.quotingwizard.CartTabSteps;
import ngbs.quotingwizard.QuoteWizardSteps;

import static base.Pages.cartPage;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToInteger;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test methods for test cases related to check Save process on {@link CartPage}
 * for the Existing Business Customer's quotes.
 */
public class SaveButtonCartTabExistingBusinessSteps {
    private final QuoteWizardSteps quoteWizardSteps;
    private final CartTabSteps cartTabSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private int dluNewQuantity;

    //  Test data
    public final Product phoneToAdd;

    /**
     * New instance for test methods related to check Save process on {@link CartPage}
     * for the Existing Business Customer's quotes.
     *
     * @param data object parsed from the JSON files with the test data
     */
    public SaveButtonCartTabExistingBusinessSteps(Dataset data) {
        quoteWizardSteps = new QuoteWizardSteps(data);
        cartTabSteps = new CartTabSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        phoneToAdd = data.getProductByDataName("LC_HDRF_590");
    }

    /**
     * Add the same number of DigitalLines as for the Phones.
     *
     * @param digitalLine   DigitalLine product to add
     * @param opportunityId ID of Opportunity that the Sales Quote relates to
     * @throws Exception in case of malformed query, DB or network errors.
     */
    public void addDigitalLinesAsMuchAsPhones(Product digitalLine, String opportunityId) throws Exception {
        var dlUnlimitedQLI = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id, NewQuantity__c " +
                        "FROM QuoteLineItem " +
                        "WHERE Quote.OpportunityId = '" + opportunityId + "' " +
                        "AND Product2.ExtId__c = '" + digitalLine.dataName + "'",
                QuoteLineItem.class);
        assertThat(dlUnlimitedQLI.getNewQuantity__c())
                .as("QuoteLineItem.NewQuantity__c value for " + digitalLine.name)
                .isNotNull();

        //  Increasing DL quantity by number of added Phones
        dluNewQuantity = doubleToInteger(dlUnlimitedQLI.getNewQuantity__c()) + phoneToAdd.quantity;

        cartPage.setNewQuantityForQLItem(digitalLine.name, dluNewQuantity);
        cartPage.setNewQuantityForQLItem(phoneToAdd.name, phoneToAdd.quantity);

        cartPage.getQliFromCartByDisplayName(digitalLine.name)
                .getDeviceAssignmentButton()
                .shouldBe(visible);
    }

    /**
     * Assign Phones to DigitalLines with Area codes and save changes.
     *
     * @param digitalLine DigitalLine product to assign with phone.
     */
    public void assignDevicesToDigitalLinesAndSave(Product digitalLine) {
        cartTabSteps.assignDevicesToDLAndSave(phoneToAdd.name, digitalLine.name, quoteWizardSteps.localAreaCode, phoneToAdd.quantity);

        cartPage.getQliFromCartByDisplayName(digitalLine.name).getNumberAssignmentLineItems()
                .shouldHave(text("1"));
        cartPage.getQliFromCartByDisplayName(phoneToAdd.name).getNumberAssignmentLineItems()
                .shouldHave(text("1"));
    }

    /**
     * Check that Quote Line Items are updated correctly,
     * and Area Code Line Item record has the correct quantity.
     *
     * @param digitalLine   DigitalLine product to check.
     * @param opportunityId ID of Opportunity that the Sales Quote relates to
     * @throws Exception in case of malformed query, DB or network errors.
     */
    public void checkUpdatedQliAndAreaCodeLineItem(Product digitalLine, String opportunityId) throws Exception {
        var qliList = enterpriseConnectionUtils.query(
                "SELECT Id, Product2.ExtId__c, Quantity, NewQuantity__c " +
                        "FROM QuoteLineItem " +
                        "WHERE Quote.OpportunityId = '" + opportunityId + "'",
                QuoteLineItem.class);

        var phoneToAddQLIActual = qliList.stream()
                .filter(qli -> qli.getProduct2().getExtID__c().equals(phoneToAdd.dataName))
                .findFirst();

        //  QLI for Phone is present and has correct quantity
        assertThat(phoneToAddQLIActual)
                .as("Quote Line Item for " + phoneToAdd.name)
                .isPresent();
        assertThat(doubleToInteger(phoneToAddQLIActual.get().getQuantity()))
                .as("QuoteLineItem.Quantity value for " + phoneToAdd.name)
                .isEqualTo(phoneToAdd.quantity);

        var digitalLineQLIActual = qliList.stream()
                .filter(qli -> qli.getProduct2().getExtID__c().equals(digitalLine.dataName))
                .findFirst();

        //  DL quantity is as was set previously
        assertThat(digitalLineQLIActual)
                .as("Quote Line Item for " + digitalLine.name)
                .isPresent();
        //  Actual quantity for DLs on Quote should be equal to the number of added Phones
        assertThat(doubleToInteger(digitalLineQLIActual.get().getQuantity()))
                .as("QuoteLineItem.Quantity value for " + digitalLine.name)
                .isEqualTo(phoneToAdd.quantity);
        assertThat(doubleToInteger(digitalLineQLIActual.get().getNewQuantity__c()))
                .as("QuoteLineItem.NewQuantity__c value for " + digitalLine.name)
                .isEqualTo(dluNewQuantity);

        //  Area Code Line item is present with correct data
        var areaCodeLineItem = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id, Quantity__c " +
                        "FROM Area_Code_Line_Item__c " +
                        "WHERE Quote_Line_Item__r.Quote.OpportunityId = '" + opportunityId + "' " +
                        "AND Quote_Line_Item__r.Product2.ExtId__c = '" + phoneToAdd.dataName + "'",
                Area_Code_Line_Item__c.class);

        assertThat(doubleToInteger(areaCodeLineItem.getQuantity__c()))
                .as("Area_Code_Line_Item__c.Quantity__c value for " + phoneToAdd.name)
                .isEqualTo(phoneToAdd.quantity);
    }
}
