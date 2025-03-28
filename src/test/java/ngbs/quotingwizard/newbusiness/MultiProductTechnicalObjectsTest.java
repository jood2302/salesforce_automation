package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteLineItemHelper.DISCOUNT_TYPE_CURRENCY;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteLineItemHelper.DISCOUNT_TYPE_PERCENTAGE;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("Opportunity")
@Tag("Multiproduct-Lite")
public class MultiProductTechnicalObjectsTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Quote masterQuote;
    private List<QuoteLineItem> masterQuoteLineItems;
    private List<Quote> techQuotes;
    private List<QuoteLineItem> techQuoteLineItems;

    private Quote masterNewlyAddedNonPrimaryQuote;
    private List<Quote> techNewlyAddedNonPrimaryQuotes;
    private List<QuoteLineItem> masterNewlyAddedNonPrimaryQlis;

    //  Test data
    private final Product[] productsToAdd;
    private final Product[] productsToUpdate;
    private final Product officeProductToAdd;
    private final Product officeProductToUpdate;

    private final List<String> allServicesList;

    private final Map<String, Package> packageFolderNameToPackageMap;

    public MultiProductTechnicalObjectsTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_Annual_Contract.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        var officePackageFolderName = data.packageFolders[0].name;
        var edPackageFolderName = data.packageFolders[2].name;
        var rcCcFolderName = data.packageFolders[3].name;

        var officePackage = data.packageFolders[0].packages[0];
        officeProductToAdd = data.getProductByDataName("LC_HD_523", officePackage);
        officeProductToUpdate = data.getProductByDataName("LC_DL-UNL_50", officePackage);
        officeProductToUpdate.quantity = 4;
        officeProductToUpdate.discount = 10;
        officeProductToUpdate.discountType = "USD";

        var engageDigitalPackage = data.packageFolders[2].packages[0];
        var edProductToAdd = data.getProductByDataName("SA_LINECRWHATSUP_11", engageDigitalPackage);
        var edProductToUpdate = data.getProductByDataName("SA_SEAT_5", engageDigitalPackage);
        edProductToUpdate.quantity = 6;
        edProductToUpdate.discount = 12;
        edProductToUpdate.discountType = "USD";

        var rcCcPackage = data.packageFolders[3].packages[0];
        var rcCcProductToAdd = data.getProductByDataName("CC_RCCCGD_576", rcCcPackage);
        var rcCcProductToUpdate = data.getProductByDataName("CC_RCCCSEAT_1", rcCcPackage);
        rcCcProductToUpdate.quantity = 7;
        rcCcProductToUpdate.discount = 13;
        rcCcProductToUpdate.discountType = "%";

        productsToAdd = new Product[]{officeProductToAdd, edProductToAdd, rcCcProductToAdd};
        productsToUpdate = new Product[]{officeProductToUpdate, edProductToUpdate, rcCcProductToUpdate};

        allServicesList = List.of(officePackageFolderName, edPackageFolderName, rcCcFolderName);

        packageFolderNameToPackageMap = Map.of(
                officePackageFolderName, officePackage,
                edPackageFolderName, engageDigitalPackage,
                rcCcFolderName, rcCcPackage
        );
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-27826")
    @TmsLink("CRM-27954")
    @TmsLink("CRM-27833")
    @TmsLink("CRM-29771")
    @TmsLink("CRM-27948")
    @DisplayName("CRM-27826 - Technical Objects auto creation (Primary Quote). \n" +
            "CRM-27954 - Technical Quotes, QLIs auto-creation (Non-Primary Quote). \n" +
            "CRM-27833 - Master Quote, QLI parameters. \n" +
            "CRM-29771 - Area_Code_Line_Item__c and AssignmentLineItem__c are synced between Master and Technical QLIs. \n" +
            "CRM-27948 - QLIs from Unified Quoting Tool are synced with Technical QLIs and Technical QLIs with Master QLIs (New Business)")
    @Description("CRM-27826 - Verify that technical Quotes and QLIs for any service (MVP, ED, EV, CC) " +
            "can be created automatically from UQT after choosing the services. \n" +
            "CRM-27954 - Verify that technical Quotes and QLIs for any service (ED, EV, CC) " +
            "can be created automatically from UQT after creation of new Non-Primary Quote. \n" +
            "CRM-27833 - Verify that Master Quote and QLIs have 'IsMultiProductTechnicalQuote__c' " +
            "and 'IsMultiProductTechnicalQLI__c' values that are equal to 'false' after Quote creation in UQT. \n" +
            "CRM-29771 - Verify that Area_Code_Line_Item__c and AssignmentLineItem__c are synced between Master and Technical QLIs. \n" +
            "CRM-27948 - Verify that saving changes in Price tab on Multiproduct Quote creates and updates QuoteLineItem records " +
            "on all related (Master and Technical) Quotes")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select Office, Engage Digital, Engage Voice and RingCentral Contact Center packages for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityForMultiProduct(steps.quoteWizard.opportunity.getId(), packageFolderNameToPackageMap)
        );

        //  CRM-27826
        step("2. Check that Master and Technical objects are created", () -> {
            masterQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, OpportunityId, Pricebook2Id, Status, Start_Date__c, End_Date__c, Auto_Renewal__c, " +
                            "Initial_Term_months__c, Approved_Status__c, Special_Terms__c, isPrimary__c, AreaCode__c, " +
                            "FaxAreaCode__c, Package_Info__c, PaymentMethod__c, IsMultiProductTechnicalQuote__c, MasterQuote__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND MasterQuote__c = '" + EMPTY_STRING + "'",
                    Quote.class);
            masterQuoteLineItems = enterpriseConnectionUtils.query(
                    "SELECT Id, QuoteId, PricebookEntryId, Quantity, UnitPrice, Discount, Product2Id, " +
                            "Discount_type__c, NewQuantity__c, IsMultiProductTechnicalQLI__c, MasterQLI__c, Display_Name__c " +
                            "FROM QuoteLineItem " +
                            "WHERE QuoteId = '" + masterQuote.getId() + "' ",
                    QuoteLineItem.class);

            techQuotes = enterpriseConnectionUtils.query(
                    "SELECT Id, ServiceName__c, OpportunityId, Pricebook2Id, Status, Start_Date__c, End_Date__c, " +
                            "Auto_Renewal__c, Initial_Term_months__c, Approved_Status__c, Special_Terms__c, IsPrimary__c, " +
                            "AreaCode__c, FaxAreaCode__c, Package_Info__c, PaymentMethod__c, " +
                            "IsMultiProductTechnicalQuote__c, MasterQuote__c " +
                            "FROM Quote " +
                            "WHERE MasterQuote__c = '" + masterQuote.getId() + "'",
                    Quote.class);
            assertThat(techQuotes.size())
                    .as("Quantity of technical Quotes")
                    .isEqualTo(allServicesList.size());

            techQuoteLineItems = enterpriseConnectionUtils.query(
                    "SELECT Id, QuoteId, PricebookEntryId, Quantity, UnitPrice, Discount, Product2Id, " +
                            "Discount_type__c, NewQuantity__c, IsMultiProductTechnicalQLI__c, MasterQLI__c, Display_Name__c " +
                            "FROM QuoteLineItem " +
                            "WHERE MasterQLI__c IN " + getSObjectIdsListAsString(masterQuoteLineItems),
                    QuoteLineItem.class);
            assertThat(techQuoteLineItems.size())
                    .as("Quantity of technical Quote Line Items")
                    .isEqualTo(masterQuoteLineItems.size());
        });

        //  CRM-27833
        step("3. Check that Master Quote has IsMultiProductTechnicalQuote__c = false and IsPrimary__c = true, " +
                "and all Master QLIs have IsMultiProductTechnicalQLI__c = false", () -> {
            assertThat(masterQuote.getIsMultiProductTechnicalQuote__c())
                    .as("Master Quote.IsMultiProductTechnicalQuote__c value")
                    .isFalse();
            assertThat(masterQuote.getIsPrimary__c())
                    .as("Master Quote.IsPrimary__c value")
                    .isTrue();

            for (var masterQli : masterQuoteLineItems) {
                step("Check the Master QuoteLineItem.IsMultiProductTechnicalQLI__c for " + masterQli.getDisplay_Name__c(), () -> {
                    assertThat(masterQli.getIsMultiProductTechnicalQLI__c())
                            .as("QuoteLineItem.IsMultiProductTechnicalQLI__c value")
                            .isFalse();
                });
            }
        });

        //  CRM-27826
        step("4. Check that technical Quotes for all Services have Quote.IsPrimary__c = false", () -> {
            for (var nonOfficeTechQuote : techQuotes) {
                step("Check the Tech Quote.IsPrimary__c for service = " + nonOfficeTechQuote.getServiceName__c(), () -> {
                    assertThat(nonOfficeTechQuote.getIsPrimary__c())
                            .as("Quote.IsPrimary__c value")
                            .isFalse();
                });
            }
        });

        //  CRM-27826
        step("5. Check that QuoteLineItem.MasterQLI__c field is populated with Master QLIs Id for all technical QLIs", () -> {
            //  Sort Master and Technical QLIs collections to have the same order of elements
            masterQuoteLineItems.sort(comparing(QuoteLineItem::getDisplay_Name__c));
            techQuoteLineItems.sort(comparing(QuoteLineItem::getDisplay_Name__c));

            for (var techQli : techQuoteLineItems) {
                step("Check the Tech QuoteLineItem.IsMultiProductTechnicalQLI__c for " + techQli.getDisplay_Name__c(), () -> {
                    var masterQli = masterQuoteLineItems.get(techQuoteLineItems.indexOf(techQli));
                    assertThat(techQli.getMasterQLI__c())
                            .as("QuoteLineItem.MasterQLI__c value")
                            .isEqualTo(masterQli.getId());
                });
            }
        });

        step("6. Open the Quote Wizard to add a new Sales Quote, " +
                "select Office, Engage Digital, and RingCentral Contact Center packages for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityForMultiProduct(steps.quoteWizard.opportunity.getId(), packageFolderNameToPackageMap)
        );

        //  CRM-27954
        step("7. Check that Master Non-Primary Quote is created " +
                "and some of its fields have the same values as on the Master Primary Quote", () -> {
            var masterNonPrimaryQuotes = enterpriseConnectionUtils.query(
                    "SELECT Id, OpportunityId, Pricebook2Id, Status, Start_Date__c, End_Date__c, Auto_Renewal__c, " +
                            "Initial_Term_months__c, Approved_Status__c, Special_Terms__c, isPrimary__c, AreaCode__c, " +
                            "FaxAreaCode__c, Package_Info__c, PaymentMethod__c, IsMultiProductTechnicalQuote__c, MasterQuote__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND MasterQuote__c = '" + EMPTY_STRING + "' " +
                            "AND IsPrimary__c = false",
                    Quote.class);
            assertThat(masterNonPrimaryQuotes.size())
                    .as("Quantity of newly added Master Non-Primary Quotes")
                    .isEqualTo(1);
            masterNewlyAddedNonPrimaryQuote = masterNonPrimaryQuotes.get(0);

            step("Check new Master Non-Primary Quote's fields against the original Master Primary Quote", () ->
                    checkFieldsOnQuote(masterQuote, masterNewlyAddedNonPrimaryQuote)
            );
        });

        //  CRM-27954
        step("8. Check that QuoteLineItems for the newly added Master Non-Primary Quote are created " +
                "and some their fields have the same values as on the Primary Master QLIs", () -> {
            masterNewlyAddedNonPrimaryQlis = enterpriseConnectionUtils.query(
                    "SELECT Id, QuoteId, PricebookEntryId, Quantity, UnitPrice, Discount, Product2Id, " +
                            "Discount_type__c, NewQuantity__c, IsMultiProductTechnicalQLI__c, MasterQLI__c, Display_Name__c " +
                            "FROM QuoteLineItem " +
                            "WHERE QuoteId = '" + masterNewlyAddedNonPrimaryQuote.getId() + "'",
                    QuoteLineItem.class);
            assertThat(masterNewlyAddedNonPrimaryQlis.size())
                    .as("Quantity of newly added Master Non-Primary Quote Line Items")
                    .isEqualTo(masterQuoteLineItems.size());

            for (var masterNewlyAddedNonPrimaryQli : masterNewlyAddedNonPrimaryQlis) {
                step("Check the newly added Master Non-Primary QuoteLineItem.QuoteId for " +
                        masterNewlyAddedNonPrimaryQli.getDisplay_Name__c(), () -> {
                    assertThat(masterNewlyAddedNonPrimaryQli.getQuoteId())
                            .as("QuoteLineItem.QuoteId value")
                            .isEqualTo(masterNewlyAddedNonPrimaryQuote.getId());
                });
            }

            checkQuoteLineItemsFields(masterQuoteLineItems, masterNewlyAddedNonPrimaryQlis);
        });

        //  CRM-27954
        step("9. Check that Technical Non-Primary Quotes are created and some of their fields have the same values " +
                "as on the Primary Technical Quotes", () -> {
            techNewlyAddedNonPrimaryQuotes = enterpriseConnectionUtils.query(
                    "SELECT Id, ServiceName__c, OpportunityId, Pricebook2Id, Status, Start_Date__c, End_Date__c, " +
                            "Auto_Renewal__c, Initial_Term_months__c, Approved_Status__c, Special_Terms__c, isPrimary__c, " +
                            "AreaCode__c, FaxAreaCode__c, Package_Info__c, PaymentMethod__c, " +
                            "IsMultiProductTechnicalQuote__c, MasterQuote__c " +
                            "FROM Quote " +
                            "WHERE MasterQuote__c = '" + masterNewlyAddedNonPrimaryQuote.getId() + "'",
                    Quote.class);
            assertThat(techNewlyAddedNonPrimaryQuotes.size())
                    .as("Quantity of newly added Technical Non-Primary Quotes")
                    .isEqualTo(techQuotes.size());

            //  Sort both Technical Quote collections to have the same order of Quotes
            techQuotes.sort(comparing(Quote::getServiceName__c));
            techNewlyAddedNonPrimaryQuotes.sort(comparing(Quote::getServiceName__c));

            for (var techNewlyAddedNonPrimaryQuote : techNewlyAddedNonPrimaryQuotes) {
                step("Check Technical Non-Primary Quote for service = " + techNewlyAddedNonPrimaryQuote.getServiceName__c(), () -> {
                    var techQuote = techQuotes.get(techNewlyAddedNonPrimaryQuotes.indexOf(techNewlyAddedNonPrimaryQuote));
                    checkFieldsOnQuote(techQuote, techNewlyAddedNonPrimaryQuote);
                });
            }
        });

        //  CRM-27954
        step("10. Check that QuoteLineItems on newly added Technical Non-Primary Quotes are created " +
                "and some of their fields have the same values as on the Primary Technical QLIs", () -> {
            var techNewlyAddedNonPrimaryQlis = enterpriseConnectionUtils.query(
                    "SELECT Id, QuoteId, PricebookEntryId, Quantity, UnitPrice, Discount, Product2Id, " +
                            "Discount_type__c, NewQuantity__c, IsMultiProductTechnicalQLI__c, MasterQLI__c, Display_Name__c " +
                            "FROM QuoteLineItem " +
                            "WHERE MasterQLI__c IN " + getSObjectIdsListAsString(masterNewlyAddedNonPrimaryQlis),
                    QuoteLineItem.class);
            assertThat(techNewlyAddedNonPrimaryQlis.size())
                    .as("Quantity of newly added Tech Non-Primary Quote Line Items")
                    .isEqualTo(techQuoteLineItems.size());

            checkQuoteLineItemsFields(techQuoteLineItems, techNewlyAddedNonPrimaryQlis);
        });

        step("11. Open the Add Products tab and add new Office, ED, and RC CC products to the cart", () ->
                steps.quoteWizard.addProductsOnProductsTab(productsToAdd)
        );

        step("12. Open the Price tab, set up quantities and discounts on the already existing items, " +
                "assign added phone to DL, and save changes", () -> {
            cartPage.openTab();
            steps.cartTab.setUpQuantities(productsToUpdate);
            steps.cartTab.setUpDiscounts(productsToUpdate);

            steps.cartTab.assignDevicesToDLAndSave(officeProductToAdd.name, officeProductToUpdate.name, steps.quoteWizard.localAreaCode,
                    officeProductToAdd.quantity);
        });

        //  CRM-27948
        step("13. Check all the created and updated QuoteLineItem records in DB", () -> {
            var masterQuoteId = wizardPage.getSelectedQuoteId();

            Stream.concat(stream(productsToAdd), stream(productsToUpdate)).forEach(product -> {
                step("Check the number of QuoteLineItem records and its fields for " + product.name, () -> {
                    var quoteLineItems = enterpriseConnectionUtils.query(
                            "SELECT Id, Quantity, NewQuantity__c, " +
                                    "Discount_Type__c, Discount_Number__c " +
                                    "FROM QuoteLineItem " +
                                    "WHERE (" +
                                    "QuoteId = '" + masterQuoteId + "' " +
                                    "OR Quote.MasterQuote__c = '" + masterQuoteId + "' " +
                                    ")" +
                                    "AND Display_Name__c = '" + product.name + "'",
                            QuoteLineItem.class);
                    assertThat(quoteLineItems.size())
                            .as(format("Number of QuoteLineItem records for a '%s' license in DB", product.name))
                            .isEqualTo(2);

                    quoteLineItems.forEach(qli -> {
                        assertThat(qli.getQuantity())
                                .as("QuoteLineItem.Quantity value for " + product.name)
                                .isEqualTo(Double.valueOf(product.quantity));
                        assertThat(qli.getNewQuantity__c())
                                .as("QuoteLineItem.NewQuantity__c value for " + product.name)
                                .isEqualTo(Double.valueOf(product.quantity));
                        assertThat(qli.getDiscount_number__c())
                                .as("QuoteLineItem.Discount_number__c value for " + product.name)
                                .isEqualTo(Double.valueOf(product.discount));

                        var expectedDiscountType = product.discountType.equals(PERCENT) ?
                                DISCOUNT_TYPE_PERCENTAGE :
                                DISCOUNT_TYPE_CURRENCY;
                        assertThat(qli.getDiscount_type__c())
                                .as("QuoteLineItem.Discount_type__c value for " + product.name)
                                .isEqualTo(expectedDiscountType);
                    });
                });
            });
        });

        step("14. Open the Quote Details tab, populate Main Area Code field, and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.saveChanges();
        });

        //  CRM-29771
        step("15. Check that the same Area_Code_Line_Item__c records are created for both Master and Technical Quotes", () -> {
            var masterQuoteId = wizardPage.getSelectedQuoteId();

            var masterAreaCodeLineItems = enterpriseConnectionUtils.query(
                    "SELECT Area_Code__c, Quantity__c, Quote_Line_Item__r.Display_Name__c " +
                            "FROM Area_Code_Line_Item__c " +
                            "WHERE Quote_Line_Item__r.Quote.Id = '" + masterQuoteId + "' " +
                            "ORDER BY Quote_Line_Item__r.Display_Name__c",
                    Area_Code_Line_Item__c.class);
            var techAreaCodeLineItems = enterpriseConnectionUtils.query(
                    "SELECT Area_Code__c, Quantity__c, Quote_Line_Item__r.Display_Name__c " +
                            "FROM Area_Code_Line_Item__c " +
                            "WHERE Quote_Line_Item__r.Quote.MasterQuote__c = '" + masterQuoteId + "' " +
                            "ORDER BY Quote_Line_Item__r.Display_Name__c",
                    Area_Code_Line_Item__c.class);

            assertThat(masterAreaCodeLineItems.size())
                    .as("Quantity of Master Area_Code_Line_Item__c records (should match Tech's)")
                    .isEqualTo(techAreaCodeLineItems.size());
            for (var masterAreaCodeLineItem : masterAreaCodeLineItems) {
                step(format("Check Master Area_Code_Line_Item__c for '%s' against the corresponding Tech Area_Code_Line_Item__c",
                        masterAreaCodeLineItem.getQuote_Line_Item__r().getDisplay_Name__c()), () -> {
                    var techAreaCodeLineItem = techAreaCodeLineItems.get(masterAreaCodeLineItems.indexOf(masterAreaCodeLineItem));

                    assertThat(masterAreaCodeLineItem.getArea_Code__c())
                            .as("Master Area_Code_Line_Item__c.Area_Code__c value")
                            .isEqualTo(techAreaCodeLineItem.getArea_Code__c());
                    assertThat(masterAreaCodeLineItem.getQuantity__c())
                            .as("Master Area_Code_Line_Item__c.Quantity__c value")
                            .isEqualTo(techAreaCodeLineItem.getQuantity__c());
                    assertThat(masterAreaCodeLineItem.getQuote_Line_Item__r().getDisplay_Name__c())
                            .as("Master Area_Code_Line_Item__c.Quote_Line_Item__r.Display_Name__c value")
                            .isEqualTo(techAreaCodeLineItem.getQuote_Line_Item__r().getDisplay_Name__c());
                });
            }
        });

        //  CRM-29771
        step("16. Check that the same AssignmentLineItem__c records are created for both Master and Technical Quotes", () -> {
            var masterQuoteId = wizardPage.getSelectedQuoteId();

            var masterAssignmentLineItems = enterpriseConnectionUtils.query(
                    "SELECT AreaCode__c, Child__r.Display_Name__c, Parent__r.Display_Name__c, Quantity__c " +
                            "FROM AssignmentLineItem__c " +
                            "WHERE Parent__r.QuoteId = '" + masterQuoteId + "' " +
                            "AND Child__r.QuoteId = '" + masterQuoteId + "'",
                    AssignmentLineItem__c.class);
            var techAssignmentLineItems = enterpriseConnectionUtils.query(
                    "SELECT AreaCode__c, Child__r.Display_Name__c, Parent__r.Display_Name__c, Quantity__c " +
                            "FROM AssignmentLineItem__c " +
                            "WHERE Parent__r.Quote.MasterQuote__c = '" + masterQuoteId + "' " +
                            "AND Child__r.Quote.MasterQuote__c = '" + masterQuoteId + "'",
                    AssignmentLineItem__c.class);

            assertThat(masterAssignmentLineItems.size())
                    .as("Quantity of Master AssignmentLineItem__c records (should match Tech's)")
                    .isEqualTo(techAssignmentLineItems.size());
            for (var masterAssignmentLineItem : masterAssignmentLineItems) {
                step(format("Check Master AssignmentLineItem__c for '%s' against the corresponding Tech AssignmentLineItem__c",
                        masterAssignmentLineItem.getParent__r().getDisplay_Name__c()), () -> {
                    var techAssignmentLineItem = techAssignmentLineItems.get(masterAssignmentLineItems.indexOf(masterAssignmentLineItem));

                    assertThat(masterAssignmentLineItem.getAreaCode__c())
                            .as("Master AssignmentLineItem__c.AreaCode__c value")
                            .isEqualTo(techAssignmentLineItem.getAreaCode__c());
                    assertThat(masterAssignmentLineItem.getChild__r().getDisplay_Name__c())
                            .as("Master AssignmentLineItem__c.Child__r.Display_Name__c value")
                            .isEqualTo(techAssignmentLineItem.getChild__r().getDisplay_Name__c());
                    assertThat(masterAssignmentLineItem.getParent__r().getDisplay_Name__c())
                            .as("Master AssignmentLineItem__c.Parent__r.Display_Name__c value")
                            .isEqualTo(techAssignmentLineItem.getParent__r().getDisplay_Name__c());
                    assertThat(masterAssignmentLineItem.getQuantity__c())
                            .as("Master AssignmentLineItem__c.Quantity__c value")
                            .isEqualTo(techAssignmentLineItem.getQuantity__c());
                });
            }
        });

        step("17. Open the Quote Selection page and make the current Quote primary", () -> {
            steps.quoteWizard.openQuoteWizardDirect(steps.quoteWizard.opportunity.getId());
            quoteSelectionWizardPage.makeQuotePrimary(masterNewlyAddedNonPrimaryQuote.getId());
        });

        //  CRM-27954
        step("18. Check that newly added Master Quote became primary", () -> {
            var masterNewlyAddedQuoteUpdated = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, IsPrimary__c " +
                            "FROM Quote " +
                            "WHERE Id = '" + masterNewlyAddedNonPrimaryQuote.getId() + "'",
                    Quote.class);
            assertThat(masterNewlyAddedQuoteUpdated.getIsPrimary__c())
                    .as("New Master Quote.IsPrimary__c value")
                    .isTrue();
        });
    }

    /**
     * Check that some fields on a new created quote have the same values as on the original Quote.
     *
     * @param originalQuote an original Quote that was created initially
     * @param newQuote      a newly added Quote
     */
    private void checkFieldsOnQuote(Quote originalQuote, Quote newQuote) {
        assertThat(newQuote.getOpportunityId())
                .as("New Quote.OpportunityId value")
                .isEqualTo(originalQuote.getOpportunityId());
        assertThat(newQuote.getPricebook2Id())
                .as("New Quote.Pricebook2Id value")
                .isEqualTo(originalQuote.getPricebook2Id());
        assertThat(newQuote.getStatus())
                .as("New Quote.Status value")
                .isEqualTo(originalQuote.getStatus());
        assertThat(newQuote.getStart_Date__c())
                .as("New Quote.Start_Date__c value")
                .isEqualTo(originalQuote.getStart_Date__c());
        assertThat(newQuote.getEnd_Date__c())
                .as("New Quote.End_Date__c value")
                .isEqualTo(originalQuote.getEnd_Date__c());
        assertThat(newQuote.getAuto_Renewal__c())
                .as("New Quote.Auto_Renewal__c value")
                .isEqualTo(originalQuote.getAuto_Renewal__c());
        assertThat(newQuote.getInitial_Term_months__c())
                .as("New Quote.Initial_Term_months__c value")
                .isEqualTo(originalQuote.getInitial_Term_months__c());
        assertThat(newQuote.getApproved_Status__c())
                .as("New Quote.Approved_Status__c value")
                .isEqualTo(originalQuote.getApproved_Status__c());
        assertThat(newQuote.getSpecial_Terms__c())
                .as("New Quote.Special_Terms__c value")
                .isEqualTo(originalQuote.getSpecial_Terms__c());
        assertThat(newQuote.getAreaCode__c())
                .as("New Quote.AreaCode__c value")
                .isEqualTo(originalQuote.getAreaCode__c());
        assertThat(newQuote.getFaxAreaCode__c())
                .as("New Quote.FaxAreaCode__c value")
                .isEqualTo(originalQuote.getFaxAreaCode__c());
        assertThat(newQuote.getPackage_Info__c())
                .as("New Quote.Package_Info__c value")
                .isEqualTo(originalQuote.getPackage_Info__c());
        assertThat(newQuote.getPaymentMethod__c())
                .as("New Quote.PaymentMethod__c value")
                .isEqualTo(originalQuote.getPaymentMethod__c());
        assertThat(newQuote.getIsMultiProductTechnicalQuote__c())
                .as("New Quote.IsMultiProductTechnicalQuote__c value")
                .isEqualTo(originalQuote.getIsMultiProductTechnicalQuote__c());
    }

    /**
     * Check that some fields on new QuoteLineItems have the same values
     * as on the corresponding original QuoteLineItems.
     *
     * @param originalQlis original QuoteLineItems that were created initially
     * @param newQlis      newly added QuoteLineItems
     */
    private void checkQuoteLineItemsFields(List<QuoteLineItem> originalQlis, List<QuoteLineItem> newQlis) {
        //  Sort original and new QLI collections to have the same order of elements
        originalQlis.sort(comparing(QuoteLineItem::getDisplay_Name__c));
        newQlis.sort(comparing(QuoteLineItem::getDisplay_Name__c));

        for (var originalQli : originalQlis) {
            step("Check new QuoteLineItem's fields against the corresponding original QLI " +
                    "for " + originalQli.getDisplay_Name__c(), () -> {
                var newQli = newQlis.get(originalQlis.indexOf(originalQli));

                assertThat(newQli.getPricebookEntryId())
                        .as("New QuoteLineItem.PricebookEntryId value")
                        .isEqualTo(originalQli.getPricebookEntryId());
                assertThat(newQli.getQuantity())
                        .as("New QuoteLineItem.Quantity value")
                        .isEqualTo(originalQli.getQuantity());
                assertThat(newQli.getUnitPrice())
                        .as("New QuoteLineItem.UnitPrice value")
                        .isEqualTo(originalQli.getUnitPrice());
                assertThat(newQli.getDiscount())
                        .as("New QuoteLineItem.Discount value")
                        .isEqualTo(originalQli.getDiscount());
                assertThat(newQli.getProduct2Id())
                        .as("New QuoteLineItem.Product2Id value")
                        .isEqualTo(originalQli.getProduct2Id());
                assertThat(newQli.getNewQuantity__c())
                        .as("New QuoteLineItem.NewQuantity__c value")
                        .isEqualTo(originalQli.getNewQuantity__c());
                assertThat(newQli.getIsMultiProductTechnicalQLI__c())
                        .as("New QuoteLineItem.IsMultiProductTechnicalQLI__c value")
                        .isEqualTo(originalQli.getIsMultiProductTechnicalQLI__c());
                assertThat(newQli.getDiscount_type__c())
                        .as("New QuoteLineItem.Discount_type__c value")
                        .isEqualTo(originalQli.getDiscount_type__c());
            });
        }
    }
}
