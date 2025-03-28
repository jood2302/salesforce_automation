package ngbs.quotingwizard.existingbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.activateAccountInNGBS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityFactory.createOpportunity;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.INVOICE_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.getTechQuote;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.setQuoteToApprovedActiveAgreement;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("P0")
@Tag("Account")
@Tag("Opportunity")
@Tag("Quote")
@Tag("LTR-569")
@Tag("MultiProduct-UB")
public class TechAccAndOppCreationExistingBusinessMpUbTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User dealDeskUserWithoutEnabledMpUbFT;
    private Account account;
    private Contact contact;
    private Opportunity opportunity;

    private User salesRepUserWithEnabledMpUbFT;
    private Opportunity newMasterOpportunity;
    private Opportunity newTechRcCcOpportunity;
    private Opportunity newTechEdOpportunity;

    //  Test data
    private final String officeServiceName;
    private final String engageDigitalServiceName;
    private final String rcCcServiceName;
    private final Package officePackage;
    private final Package edPackage;
    private final Package rcCcPackage;
    private final List<String> allSelectedServices;
    private final Map<String, Package> packageFolderNameToPackageMap;

    private final Product digitalLineUnlimited;
    private final Product officePhone;

    public TechAccAndOppCreationExistingBusinessMpUbTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_Annual_Contract.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        officeServiceName = data.packageFolders[0].name;
        engageDigitalServiceName = data.packageFolders[2].name;
        rcCcServiceName = data.packageFolders[3].name;
        officePackage = data.packageFolders[0].packages[0];
        edPackage = data.packageFolders[2].packages[0];
        rcCcPackage = data.packageFolders[3].packages[0];
        allSelectedServices = List.of(officeServiceName, engageDigitalServiceName, rcCcServiceName);

        packageFolderNameToPackageMap = Map.of(
                officeServiceName, officePackage,
                engageDigitalServiceName, edPackage
        );

        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        officePhone = data.getProductByDataName("LC_HD_523");
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with 'Deal Desk Lightning' profile, 'Allow Process Order Without Shipping' Permission Set, " +
                "and WITHOUT 'Enable Multi-Product Unified Billing' Feature Toggle", () -> {
            dealDeskUserWithoutEnabledMpUbFT = getUser()
                    .withProfile(DEAL_DESK_LIGHTNING_PROFILE)
                    .withPermissionSet(ALLOW_PROCESS_ORDER_WITHOUT_SHIPPING_PS)
                    .withFeatureToggles(Map.of(ENABLE_MULTIPRODUCT_UNIFIED_BILLING_FT, false))
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUserWithoutEnabledMpUbFT);
        account = steps.salesFlow.account;
        contact = steps.salesFlow.contact;

        steps.quoteWizard.createOpportunity(account, contact, dealDeskUserWithoutEnabledMpUbFT);
        opportunity = steps.quoteWizard.opportunity;

        step("Set Office Account's Account_Payment_Method__c = 'Invoice', " +
                "Service_Type__c and RC_Service_name__c = 'Office' via API", () -> {
            account.setAccount_Payment_Method__c(INVOICE_PAYMENT_METHOD);
            account.setService_Type__c(officeServiceName);
            account.setRC_Service_name__c(officeServiceName);
            enterpriseConnectionUtils.update(account);
        });

        step("Login as a user with 'Deal Desk Lightning' profile and WITHOUT 'Enable Multi-Product Unified Billing' Feature Toggle", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUserWithoutEnabledMpUbFT);
        });

        step("Create an Multiproduct Active Agreement for the MVP and Engage Digital services with approved Invoicing Request", () -> {
            step("Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                    "select MVP and Engage Digital packages for it, and save changes", () ->
                    steps.quoteWizard.prepareOpportunityForMultiProduct(opportunity.getId(), packageFolderNameToPackageMap)
            );

            step("Add necessary products on the Add Products tab, " +
                    "open the Price tab, and assign phone to DL", () -> {
                steps.quoteWizard.addProductsOnProductsTab(officePhone);

                cartPage.openTab();
                steps.cartTab.assignDevicesToDL(officePhone.name, digitalLineUnlimited.name, steps.quoteWizard.localAreaCode,
                        officePhone.quantity);
            });

            step("Open the Quote Details tab, set Start Date and Main Area Code, save changes, " +
                    "and update it to Active Agreement via API", () -> {
                quotePage.openTab();
                quotePage.setDefaultStartDate();
                quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
                quotePage.saveChanges();

                var masterQuoteToUpdate = new Quote();
                masterQuoteToUpdate.setId(wizardPage.getSelectedQuoteId());
                setQuoteToApprovedActiveAgreement(masterQuoteToUpdate);
                enterpriseConnectionUtils.update(masterQuoteToUpdate);
            });

            step("Create Invoice Request Approval for the test Account " +
                    "with related 'Accounts Payable' AccountContactRole record, " +
                    "set Approval__c.Status = 'Approved' (all via API)", () ->
                    createInvoiceApprovalApproved(opportunity, account, contact, dealDeskUserWithoutEnabledMpUbFT.getId(), true)
            );
        });

        step("Complete the sign-up process via Process Order modal for the test Opportunity for the MVP and ED services", () -> {
            step("Open the Opportunity record page, open Process Order modal, select value in TimeZone picklist " +
                    "and click Sign Up MVP button", () -> {
                opportunityPage.openPage(opportunity.getId());
                opportunityPage.clickProcessOrderButton();
                opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();

                opportunityPage.processOrderModal.selectDefaultTimezone();
                opportunityPage.processOrderModal.signUpButton.click();

                opportunityPage.processOrderModal.signUpMvpStatus
                        .shouldHave(exactTextCaseSensitive(format(YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE, MVP_SERVICE)), ofSeconds(60));
            });

            step("Check that Billing_ID__c and RC_User_ID__c fields are populated on the Master Account, " +
                    "activate the Account in the NGBS via API, and close Process Order modal window", () -> {
                var billingId = step("Wait until Account's Billing_ID__c and RC_User_ID__c will get the values from NGBS", () -> {
                    return assertWithTimeout(() -> {
                        var accountUpdated = enterpriseConnectionUtils.querySingleRecord(
                                "SELECT Id, Billing_ID__c, RC_User_ID__c " +
                                        "FROM Account " +
                                        "WHERE Id = '" + account.getId() + "'",
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

            step("Click 'Process Order' button, " +
                    "and make sure that MVP Service is signed up", () -> {
                opportunityPage.clickProcessOrderButton();
                opportunityPage.processOrderModal.mvpTierStatus
                        .shouldHave(exactTextCaseSensitive(SIGNED_UP_STATUS), ofSeconds(60));
                opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(60));
                opportunityPage.processOrderModal.errorNotifications.shouldHave(size(0));
            });

            step("Sign Up Engage Digital service in the Process Order modal", () -> {
                step("Expand 'Engage Digital' service and click 'Process Engage Digital' button", () -> {
                    opportunityPage.processOrderModal.engageDigitalExpandButton.click();

                    opportunityPage.processOrderModal.processEngageDigitalButton.click();
                    opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(60));
                    opportunityPage.processOrderModal.engageDigitalValidateAddressButton.shouldBe(disabled);
                });

                step("Select Engage Digital Platform Location, RC Engage Digital Platform, Language and Timezone " +
                        "and populate RC Engage Digital Domain field", () -> {
                    opportunityPage.processOrderModal.engageDigitalPlatformLocationSelect.selectOption(US1_PLATFORM_LOCATION);
                    opportunityPage.processOrderModal.selectRcEngagePlatformFirstOption(ENGAGE_DIGITAL_SERVICE);
                    opportunityPage.processOrderModal.engageDigitalLanguageSelect.selectOption(EN_US_LANGUAGE);
                    opportunityPage.processOrderModal.engageDigitalTimezoneSelect.selectOption(ANCHORAGE_TIME_ZONE);

                    //  Engage Digital Domain should contain up to 32 symbols
                    var randomEngageDomainValue = UUID.randomUUID().toString().substring(0, 31);
                    opportunityPage.processOrderModal.engageDigitalDomainInput.setValue(randomEngageDomainValue);
                });

                step("Click 'Validate' button and make sure that the address is validated", () -> {
                    opportunityPage.processOrderModal.engageDigitalValidateAddressButton.click();
                    opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(30));

                    //  If the address is not verified automatically, user need to handle a pop-up modal
                    if (opportunityPage.processOrderModal.addressVerificationContinueButton.isDisplayed()) {
                        opportunityPage.processOrderModal.addressVerificationContinueButton.click();
                        opportunityPage.processOrderModal.addressVerificationContinueButton.shouldBe(hidden);
                    }

                    opportunityPage.processOrderModal.engageDigitalValidateAddressButton.shouldBe(disabled);
                });

                step("Click 'Sign Up Engage Digital' button and check notification that Engage Digital submitted successfully", () -> {
                    opportunityPage.processOrderModal.signUpButton.click();
                    opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(150));
                    opportunityPage.processOrderModal.successNotifications
                            .shouldHave(exactTexts(format(SERVICE_SUBMITTED_SUCCESSFULLY, ENGAGE_DIGITAL_SERVICE)), ofSeconds(60));
                });

                step("Check that the Engage Digital Account received Billing_ID__c and RC_User_ID__c from the NGBS", () ->
                        assertWithTimeout(() -> {
                            var techAccounts = enterpriseConnectionUtils.query(
                                    "SELECT Id, Billing_ID__c, RC_User_ID__c " +
                                            "FROM Account " +
                                            "WHERE Master_Account__c = '" + account.getId() + "' " +
                                            "AND Service_Type__c = '" + engageDigitalServiceName + "'",
                                    Account.class);
                            assertEquals(1, techAccounts.size(), "Number of Tech Accounts for service = " + engageDigitalServiceName);

                            var techAccount = techAccounts.get(0);
                            assertNotNull(techAccount.getBilling_ID__c(), "Technical Account.Billing_ID__c value");
                            assertNotNull(techAccount.getRC_User_ID__c(), "Technical Account.RC_User_ID__c value");

                            return techAccount;
                        }, ofSeconds(60), ofSeconds(10))
                );
            });
        });

        step("Transfer the ownership of the test Account and Contact " +
                "to the user with 'Sales Rep - Lightning' profile and WITH 'Enable Multi-Product Unified Billing' Feature Toggle, " +
                "and re-login as this user into SFDC", () -> {
            step("Find a user with 'Sales Rep - Lightning' profile and WITH 'Enable Multi-Product Unified Billing' Feature Toggle", () -> {
                salesRepUserWithEnabledMpUbFT = getUser()
                        .withProfile(SALES_REP_LIGHTNING_PROFILE)
                        .withFeatureToggles(List.of(ENABLE_MULTIPRODUCT_UNIFIED_BILLING_FT))
                        //  to avoid issues with records sharing during MultiProduct Quote creation 
                        //  (access to the Tech Accounts to correctly identify MPL/MPUB flow)
                        .withGroupMembership(NON_GSP_GROUP)
                        .execute();
            });

            account.setOwnerId(salesRepUserWithEnabledMpUbFT.getId());
            contact.setOwnerId(salesRepUserWithEnabledMpUbFT.getId());
            enterpriseConnectionUtils.update(account, contact);

            steps.sfdc.reLoginAsUser(salesRepUserWithEnabledMpUbFT);
        });

        step("Create a new Opportunity for the same test Account via API", () -> {
            newMasterOpportunity = createOpportunity(account, contact, false,
                    data.getBrandName(), data.businessIdentity.id, salesRepUserWithEnabledMpUbFT, data.getCurrencyIsoCode(),
                    officeServiceName);
        });
    }

    @Test
    @TmsLink("CRM-36667")
    @DisplayName("CRM-36667 - UB. Technical Accounts and Opportunities are created while creating Quote " +
            "by user with enabled FT 'Enable Multi-Product Unified Billing' on Account with existing linked Technical Accounts. Existing Business")
    @Description("Verify that required technical Accounts and Opportunities are created " +
            "while creating Quote on Account with existing linked Technical Accounts " +
            "by user with enabled FT 'Enable Multi-Product Unified Billing' for Existing Business Account")
    public void test() {
        step("1. Open the Quote Wizard for the NEW test Opportunity to add a new Sales Quote, " +
                "select Office, Engage Digital, and RingCentral Contact Center packages for it, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(newMasterOpportunity.getId());
            packagePage.packageSelector.selectPackage(data.chargeTerm, officeServiceName, officePackage);
            packagePage.packageSelector.selectPackageWithoutSeatsSetting(data.chargeTerm, engageDigitalServiceName, edPackage);
            packagePage.packageSelector.selectPackage(data.chargeTerm, rcCcServiceName, rcCcPackage);
            packagePage.saveChanges();
        });

        step("2. Check that there are 2 Technical Accounts created for ED and RC CC Services (1 for each)", () -> {
            var edTechAccounts = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Account " +
                            "WHERE Master_Account__c = '" + account.getId() + "' " +
                            "AND Service_Type__c = '" + engageDigitalServiceName + "'",
                    Account.class);
            assertThat(edTechAccounts.size())
                    .as("Number of Technical Accounts for Engage Digital service")
                    .isEqualTo(1);

            var rcCcTechAccounts = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Account " +
                            "WHERE Master_Account__c = '" + account.getId() + "' " +
                            "AND Service_Type__c = '" + rcCcServiceName + "'",
                    Account.class);
            assertThat(rcCcTechAccounts.size())
                    .as("Number of Technical Accounts for RingCentral Contact Center service")
                    .isEqualTo(1);
        });

        step("3. Check that there are 2 Technical Opportunities created for the RC CC and ED Services (1 for each) " +
                "linked to the new (Master) Opportunity", () -> {
            var edTechOpportunities = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Opportunity " +
                            "WHERE MasterOpportunity__c = '" + newMasterOpportunity.getId() + "' " +
                            "AND Tier_Name__c = '" + engageDigitalServiceName + "'",
                    Opportunity.class);
            assertThat(edTechOpportunities.size())
                    .as("Number of Technical Opportunities for Engage Digital service")
                    .isEqualTo(1);

            newTechEdOpportunity = edTechOpportunities.get(0);

            var rcCcTechOpportunities = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Opportunity " +
                            "WHERE MasterOpportunity__c = '" + newMasterOpportunity.getId() + "' " +
                            "AND Tier_Name__c = '" + rcCcServiceName + "'",
                    Opportunity.class);
            assertThat(rcCcTechOpportunities.size())
                    .as("Number of Technical Opportunities for RC CC service")
                    .isEqualTo(1);

            newTechRcCcOpportunity = rcCcTechOpportunities.get(0);
        });

        step("4. Check that there are 1 Master and 3 Technical Quotes created, " +
                "and that they are linked to the correct Opportunities", () -> {
            var masterQuoteId = wizardPage.getSelectedQuoteId();
            var allTechQuotes = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Quote " +
                            "WHERE MasterQuote__c = '" + masterQuoteId + "'",
                    Quote.class);
            assertThat(allTechQuotes.size())
                    .as("Number of Technical Quotes")
                    .isEqualTo(allSelectedServices.size());

            step("Check that the Master Quote and Office Technical Quote are linked " +
                    "to the new (Master) Opportunity", () -> {
                var masterQuote = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id, OpportunityId " +
                                "FROM Quote " +
                                "WHERE Id = '" + masterQuoteId + "'",
                        Quote.class);
                assertThat(masterQuote.getOpportunityId())
                        .as("Master Quote.OpportunityId value")
                        .isEqualTo(newMasterOpportunity.getId());

                var officeTechQuote = getTechQuote(masterQuoteId, officeServiceName);
                assertThat(officeTechQuote.getOpportunityId())
                        .as("Tech Office Quote.OpportunityId value")
                        .isEqualTo(newMasterOpportunity.getId());
            });

            step("Check that the Technical Quotes for RC CC and ED are linked " +
                    "to the Technical Opportunities for the corresponding Service", () -> {
                var rcCcTechQuote = getTechQuote(masterQuoteId, rcCcServiceName);
                assertThat(rcCcTechQuote.getOpportunityId())
                        .as("Tech RC CC Quote.OpportunityId value")
                        .isEqualTo(newTechRcCcOpportunity.getId());

                var edTechQuote = getTechQuote(masterQuoteId, engageDigitalServiceName);
                assertThat(edTechQuote.getOpportunityId())
                        .as("Tech ED Quote.OpportunityId value")
                        .isEqualTo(newTechEdOpportunity.getId());
            });
        });
    }
}
