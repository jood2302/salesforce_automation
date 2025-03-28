package ngbs;

import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.*;

import static base.Pages.closeWizardPage;
import static base.Pages.opportunityPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.getPrimaryContactOnAccount;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

/**
 * Test methods related to the Sales Flow functionality:
 * e.g. finding Sales Rep users, creating Accounts/Contacts, etc.
 */
public class SalesFlowSteps {
    private final Dataset data;

    public Account account;
    public Contact contact;

    /**
     * New instance for the class with the test methods/steps related Sales Flow functionality.
     *
     * @param data object parsed from the JSON files with the test data
     */
    public SalesFlowSteps(Dataset data) {
        this.data = data;
    }

    /**
     * Get a user with 'Sales Rep - Lightning' profile via SFDC API.
     */
    public User getSalesRepUser() {
        return step("Get a user with 'Sales Rep - Lightning' profile via SFDC API", () -> {
            return getUser().withProfile(SALES_REP_LIGHTNING_PROFILE).execute();
        });
    }

    /**
     * Get a user with 'Deal Desk Lightning' profile via SFDC API.
     */
    public User getDealDeskUser() {
        return step("Get a user with 'Deal Desk Lightning' profile via SFDC API", () -> {
            return getUser().withProfile(DEAL_DESK_LIGHTNING_PROFILE).execute();
        });
    }

    /**
     * Create a new Account, Contact, AccountContactRole via API.
     *
     * @param ownerUser user intended to be the owner of the created records
     */
    public void createAccountWithContactAndContactRole(User ownerUser) {
        step("Create Account with related Contact and AccountContactRole records via SFDC API", () -> {
            account = createAccountInSFDC(ownerUser, new AccountData(data));
            contact = getPrimaryContactOnAccount(account);
        });
    }

    /**
     * Wait until the Close/Downgrade Wizard is loaded
     * after the user clicked 'Close' on the Opportunity record page.
     */
    public void waitUntilCloseWizardIsLoaded() {
        opportunityPage.entityTitle.shouldBe(hidden, ofSeconds(30));
        opportunityPage.spinner.shouldBe(hidden, ofSeconds(30));
        closeWizardPage.switchToIFrame();
        closeWizardPage.whyWeWonBlock.shouldBe(visible, ofSeconds(60));
    }
}
