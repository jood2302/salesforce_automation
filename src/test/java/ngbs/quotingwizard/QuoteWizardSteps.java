package ngbs.quotingwizard;

import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityFactory;
import com.sforce.soap.enterprise.sobject.*;
import com.sforce.ws.ConnectionException;
import io.qameta.allure.Step;

import java.util.Arrays;
import java.util.Map;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.CLOSED_WON_STAGE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.setRequiredFieldsForOpportunityStageChange;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.APPROVED_APPROVAL_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.setQuoteToApprovedActiveAgreement;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.qameta.allure.Allure.step;

/**
 * Test methods for the flows related to the NGBS Quote Wizard.
 */
public class QuoteWizardSteps {
    private final Dataset data;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public Opportunity opportunity;

    //  Test data
    public AreaCode tollFreeAreaCode;
    public AreaCode localAreaCode;
    public AreaCode indiaAreaCode;
    public String currencyPrefix;

    /**
     * New instance for the class with the test methods/steps related to the NGBS Quote Wizard.
     *
     * @param data object parsed from the JSON files with the test data
     */
    public QuoteWizardSteps(Dataset data) {
        this.data = data;

        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        localAreaCode = new AreaCode("Local", "United States", "California", EMPTY_STRING, "619");
        tollFreeAreaCode = new AreaCode("Toll-Free", "United States", EMPTY_STRING, EMPTY_STRING, "888");
        indiaAreaCode = new AreaCode("Local", "India", "Maharashtra", EMPTY_STRING, "22");
        currencyPrefix = data.getCurrencyIsoCode() + " ";
    }

    /**
     * Create a new Opportunity via API.
     *
     * @param account   Account to create an Opportunity for
     * @param contact   Account's primary signatory Contact to associate with the Opportunity via OpportunityContactRole
     * @param ownerUser user intended to be the owner of the created records
     */
    public void createOpportunity(Account account, Contact contact, User ownerUser) {
        step("Create a test Opportunity via API", () -> {
            var isNewCustomer = data.getBillingId().isEmpty();
            opportunity = OpportunityFactory.createOpportunity(account, contact, isNewCustomer,
                    data.getBrandName(), data.businessIdentity.id, ownerUser, data.getCurrencyIsoCode(),
                    data.packageFolders[0].name);
        });
    }

    /**
     * <p> 1. Open record page for the given Opportunity object.
     * <p> 2. Switch to the Quote Wizard section.
     *
     * @param opportunityId ID of the test Opportunity to be opened
     */
    @Step("Open Opportunity record page and switch to the Quote Wizard section")
    public void openQuoteWizardOnOpportunityRecordPage(String opportunityId) {
        opportunityPage.openPage(opportunityId);
        opportunityPage.switchToNGBSQW();
        quoteSelectionWizardPage.waitUntilLoaded();
    }

    /**
     * Click 'Add New' in the 'Sales Quote' section of the Quote Selection page,
     * and switch to the Quote Wizard page for the new quote.
     */
    @Step("Add a new Sales Quote from the Quote Selection page and switch to the Quote Wizard")
    public void addNewSalesQuote() {
        quoteSelectionWizardPage.addNewSalesQuoteButton.click();
        switchTo().window(1);
        wizardPage.waitUntilLoaded();
        packagePage.packageSelector.waitUntilLoaded();
    }

    /**
     * Click 'Add New' in the 'POC Quote' section of the Quote Selection page,
     * and switch to the Quote Wizard page for the new quote.
     */
    @Step("Add a new POC Quote from the Quote Selection page and switch to the Quote Wizard")
    public void addNewPocQuote() {
        quoteSelectionWizardPage.addNewPocQuoteButton.click();
        switchTo().window(1);
        wizardPage.waitUntilLoaded();
        packagePage.packageSelector.waitUntilLoaded();
    }

    /**
     * <p> 1. Open Opportunity record page and switch to Quote Wizard section.
     * <p> 2. Click on the 'Add New' button for Sales Quote and switch to the opened window.
     * <p> 3. Select a Package from provided Test data on the Select Package tab of Quote Wizard (for New Business),
     * and create a new quote with it.
     *
     * @param opportunityId ID of the Opportunity for which the Quote Wizard is open
     * @see #prepareOpportunityViaQuoteWizardVfPage(String)
     */
    public void prepareOpportunity(String opportunityId) {
        openQuoteWizardOnOpportunityRecordPage(opportunityId);
        addNewSalesQuote();
        selectPackageFromTestDataAndCreateQuote();
    }

    /**
     * <p> 1. Open the Quote Wizard for the test Opportunity to create a new Sales Quote via direct VF page link.
     * <p> 2. Select a Package from provided Test data on the Select Package tab of Quote Wizard (for New Business),
     * and create a new quote with it.
     * <br/><br/>
     * Note: use this method for test cases that only deal with the test Opportunity
     * in the Quote Wizard, without interacting with Opportunity record page's elements
     * (e.g. Lightning buttons, "Close", "Sign Up", etc...).
     * <br/>
     * For other cases, see {@link #prepareOpportunity(String)}.
     *
     * @param opportunityId ID of the Opportunity for which the Quote Wizard is open
     */
    public void prepareOpportunityViaQuoteWizardVfPage(String opportunityId) {
        openQuoteWizardForNewSalesQuoteDirect(opportunityId);
        selectPackageFromTestDataAndCreateQuote();
    }

    /**
     * <p> 1. Open the Quote Wizard for the test New Business Opportunity to create a new Sales Quote via direct VF page link.
     * <p> 2. Select <b>several packages</b> from provided Test data on the Select Package tab of Quote Wizard (for New Business),
     * populate 'Number of Seats' field in case of Engage packages, and create a new Multi-Product quote with it.
     * <br/><br/>
     * Note: make sure to provide packages that can be selected together for the Multi-Product Quotes, e.g.
     * <b>Office + Engage Digital Standalone + Engage Voice Standalone + RingCentral Contact Center</b>.
     *
     * @param opportunityId                 ID of the Opportunity for which the Quote Wizard is open
     * @param packageFolderNameToPackageMap mapping between Package Folder names (e.g. "Office", "Engage Voice Standalone")
     *                                      and their respective Packages objects
     */
    public void prepareOpportunityForMultiProduct(String opportunityId, Map<String, Package> packageFolderNameToPackageMap) {
        openQuoteWizardForNewSalesQuoteDirect(opportunityId);
        selectPackagesForMultiProductQuote(packageFolderNameToPackageMap);
        packagePage.saveChanges();
    }

    /**
     * Select <b>several packages</b> from provided Test data on the Select Package tab of Quote Wizard (for New Business),
     * and create a new Multi-Product quote with it.
     * <br/><br/>
     * Note: make sure to provide packages that can be selected together for the Multi-Product Quotes, e.g.
     * <b>Office + Engage Digital Standalone + Engage Voice Standalone + RingCentral Contact Center</b>.
     *
     * @param packageFolderNameToPackageMap mapping between Package Folder names (e.g. "Office", "Engage Voice Standalone")
     *                                      and their respective Packages objects
     */
    public void selectPackagesForMultiProductQuote(Map<String, Package> packageFolderNameToPackageMap) {
        packageFolderNameToPackageMap.forEach((packageFolderName, pkg) ->
                step("Select the package " + pkg.getFullName() + " for the service " + packageFolderName, () ->
                        packagePage.packageSelector.selectPackage(data.chargeTerm, packageFolderName, pkg)));
    }

    /**
     * <p> 1. Open Opportunity record page and switch to Quote Wizard section.
     * <p> 2. Click on the 'Add New' button for POC Quote and switch to the opened window.
     * <p> 3. Select a Package from provided Test data on the Select Package tab of Quote Wizard (for New Business),
     * and create a new quote with it.
     *
     * @param opportunityId ID of the Opportunity for which the Quote Wizard is open
     * @see #preparePocQuoteViaQuoteWizardVfPage(String)
     */
    public void preparePocQuote(String opportunityId) {
        openQuoteWizardOnOpportunityRecordPage(opportunityId);
        addNewPocQuote();
        selectPackageFromTestDataAndCreateQuote();
    }

    /**
     * <p> 1. Open the Quote Wizard for the test Opportunity to create a new POC Quote via direct VF page link.
     * <p> 2. Select a Package from provided Test data on the Select Package tab of the Quote Wizard (for New Business),
     * and create a new quote with it.
     * <br/><br/>
     * Note: use this method for test cases that only deal with the test Opportunity
     * in the Quote Wizard, without interacting with Opportunity record page's elements
     * (e.g. Lightning buttons, "Close", "Sign Up", etc...).
     * <br/>
     * For other cases, see {@link #preparePocQuote(String)}.
     *
     * @param opportunityId ID of the Opportunity for which the Quote Wizard is open
     */
    public void preparePocQuoteViaQuoteWizardVfPage(String opportunityId) {
        openQuoteWizardForNewPocQuoteDirect(opportunityId);
        selectPackageFromTestDataAndCreateQuote();
    }

    /**
     * <p> 1. Open Quote selection page in the Quote Wizard for the given Opportunity Id via direct VF page link.
     * <p> 2. Wait until QW is loaded.
     *
     * @param opportunityId Id of the test Opportunity to be opened
     */
    public void openQuoteWizardDirect(String opportunityId) {
        wizardBodyPage.openPage(opportunityId);
        quoteSelectionWizardPage.waitUntilLoaded();
    }

    /**
     * <p> 1. Open the Quote Wizard to add a new Sales Quote for the given Opportunity Id via direct VF page link.
     * <p> 2. Wait until QW is loaded.
     *
     * @param opportunityId ID of the test Opportunity to be opened
     */
    public void openQuoteWizardForNewSalesQuoteDirect(String opportunityId) {
        wizardPage.openPageForNewSalesQuote(opportunityId);
        packagePage.packageSelector.waitUntilLoaded();
    }

    /**
     * <p> 1. Open the Quote Wizard to add a new POC Quote for the given Opportunity Id via direct VF page link.
     * <p> 2. Wait until QW is loaded.
     *
     * @param opportunityId ID of the test Opportunity to be opened
     */
    public void openQuoteWizardForNewPocQuoteDirect(String opportunityId) {
        wizardPage.openPageForNewPocQuote(opportunityId);
        packagePage.packageSelector.waitUntilLoaded();
    }

    /**
     * Select package on the Select Package tab with default test data from {@link QuoteWizardSteps#data} object.
     * <p>
     * If there's a non-null existing <b>billingId</b> in test data – package selection is skipped!
     * (useful for Existing Business opportunities).
     */
    public void selectDefaultPackageFromTestData() {
        if (data.getBillingId().isEmpty()) {
            step("Select a package from the provided test data", () -> {
                packagePage.packageSelector.selectPackage(
                        data.chargeTerm,
                        data.packageFolders[0].name,
                        data.packageFolders[0].packages[0]);
            });
        }
    }

    /**
     * Select package on the Select Package tab with default test data from {@link QuoteWizardSteps#data} object
     * and click 'Save and Continue' button.
     * <p>
     * If there's a non-null existing <b>billingId</b> in test data – package selection is skipped!
     * (useful for Existing Business opportunities).
     */
    public void selectPackageFromTestDataAndCreateQuote() {
        selectDefaultPackageFromTestData();
        packagePage.saveChanges();
    }

    /**
     * Open the Add Products tab, wait for products to load, and add products to Cart.
     *
     * @param productsToAdd collection of {@link Product} items (or single product) to add to Cart
     */
    @Step("Add products on the Add Products tab")
    public void addProductsOnProductsTab(Product... productsToAdd) {
        productsPage.openTab();
        Arrays.stream(productsToAdd).forEach(productsPage::addProduct);
    }

    /**
     * Set 'Approved_Status__c' = 'Approved' for current Quote on Opportunity in DB.
     *
     * @param opportunityId ID of the Opportunity for which the single main Quote is updated
     * @throws ConnectionException in case of malformed DB queries or network errors
     */
    @Step("Set 'Approved_Status__c' to 'Approved' for the current Quote in DB")
    public void stepUpdateQuoteToApprovedStatus(String opportunityId) throws ConnectionException {
        var quote = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id " +
                        "FROM Quote " +
                        "WHERE OpportunityId = '" + opportunityId + "'",
                Quote.class);
        quote.setApproved_Status__c(APPROVED_APPROVAL_STATUS);

        enterpriseConnectionUtils.update(quote);
    }

    /**
     * Update current quote of the given Opportunity to Active Agreement status.
     * <br/>
     * Note: only works for the Opportunity with a single quote!
     *
     * @param opportunity opportunity object that has a quote on it
     * @throws Exception in case of malformed DB queries or network errors.
     */
    @Step("Update current quote to Active Agreement status")
    public void stepUpdateQuoteToApprovedActiveAgreement(Opportunity opportunity) throws Exception {
        var quote = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id " +
                        "FROM Quote " +
                        "WHERE OpportunityId = '" + opportunity.getId() + "'",
                Quote.class);

        setQuoteToApprovedActiveAgreement(quote);

        enterpriseConnectionUtils.update(quote);
    }

    /**
     * Set the required fields for Stage changing, set StageName = "7. Closed Won",
     * and update the current Opportunity (all via API).
     *
     * @param opportunity Opportunity object to "close"
     * @throws ConnectionException in case of DB or network errors
     */
    @Step("Set the required fields for Stage changing and StageName = '7. Closed Won' for the current Opportunity via API")
    public void stepCloseOpportunity(Opportunity opportunity) throws ConnectionException {
        setRequiredFieldsForOpportunityStageChange(opportunity);
        opportunity.setStageName(CLOSED_WON_STAGE);
        enterpriseConnectionUtils.update(opportunity);
    }
}
