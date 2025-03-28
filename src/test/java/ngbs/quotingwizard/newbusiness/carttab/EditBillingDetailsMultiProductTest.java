package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static base.Pages.cartPage;
import static base.Pages.wizardPage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal.BillingDetailsAndTermsModal.FREE_SERVICE_CREDIT_HEADER_TEXT;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitive;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.Double.valueOf;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("BillingDetails")
@Tag("Multiproduct-Lite")
public class EditBillingDetailsMultiProductTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private String fscAmountMvpCalculated;
    private String fscAmountCcCalculated;
    private String fscAmountEdCalculated;
    private String expectedFreeServiceCreditTotalMaster;
    private List<Quote> quotes;

    //  Test data
    private final String officeService;
    private final String rcContactCenterService;
    private final String engageDigitalService;

    private final List<String> expectedFscServices;

    private final Map<String, Package> packageFolderNameToPackageMap;

    private final String annualChargeTerm;
    private final String initialTerm;
    private final String renewalTerm;
    private final String freeShippingTerms;
    private final String specialTermsMVP;
    private final String specialTermsCC;
    private final String specialTermsED;

    public EditBillingDetailsMultiProductTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_ProServ_Monthly_Contract.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        officeService = data.packageFolders[0].name;
        rcContactCenterService = data.packageFolders[1].name;
        engageDigitalService = data.packageFolders[3].name;

        expectedFscServices = List.of("MVP", "Contact Center", "Engage Digital");

        packageFolderNameToPackageMap = Map.of(
                officeService, data.packageFolders[0].packages[0],
                rcContactCenterService, data.packageFolders[1].packages[0],
                engageDigitalService, data.packageFolders[3].packages[0]
        );

        annualChargeTerm = "Annual";
        initialTerm = data.packageFolders[0].packages[0].contractTerms.initialTerm[0];
        renewalTerm = data.packageFolders[0].packages[0].contractTerms.renewalTerm;
        freeShippingTerms = "Free shipping";

        specialTermsMVP = "2 Free Months of Service";
        specialTermsCC = "3 Free Months of Service";
        specialTermsED = "5 Free Months of Service";
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-30112")
    @TmsLink("CRM-29472")
    @DisplayName("CRM-30112 - 'Billing Details and Terms' Button logic for Multiproduct. \n" +
            "CRM-29472 - Billing Details and Terms modal contains FSC selector with total for each service and FSC header " +
            "(sum of all services FSC). New Business")
    @Description("CRM-30112 - Verify that 'Billing Details and Terms' modal window saves values to the Master and Technical Quotes. \n" +
            "CRM-29472 - Verify that Billing Details and Terms modal contains FSC selector with total for each service and FSC header " +
            "(sum of all services FSC)")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select MVP, RC Contact Center and Engage Digital packages for it, and save changes", () ->
            steps.quoteWizard.prepareOpportunityForMultiProduct(steps.quoteWizard.opportunity.getId(), packageFolderNameToPackageMap)
        );

        step("2. Open the Price tab, click 'Billing Details and Terms', and check the Contract checkbox " +
                "and Free Service Credit section fields in the modal", () -> {
            cartPage.openTab();
            cartPage.footer.billingDetailsAndTermsButton.click();

            //  CRM-30112
            cartPage.billingDetailsAndTermsModal.contractSelectInput.shouldBe(disabled);

            //  CRM-29472
            cartPage.billingDetailsAndTermsModal.fscSectionHeader
                    .shouldHave(exactTextCaseSensitive(format(FREE_SERVICE_CREDIT_HEADER_TEXT, data.currencyISOCode, "0")));

            cartPage.billingDetailsAndTermsModal.specialTermsMvpPicklist.shouldBe(visible);
            cartPage.billingDetailsAndTermsModal.specialTermsCcPicklist.shouldBe(visible);
            cartPage.billingDetailsAndTermsModal.specialTermsEdPicklist.shouldBe(visible);

            cartPage.billingDetailsAndTermsModal.fscAmountMvpInput.shouldBe(visible, disabled).shouldHave(exactValue("0"));
            cartPage.billingDetailsAndTermsModal.fscAmountCcInput.shouldBe(visible, disabled).shouldHave(exactValue("0"));
            cartPage.billingDetailsAndTermsModal.fscAmountEdInput.shouldBe(visible, disabled).shouldHave(exactValue("0"));

            cartPage.billingDetailsAndTermsModal.fscServiceHeaders.shouldHave(exactTextsCaseSensitive(expectedFscServices));
        });

        step("3. Change Payment Plan, Initial and Renewal Terms, Special Shipping Terms, " +
                "Special Terms (different for each Service), and click 'Save' button on the modal window", () -> {
            //  CRM-30112
            cartPage.billingDetailsAndTermsModal.selectChargeTerm(annualChargeTerm);
            cartPage.billingDetailsAndTermsModal.initialTermPicklist.selectOption(initialTerm);
            cartPage.billingDetailsAndTermsModal.renewalTermPicklist.selectOption(renewalTerm);
            cartPage.billingDetailsAndTermsModal.freeShippingTermsPicklist.selectOption(freeShippingTerms);

            //  CRM-30112, CRM-29472
            cartPage.billingDetailsAndTermsModal.specialTermsMvpPicklist.selectOption(specialTermsMVP);
            cartPage.billingDetailsAndTermsModal.fscAmountMvpInput.shouldNotHave(exactValue("0"), ofSeconds(10));
            cartPage.billingDetailsAndTermsModal.placeholderLoading.shouldBe(hidden);
            fscAmountMvpCalculated = cartPage.billingDetailsAndTermsModal.fscAmountMvpInput.getValue();

            cartPage.billingDetailsAndTermsModal.specialTermsCcPicklist.selectOption(specialTermsCC);
            cartPage.billingDetailsAndTermsModal.fscAmountCcInput.shouldNotHave(exactValue("0"), ofSeconds(10));
            cartPage.billingDetailsAndTermsModal.placeholderLoading.shouldBe(hidden);
            fscAmountCcCalculated = cartPage.billingDetailsAndTermsModal.fscAmountCcInput.getValue();

            cartPage.billingDetailsAndTermsModal.specialTermsEdPicklist.selectOption(specialTermsED);
            cartPage.billingDetailsAndTermsModal.fscAmountEdInput.shouldNotHave(exactValue("0"), ofSeconds(10));
            cartPage.billingDetailsAndTermsModal.placeholderLoading.shouldBe(hidden);
            fscAmountEdCalculated = cartPage.billingDetailsAndTermsModal.fscAmountEdInput.getValue();

            //  CRM-29472
            //  should be a sum of Free_Service_Credit_Total__c from Technical Quotes
            expectedFreeServiceCreditTotalMaster = new BigDecimal(fscAmountMvpCalculated)
                    .add(new BigDecimal(fscAmountCcCalculated))
                    .add(new BigDecimal(fscAmountEdCalculated))
                    .toString();
            cartPage.billingDetailsAndTermsModal.fscSectionHeader
                    .shouldHave(exactTextCaseSensitive(format(FREE_SERVICE_CREDIT_HEADER_TEXT,
                            data.currencyISOCode, expectedFreeServiceCreditTotalMaster)));

            //  CRM-30112
            cartPage.applyChangesInBillingDetailsAndTermsModal();
            cartPage.saveChanges();
        });

        //  CRM-30112
        step("4. Check that the values from the modal is set correctly on the Master/Tech Quotes", () -> {
            quotes = enterpriseConnectionUtils.query(
                    "SELECT Id, Name, IsMultiProductTechnicalQuote__c, " +
                            "Payment_Plan__c, Initial_Term_months__c, Term_months__c, FreeShippingTerms__c, " +
                            "Special_Terms__c, Free_Service_Credit_Total__c, Free_Service_Taxes__c " +
                            "FROM Quote " +
                            "WHERE Id = '" + wizardPage.getSelectedQuoteId() + "' " +
                            "OR MasterQuote__c = '" + wizardPage.getSelectedQuoteId() + "'",
                    Quote.class);

            step("Check the Master Quote", () -> {
                var masterQuoteOptional = quotes.stream()
                        .filter(q -> !q.getIsMultiProductTechnicalQuote__c())
                        .findFirst();
                assertThat(masterQuoteOptional)
                        .as("Master Quote")
                        .isPresent();
                var masterQuote = masterQuoteOptional.get();

                assertThat(masterQuote.getPayment_Plan__c())
                        .as("Master Quote.Payment_Plan__c value")
                        .isEqualTo(annualChargeTerm);
                assertThat(masterQuote.getInitial_Term_months__c())
                        .as("Master Quote.Initial_Term_months__c value")
                        .isEqualTo(initialTerm);
                assertThat(masterQuote.getTerm_months__c())
                        .as("Master Quote.Term_months__c value")
                        .isEqualTo(renewalTerm);

                assertThat(masterQuote.getFree_Service_Credit_Total__c())
                        .as("Master Quote.Free_Service_Credit_Total__c value")
                        .isEqualTo(valueOf(expectedFreeServiceCreditTotalMaster));
                assertThat(masterQuote.getSpecial_Terms__c())
                        .as("Master Quote.Special_Terms__c value")
                        .isEqualTo(specialTermsMVP);
                assertThat(masterQuote.getFree_Service_Taxes__c())
                        .as("Master Quote.Free_Service_Taxes__c value")
                        .isNotNull();
            });

            step("Check the Technical MVP Quote", () ->
                    checkTechQuote(officeService, fscAmountMvpCalculated, specialTermsMVP)
            );

            step("Check the Technical RingCentral Contact Center Quote", () ->
                    checkTechQuote(rcContactCenterService, fscAmountCcCalculated, specialTermsCC)
            );

            step("Check the Technical Engage Digital Quote", () ->
                    checkTechQuote(engageDigitalService, fscAmountEdCalculated, specialTermsED)
            );
        });
    }

    /**
     * Check the necessary fields on the Technical Quote records.
     *
     * @param serviceName          service name for the quote (e.g. "Office", "RingCentral Contact Center")
     * @param expectedFscTotal     expected value for the Free Service Credit amount (e.g. "89.03")
     * @param expectedSpecialTerms expected value for the Special Terms (e.g. "2 Free Months of Service")
     */
    private void checkTechQuote(String serviceName, String expectedFscTotal, String expectedSpecialTerms) {
        var techQuoteOptional = quotes.stream()
                .filter(q -> q.getName().contains("[system: " + serviceName))
                .findFirst();
        assertThat(techQuoteOptional)
                .as("Technical Quote [" + serviceName + "]")
                .isPresent();
        var techQuote = techQuoteOptional.get();

        assertThat(techQuote.getPayment_Plan__c())
                .as("Technical Quote.Payment_Plan__c value")
                .isEqualTo(annualChargeTerm);
        assertThat(techQuote.getInitial_Term_months__c())
                .as("Technical Quote.Initial_Term_months__c value")
                .isEqualTo(initialTerm);
        assertThat(techQuote.getTerm_months__c())
                .as("Technical Quote.Term_months__c value")
                .isEqualTo(renewalTerm);

        assertThat(techQuote.getFree_Service_Credit_Total__c())
                .as("Technical Quote.Free_Service_Credit_Total__c value")
                .isEqualTo(valueOf(expectedFscTotal));
        assertThat(techQuote.getSpecial_Terms__c())
                .as("Technical Quote.Special_Terms__c value")
                .isEqualTo(expectedSpecialTerms);
        assertThat(techQuote.getFree_Service_Taxes__c())
                .as("Technical Quote.Free_Service_Taxes__c value")
                .isNotNull();

        if (serviceName.equals(officeService)) {
            assertThat(techQuote.getFreeShippingTerms__c())
                    .as("Technical Quote.FreeShippingTerms__c value")
                    .isEqualTo(freeShippingTerms);
        }
    }
}
