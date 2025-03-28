package ngbs.quotingwizard.newbusiness;

import base.SfdcSteps;
import com.aquiva.autotests.rc.model.leadConvert.Datasets;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import com.sforce.ws.ConnectionException;
import ngbs.SalesFlowSteps;
import ngbs.quotingwizard.QuoteWizardSteps;

import static base.Pages.closeWizardPage;
import static base.Pages.opportunityPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.PARTNER_ACCOUNT_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.CLOSED_WON_STAGE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.SALES_REP_LIGHTNING_PROFILE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.getUser;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test steps for test cases that check opportunities created for the Rise America/Rise International Brand.
 */
public class RiseOpportunitySteps {
    public final Dataset data;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final SfdcSteps sfdcSteps;
    private final SalesFlowSteps salesFlowSteps;
    private final QuoteWizardSteps quoteWizardSteps;

    private User salesRepUserFromGspGroup;
    private SubBrandsMapping__c subBrandsMapping;
    private Account partnerAccount;

    public Account customerAccount;
    public Contact customerContact;
    public Opportunity customerOpportunity;

    public RiseOpportunitySteps(int index) {
        var datasets = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/Rise_America_Rise_International_Monthly_NonContract.json",
                Datasets.class);

        data = datasets.dataSets[index];
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        sfdcSteps = new SfdcSteps();
        salesFlowSteps = new SalesFlowSteps(data);
        quoteWizardSteps = new QuoteWizardSteps(data);
    }

    /**
     * Create Customer's Account/Contact/AccountContactRole and Opportunity records via API
     * for the Rise Brand (e.g. "Rise America", "Rise International")
     * and Rise Sub-brand (e.g. "3000.Brightspeed", "2000.Optus"),
     * and login to the SFDC as a Sales Rep user from the GSP Public Group.
     *
     * @param gspPublicGroupNameForUser name of the GSP Public Group to get a user from (e.g. 'Brightspeed Users')
     */
    public void loginAndSetUpRiseOpportunitySteps(String gspPublicGroupNameForUser) {
        step("Find a user with 'Sales Rep - Lightning' profile and membership in the '" + gspPublicGroupNameForUser + "' Public Group", () -> {
            salesRepUserFromGspGroup = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withGroupMembership(gspPublicGroupNameForUser)
                    .execute();
        });

        step("Check that 'SubBrandsMapping__c' record exists for '" + data.subBrandName + "' sub-brand", () -> {
            subBrandsMapping = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, PartnerID__c " +
                            "FROM SubBrandsMapping__c " +
                            "WHERE Sub_Brand__c = '" + data.subBrandName + "'",
                    SubBrandsMapping__c.class);
            assertThat(subBrandsMapping.getPartnerID__c())
                    .as("SubBrandsMapping__c.PartnerID__c value for '" + data.subBrandName + "' sub-brand")
                    .isNotNull();
        });

        step("Check that Partner Account record exists for '" + data.subBrandName + "' sub-brand", () -> {
            partnerAccount = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Name, Partner_ID__c, Partner_Contact__c  " +
                            "FROM Account " +
                            "WHERE Partner_ID__c = '" + subBrandsMapping.getPartnerID__c() + "' " +
                            "AND Type = '" + PARTNER_ACCOUNT_TYPE + "' " +
                            "LIMIT 1",
                    Account.class);
        });

        salesFlowSteps.createAccountWithContactAndContactRole(salesRepUserFromGspGroup);
        customerAccount = salesFlowSteps.account;
        customerContact = salesFlowSteps.contact;

        step("Populate Customer Account's necessary fields " +
                "(RC_Brand__c, Sub_Brand__c, Partner_Account__c, Partner_Contact__c, Partner_ID__c, " +
                "Ultimate_Partner_Name__c, Ultimate_Partner_ID__c, GSP__c) via API", () -> {
            customerAccount.setRC_Brand__c(data.brandName);
            customerAccount.setSub_Brand__c(data.subBrandName);
            customerAccount.setPartner_Account__c(partnerAccount.getId());
            customerAccount.setPartner_Contact__c(partnerAccount.getPartner_Contact__c());
            customerAccount.setPartner_ID__c(partnerAccount.getPartner_ID__c());
            customerAccount.setUltimate_Partner_Name__c(partnerAccount.getName());
            customerAccount.setUltimate_Partner_ID__c(partnerAccount.getPartner_ID__c());
            customerAccount.setGSP__c(true);

            enterpriseConnectionUtils.update(customerAccount);
        });

        step("Create a test Opportunity with Brand_Name__c = '" + data.brandName + "' " +
                "and Sub_Brand__c = '" + data.subBrandName + "' via API", () -> {
            quoteWizardSteps.createOpportunity(customerAccount, customerContact, salesRepUserFromGspGroup);
            customerOpportunity = quoteWizardSteps.opportunity;

            //  Sub_Brand__c is only set automatically on the Lead Conversion and when creating Opportunity via QOP.
            //  We are setting it here manually so that Quoting will be fully valid (e.g. 'Add New' button is enabled on the QW/UQT Landing page)
            customerOpportunity.setSub_Brand__c(data.subBrandName);
            enterpriseConnectionUtils.update(customerOpportunity);
        });

        step("Login as a user with 'Sales Rep - Lightning' profile and membership in the '" + gspPublicGroupNameForUser + "' Public Group", () -> {
            sfdcSteps.initLoginToSfdcAsTestUser(salesRepUserFromGspGroup);
        });
    }

    /**
     * Switch to the Opportunity record page, click 'Close' button,
     * populate required fields in the Close Wizard, submit the form
     * and verify that Opportunity.StageName = '7. Closed Won'.
     *
     * @throws ConnectionException in case of malformed DB queries or network failures
     */
    public void stepCloseOpportunityAndCheckItsStatus() throws ConnectionException {
        opportunityPage.clickCloseButton();

        salesFlowSteps.waitUntilCloseWizardIsLoaded();
        closeWizardPage.submitCloseWizard();
        closeWizardPage.switchFromIFrame();
        opportunityPage.detailsTab.shouldBe(visible, ofSeconds(120));

        var updatedOpportunity = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id, StageName " +
                        "FROM Opportunity " +
                        "WHERE Id = '" + customerOpportunity.getId() + "'",
                Opportunity.class);
        assertThat(updatedOpportunity.getStageName())
                .as("Opportunity.StageName value")
                .isEqualTo(CLOSED_WON_STAGE);
    }
}
