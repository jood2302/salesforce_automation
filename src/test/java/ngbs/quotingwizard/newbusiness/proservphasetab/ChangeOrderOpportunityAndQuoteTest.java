package ngbs.quotingwizard.newbusiness.proservphasetab;

import base.BaseTest;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Order;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import ngbs.quotingwizard.ProServInNgbsSteps;
import org.junit.jupiter.api.*;

import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal.BillingDetailsAndTermsModal.NOT_AVAILABLE_FOR_CHANGE_ORDER_TOOLTIP;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.psphasestab.PsPhasesPage.PHASE_SECTION_NAME_FORMAT;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.*;
import static com.codeborne.selenide.CollectionCondition.*;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("LTR-121")
@Tag("ProServInNGBS")
public class ChangeOrderOpportunityAndQuoteTest extends BaseTest {
    private final Dataset data;
    private final ProServInNgbsSteps proServInNgbsSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private String changeOrderOpportunityId;
    private String proServOrderId;
    private String changeOrderQuoteId;

    private final List<String> proServProductGroups;
    private final List<String> changeOrderQuoteTabs;

    public ChangeOrderOpportunityAndQuoteTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ProServ_Monthly_Contract.json",
                Dataset.class);

        proServInNgbsSteps = new ProServInNgbsSteps(data, data.packageFolders[1]);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        changeOrderQuoteTabs = List.of("Select Package", "Add Products", "Price", "PS Phases", "Quote Details");
        proServProductGroups = List.of("UC", "CC");
    }

    @BeforeEach
    public void setUpTest() {
        proServInNgbsSteps.initSignUpOpportunityWithProServServiceInNgbs();
    }

    @Test
    @TmsLink("CRM-37465")
    @TmsLink("CRM-37493")
    @TmsLink("CRM-37532")
    @DisplayName("CRM-37465 - Change Order Opportunity creation. \n" +
            "CRM-37493 - Change Order Quote creation. \n" +
            "CRM-37532 - Change Order Quote and Phase line items creation")
    @Description("CRM-37465 - Verify that: \n" +
            "- Opportunity and Quote are created by clicking the 'Change Order' button in the 'ProServ Project' object. \n" +
            "- Legacy ProServ flow should be enabled If there are no existing Suborders on the Order. \n" +
            "- New ProServ flow should be enabled If there are existing Suborders on the Order. \n\n" +
            "CRM-37493 - Verify that: \n" +
            "- The ProServ Quote has been created and saved in the Change Order Opportunity. The Quote is pre-filled with data from NGBS billing account. \n" +
            "- Select Package section is disabled. Only the Proserv package is displayed in the 'Select package'. \n" +
            "- The Billing Details Section displays the current data from NGBS and is set to view-only mode (grayed out). The Service field is active. \n" +
            "- Billing Details and Terms button is disabled with a Tooltip. \n\n" +
            "CRM-37532 - Verify that: \n" +
            "- Quote Line items created based on order/suborder. \n" +
            "- Phase and Phase Line Items are created based on the Suborders in the 'New' Status. Each Phase is linked to the Quote. \n" +
            "- The 'New Total Quantity', 'Delivered Quantity' and 'Existing Quantity' fields are present")
    public void test() {
        //  CRM-37465
        step("1. Open the ProServ Project, open Change Order modal, " +
                "and check that Change Order opportunity is created in SFDC", () -> {
            var proServProject = proServInNgbsSteps.getProServProject(proServInNgbsSteps.account.getId());
            var changeOrderOpportunity = proServInNgbsSteps.createChangeOrderFromProServProject(proServInNgbsSteps.account.getId(), proServProject.getId());
            changeOrderOpportunityId = changeOrderOpportunity.getId();

            //  e.g. "AccountName123 (Change Order) 123"
            var changeOrderOpportunityNamePattern = Pattern.compile("^" + proServInNgbsSteps.account.getName() + " \\(Change Order\\) \\d+$");
            assertThat(changeOrderOpportunity.getName())
                    .as("Change Order Opportunity.Name value")
                    .matches(changeOrderOpportunityNamePattern);
        });

        //  CRM-37465
        step("2. Check RelatedChangeOpportunity__c value on the related Order record", () -> {
            var proServOrder = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, RelatedChangeOpportunity__c " +
                            "FROM Order " +
                            "WHERE AccountId = '" + proServInNgbsSteps.account.getId() + "' " +
                            "AND RecordType.Name = '" + proServInNgbsSteps.proServService + "'",
                    Order.class);
            assertThat(proServOrder.getRelatedChangeOpportunity__c())
                    .as("Order.RelatedChangeOpportunity__c value")
                    .isEqualTo(changeOrderOpportunityId);

            proServOrderId = proServOrder.getId();
        });

        //  CRM-37465
        step("3. Check the Change Order Opportunity fields' values", () -> {
            var changeOrderOpportunity = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, RecordType.Name, CloseDate, Is_Billing_Opportunity__c, " +
                            "StageName, Type, Parent_Order__c, AccountId, OwnerId, BusinessIdentity__c, " +
                            "Brand_Name__c, CurrencyIsoCode, Primary_Opportunity_Contact__c, " +
                            "Tier_Name__c, LeadSource, " +
                            "CreatedBy.TimeZoneSidKey " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + changeOrderOpportunityId + "'",
                    Opportunity.class);

            assertThat(changeOrderOpportunity.getRecordType().getName())
                    .as("Opportunity.RecordType.Name value")
                    .isEqualTo(NEW_SALES_OPPORTUNITY_RECORD_TYPE);

            //  UTC timezone here because Opportunity.CloseDate has 'Date' datatype in SFDC and does not contain any info about time
            //  UTC has 0 offset, and this way the Close Date, as a particular day, is the same in Java code as in the SFDC 
            var actualCloseDateAsLocalDate = changeOrderOpportunity.getCloseDate()
                    .toInstant().atZone(UTC).toLocalDate();
            //  In Apex, Opportunity.CloseDate = System.today(), which is user's timezone that could be different from UTC,
            //  User.TimeZoneSidKey examples: 'America/Los_Angeles', 'Australia/Sydney', 'Europe/London', etc.
            var expectedCloseDateInUserTimeZone = now(ZoneId.of(changeOrderOpportunity.getCreatedBy().getTimeZoneSidKey()));
            assertThat(actualCloseDateAsLocalDate)
                    .as("Opportunity.CloseDate value")
                    .isEqualTo(expectedCloseDateInUserTimeZone);

            assertThat(changeOrderOpportunity.getIs_Billing_Opportunity__c())
                    .as("Opportunity.Is_Billing_Opportunity__c value")
                    .isTrue();
            assertThat(changeOrderOpportunity.getStageName())
                    .as("Opportunity.StageName value")
                    .isEqualTo(QUALIFY_STAGE);
            assertThat(changeOrderOpportunity.getType())
                    .as("Opportunity.Type value")
                    .isEqualTo(EXISTING_BUSINESS_TYPE);
            assertThat(changeOrderOpportunity.getParent_Order__c())
                    .as("Opportunity.Parent_Order__c value")
                    .isEqualTo(proServOrderId);
            assertThat(changeOrderOpportunity.getAccountId())
                    .as("Opportunity.AccountId value")
                    .isEqualTo(proServInNgbsSteps.account.getId());
            assertThat(changeOrderOpportunity.getOwnerId())
                    .as("Opportunity.OwnerId value")
                    .isEqualTo(proServInNgbsSteps.account.getOwnerId());
            assertThat(changeOrderOpportunity.getBusinessIdentity__c())
                    .as("Opportunity.BusinessIdentity__c value")
                    .isEqualTo(format(BI_FORMAT, data.getCurrencyIsoCode(), data.businessIdentity.id));
            assertThat(changeOrderOpportunity.getBrand_Name__c())
                    .as("Opportunity.Brand_Name__c value")
                    .isEqualTo(data.brandName);
            assertThat(changeOrderOpportunity.getCurrencyIsoCode())
                    .as("Opportunity.CurrencyIsoCode value")
                    .isEqualTo(data.getCurrencyIsoCode());
            assertThat(changeOrderOpportunity.getPrimary_Opportunity_Contact__c())
                    .as("Opportunity.Primary_Opportunity_Contact__c value")
                    .isEqualTo(proServInNgbsSteps.contact.getId());
            assertThat(changeOrderOpportunity.getTier_Name__c())
                    .as("Opportunity.Tier_Name__c value")
                    .isEqualTo(proServInNgbsSteps.proServService);
            assertThat(changeOrderOpportunity.getLeadSource())
                    .as("Opportunity.LeadSource value")
                    .isEqualTo(INBOUND_CALL_LEAD_SOURCE);
        });

        //  CRM-37465
        step("4. Check the Quote for the Change Order Opportunity", () -> {
            var changeOrderQuotes = enterpriseConnectionUtils.query(
                    "SELECT Id, IsMultiProductTechnicalQuote__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + changeOrderOpportunityId + "'",
                    Quote.class);
            assertThat(changeOrderQuotes.size())
                    .as("Number of Quotes for the Change Order Opportunity")
                    .isEqualTo(1);
            assertThat(changeOrderQuotes.get(0).getIsMultiProductTechnicalQuote__c())
                    .as("Quote.IsMultiProductTechnicalQuote__c value")
                    .isFalse();

            changeOrderQuoteId = changeOrderQuotes.get(0).getId();
        });

        //  CRM-37493
        step("5. Open the Change Order Quote in the Quote Wizard, and check the displayed tabs", () -> {
            wizardPage.openPage(changeOrderOpportunityId, changeOrderQuoteId);
            wizardPage.tabButtons.shouldHave(exactTextsCaseSensitive(changeOrderQuoteTabs));
        });

        //  CRM-37493
        step("6. Open the Select Package tab, and check the Select Package tab contents", () -> {
            packagePage.openTab();

            //  Billing Details section
            packagePage.packageSelector.packageFilter.getChargeTermInput(data.chargeTerm)
                    .shouldBe(disabled, selected);
            packagePage.packageSelector.packageFilter.contractSelectInput.shouldBe(disabled);
            packagePage.packageSelector.packageFilter.servicePicklist
                    .shouldBe(enabled)
                    .getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(proServInNgbsSteps.proServService));

            //  Selected Package section
            var selectedPackage = packagePage.packageSelector.getSelectedPackage();
            selectedPackage.getSelf().shouldHave(cssClass("disabled"));
            selectedPackage.getName().shouldHave(exactTextCaseSensitive(proServInNgbsSteps.proServPackage.getFullName()));

            var proServOptionsSection = packagePage.packageSelector
                    .getPackageFolderByName(proServInNgbsSteps.proServService)
                    .getProServOptionsSection();
            proServOptionsSection.ucCaseCheckbox.getCheckbox().shouldBe(hidden);
            proServOptionsSection.ccCaseCheckbox.getCheckbox().shouldBe(hidden);

            //  Header buttons section
            packagePage.saveAndContinueButton.shouldBe(disabled);
        });

        //  CRM-37493
        step("7. Open the Add Products tab and check its contents", () -> {
            productsPage.openTab();

            productsPage.serviceNames.shouldHave(exactTexts(proServInNgbsSteps.proServService));
            productsPage.groupNames.should(containExactTextsCaseSensitive(proServProductGroups));
        });

        //  CRM-37493
        step("8. Check the Billing Details and Terms button and its tooltip on hover", () -> {
            quotingWizardFooter.billingDetailsAndTermsButton.shouldBe(disabled);
            quotingWizardFooter.billingDetailsAndTermsButton.hover();
            wizardPage.tooltip.shouldHave(exactTextCaseSensitive(NOT_AVAILABLE_FOR_CHANGE_ORDER_TOOLTIP));
        });

        //  CRM-37532
        step("9. Open the Price tab and check its contents", () -> {
            cartPage.openTab();

            stream(proServInNgbsSteps.proServProductsToAdd).forEach(product -> {
                step("Check the Cart Item for " + product.name, () -> {
                    var cartItem = cartPage.getQliFromCartByDisplayName(product.name);

                    cartItem.getCartItemElement().shouldBe(visible);
                    cartItem.getNewQuantityInput().shouldBe(visible, enabled);
                    cartItem.getExistingQuantityInput().shouldBe(visible, disabled);
                    cartItem.getDeliveredQuantityInput().shouldBe(visible, disabled);
                });
            });
        });

        //  CRM-37532
        step("10. Open the PS Phases tab and check its contents", () -> {
            psPhasesPage.openTab();

            psPhasesPage.phases.shouldHave(size(proServInNgbsSteps.proServPhasesQuantityDefault));
            psPhasesPage.phasesSectionName
                    .shouldHave(exactTextCaseSensitive(format(PHASE_SECTION_NAME_FORMAT, proServInNgbsSteps.proServPhasesQuantityDefault)));

            var phases = enterpriseConnectionUtils.query(
                    "SELECT Id, AutoNumberName__c " +
                            "FROM Phase__c " +
                            "WHERE Opportunity__c = '" + changeOrderOpportunityId + "'",
                    Phase__c.class);
            assertThat(phases.size())
                    .as("Number of Phase__c records")
                    .isEqualTo(proServInNgbsSteps.proServPhasesQuantityDefault);

            phases.forEach(phase -> {
                step("Check the Phase with Auto Number Name = " + phase.getAutoNumberName__c(), () -> {
                    psPhasesPage.getPhaseByAutoNumber(phase.getAutoNumberName__c()).getSelf().shouldBe(visible);
                });
            });
        });
    }
}
