package ngbs.quotingwizard;

import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.*;

import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomPositiveInteger;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountContactRoleFactory.createAccountContactRole;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewPartnerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.ACCOUNTS_PAYABLE_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.ACTIVE_PARTNER_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.getPrimaryContactOnAccount;
import static io.qameta.allure.Allure.step;

/**
 * Test methods for test cases with partner's payment methods (e.g. "Bill-on-Behalf").
 */
public class PartnerPaymentMethodSteps {
    private final Dataset data;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public Account partnerAccount;

    /**
     * New instance of test methods for test cases with partner's payment methods (e.g. "Bill-on-Behalf").
     *
     * @param data object parsed from the JSON files with the test data
     */
    public PartnerPaymentMethodSteps(Dataset data) {
        this.data = data;
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    /**
     * Create a Partner Account for the given partner type
     * with the related Contact and primary 'Signatory' AccountContactRole records,
     * additional non-primary AccountContactRole records for Partner and Customer Accounts,
     * and link Customer and Partner Accounts.
     *
     * @param ngbsPartnerId   unique external ID for the Partner from NGBS
     * @param partnerType     partner's type for the Partner Account (e.g. "Bill-on-Behalf", "Wholesale-Reseller")
     * @param customerAccount a related Customer Account
     * @param customerContact a related Customer Account's Contact
     * @param ownerUser       user intended to be the owner of the created records
     */
    public void setUpPartnerAccountForCustomerAccountTestSteps(Double ngbsPartnerId, String partnerType,
                                                               Account customerAccount, Contact customerContact, User ownerUser) {
        var randomPartnerId = getRandomPositiveInteger();

        step("Create new Partner Account with related Contact and AccountContactRole records, " +
                "and make it '" + partnerType + "' using SFDC API", () -> {
            step("Create '" + partnerType + "' Partner Account with related Contact " +
                    "and primary 'Signatory' AccountContactRole via API", () -> {
                partnerAccount = createNewPartnerAccountInSFDC(ownerUser, new AccountData(data));
            });

            step("Update Partner_Type__c, BusinessIdentity__c, PartnerStatus__c, Partner_ID__c, NGBS_Partner_ID__c fields " +
                    "on the Partner Account via API", () -> {
                partnerAccount.setPartner_Type__c(partnerType);
                partnerAccount.setBusinessIdentity__c(data.getBusinessIdentityName());
                partnerAccount.setPartnerStatus__c(ACTIVE_PARTNER_STATUS);
                partnerAccount.setPartner_ID__c(randomPartnerId);
                partnerAccount.setNGBS_Partner_ID__c(ngbsPartnerId);
                enterpriseConnectionUtils.update(partnerAccount);
            });

            step("Create non-primary 'Accounts Payable' contact role and link it with Partner Account via API", () -> {
                var partnerAccountContact = getPrimaryContactOnAccount(partnerAccount);
                createAccountContactRole(partnerAccount, partnerAccountContact, ACCOUNTS_PAYABLE_ROLE, false);
            });
        });

        step("Update customer Account as '" + partnerType + "' type and link it with Partner Account via API", () -> {
            step("Update Partner_Type__c, BusinessIdentity__c, Partner_ID__c, Partner_Account__c, " +
                    "RC_Attribution_Campaign__c, Attribution_partner_id__c fields on the Customer Account via API", () -> {
                customerAccount.setPartner_Type__c(partnerType);
                customerAccount.setBusinessIdentity__c(data.getBusinessIdentityName());
                customerAccount.setPartner_Account__c(partnerAccount.getId());
                customerAccount.setPartner_ID__c(randomPartnerId);
                customerAccount.setRC_Attribution_Campaign__c(TEST_STRING);
                customerAccount.setAttribution_partner_id__c(partnerAccount.getId());
                enterpriseConnectionUtils.update(customerAccount);
            });

            step("Create non-primary 'Accounts Payable' contact role and link it with Customer Account via API", () -> {
                createAccountContactRole(customerAccount, customerContact, ACCOUNTS_PAYABLE_ROLE, false);
            });
        });
    }
}
