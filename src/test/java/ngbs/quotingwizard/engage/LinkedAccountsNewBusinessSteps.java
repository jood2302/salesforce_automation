package ngbs.quotingwizard.engage;

import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.*;
import com.sforce.ws.ConnectionException;
import ngbs.quotingwizard.QuoteWizardSteps;

import static base.Pages.packagePage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountContactRoleFactory.createAccountContactRole;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewCustomerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityFactory.createOpportunity;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.INFLUENCER_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.INVOICE_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.getPrimaryContactOnAccount;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.setQuoteToApprovedActiveAgreement;
import static io.qameta.allure.Allure.step;

/**
 * Test methods related to the test cases where Office (Master) Account is not Signed Up
 * and Office and Engage Accounts are to be linked.
 */
public class LinkedAccountsNewBusinessSteps {
    public final Dataset data;
    private final QuoteWizardSteps quoteWizardSteps;
    private final EngageSteps engageSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public Account officeAccount;
    public Opportunity officeOpportunity;
    public Contact officeAccountContact;

    //  Test data
    public final String officeChargeTerm;
    public final Package officePackage;
    public final String officeInitialTerm;
    public final String officeService;
    public final String officePackageFolderName;
    public final Product engageProduct;

    /**
     * New instance for the class with the test methods/steps for the test cases
     * where Office (Master) Account is not Signed Up and Office and Engage Accounts are linked.
     */
    public LinkedAccountsNewBusinessSteps() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_EngageDSAndMVP_Monthly_Contract_WithProducts.json",
                Dataset.class);

        quoteWizardSteps = new QuoteWizardSteps(data);
        engageSteps = new EngageSteps();
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        officeChargeTerm = data.chargeTerm;
        officePackage = data.packageFolders[1].packages[0];
        officeInitialTerm = officePackage.contractTerms.initialTerm[0];
        officePackageFolderName = data.packageFolders[1].name;
        officeService = officePackageFolderName;
        engageProduct = data.getProductByDataName("SA_SEAT_5");
    }

    /**
     * Preconditions for the test cases related linking New Business Office and Engage Accounts:
     * create the Office Account/Contact/Opportunity/Sales Quote/additional AccountContactRole,
     * create Invoicing Request Approval for the Engage Account,
     * link Office and Engage accounts.
     *
     * @param engageAccount     Engage Account to link with Office and create an Approval for
     * @param engageContact     Engage Account to create an Approval and AccountContactRole for
     * @param engageOpportunity Engage Opportunity to create an Approval for
     * @param ownerUser         owner of the records that are created here
     * @param localAreaCode     area code to use for the Sales Quote
     */
    public void setUpLinkedAccountsNewBusinessSteps(Account engageAccount, Contact engageContact,
                                                    Opportunity engageOpportunity, User ownerUser,
                                                    AreaCode localAreaCode) {
        step("Create New Business Office Account with Payment Method = 'Invoice' " +
                "with related Contact and AccountContactRole records via API", () -> {
            officeAccount = createNewCustomerAccountInSFDC(ownerUser, new AccountData(data));
            officeAccount.setPayment_Method__c(INVOICE_PAYMENT_METHOD);

            enterpriseConnectionUtils.update(officeAccount);
            officeAccountContact = getPrimaryContactOnAccount(officeAccount);
        });

        step("Create Office Account's Opportunity via API", () -> {
            officeOpportunity = createOpportunity(officeAccount, officeAccountContact, true,
                    data.getBrandName(), data.businessIdentity.id, ownerUser, data.getCurrencyIsoCode(), officeService);
        });

        step("Open the Quote Wizard for Office Opportunity, add a new Sales Quote, and make quote an Active Agreement", () -> {
            step("Open the Quote Wizard for the Office Opportunity to add a new Sales Quote, " +
                    "select a package for it, and save changes", () -> {
                quoteWizardSteps.openQuoteWizardForNewSalesQuoteDirect(officeOpportunity.getId());
                packagePage.packageSelector.selectPackage(officeChargeTerm, officePackageFolderName, officePackage);
                packagePage.saveChanges();
            });

            step("Open the Quote Details tab, populate Initial Term, Main Area Code, Start Date fields and save changes", () -> {
                quotePage.openTab();
                quotePage.initialTermPicklist.selectOption(officeInitialTerm);
                quotePage.setMainAreaCode(localAreaCode);
                quotePage.setDefaultStartDate();
                quotePage.saveChanges();
            });

            quoteWizardSteps.stepUpdateQuoteToApprovedActiveAgreement(officeOpportunity);
        });

        step("Add second Contact role for Office Account with the same Contact " +
                "as on Engage Account's Primary Contact Role via API", () -> {
            createAccountContactRole(officeAccount, engageContact, INFLUENCER_ROLE, false);
        });

        step("Create Invoice Request Approval for Engage Account " +
                "with related 'Accounts Payable' AccountContactRole record, " +
                "and set Approval__c.Status = 'Approved' (all via API)", () -> {
            createInvoiceApprovalApproved(engageOpportunity, engageAccount, engageContact, ownerUser.getId(), false);
        });

        step("Link Office and Engage Accounts via API", () ->
                engageSteps.linkAccounts(engageAccount, officeAccount)
        );
    }

    /**
     * Link Engage and Office Quotes and set Engage Quote to Active Agreement via SFDC API.
     *
     * @throws ConnectionException in case of errors while accessing API
     */
    public void stepPrepareEngageQuoteForOpportunityCloseOrSignUp(String engageOpportunityId, String officeOpportunityId)
            throws ConnectionException {
        var engageQuote = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id " +
                        "FROM Quote " +
                        "WHERE OpportunityId = '" + engageOpportunityId + "'",
                Quote.class);
        var officeQuote = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id " +
                        "FROM Quote " +
                        "WHERE OpportunityId = '" + officeOpportunityId + "'",
                Quote.class);
        engageQuote.setParentQuote__c(officeQuote.getId());
        setQuoteToApprovedActiveAgreement(engageQuote);
        enterpriseConnectionUtils.update(engageQuote);
    }
}
