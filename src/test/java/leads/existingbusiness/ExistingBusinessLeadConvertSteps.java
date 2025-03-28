package leads.existingbusiness;

import base.NgbsSteps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.*;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createExistingCustomerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.getPrimaryContactOnAccount;
import static io.qameta.allure.Allure.step;

/**
 * Test methods related to the test cases for Lead Convert flow with Existing Business Accounts.
 */
public class ExistingBusinessLeadConvertSteps {
    public final Dataset data;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final NgbsSteps ngbsSteps;

    public Account existingBusinessAccount;
    public Contact existingBusinessAccountContact;

    /**
     * New instance for the class with the test methods/steps for the test cases
     * for Lead Convert flow with RingCentral Existing Business Accounts.
     */
    public ExistingBusinessLeadConvertSteps(Dataset data) {
        this.data = data;
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        ngbsSteps = new NgbsSteps(data);
    }

    /**
     * Preconditions for the test cases related Lead Convert flow for the Existing Business Accounts:
     * create the Existing Business Account/Contact/AccountContactRole,
     * create the account's contract in the NGBS,
     * make the test Lead and the EB Account match via the same email.
     *
     * @param lead      lead to be converted
     * @param ownerUser owner of the records that are created here
     */
    public void setUpSteps(Lead lead, User ownerUser) {
        if (ngbsSteps.isGenerateAccounts()) {
            ngbsSteps.generateBillingAccount();

            if (!data.packageFolders[0].packages[0].contract.equals("None") &&
                    !data.packageFolders[0].packages[0].contract.isBlank()) {
                ngbsSteps.stepCreateContractInNGBS();
            }
        }

        step("Create Existing Business Account with related Contact and AccountContactRole via API", () -> {
            existingBusinessAccount = createExistingCustomerAccountInSFDC(ownerUser, new AccountData(data));
            existingBusinessAccountContact = getPrimaryContactOnAccount(existingBusinessAccount);
        });

        //  to be able to select the existing account from the 'Matched Accounts' list instead of flaky Account Lookup
        step("Set the same email on the test Lead and the Account's Contact via API", () -> {
            lead.setEmail(existingBusinessAccountContact.getEmail());
            enterpriseConnectionUtils.update(lead);
        });
    }
}
