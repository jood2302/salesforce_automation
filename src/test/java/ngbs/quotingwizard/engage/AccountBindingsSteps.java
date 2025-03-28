package ngbs.quotingwizard.engage;

import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.*;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewCustomerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityFactory.createOpportunity;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.getPrimaryContactOnAccount;
import static io.qameta.allure.Allure.step;

/**
 * Test methods for test cases related to Engage Account Bindings.
 */
public class AccountBindingsSteps {
    private final Dataset data;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public Account officeAccount;
    public Contact officeAccountContact;
    public Opportunity officeOpportunity;

    //  Test data
    private final String officeAccountService;

    /**
     * New instance for the class with the test methods/steps related to Engage Account Bindings.
     *
     * @param data object parsed from the JSON files with the test data
     */
    public AccountBindingsSteps(Dataset data) {
        this.data = data;
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        officeAccountService = data.packageFolders[1].name;
    }

    /**
     * Create new Office Account, Contact and Opportunity to link them with Engage objects.
     *
     * @param ownerUser user intended to be the owner of the records
     */
    public void createOfficeAccountRecordsForBinding(User ownerUser) {
        step("Create New Business Account (Office Service) with related Contact and AccountContactRole via API", () -> {
            officeAccount = createNewCustomerAccountInSFDC(ownerUser, new AccountData(data));
            officeAccountContact = getPrimaryContactOnAccount(officeAccount);
            officeAccount.setService_Type__c(officeAccountService);
            officeAccount.setRC_Service_name__c(officeAccountService);

            enterpriseConnectionUtils.update(officeAccount);
        });

        step("Create Office Opportunity via API", () -> {
            officeOpportunity = createOpportunity(officeAccount, officeAccountContact,
                    true, data.getBrandName(), data.businessIdentity.id,
                    ownerUser, data.getCurrencyIsoCode(), officeAccountService);
        });
    }
}
