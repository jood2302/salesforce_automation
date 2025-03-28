package ngbs.opportunitycreation.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.Account;
import com.sforce.soap.enterprise.sobject.SubBrandsMapping__c;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewPartnerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.DefaultBusinessIdentityMappingFactory.createDefaultBusinessIdentityMapping;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.SubBrandsMappingFactory.createNewSubBrandsMapping;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("Ignite")
public class AvailableBusinessIdentitiesLinkedPartnerAccountOnQopTest extends BaseTest {
    private final AvailableBusinessIdentityOnQopSteps availableBiOnQopSteps;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Account customerAccount;
    private Account partnerAccount;
    private SubBrandsMapping__c testSubBrandsMapping;

    //  Test data
    private final String partnerAccountPermittedBrands;

    public AvailableBusinessIdentitiesLinkedPartnerAccountOnQopTest() {
        availableBiOnQopSteps = new AvailableBusinessIdentityOnQopSteps();
        steps = new Steps(availableBiOnQopSteps.data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        partnerAccountPermittedBrands = "RingCentral; RingCentral EU";
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        customerAccount = steps.salesFlow.account;
        availableBiOnQopSteps.setUpCustomerAccountStep(customerAccount);

        step("Create a new test Partner Account with Permitted_Brands__c = '" + partnerAccountPermittedBrands + "', " +
                "RC_Brand__c = 'RingCentral EU' and BillingCountry = 'Germany' via API", () -> {
            partnerAccount = createNewPartnerAccountInSFDC(salesRepUser,
                    new AccountData()
                            .withCurrencyIsoCode(availableBiOnQopSteps.data.currencyISOCode)
                            .withPermittedBrands(partnerAccountPermittedBrands)
                            .withBillingCountry(availableBiOnQopSteps.countryGermany)
                            .withRcBrand(availableBiOnQopSteps.data.brandName)
            );
        });

        step("Set the test Customer Account's fields with Partner Account's fields values via API", () -> {
            customerAccount.setPartner_Account__c(partnerAccount.getId());
            customerAccount.setPartner_Contact__c(partnerAccount.getPartner_Contact__c());
            customerAccount.setPartner_ID__c(partnerAccount.getPartner_ID__c());
            enterpriseConnectionUtils.update(customerAccount);
        });

        step("Create a new SubBrandsMapping__c Custom Setting record for the Partner Account via API", () -> {
            testSubBrandsMapping = createNewSubBrandsMapping(partnerAccount);
        });

        step("Create a new 'Default Business Identity Mapping' record of custom metadata type " +
                "for 'RingCentral Germany' Default Business Identity " +
                "with Available_Business_Identities__c = 'RingCentral, Ltd (France)' " +
                "and Sub_Brand__c = SubBrandsMapping__c.Sub_Brand__c value via API", () -> {
            createDefaultBusinessIdentityMapping(availableBiOnQopSteps.defaultBiMappingLabel, testSubBrandsMapping.getSub_Brand__c(),
                    availableBiOnQopSteps.data.brandName, availableBiOnQopSteps.countryGermany,
                    availableBiOnQopSteps.rcGermanyDefaultBusinessIdentity, availableBiOnQopSteps.rcFranceBusinessIdentity);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-33689")
    @DisplayName("CRM-33689 - Availability of 'Available Business Identity' from 'Business Identity Mapping' Custom MetaData " +
            "on Quick Opportunity Page for Customer Account linked with Partner Account")
    @Description("Verify that 'Available Business Identity' is retrieved from 'Business Identity Mapping' Custom MetaData " +
            "on Quick Opportunity Page for Customer Account linked with Partner Account")
    public void test() {
        availableBiOnQopSteps.checkBrandAndBiPicklistOnQopForDifferentUsers(customerAccount);
    }
}
