package ngbs.quotingwizard.engage;

import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Account;
import com.sforce.ws.ConnectionException;
import io.qameta.allure.Step;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test methods related to the Engage functionality.
 */
public class EngageSteps {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    /**
     * New instance for the class with the test methods/steps related to the Engage functionality.
     */
    public EngageSteps() {
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    /**
     * Link Engage and Office accounts via API
     * through populating 'Master Account__c' field value
     * on Engage account with Office account ID.
     *
     * @param engageAccount Account object with Engage service
     * @param officeAccount Account object with Office service
     * @throws ConnectionException in case of errors while accessing API
     */
    @Step("Link Engage and Office Accounts via SFDC API")
    public void linkAccounts(Account engageAccount, Account officeAccount) throws ConnectionException {
        engageAccount.setMaster_Account__c(officeAccount.getId());
        enterpriseConnectionUtils.update(engageAccount);
    }

    /**
     * Get Account's 'Internal_Enterprise_Account_ID__c' field value and compare it
     * with provided expected value.
     *
     * @param expectedValue Internal Enterprise Account ID expected value
     * @param accountId     ID of Account that will be queried
     * @throws ConnectionException in case of errors while accessing API
     */
    @Step("Check Internal_Enterprise_Account_ID__c field for the given Account")
    public void stepCheckInternalEnterpriseAccountId(String expectedValue, String accountId) throws ConnectionException {
        var engageAccount = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id, Internal_Enterprise_Account_ID__c " +
                        "FROM Account " +
                        "WHERE Id = '" + accountId + "'",
                Account.class);
        assertThat(engageAccount.getInternal_Enterprise_Account_ID__c())
                .as("Account.Internal_Enterprise_Account_ID__c value")
                .isEqualTo(expectedValue);
    }
}
