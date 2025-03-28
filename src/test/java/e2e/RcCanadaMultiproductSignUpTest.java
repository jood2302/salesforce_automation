package e2e;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityShareFactory;
import com.sforce.soap.enterprise.sobject.*;
import com.sforce.soap.enterprise.sobject.Lead;
import io.qameta.allure.*;
import ngbs.quotingwizard.newbusiness.signup.MultiProductSignUpSteps;
import org.junit.jupiter.api.*;

import java.util.Map;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.page.salesforce.approval.TaxExemptionManagerPage.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.activateAccountInNGBS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createTeaApproval;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.DqDealQualificationHelper.APPROVED_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.ACTIVE_QUOTE_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.AGREEMENT_QUOTE_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.PROFESSIONAL_SERVICES_LIGHTNING_PROFILE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.getUser;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.closeWindow;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.openqa.selenium.WindowType.TAB;

@Tag("P1")
@Tag("E2E")
@Tag("LeadConvert")
@Tag("SignUp")
public class RcCanadaMultiproductSignUpTest extends BaseTest {
    private final Steps steps;
    private final MultiProductSignUpSteps multiProductSignUpSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesRepUser;
    private Lead createdLead;
    private Account convertedAccount;
    private Contact convertedContact;
    private Opportunity convertedOpportunity;
    private String teaApprovalId;
    private User dealDeskUser;
    private String billingId;

    //  Test data
    private final Map<String, Package> packageFolderNameToPackageMap;
    private final Product internationalCallingCreditBundle;
    private final Product addLocalNumber;
    private final Product polycomRentalPhone;
    private final Product polycomPhone;
    private final Product ciscoPhone;
    private final Product dlUnlimited;
    private final Product premiumEditionSeat;
    private final Product internationalCallingCredit;
    private final Product sfConnector;
    private final Product managementFees;
    private final Product ccProServProductToAdd;

    private final String oneFreeMonthServiceCredit;
    private final String initialTerm;
    private final String renewalTerm;

    private final String rcCanadaCountry;
    private final String officeServiceName;
    private final String rcCanadaBusinessIdentity;
    private final String streetCanada;
    private final String cityCanada;
    private final String stateCanada;
    private final String postalCodeCanada;
    private final AreaCode areaCodeCanada;

    public RcCanadaMultiproductSignUpTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_Canada_MVP_ED_CC_Monthly_Contract.json",
                Dataset.class);
        steps = new Steps(data);
        multiProductSignUpSteps = new MultiProductSignUpSteps();
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        packageFolderNameToPackageMap = Map.of(
                data.packageFolders[0].name, data.packageFolders[0].packages[0],
                data.packageFolders[1].name, data.packageFolders[1].packages[0],
                data.packageFolders[2].name, data.packageFolders[2].packages[0]
        );

        internationalCallingCreditBundle = data.getProductByDataName("LC_IB_360");
        addLocalNumber = data.getProductByDataName("LC_ALN_38");
        polycomRentalPhone = data.getProductByDataName("LC_HDR_619");
        polycomPhone = data.getProductByDataName("LC_HD_936");
        ciscoPhone = data.getProductByDataName("LC_HD_580");
        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        premiumEditionSeat = data.getProductByDataName("CC_RCCCSEAT_1", data.packageFolders[1].packages[0]);
        internationalCallingCredit = data.getProductByDataName("CC_ICINTCC300_130", data.packageFolders[1].packages[0]);
        sfConnector = data.getProductByDataName("SA_SFDCAPP_9", data.packageFolders[2].packages[0]);
        managementFees = data.getProductByDataName("SA_MANFEESWHATSUP_12", data.packageFolders[2].packages[0]);
        ccProServProductToAdd = data.packageFolders[1].packages[0].productsOther[0];

        oneFreeMonthServiceCredit = "1 Free Month of Service";
        initialTerm = data.packageFolders[0].packages[0].contractTerms.initialTerm[0];
        renewalTerm = data.packageFolders[0].packages[0].contractTerms.renewalTerm;

        rcCanadaCountry = "Canada";
        officeServiceName = data.packageFolders[0].name;
        rcCanadaBusinessIdentity = data.getBusinessIdentityName();

        streetCanada = "802 Hastings St W";
        cityCanada = "Vancouver";
        stateCanada = "British Columbia";
        postalCodeCanada = "V6C 1C8";
        areaCodeCanada = new AreaCode("Local", rcCanadaCountry, "Nova Scotia", EMPTY_STRING, "902");
    }

    @BeforeEach
    public void setUpTest() {
        salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);

        createdLead = steps.leadConvert.createSalesLeadViaLeadCreationPage();

        step("Click 'Edit' on the Lead Record page, " +
                "populate Address fields, Lead Qualification fields, and save changes", () -> {
            leadRecordPage.clickEditButton();
            leadRecordPage.populateAddressFields(rcCanadaCountry, stateCanada, cityCanada, streetCanada, postalCodeCanada);
            leadRecordPage.populateLeadQualificationFields();
            leadRecordPage.saveChanges();
        });
    }

    @Test
    @Tag("KnownIssue")
    @Issue("PBC-25419")
    @TmsLink("CRM-36127")
    @DisplayName("CRM-36127 - RC Canada MVP + RC CC + ED Multi-Product Sign Up")
    @Description("Verify that RingCentral Canada Multiproduct Account with MVP, RingCentral Contact Center " +
            "and Engage Digital can be signed up in a flow. This does not include verifications of DocuSign CLM integration")
    public void test() {
        step("1. Click 'Convert' button on the Lead Record page", () -> {
            leadRecordPage.clickConvertButton();

            leadConvertPage.switchToIFrame();
            leadConvertPage.newExistingAccountToggle.shouldBe(visible, ofSeconds(60));
            leadConvertPage.existingAccountSearchInput.getSelf().shouldBe(visible);
        });

        step("2. Check the 'Lead Qualification' section", () -> {
            leadConvertPage.leadQualificationSection.shouldBe(visible);
            leadConvertPage.selectedQualification.shouldBe(visible);
        });

        step("3. Switch the toggle into 'Create New Account' position, " +
                "check Business Identity, Service and Country picklist fields values, " +
                "populate Close Date field and click 'Apply' button", () -> {
            leadConvertPage.newExistingAccountToggle.click();
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.countryPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(rcCanadaCountry));
            leadConvertPage.servicePickList.getSelectedOption().shouldHave(exactTextCaseSensitive(officeServiceName));
            leadConvertPage.businessIdentityPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(rcCanadaBusinessIdentity));

            leadConvertPage.closeDateDatepicker.setTomorrowDate();

            leadConvertPage.opportunityInfoApplyButton.click();
        });

        step("4. Press 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        step("5. Check that Lead is converted and Opportunity, Account and Contact are created", () -> {
            var convertedLead = steps.leadConvert.checkLeadConversion(createdLead);

            convertedAccount = convertedLead.getConvertedAccount();
            convertedContact = convertedLead.getConvertedContact();
            convertedOpportunity = convertedLead.getConvertedOpportunity();
        });

        step("6. Create Tax Exempt Approval via API", () -> {
            var teaApproval = createTeaApproval(convertedAccount.getId(),
                    convertedOpportunity.getId(), convertedContact.getId(),
                    salesRepUser.getId());

            teaApprovalId = teaApproval.getId();
        });

        step("7. Open the Tax Exemption Manager for the given TEA Approval record, " +
                "select 'Federal tax', 'State tax' and 'Local tax' as 'Requested', save changes, " +
                "and submit the record for approval", () -> {
            switchTo().newWindow(TAB);
            taxExemptionManagerPage.openPage(teaApprovalId);

            taxExemptionManagerPage.setExemptionStatus(FEDERAL_TAX_TYPE, REQUESTED_EXEMPTION_STATUS);
            taxExemptionManagerPage.setExemptionStatus(STATE_TAX_TYPE, REQUESTED_EXEMPTION_STATUS);
            taxExemptionManagerPage.setExemptionStatus(LOCAL_TAX_TYPE, REQUESTED_EXEMPTION_STATUS);
            taxExemptionManagerPage.saveChanges();

            taxExemptionManagerPage.clickSubmitForApproval();

            closeWindow();
            switchTo().window(0);
        });

        step("8. Approve the Tax Exempt Approval via API", () ->
                enterpriseConnectionUtils.approveSingleRecord(teaApprovalId)
        );

        step("9. Open the the Quote Wizard to add a new Sales Quote, select RingEX (Office), Engage Digital and RC CC packages", () -> {
            opportunityPage.switchToNGBSQW();
            quoteSelectionWizardPage.waitUntilLoaded();
            steps.quoteWizard.addNewSalesQuote();
            steps.quoteWizard.selectPackagesForMultiProductQuote(packageFolderNameToPackageMap);
        });

        step("10. Open the Add Products tab, add necessary products to the Cart, open the Price tab, " +
                "set up their discounts and quantities, and save changes", () -> {
            steps.quoteWizard.addProductsOnProductsTab(internationalCallingCreditBundle, addLocalNumber,
                    polycomRentalPhone, polycomPhone, ciscoPhone, internationalCallingCredit, sfConnector, managementFees);

            cartPage.openTab();
            steps.cartTab.setUpQuantities(addLocalNumber, polycomRentalPhone,
                    polycomPhone, ciscoPhone, internationalCallingCredit, dlUnlimited, premiumEditionSeat, managementFees);
            steps.cartTab.setUpDiscounts(internationalCallingCreditBundle, addLocalNumber, polycomRentalPhone,
                    polycomPhone, ciscoPhone, internationalCallingCredit, managementFees, dlUnlimited, premiumEditionSeat);
            cartPage.saveChanges();
        });

        step("11. Click 'Add Taxes' button and check that no taxes are added", () -> {
            cartPage.addTaxes();
            cartPage.taxCartItems.shouldHave(size(0));
        });

        step("12. Open the Quote Details tab, set the Main Area Code, the Start Date, and Discount Justification", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(areaCodeCanada);
            quotePage.setDefaultStartDate();
            quotePage.discountJustificationTextArea.setValue(TEST_STRING);
        });

        step("13. Open Billing Details and Terms modal window, select Special Terms for each service, " +
                "Initial and Renewal Term, and save changes", () -> {
            quotePage.footer.billingDetailsAndTermsButton.click();
            quotePage.billingDetailsAndTermsModal.specialTermsMvpPicklist.selectOption(oneFreeMonthServiceCredit);
            quotePage.billingDetailsAndTermsModal.specialTermsCcPicklist.selectOption(oneFreeMonthServiceCredit);
            quotePage.billingDetailsAndTermsModal.specialTermsEdPicklist.selectOption(oneFreeMonthServiceCredit);
            quotePage.billingDetailsAndTermsModal.initialTermPicklist.selectOption(initialTerm);
            quotePage.billingDetailsAndTermsModal.renewalTermPicklist.selectOption(renewalTerm);
            quotePage.applyChangesInBillingDetailsAndTermsModal();
            quotePage.saveChanges();
        });

        step("14. Open the Price tab, and submit the quote for approval via 'Submit for Approval' button", () -> {
            cartPage.openTab();
            cartPage.submitForApproval();
        });

        //  shortcut to avoid approving DQ via Deal Desk user on the Deal Qualification tab
        step("15. Set the related DQ_Deal_Qualification__c.Status__c = 'Approved' via API", () -> {
            var dealQualification = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM DQ_Deal_Qualification__c " +
                            "WHERE Opportunity__c = '" + convertedOpportunity.getId() + "'",
                    DQ_Deal_Qualification__c.class);
            dealQualification.setStatus__c(APPROVED_STATUS);
            enterpriseConnectionUtils.update(dealQualification);
        });

        step("16. Click 'Initiate CC ProServ' button, and click 'Submit' button in popup window", () -> {
            cartPage.initiateCcProServ();
            cartPage.waitUntilLoaded();
        });

        step("17. Re-login as a user with 'Professional Services Lightning' profile, " +
                "manually share all the Opportunities with this user via API, " +
                "and re-open the Sales Quote in the Quote Wizard for the test Opportunity", () -> {
            var masterQuoteId = wizardPage.getSelectedQuoteId();

            closeWindow();
            switchTo().window(0);

            var proServUser = getUser().withProfile(PROFESSIONAL_SERVICES_LIGHTNING_PROFILE).execute();
            steps.sfdc.reLoginAsUser(proServUser);

            OpportunityShareFactory.shareOpportunity(convertedOpportunity.getId(), proServUser.getId());

            //  this is needed in order to make changes on the Master Quote in the Quote Wizard ('Main Quote' tab)
            //  because some changes may trigger updates on the technical records
            var techOpportunities = enterpriseConnectionUtils.query(
                    "SELECT Id, Name " +
                            "FROM Opportunity " +
                            "WHERE MasterOpportunity__c = '" + convertedOpportunity.getId() + "' ",
                    Opportunity.class);
            for (var techOpportunity : techOpportunities) {
                step("Sharing a Tech Opportunity with Name = '" + techOpportunity.getName() + "' via API", () ->
                        OpportunityShareFactory.shareOpportunity(techOpportunity.getId(), proServUser.getId())
                );
            }

            wizardPage.openPage(convertedOpportunity.getId(), masterQuoteId);
            wizardPage.waitUntilLoaded();
        });

        step("18. Open the ProServ Quote tab and prepare the CC ProServ Quote for the sign-up flow", () -> {
            steps.proServ.prepareCcProServQuoteForSignUp(ccProServProductToAdd.name);
        });

        step("19. Create Invoicing Request Approval for the test Account " +
                "with related 'Accounts Payable' AccountContactRole record, " +
                "set Approval__c.Status = 'Approved' (all via API)", () -> {
            //  DD user should have access to the Approval record when signing up everything later on
            dealDeskUser = steps.salesFlow.getDealDeskUser();

            createInvoiceApprovalApproved(convertedOpportunity, convertedAccount, convertedContact,
                    dealDeskUser.getId(), true);
        });

        step("20. Switch back to the Main Quote, open the Quote Details tab, " +
                "set Quote's Stage = 'Agreement', and save changes", () -> {
            wizardBodyPage.mainQuoteTab.click();
            wizardPage.waitUntilLoaded();
            //  helps to avoid unexpected 'loading...' on the Quote Details tab
            cartPage.openTab();

            quotePage.openTab();
            //  TODO Known Issue PBC-25419 (The picklist is disabled, but it should be enabled)
            quotePage.stagePicklist.selectOption(AGREEMENT_QUOTE_TYPE);
            quotePage.saveChanges();
        });

        step("21. Set Master Quote's Status = 'Active' via API", () -> {
            var masterQuoteToUpdate = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id  " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + convertedOpportunity.getId() + "' " +
                            "AND isPrimary__c = true",
                    Quote.class);
            masterQuoteToUpdate.setStatus(ACTIVE_QUOTE_STATUS);

            enterpriseConnectionUtils.update(masterQuoteToUpdate);
        });

        step("22. Re-login as a user with 'Deal Desk Lightning' profile, " +
                "transfer the ownership of the Account, Contact, and Opportunity to this user, " +
                "and open the Opportunity's record page", () -> {
            convertedAccount.setOwnerId(dealDeskUser.getId());
            convertedContact.setOwnerId(dealDeskUser.getId());
            convertedOpportunity.setOwnerId(dealDeskUser.getId());
            enterpriseConnectionUtils.update(convertedAccount, convertedContact, convertedOpportunity);

            steps.sfdc.reLoginAsUserWithSessionReset(dealDeskUser);

            opportunityPage.openPage(convertedOpportunity.getId());
        });

        step("23. Click 'Process Order' button, " +
                "verify that 'Preparing Data' step is completed, select Timezone, " +
                "click 'Sign Up MVP', and check that the account is processed for signing up", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();

            opportunityPage.processOrderModal.selectDefaultTimezone();
            opportunityPage.processOrderModal.signUpButton.click();

            opportunityPage.processOrderModal.signUpMvpStatus
                    .shouldHave(exactTextCaseSensitive(format(YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE, MVP_SERVICE)), ofSeconds(60));
        });

        step("24. Check that Billing_ID__c and RC_User_ID__c fields are populated on the Master Account, " +
                "activate the Account in the NGBS via API, and close Process Order modal window", () -> {
            billingId = step("Wait until Account's Billing_ID__c and RC_User_ID__c will get the values from NGBS", () -> {
                return assertWithTimeout(() -> {
                    var accountUpdated = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Id, Billing_ID__c, RC_User_ID__c " +
                                    "FROM Account " +
                                    "WHERE Id = '" + convertedAccount.getId() + "'",
                            Account.class);
                    assertNotNull(accountUpdated.getBilling_ID__c(), "Account.Billing_ID__c field");
                    assertNotNull(accountUpdated.getRC_User_ID__c(), "Account.RC_User_ID__c field");

                    step("Account.Billing_ID__c = " + accountUpdated.getBilling_ID__c());
                    step("Account.RC_User_ID__c = " + accountUpdated.getRC_User_ID__c());

                    return accountUpdated.getBilling_ID__c();
                }, ofSeconds(120), ofSeconds(5));
            });

            activateAccountInNGBS(billingId);

            opportunityPage.processOrderModal.closeWindow();
        });

        step("25. Open the Process Order modal window again, " +
                "check that the MVP service is displayed in 'Signed Up' status, " +
                "and no errors are displayed", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.mvpTierStatus.shouldHave(exactText(SIGNED_UP_STATUS), ofSeconds(60));
            opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(60));
            opportunityPage.processOrderModal.errorNotifications.shouldHave(size(0));
        });

        step("26. Expand the RingCentral Contact Center service's section, " +
                "sign up 'Contact Center' service via the Process Order modal window, " +
                "and check that the RC Contact Center package is added to the account in NGBS", () -> {
            multiProductSignUpSteps.signUpRcContactCenterServiceStep(billingId, convertedOpportunity.getId());
        });

        step("27. Expand the Engage Digital service service's section, " +
                "sign up 'Engage Digital' service via the Process Order modal window, " +
                "and check that the Engage Digital package is added to the account in NGBS", () -> {
            multiProductSignUpSteps.signUpEngageDigitalServiceStep(billingId);
        });
    }
}
