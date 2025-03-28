package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Approval__c;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToInteger;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.LINK_TO_SIGNED_EVALUATION_AGREEMENT;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.POC_APPROVAL_RECORD_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.POC_QUOTE_RECORD_TYPE;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("P1")
@Tag("NGBS")
@Tag("QuoteTab")
@Tag("POC")
public class PocApprovalRequestTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Product digitalLineUnlimited;
    private final Product phoneToAdd;
    private final String linkToSignedEvaluationAgreement;

    public PocApprovalRequestTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_Annual_RegularAndPOC.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        phoneToAdd = data.getProductByDataName("LC_HD_687");
        linkToSignedEvaluationAgreement = LINK_TO_SIGNED_EVALUATION_AGREEMENT;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-9335")
    @TmsLink("CRM-19764")
    @DisplayName("CRM-9335 - 'Create POC Approval' button - Create Approval. \n" +
            "CRM-19764 - 'Number of POC Users' and 'Phones Amount' (DLs quantity > 0)")
    @Description("CRM-9335 - Verify that when user creates an approval via 'Create POC Approval' modal window " +
            "then Approval__c record is created that is linked to Account/Opportunity/Quote, " +
            "and a link to the signed Evaluation Agreement is copied to the Approval. \n" +
            "CRM-19764 - Verify that with creating POC Approval for POC Quote 'Number of POC Users' and 'Phones Amount' fields " +
            "on Approval record will be populated with the Quantity of DLs")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new POC quote, " +
                        "select a package for it, and save changes", () ->
                steps.quoteWizard.preparePocQuoteViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Add products on the Add Products tab", () ->
                steps.quoteWizard.addProductsOnProductsTab(phoneToAdd)
        );

        step("3. Open the Price Tab, change items' quantity, assign the devices to DLs, and save changes", () -> {
            cartPage.openTab();
            steps.cartTab.setUpQuantities(digitalLineUnlimited, phoneToAdd);
            steps.cartTab.assignDevicesToDLAndSave(phoneToAdd.name, digitalLineUnlimited.name, steps.quoteWizard.localAreaCode,
                    phoneToAdd.quantity);
        });

        step("4. Open the Quote Details tab, click 'Create POC Approval' button, " +
                "populate Link to the signed Evaluation Agreement field, and click 'Create' button", () -> {
            quotePage.openTab();
            quotePage.createPocApproval(linkToSignedEvaluationAgreement);
        });

        step("5. Check created 'POC Account' Approval__c record", () -> {
            var pocQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND RecordType.Name = '" + POC_QUOTE_RECORD_TYPE + "'",
                    Quote.class);

            var pocApproval = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Quote__c, Account__c, Opportunity__c, POC_Application_Form__c, " +
                            "Number_of_POC_Users__c, PhonesAmount__c " +
                            "FROM Approval__c " +
                            "WHERE Quote__c = '" + pocQuote.getId() + "' " +
                            "AND RecordType.Name = '" + POC_APPROVAL_RECORD_TYPE + "'",
                    Approval__c.class);

            //  CRM-9335
            assertThat(pocApproval.getQuote__c())
                    .as("Approval__c.Quote__c value")
                    .isEqualTo(pocQuote.getId());
            assertThat(pocApproval.getAccount__c())
                    .as("Approval__c.Account__c value")
                    .isEqualTo(steps.salesFlow.account.getId());
            assertThat(pocApproval.getOpportunity__c())
                    .as("Approval__c.Opportunity__c value")
                    .isEqualTo(steps.quoteWizard.opportunity.getId());
            assertThat(pocApproval.getPOC_Application_Form__c())
                    .as("Approval__c.POC_Application_Form__c value " +
                            "(link to the signed Evaluation Agreement)")
                    .isEqualTo(linkToSignedEvaluationAgreement);

            //  CRM-19764
            assertThat(doubleToInteger(pocApproval.getNumber_of_POC_Users__c()))
                    .as("Approval__c.Number_of_POC_Users__c value")
                    .isEqualTo(digitalLineUnlimited.quantity);
            assertThat(doubleToInteger(pocApproval.getPhonesAmount__c()))
                    .as("Approval__c.PhonesAmount__c value")
                    .isEqualTo(digitalLineUnlimited.quantity);
        });
    }
}
