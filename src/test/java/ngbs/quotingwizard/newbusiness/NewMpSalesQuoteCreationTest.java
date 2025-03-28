package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.page.components.ShippingAddressForm;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.components.ShippingAddressForm.OVERNIGHT_SHIPPING_OPTION;
import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomUSPhone;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.refresh;
import static com.codeborne.selenide.SetValueOptions.withDate;
import static io.qameta.allure.Allure.step;
import static java.lang.Long.parseLong;
import static java.time.Duration.ofSeconds;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Multiproduct-Lite")
@Tag("NGBS")
@Tag("QuoteTab")
public class NewMpSalesQuoteCreationTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User ddUserWithEditStatusOnQuotePS;
    private LocalDate startDateAsLocalDate;
    private LocalDate expirationDateAsLocalDate;
    private List<Quote> techQuotesList;

    //  Test data
    private final String engageVoiceServiceName;
    private final String contactCenterServiceName;
    private final Map<String, Package> packageFolderNameToPackageMap;
    private final Product evProduct;

    private final String initialTerm;
    private final String renewalTerm;
    private final Boolean autoRenewalAsBoolean;
    private final Boolean selfProvisionedAsBoolean;
    private final String discountJustification;
    private final String noteToCustomer;
    private final String quoteNewName;
    private final String rcMainNumber;
    private final String dataRetentionDuration;

    private final String shippingCustomerName;
    private final String shippingCountry;
    private final String shippingCity;
    private final String shippingState;
    private final String shippingAddressLine;
    private final String shippingZipCode;
    private final String shippingAdditionalAddress;
    private final String shippingAttentionTo;
    private final String shippingOption;

    public NewMpSalesQuoteCreationTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_Annual_Contract.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        engageVoiceServiceName = data.packageFolders[1].name;
        contactCenterServiceName = data.packageFolders[3].name;

        packageFolderNameToPackageMap = Map.of(
                data.packageFolders[0].name, data.packageFolders[0].packages[0],
                data.packageFolders[1].name, data.packageFolders[1].packages[0],
                data.packageFolders[3].name, data.packageFolders[3].packages[0]
        );
        evProduct = data.getProductByDataName("SA_CRS30_24", data.packageFolders[1].packages[0]);

        var contractTerms = data.packageFolders[0].packages[0].contractTerms;
        initialTerm = contractTerms.initialTerm[0];
        renewalTerm = contractTerms.renewalTerm;
        autoRenewalAsBoolean = true;
        selfProvisionedAsBoolean = true;
        discountJustification = "Test Discount Justification";
        noteToCustomer = "Test Note To Customer";
        quoteNewName = "New Quote Name " + UUID.randomUUID();
        rcMainNumber = getRandomUSPhone();
        dataRetentionDuration = "4 years";

        shippingCustomerName = "John Wayne";
        shippingCountry = "United States";
        shippingCity = "Miami Beach";
        shippingState = "FL";
        shippingAddressLine = "2100 Collins Ave";
        shippingZipCode = "33139";
        shippingAdditionalAddress = "Collins Park";
        shippingAttentionTo = "John Wayne (Attention To field)";
        shippingOption = OVERNIGHT_SHIPPING_OPTION;
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with 'Deal Desk Lightning' profile and with 'Edit_Status_on_Quote' permission set", () -> {
            ddUserWithEditStatusOnQuotePS = getUser()
                    .withProfile(DEAL_DESK_LIGHTNING_PROFILE)
                    .withPermissionSet(EDIT_STATUS_ON_QUOTE_PS)
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(ddUserWithEditStatusOnQuotePS);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, ddUserWithEditStatusOnQuotePS);

        step("Login as a user with 'Deal Desk Lightning' profile and with 'Edit_Status_on_Quote' permission set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(ddUserWithEditStatusOnQuotePS);
        });
    }

    @Test
    @TmsLink("CRM-29763")
    @DisplayName("CRM-29763 - Quote tab saves changes to Technical Quotes on Multiproduct Opp")
    @Description("Verify that if: \n" +
            "- Multiple services are selected \n" +
            "- User saves changes on the Quote Details tab \n\n" +
            "then most field values are propagated to both Master and Technical Quotes")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select MVP, Contact Center and Engage Voice packages for it, and save changes", () -> {
            steps.quoteWizard.prepareOpportunityForMultiProduct(steps.quoteWizard.opportunity.getId(), packageFolderNameToPackageMap);
        });

        //  to remove error message 'Choose exactly 1 license from Call recording storage'
        step("2. Add necessary products to Cart", () -> {
            steps.quoteWizard.addProductsOnProductsTab(evProduct);
        });

        step("3. Open the Quote Details tab, populate necessary fields, and save changes", () -> {
            quotePage.openTab();
            quotePage.setQuoteName(quoteNewName);

            startDateAsLocalDate = LocalDate.now();
            quotePage.startDateInput.setValue(withDate(startDateAsLocalDate));
            expirationDateAsLocalDate = startDateAsLocalDate.plusDays(1);
            quotePage.expirationDateInput.setValue(withDate(expirationDateAsLocalDate));

            quotePage.initialTermPicklist.selectOption(initialTerm);
            quotePage.renewalTermPicklist.selectOption(renewalTerm);
            quotePage.setAutoRenewalSelected(autoRenewalAsBoolean);
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.selfProvisionedCheckbox.setSelected(selfProvisionedAsBoolean);
            quotePage.rcMainNumberInput.setValue(rcMainNumber);
            quotePage.dataRetentionDurationPicklist.selectOption(dataRetentionDuration);
            quotePage.discountJustificationTextArea.setValue(discountJustification);
            quotePage.noteToCustomerEditorArea.sendKeys(noteToCustomer);

            quotePage.shippingAddressTextArea.click();
            quotePage.shippingAddressForm.getCustomerName().setValue(shippingCustomerName);
            quotePage.shippingAddressForm.getCountry().setValue(shippingCountry);
            quotePage.shippingAddressForm.getCity().setValue(shippingCity);
            quotePage.shippingAddressForm.getState().setValue(shippingState);
            quotePage.shippingAddressForm.getAddressLine().setValue(shippingAddressLine);
            quotePage.shippingAddressForm.getZipCode().setValue(shippingZipCode);
            quotePage.shippingAddressForm.getAdditionalAddressLine().setValue(shippingAdditionalAddress);
            quotePage.shippingAddressForm.getShipAttentionTo().setValue(shippingAttentionTo);
            quotePage.shippingAddressForm.getShippingOptionPicklist().selectOption(shippingOption);
            quotePage.shippingAddressForm.applyShippingForm();

            quotePage.saveChanges();
        });

        //  to remove error message 'CC ProServ quote is required for Contact Center service'
        step("4. Click 'Initiate CC ProServ' button, click 'Submit' button in popup window, " +
                "check that CC ProServ is initiated, and set СС ProServ Quote.ProServ_Status__c = 'Sold' via API", () -> {
            quotePage.initiateCcProServ();
            quotePage.waitUntilLoaded();

            var ccProServQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ProServ_Status__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND RecordType.Name = '" + CC_PROSERV_QUOTE_RECORD_TYPE + "'",
                    Quote.class);
            assertThat(ccProServQuote.getProServ_Status__c())
                    .as("СС ProServ Quote.ProServ_Status__c value")
                    .isEqualTo(CREATED_PROSERV_STATUS);

            ccProServQuote.setProServ_Status__c(SOLD_PROSERV_STATUS);
            enterpriseConnectionUtils.update(ccProServQuote);
        });

        step("5. Reload the Quote Wizard, open the Quote Details tab, " +
                "set new values for Quote Stage and Agreement Status, and save changes", () -> {
            refresh();
            wizardPage.waitUntilLoaded();
            packagePage.packageSelector.getSelectedPackage().getSelf().shouldBe(visible, ofSeconds(10));

            quotePage.openTab();
            quotePage.stagePicklist.selectOption(AGREEMENT_QUOTE_TYPE);
            quotePage.agreementStatusPicklist.selectOption(ACTIVE_QUOTE_STATUS);

            quotePage.saveChanges();
        });

        step("6. Verify that Master and Technical Quotes fields are updated with new values", () -> {
            var masterMvpQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Name, " +
                            "Start_Date__c, End_Date__c, ExpirationDate, " +
                            "Initial_Term_months__c, Term_months__c, Auto_Renewal__c, " +
                            "Note_To_Customer__c, IsMultiProductTechnicalQuote__c, " +
                            "AreaCode__r.Name, FaxAreaCode__r.Name, JustificationandDescription__c, " +
                            "Shipping_Customer_Name__c, Shipping_Country__c, Shipping_City__c, Shipping_State__c, " +
                            "Shipping_Address_Line__c, Shipping_Postal_Code__c, Shipping_Additional_Address_Line__c, " +
                            "Ship_Attention_To__c, Shipping_Option__c, " +
                            "QuoteType__c, Status, DataRetentionDuration__c, " +
                            "Opportunity.SelfProvisioned__c, Opportunity.NumberToBeRCMainNumber__c, Opportunity.NumberToBeCCNumber__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND IsMultiProductTechnicalQuote__c = false " +
                            "AND RecordType.Name = '" + SALES_QUOTE_RECORD_TYPE + "'",
                    Quote.class);

            step("Check the updated fields on the Master Quote", () -> {
                assertThat(masterMvpQuote.getName())
                        .as("Master Quote.Name value")
                        .isEqualTo(quoteNewName);

                //  Angular Editor in the QW might save the note with some additional HTML formatting,
                //  and we cannot control that consistently via UI => can only check that the field "contains" the text
                assertThat(masterMvpQuote.getNote_To_Customer__c())
                        .as("Master Quote.Note_To_Customer__c value")
                        .contains(noteToCustomer);

                var actualStartDateUpdated = masterMvpQuote.getStart_Date__c()
                        .toInstant().atZone(UTC).toLocalDate();
                assertThat(actualStartDateUpdated)
                        .as("Master Quote.Start_Date__c value (as LocalDate)")
                        .isEqualTo(startDateAsLocalDate);

                var actualEndDateUpdated = masterMvpQuote.getEnd_Date__c()
                        .toInstant().atZone(UTC).toLocalDate();
                var expectedEndDateUpdated = startDateAsLocalDate.plusMonths(parseLong(initialTerm));
                assertThat(actualEndDateUpdated)
                        .as("Master Quote.End_Date__c value (as LocalDate)")
                        .isEqualTo(expectedEndDateUpdated);

                var actualExpirationDateUpdated = masterMvpQuote.getExpirationDate()
                        .toInstant().atZone(UTC).toLocalDate();
                assertThat(actualExpirationDateUpdated)
                        .as("Master Quote.ExpirationDate value (as LocalDate)")
                        .isEqualTo(expirationDateAsLocalDate);

                assertThat(masterMvpQuote.getInitial_Term_months__c())
                        .as("Master Quote.Initial_Term_months__c value")
                        .isEqualTo(initialTerm);

                assertThat(masterMvpQuote.getTerm_months__c())
                        .as("Master Quote.Term_months__c value")
                        .isEqualTo(renewalTerm);

                assertThat(masterMvpQuote.getAuto_Renewal__c())
                        .as("Master Quote.Auto_Renewal__c value")
                        .isEqualTo(autoRenewalAsBoolean);

                assertThat(masterMvpQuote.getDataRetentionDuration__c())
                        .as("Master Quote.DataRetentionDuration__c value")
                        .isEqualTo(dataRetentionDuration);

                assertThat(masterMvpQuote.getAreaCode__r().getName())
                        .as("Master Quote.AreaCode__r.Name value")
                        .isEqualTo(steps.quoteWizard.localAreaCode.code);

                //  by default, Fax Area Code gets the same value as Main Area Code on the Quote Details tab
                assertThat(masterMvpQuote.getFaxAreaCode__r().getName())
                        .as("Master Quote.FaxAreaCode__r.Name value")
                        .isEqualTo(steps.quoteWizard.localAreaCode.code);

                assertThat(masterMvpQuote.getJustificationandDescription__c())
                        .as("Master Quote.JustificationandDescription__c value")
                        .isEqualTo(discountJustification);

                assertThat(masterMvpQuote.getQuoteType__c())
                        .as("Master Quote.QuoteType__c value")
                        .isEqualTo(AGREEMENT_QUOTE_TYPE);

                assertThat(masterMvpQuote.getStatus())
                        .as("Master Quote.Status value")
                        .isEqualTo(ACTIVE_QUOTE_STATUS);

                checkShippingFields(masterMvpQuote);

                assertThat(masterMvpQuote.getOpportunity().getSelfProvisioned__c())
                        .as("Opportunity.SelfProvisioned__c value")
                        .isEqualTo(selfProvisionedAsBoolean);

                assertThat(masterMvpQuote.getOpportunity().getNumberToBeRCMainNumber__c())
                        .as("Opportunity.NumberToBeRCMainNumber__c value")
                        .isEqualTo(rcMainNumber);

                //  by default, Contact Center Main Number gets the same value as RingCentral Main Number on the Quote Details tab 
                //  (for 'Is a customer porting a number that will become main?' = 'Yes')
                assertThat(masterMvpQuote.getOpportunity().getNumberToBeCCNumber__c())
                        .as("Opportunity.NumberToBeCCNumber__c value")
                        .isEqualTo(rcMainNumber);
            });

            techQuotesList = enterpriseConnectionUtils.query(
                    "SELECT Id, ServiceName__c, IsMultiProductTechnicalQuote__c, " +
                            "Start_Date__c, End_Date__c, ExpirationDate, " +
                            "Initial_Term_months__c, Term_months__c, Auto_Renewal__c, " +
                            "AreaCode__r.Name, FaxAreaCode__r.Name, " +
                            "QuoteType__c, Status, JustificationandDescription__c, " +
                            "Opportunity.SelfProvisioned__c, Opportunity.NumberToBeRCMainNumber__c, " +
                            "Opportunity.NumberToBeCCNumber__c, " +
                            "Ship_Attention_To__c, Shipping_Additional_Address_Line__c, Shipping_Address_Line__c, " +
                            "Shipping_City__c, Shipping_Country__c,  Shipping_Customer_Name__c, " +
                            "Shipping_Postal_Code__c, Shipping_State__c, DataRetentionDuration__c " +
                            "FROM Quote " +
                            "WHERE MasterQuote__c = '" + masterMvpQuote.getId() + "' ",
                    Quote.class);

            for (var techQuote : techQuotesList) {
                step("Check the updated fields on the Tech Quote for Service = " + techQuote.getServiceName__c(), () -> {
                    assertThat(techQuote.getStart_Date__c())
                            .as("Tech Quote.Start_Date__c value")
                            .isEqualTo(masterMvpQuote.getStart_Date__c());

                    assertThat(techQuote.getEnd_Date__c())
                            .as("Tech Quote.End_Date__c value")
                            .isEqualTo(masterMvpQuote.getEnd_Date__c());

                    assertThat(techQuote.getExpirationDate())
                            .as("Tech Quote.ExpirationDate value")
                            .isEqualTo(masterMvpQuote.getExpirationDate());

                    assertThat(techQuote.getInitial_Term_months__c())
                            .as("Tech Quote.Initial_Term_months__c value")
                            .isEqualTo(masterMvpQuote.getInitial_Term_months__c());

                    assertThat(techQuote.getTerm_months__c())
                            .as("Tech Quote.Term_months__c value")
                            .isEqualTo(masterMvpQuote.getTerm_months__c());

                    assertThat(techQuote.getAuto_Renewal__c())
                            .as("Tech Quote.Auto_Renewal__c value")
                            .isEqualTo(masterMvpQuote.getAuto_Renewal__c());

                    assertThat(techQuote.getAreaCode__r().getName())
                            .as("Tech Quote.AreaCode__r.Name value")
                            .isEqualTo(masterMvpQuote.getAreaCode__r().getName());

                    assertThat(techQuote.getFaxAreaCode__r().getName())
                            .as("Tech Quote.FaxAreaCode__r.Name value")
                            .isEqualTo(masterMvpQuote.getFaxAreaCode__r().getName());

                    assertThat(techQuote.getJustificationandDescription__c())
                            .as("Tech Quote.JustificationandDescription__c value")
                            .isEqualTo(masterMvpQuote.getJustificationandDescription__c());

                    assertThat(techQuote.getQuoteType__c())
                            .as("Tech Quote.QuoteType__c value")
                            .isEqualTo(masterMvpQuote.getQuoteType__c());

                    assertThat(techQuote.getStatus())
                            .as("Tech Quote.Status value")
                            .isEqualTo(masterMvpQuote.getStatus());

                    checkShippingFields(techQuote);

                    assertThat(techQuote.getOpportunity().getSelfProvisioned__c())
                            .as("Opportunity.SelfProvisioned__c value")
                            .isEqualTo(masterMvpQuote.getOpportunity().getSelfProvisioned__c());

                    if (techQuote.getServiceName__c().equals(contactCenterServiceName)) {
                        assertThat(techQuote.getOpportunity().getNumberToBeRCMainNumber__c())
                                .as("Opportunity.NumberToBeRCMainNumber__c value")
                                .isEqualTo(masterMvpQuote.getOpportunity().getNumberToBeRCMainNumber__c());

                        assertThat(techQuote.getOpportunity().getNumberToBeCCNumber__c())
                                .as("Opportunity.NumberToBeCCNumber__c value")
                                .isEqualTo(masterMvpQuote.getOpportunity().getNumberToBeCCNumber__c());
                    } else if (techQuote.getServiceName__c().equals(engageVoiceServiceName)) {
                        assertThat(techQuote.getDataRetentionDuration__c())
                                .as("Tech Quote.DataRetentionDuration__c value")
                                .isEqualTo(masterMvpQuote.getDataRetentionDuration__c());
                    }
                });
            }
        });
    }

    /**
     * Check that Shipping fields are populated in SFDC on provided {@link Quote} object,
     * and their values are equal to values specified on {@link ShippingAddressForm} on {@link QuotePage}.
     *
     * @param targetQuote quote record to check
     */
    private void checkShippingFields(Quote targetQuote) {
        var quoteType = targetQuote.getIsMultiProductTechnicalQuote__c() ? "Tech" : "Master";

        assertThat(targetQuote.getShip_Attention_To__c())
                .as(quoteType + " Quote.Ship_Attention_To__c value")
                .isEqualTo(shippingAttentionTo);

        assertThat(targetQuote.getShipping_Additional_Address_Line__c())
                .as(quoteType + " Quote.Shipping_Additional_Address_Line__c value")
                .isEqualTo(shippingAdditionalAddress);

        assertThat(targetQuote.getShipping_Address_Line__c())
                .as(quoteType + " Quote.Shipping_Address_Line__c value")
                .isEqualTo(shippingAddressLine);

        assertThat(targetQuote.getShipping_City__c())
                .as(quoteType + " Quote.Shipping_City__c value")
                .isEqualTo(shippingCity);

        assertThat(targetQuote.getShipping_Country__c())
                .as(quoteType + " Quote.Shipping_Country__c value")
                .isEqualTo(shippingCountry);

        assertThat(targetQuote.getShipping_Customer_Name__c())
                .as(quoteType + " Quote.Shipping_Customer_Name__c value")
                .isEqualTo(shippingCustomerName);

        assertThat(targetQuote.getShipping_Postal_Code__c())
                .as(quoteType + " Quote.Shipping_Postal_Code__c value")
                .isEqualTo(shippingZipCode);

        assertThat(targetQuote.getShipping_State__c())
                .as(quoteType + " Quote.Shipping_State__c value")
                .isEqualTo(shippingState);
    }
}
