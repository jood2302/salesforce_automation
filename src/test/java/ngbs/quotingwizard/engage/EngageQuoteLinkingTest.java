package ngbs.quotingwizard.engage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountContactRoleFactory.createAccountContactRole;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.INFLUENCER_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.AGREEMENT_QUOTE_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.setQuoteToApprovedActiveAgreement;
import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("Engage")
@Tag("QuoteLinking")
public class EngageQuoteLinkingTest extends BaseTest {
    private final Steps steps;
    private final AccountBindingsSteps accountBindingsSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User dealDeskUser;
    private Account engageAccount;
    private Contact engageContact;
    private Opportunity engageOpportunity;
    private Account officeAccount;
    private Opportunity officeOpportunity;
    private Quote officeQuote;

    //  Test data
    private final String chargeTerm;
    private final String officeServiceName;
    private final Product concurrentSeatOmniChannel;
    private final Package officePackage;
    private final Product officePhone;
    private final Product dlUnlimited;
    private final String officeInitialTerm;

    public EngageQuoteLinkingTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_EngageDSAndMVP_Monthly_Contract_WithProducts.json",
                Dataset.class);

        steps = new Steps(data);
        accountBindingsSteps = new AccountBindingsSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        chargeTerm = data.chargeTerm;
        officeServiceName = data.packageFolders[1].name;
        concurrentSeatOmniChannel = data.getProductByDataName("SA_SEAT_5");

        officePackage = data.packageFolders[1].packages[0];
        officePhone = data.getProductByDataName("LC_HDR_619", officePackage);
        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50", officePackage);
        officeInitialTerm = officePackage.contractTerms.initialTerm[0];
    }

    @BeforeEach
    public void setUpTest() {
        dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        engageAccount = steps.salesFlow.account;
        engageContact = steps.salesFlow.contact;

        steps.quoteWizard.createOpportunity(engageAccount, engageContact, dealDeskUser);
        engageOpportunity = steps.quoteWizard.opportunity;

        accountBindingsSteps.createOfficeAccountRecordsForBinding(dealDeskUser);
        officeAccount = accountBindingsSteps.officeAccount;
        officeOpportunity = accountBindingsSteps.officeOpportunity;

        step("Create the second contact role for Office Account with the same Contact " +
                "as on Engage Account's Primary Contact Role via API", () -> {
            createAccountContactRole(officeAccount, engageContact, INFLUENCER_ROLE, false);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);

        step("Open the Quote Wizard for the Office Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(officeOpportunity.getId());

            packagePage.packageSelector.selectPackage(chargeTerm, officeServiceName, officePackage);
            packagePage.saveChanges();

            officeQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Name " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + officeOpportunity.getId() + "'",
                    Quote.class);
        });
    }

    @Test
    @TmsLink("CRM-20438")
    @DisplayName("CRM-20438 - Quotes can be linked from the Quote Wizard")
    @Description("Verify that Quotes can be linked from the Quote Wizard when either:\n" +
            "- Engage and Office Quotes have errors, both in Quote stage \n" +
            "OR \n" +
            "- Office Quote have errors, Engage Quote is in Active Agreement stage and doesn’t have errors \n" +
            "OR \n" +
            "- Engage and Office Quotes don’t have errors and are in Active Agreement stages.")
    public void test() {
        step("1. Open the Quote Wizard for the Engage Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(engageOpportunity.getId())
        );

        step("2. Open the Quote Details tab, open Account Bindings modal window, " +
                "link Office and Engage Accounts and Quotes and check that Accounts and Quotes are linked", () -> {
            quotePage.openTab();
            quotePage.manageAccountBindingsButton.click();
            stepLinkAccountsViaAccountManagerModal();
            stepLinkQuotesViaAccountManagerModal();

            quotePage.submitAccountBindingChanges();

            stepCheckLinkedAccountsInSFDC();
            stepCheckLinkedQuotesInSFDC();
        });

        step("3. Resolve all errors on Engage quote and set its stage to 'Agreement'", () -> {
            step("Create Invoice Request Approval for Engage Account " +
                    "with related 'Accounts Payable' AccountContactRole record, " +
                    "and set Approval__c.Status = 'Approved' (all via API)", () -> {
                createInvoiceApprovalApproved(engageOpportunity, engageAccount, engageContact, dealDeskUser.getId(), false);
            });

            step("Re-open the Quote Wizard, open the Price tab, set quantity for added omni-channel license, save changes, " +
                    "and check that the error notifications don't exist", () -> {
                //  reload here to update the state with existing Invoicing Approval Request
                refresh();
                wizardPage.waitUntilLoaded();

                cartPage.openTab();
                cartPage.setQuantityForQLItem(concurrentSeatOmniChannel.name, concurrentSeatOmniChannel.quantity);
                cartPage.saveChanges();

                cartPage.notifications.shouldBe(empty);
            });

            step("Open the Quote Details tab, populate Start date, set Quote's Stage = 'Agreement', " +
                    "and save changes", () -> {
                quotePage.openTab();
                quotePage.setDefaultStartDate();
                quotePage.stagePicklist.selectOption(AGREEMENT_QUOTE_TYPE);
                quotePage.saveChanges();
            });
        });

        step("4. Unlink quotes via Account Bindings modal window", () -> {
            quotePage.manageAccountBindingsButton.click();
            quotePage.manageAccountBindings.quoteSearchInput.clear();
            quotePage.submitAccountBindingChanges();

            stepCheckUnlinkedQuotesInSFDC();
        });

        step("5. Refresh the Quote Wizard for the Engage Opportunity, open the Quote Details tab, " +
                "link Engage and Office quotes via Account Bindings modal window, " +
                "and check that Quotes are linked in SFDC", () -> {
            refresh();
            wizardPage.waitUntilLoaded();
            packagePage.packageSelector.waitUntilLoaded();

            quotePage.openTab();
            quotePage.manageAccountBindingsButton.click();
            stepLinkQuotesViaAccountManagerModal();

            quotePage.submitAccountBindingChanges();

            stepCheckLinkedQuotesInSFDC();
        });

        step("6. Unlink Accounts and Quotes via Account Bindings modal window", () -> {
            quotePage.manageAccountBindingsButton.click();
            quotePage.manageAccountBindings.accountSearchInput.clear();
            quotePage.manageAccountBindings.notifications.shouldHave(size(0));

            quotePage.submitAccountBindingChanges();

            stepCheckUnlinkedQuotesInSFDC();

            step("Check that Office and Engage Accounts are no longer linked", () -> {
                var engageAccountUpdated = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id, Master_Account__c " +
                                "FROM Account " +
                                "WHERE Id = '" + engageAccount.getId() + "'",
                        Account.class);
                assertThat(engageAccountUpdated.getMaster_Account__c())
                        .as("Engage Account.Master_Account__c value")
                        .isNull();
            });
        });

        step("7. Resolve all errors on Office quote and set its stage to Active Agreement", () -> {
            step("Open the Quote Wizard for the Office Opportunity and add products on the Add Products tab", () -> {
                wizardPage.openPage(officeOpportunity.getId(), officeQuote.getId());

                steps.quoteWizard.addProductsOnProductsTab(officePhone);
            });

            step("Open the Price tab, assign devices to the digital lines, save changes, " +
                    "and check that there are no notifications in the Quote Wizard", () -> {
                cartPage.openTab();
                steps.cartTab.assignDevicesToDLAndSave(officePhone.name, dlUnlimited.name, steps.quoteWizard.localAreaCode,
                        officePhone.quantity);

                cartPage.notificationBar.shouldBe(hidden);
            });

            step("Open the Quote Details tab, populate initial term, Main Area Code, Start Date and save changes", () -> {
                quotePage.openTab();

                quotePage.initialTermPicklist.selectOption(officeInitialTerm);
                quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
                quotePage.setDefaultStartDate();
                quotePage.saveChanges();
            });

            step("Set the Office quote to Active Agreement stage via API", () -> {
                setQuoteToApprovedActiveAgreement(officeQuote);

                enterpriseConnectionUtils.update(officeQuote);
            });
        });

        step("8. Open the Quote Wizard for Engage Opportunity, switch to the Quote Details tab, " +
                "link Engage and Office accounts via Account Bindings modal window " +
                "and check that Accounts are linked in SFDC", () -> {
            var engageQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + engageOpportunity.getId() + "'",
                    Quote.class);
            wizardPage.openPage(engageOpportunity.getId(), engageQuote.getId());
            packagePage.packageSelector.waitUntilLoaded();

            quotePage.openTab();
            quotePage.manageAccountBindingsButton.click();
            stepLinkAccountsViaAccountManagerModal();
            quotePage.manageAccountBindings.quoteSearchInput.getSelf().shouldNot(exist);
            quotePage.submitAccountBindingChanges();

            stepCheckLinkedAccountsInSFDC();
        });
    }

    /**
     * Link Office and Engage Accounts via Account Bindings modal window.
     */
    private void stepLinkAccountsViaAccountManagerModal() {
        step("Select Master Account in Account Binding modal window in search input", () -> {
            quotePage.manageAccountBindings.accountSearchInput.selectItemInCombobox(officeAccount.getName());
            quotePage.manageAccountBindings.notifications.shouldHave(size(0));
        });
    }

    /**
     * Link Office and Engage Quotes via Account Bindings modal window.
     */
    private void stepLinkQuotesViaAccountManagerModal() {
        step("Select Parent Quote in Account Binding modal window in search input", () -> {
            quotePage.manageAccountBindings.quoteSearchInput.selectItemInCombobox(officeOpportunity.getName());
            quotePage.manageAccountBindings.notifications.shouldHave(size(0));
        });
    }

    /**
     * Check that Office and Engage Accounts are linked in SFDC.
     */
    private void stepCheckLinkedAccountsInSFDC() {
        step("Check that Office and Engage Accounts are linked in SFDC", () -> {
            var engageAccountUpdated = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Master_Account__c " +
                            "FROM Account " +
                            "WHERE Id = '" + engageAccount.getId() + "'",
                    Account.class);
            assertThat(engageAccountUpdated.getMaster_Account__c())
                    .as("Engage Account.Master_Account__c value (should be Office Account.Id)")
                    .isEqualTo(officeAccount.getId());
        });
    }

    /**
     * Check that Office and Engage Quotes are linked in SFDC.
     */
    private void stepCheckLinkedQuotesInSFDC() {
        step("Check that Office and Engage Quotes are linked in SFDC", () -> {
            var engageQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ParentQuote__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + engageOpportunity.getId() + "'",
                    Quote.class);
            assertThat(engageQuote.getParentQuote__c())
                    .as("Engage Quote.ParentQuote__c value (should be Office Quote.Id)")
                    .isEqualTo(officeQuote.getId());
        });
    }

    /**
     * Check that Office and Engage Quotes are no longer linked in SFDC.
     */
    private void stepCheckUnlinkedQuotesInSFDC() {
        step("Check that Office and Engage Quotes are no longer linked in SFDC", () -> {
            var engageQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ParentQuote__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + engageOpportunity.getId() + "'",
                    Quote.class);
            assertThat(engageQuote.getParentQuote__c())
                    .as("Engage Quote.ParentQuote__c value (should be null)")
                    .isNull();
        });
    }
}
