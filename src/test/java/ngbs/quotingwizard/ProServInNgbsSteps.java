package ngbs.quotingwizard;

import base.NgbsSteps;
import base.SfdcSteps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import ngbs.SalesFlowSteps;

import java.util.List;
import java.util.Map;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.addBillingAddressInNGBS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.PackageFactory.createBillingAccountPackage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.INVOICE_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.PAID_RC_ACCOUNT_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.setProServQuoteRequiredFields;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.setQuoteToApprovedActiveAgreement;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.SuborderLineItemHelper.setLocationDetails;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps for the flows related ProServ service in NGBS:
 * sign up the Opportunity with Multiproduct Quote with ProServ service;
 * working with Orders and Suborders related to the signed up ProServ service, etc.
 * <br/><br/>
 * For the "legacy" ProServ flows see {@link ProServSteps}.
 */
public class ProServInNgbsSteps {
    private final Dataset data;
    private final SfdcSteps sfdcSteps;
    private final NgbsSteps ngbsSteps;
    private final SalesFlowSteps salesFlowSteps;
    private final CartTabSteps cartTabSteps;
    private final QuoteWizardSteps quoteWizardSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    public final String officeService;
    public final Package officePackage;
    public final String proServService;
    public final Package proServPackage;
    public final Map<String, Package> packageFolderNameToPackageMap;
    public final Product[] officeProductsDefault;
    public final Product[] proServProductsToAdd;
    public final int proServPhasesQuantityDefault;

    private User salesRepUserWithProServInNgbsFT;
    public User proservUserWithProServInNgbsFT;
    public Account account;
    public Contact contact;
    public Opportunity opportunity;

    /**
     * New instance for the class with the test methods/steps related to ProServ In NGBS flows.
     *
     * @param data                 object parsed from the JSON files with the test data
     * @param proServPackageFolder folder in the test data that contains ProServ package to work with
     */
    public ProServInNgbsSteps(Dataset data, PackageFolder proServPackageFolder) {
        this.data = data;
        sfdcSteps = new SfdcSteps();
        ngbsSteps = new NgbsSteps(data);
        salesFlowSteps = new SalesFlowSteps(data);
        quoteWizardSteps = new QuoteWizardSteps(data);
        cartTabSteps = new CartTabSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        officeService = data.packageFolders[0].name;
        officePackage = data.packageFolders[0].packages[0];
        proServService = proServPackageFolder.name;
        proServPackage = proServPackageFolder.packages[0];
        packageFolderNameToPackageMap = Map.of(
                officeService, officePackage,
                proServService, proServPackage
        );

        officeProductsDefault = officePackage.productsDefault;
        proServProductsToAdd = proServPackage.products;

        proServPhasesQuantityDefault = 4;

        //  to generate the EB MP Account later in the test, bypassing Process Order flow
        ngbsSteps.isGenerateAccountsForSingleTest = true;
    }

    /**
     * Sign up an Opportunity with Multiproduct Quote that include ProServ service in NGBS
     * (via full sign-up flow, including UQT/QW, Process Order modal, etc.).
     * <br/>
     * The full sign-up flow is necessary when testing Order/Suborder functionality for the ProServ,
     * as it requires many valid records created during its execution
     * (Quotes, Quote Line Items, Phases, Phase Line Items, Orders, Order Line Items, Suborders, Suborder Line Items, etc.).
     * <br/>
     * Right now, it's too complicated to create all these records via API, so the full sign-up flow via CRM UI is used.
     */
    public void initSignUpOpportunityWithProServServiceInNgbs() {
        step("Find a user with 'Sales Rep - Lightning' profile, 'ProServ in NGBS' feature toggle " +
                "and 'Enable Super User ProServ In UQT' permission set", () -> {
            salesRepUserWithProServInNgbsFT = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    //  to add ProServ Products, and to assign them to the PS Phases (as a Sales Rep user)
                    .withFeatureToggles(List.of(PROSERV_IN_NGBS_FT))
                    .withPermissionSet(ENABLE_SUPER_USER_PROSERV_IN_UQT_PS)
                    .execute();
        });

        step("Create an Account, Contact, AccountContactRole, Opportunity records via API ", () -> {
            salesFlowSteps.createAccountWithContactAndContactRole(salesRepUserWithProServInNgbsFT);
            account = salesFlowSteps.account;
            contact = salesFlowSteps.contact;

            quoteWizardSteps.createOpportunity(account, contact, salesRepUserWithProServInNgbsFT);
            opportunity = quoteWizardSteps.opportunity;
        });

        step("Log in as a user with 'Sales Rep - Lightning' profile, 'ProServ in NGBS' feature toggle " +
                "and 'Enable Super User ProServ In UQT' permission set", () -> {
            sfdcSteps.initLoginToSfdcAsTestUser(salesRepUserWithProServInNgbsFT);
        });

        step("Create a new Sales Quote (Multiproduct) with ProServ package, and prepare it for the sign up", () -> {
            prepareOpportunityWithProServForSignUp(account, contact, opportunity, salesRepUserWithProServInNgbsFT.getId(),
                    packageFolderNameToPackageMap, proServProductsToAdd, proServPhasesQuantityDefault);
        });

        step("Open the Opportunity record page, click 'Process Order' button and check that 'Preparing data' is completed", () -> {
            opportunityPage.openPage(opportunity.getId());
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();
            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(hidden);
        });

        //  to bypass signing up MVP + ProServ account in the 'Process Order' modal
        step("Generate Existing Business MVP + ProServ Account in NGBS via AGS API, " +
                "populate Billing_ID__c and RC_User_ID__c fields on the SFDC Account " +
                "and create new Package__c objects for the Office and ProServ NGBS packages (all via SFDC API)", () -> {
            //  to create an Existing Business MPUB Account in NGBS via AGS and NGBS APIs
            officePackage.productsFromBilling = officeProductsDefault;
            proServPackage.productsFromBilling = proServProductsToAdd;

            ngbsSteps.generateMultiProductUnifiedBillingAccount();

            account.setBilling_ID__c(data.billingId);
            account.setRC_User_ID__c(data.rcUserId);
            enterpriseConnectionUtils.update(account);

            step("Create new Billing Account Package objects (Package__c) for the Account " +
                    "for the Office and ProServ NGBS packages via SFDC API", () -> {
                createBillingAccountPackage(account.getId(), data.packageId, officePackage.id,
                        data.brandName, officeService, INVOICE_PAYMENT_METHOD, PAID_RC_ACCOUNT_STATUS);

                createBillingAccountPackage(account.getId(), proServPackage.ngbsPackageId, proServPackage.id,
                        data.brandName, proServService, INVOICE_PAYMENT_METHOD, PAID_RC_ACCOUNT_STATUS);
            });
        });
        
        step("Find a user with 'Professional Service Lightning' profile and 'ProServ in NGBS' feature toggle, " +
                "transfer the ownership of test Account, Opportunity and Contact to this user via API, " +
                "and re-login as this user", () -> {
            proservUserWithProServInNgbsFT = getUser()
                    .withProfile(PROFESSIONAL_SERVICES_LIGHTNING_PROFILE)
                    .withFeatureToggles(List.of(PROSERV_IN_NGBS_FT))
                    .execute();

            account.setOwnerId(proservUserWithProServInNgbsFT.getId());
            contact.setOwnerId(proservUserWithProServInNgbsFT.getId());
            opportunity.setOwnerId(proservUserWithProServInNgbsFT.getId());
            enterpriseConnectionUtils.update(account, contact, opportunity);

            sfdcSteps.reLoginAsUser(proservUserWithProServInNgbsFT);
        });
    }

    /**
     * Prepare the Opportunity with Multiproduct Quote with ProServ package for the sign-up in the NGBS.
     *
     * @param account                       Account record in SFDC
     * @param contact                       Contact record in SFDC
     * @param opportunity                   Opportunity record in SFDC
     * @param ownerId                       ID of the user who is the owner of the Account, Contact, Opportunity
     * @param packageFolderNameToPackageMap mapping for selecting packages for the Multiproduct Quote
     * @param proServProductsToAdd          ProServ products to add to the Cart
     * @param proServPhasesQuantity         number of phases to add on the 'PS Phases' tab of the Quote Wizard
     */
    public void prepareOpportunityWithProServForSignUp(Account account, Contact contact, Opportunity opportunity, String ownerId,
                                                       Map<String, Package> packageFolderNameToPackageMap,
                                                       Product[] proServProductsToAdd, int proServPhasesQuantity) {
        step("Open the Quote Wizard to add a new Sales Quote, select MVP and ProServ packages, and save changes", () -> {
            quoteWizardSteps.prepareOpportunityForMultiProduct(opportunity.getId(), packageFolderNameToPackageMap);
        });

        step("Open the Add Products tab and add ProServ products to the Cart", () -> {
            quoteWizardSteps.addProductsOnProductsTab(proServProductsToAdd);
        });

        step("Open the Price tab and set up quantities for ProServ products", () -> {
            cartPage.openTab();
            cartTabSteps.setUpQuantities(proServProductsToAdd);
        });

        step("Open the PS Phases tab, add phase(s), and assign ProServ product(s) to the phases", () -> {
            psPhasesPage.openTab();

            for (var phaseNumber = 0; phaseNumber < proServPhasesQuantity; phaseNumber++) {
                psPhasesPage.addPhaseButton.click();
                for (var proServProduct : proServProductsToAdd) {
                    psPhasesPage.addProductToPhase(phaseNumber, proServProduct.name, proServProduct.phaseLineItemQuantity);
                }
            }
        });

        step("Open the Quote Details tab, select Main Area Code, Start Date, and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(quoteWizardSteps.localAreaCode);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();
        });

        step("Update the Master Quote to the Active Agreement stage and populate required ProServ fields via API", () -> {
            var masterQuoteToUpdate = new Quote();
            masterQuoteToUpdate.setId(wizardPage.getSelectedQuoteId());

            setQuoteToApprovedActiveAgreement(masterQuoteToUpdate);
            setProServQuoteRequiredFields(masterQuoteToUpdate);
            enterpriseConnectionUtils.update(masterQuoteToUpdate);
        });

        step("Create Approved Invoice Approval for Account via API", () -> {
            createInvoiceApprovalApproved(opportunity, account, contact, ownerId, true);
        });
    }

    /**
     * Create a Change Order (Opportunity, Quote, Order) from the ProServ Project record.
     *
     * @param accountId        ID of the main Account record in SFDC
     * @param proServProjectId ID of the ProServ Project record in SFDC
     *                         (should be created automatically
     *                         when user signs up the Opportunity via Process Order modal)
     */
    public Opportunity createChangeOrderFromProServProject(String accountId, String proServProjectId) {
        return step("Open the ProServ Project, open Change Order modal, " +
                "and check that Change Order opportunity is created in SFDC", () -> {
            proServProjectRecordPage.openPage(proServProjectId);
            proServProjectRecordPage.openChangeOrderModal();

            proServProjectRecordPage.changeOrderModal.spinnerContainer.shouldBe(visible, ofSeconds(10));
            proServProjectRecordPage.changeOrderModal.successNotification.shouldBe(visible, ofSeconds(120));

            var changeOrderOpportunity = enterpriseConnectionUtils.query(
                    "SELECT Id, Name " +
                            "FROM Opportunity " +
                            "WHERE AccountId = '" + accountId + "' " +
                            "AND Name LIKE '%Change Order%'",
                    Opportunity.class);
            assertThat(changeOrderOpportunity.size())
                    .as("Number of Change Order Opportunities")
                    .isEqualTo(1);

            return changeOrderOpportunity.get(0);
        });
    }

    /**
     * Find the ProServ_Project__c record for the ProServ service
     * (Account should be signed up with ProServ service).
     *
     * @param accountId ID of the main Account record in SFDC
     */
    public ProServ_Project__c getProServProject(String accountId) {
        return step("Find ProServ_Project__c record for ProServ service via API", () -> {
            return enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Order__c " +
                            "FROM ProServ_Project__c " +
                            "WHERE Account__c = '" + accountId + "' " +
                            "AND QuoteType__c = '" + proServService + "'",
                    ProServ_Project__c.class);
        });
    }

    /**
     * Populate LocationDetails__c fields of all SuborderLineItem__c records via API.
     * <br/.>
     * Note: 'Process Suborder' button is only correctly enabled for the Failed Suborder
     * if the locations are added for all its Suborder Line Items.
     *
     * @param proServOrderId ID of the ProServ Order record in SFDC
     *                       (can be found via {@link ProServ_Project__c#getOrder__c()}).
     */
    public void populateLocationsOnSuborderLineItems(String proServOrderId) {
        step("Populate SuborderLineItem__c.LocationDetails__c fields on all SuborderLineItem__c records via API", () -> {
            var serviceLocationAddressId = addBillingAddressInNGBS(data.billingId).id;
            var suborderLineItems = enterpriseConnectionUtils.query(
                    "SELECT Id, Quantity__c " +
                            "FROM SuborderLineItem__c " +
                            "WHERE Suborder__r.Order__c = '" + proServOrderId + "'",
                    SuborderLineItem__c.class);
            suborderLineItems.forEach(suborderLineItem -> {
                setLocationDetails(suborderLineItem, serviceLocationAddressId.toString());
            });
            enterpriseConnectionUtils.update(suborderLineItems);
        });
    }
}
