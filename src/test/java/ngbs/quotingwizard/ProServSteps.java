package ngbs.quotingwizard;

import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import com.sforce.ws.ConnectionException;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.quotetab.LegacyQuotePage.BUDGETARY_FORECAST_CATEGORY;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.CC_PROSERV_QUOTE_RECORD_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.PROSERV_QUOTE_RECORD_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.PROFESSIONAL_SERVICES_LIGHTNING_PROFILE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.getUser;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test methods for test cases related to Professional Services (ProServ) functionality:
 * creating ProServ / CC ProServ quotes, syncing them with the primary quotes, etc...
 */
public class ProServSteps {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String sowQuoteNumber;
    private final String proServUsers;
    private final String proServSowType;
    private final String testSowInput;

    /**
     * New instance for the class with the test methods/steps
     * related to Professional Services (ProServ) functionality.
     */
    public ProServSteps() {
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        sowQuoteNumber = "1";
        proServUsers = "2";
        proServSowType = "Single Phase";
        testSowInput = "test.com";
    }

    /**
     * <p> 1. Open the ProServ tab on the Quote Wizard page. </p>
     * <p> 2. Switch to the Products tab and select any product by clicking the 'Add to Cart' button. </p>
     * <p> 3. Open the Phase tab, click the 'Add phase' button, move the added product to the added phase and save changes. </p>
     * <p> 4. Open the Quote tab, populate necessary fields, and save changes. </p>
     * <p> 5. Sync the CC ProServ Quote to the Primary Quote and follow up until the CC ProServ quote is marked as 'Sold'. </p>
     *
     * @param ccProServProductName the name of the CC ProServ product to add to the quote
     */
    public void prepareCcProServQuoteForSignUp(String ccProServProductName) {
        step("Open the ProServ Quote tab, switch to the Products tab, select a product by 'Add to Cart' button, " +
                "and check that the product is added on the Cart tab", () -> {
            addProductOnProServQuoteTab(ccProServProductName);
        });

        step("Open the Phase tab, click 'Add phase' button, move added product to the added phase and save changes", () -> {
            proServWizardPage.phaseTabButton.click();
            phasePage.addAndSavePhase();
        });

        step("Open the Quote tab, populate necessary fields, and save changes", () -> {
            populateMandatoryFieldsOnQuoteTabAndSave(true);
        });

        step("Click 'Sync to Primary Quote' button, click 'ProServ is Out for Signature' button, " +
                "click 'Mark as 'Out for Signature' and Lock Quote' button in pop-up window " +
                "and check that 'Mark as 'Out for Signature' and Lock Quote' button is hidden " +
                "and 'Cancel CC ProServ Engagement' and 'ProServ is Sold' buttons are shown", () -> {
            proServWizardPage.syncProServQuoteWithPrimary();
            //  user needs to un-focus 'Sync to Primary Quote' button
            //  to hide the popover modal 'Sync Is Disabled' that covers the 'ProServ Is Out For Signature' button
            proServWizardPage.quoteTabButton.hover();

            proServWizardPage.markProServIsOutForSignature();

            proServWizardPage.proServIsOutForSignatureButton.shouldBe(hidden, ofSeconds(10));
            proServWizardPage.cancelCcProServEngagementButton.shouldBe(visible, ofSeconds(10));
            proServWizardPage.proServIsSoldButton.shouldBe(visible, ofSeconds(10));
        });

        step("Click 'ProServ is Sold' button and click 'Mark as Sold' button in the opened pop-up window", () ->
                proServWizardPage.markProServAsSold()
        );
    }

    /**
     * Open the ProServ tab on the Quote Wizard page, switch to the Products tab,
     * add a given product to the quote, and check it is added on the Cart tab.
     *
     * @param productName the name of the product to add to the quote
     */
    public void addProductOnProServQuoteTab(String productName) {
        wizardBodyPage.proServTab.click();
        proServWizardPage.waitUntilLoaded();

        legacyProductsPage.openTab();
        legacyProductsPage.addProduct(productName);

        legacyCartPage.openTab();
        legacyCartPage.getQliFromCart(productName).getName().shouldBe(visible, ofSeconds(30));
    }

    /**
     * Open the Quote tab, populate some mandatory fields on it, and save the changes.
     *
     * @param isCcProServQuote {@code true}, if the legacy quote is "CC ProServ Quote";
     *                         {@code false}, if the legacy quote is "ProServ Quote".
     * @throws ConnectionException in case of malformed DB queries or network failures
     *                             when searching for ProServ Architect via API
     */
    public void populateMandatoryFieldsOnQuoteTabAndSave(boolean isCcProServQuote) throws ConnectionException {
        proServWizardPage.quoteTabButton.click();

        var proServArchitectUser = getUser().withProfile(PROFESSIONAL_SERVICES_LIGHTNING_PROFILE).execute();
        legacyQuotePage.selectProServArchitect(proServArchitectUser.getName());
        legacyQuotePage.originalSOWQuoteNumberInput.setValue(sowQuoteNumber);
        legacyQuotePage.proServForecastCategorySelect.selectOption(BUDGETARY_FORECAST_CATEGORY);
        legacyQuotePage.selectDefaultProjectComplexity();
        legacyQuotePage.proServUsersInput.setValue(proServUsers);
        legacyQuotePage.signedSowInput.setValue(testSowInput);
        legacyQuotePage.proServSowTypeSelect.selectOption(proServSowType);

        if (isCcProServQuote) {
            legacyQuotePage.selectDefaultForecastedCloseDateOnCcProServQuote();
        } else {
            legacyQuotePage.selectDefaultForecastedCloseDateOnProServQuote();
        }

        legacyQuotePage.saveQuote();
    }

    /**
     * Check ProServ_Status__c field value of the ProServ Quote of a customer Opportunity.
     *
     * @param expectedStatus expected status of the ProServ Quote.ProServ_Status__c field (example: 'Created')
     */
    public void checkProServQuoteStatus(String opportunityId, String expectedStatus) {
        step("Check that ProServ Quote.ProServ_Status__c = '" + expectedStatus + "' via API", () -> {
            var proServQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ProServ_Status__c " +
                            "FROM Quote " +
                            "WHERE Opportunity.Id = '" + opportunityId + "' " +
                            "AND RecordType.Name = '" + PROSERV_QUOTE_RECORD_TYPE + "'",
                    Quote.class);
            assertThat(proServQuote.getProServ_Status__c())
                    .as("ProServ Quote.ProServ_Status__c value")
                    .isEqualTo(expectedStatus);
        });
    }

    /**
     * Check ProServ_Status__c field value of the СС ProServ Quote of a customer Opportunity.
     *
     * @param expectedStatus expected status of the СС ProServ Quote.ProServ_Status__c field (example: 'Sold')
     */
    public void checkCcProServQuoteStatus(String opportunityId, String expectedStatus) {
        step("Check that CC ProServ Quote.ProServ_Status__c = '" + expectedStatus + "' via API", () -> {
            var ccProServQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ProServ_Status__c, Name " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + opportunityId + "' " +
                            "AND RecordType.Name = '" + CC_PROSERV_QUOTE_RECORD_TYPE + "'",
                    Quote.class);
            assertThat(ccProServQuote.getProServ_Status__c())
                    .as("СС ProServ Quote.ProServ_Status__c value")
                    .isEqualTo(expectedStatus);
        });
    }
}
