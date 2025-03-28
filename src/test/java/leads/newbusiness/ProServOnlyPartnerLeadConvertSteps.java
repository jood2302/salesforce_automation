package leads.newbusiness;

import base.SfdcSteps;
import com.aquiva.autotests.rc.model.leadConvert.Datasets;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import leads.LeadConvertSteps;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.LeadFactory.createPartnerLeadInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.PARTNER_ACCOUNT_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.SALES_REP_LIGHTNING_PROFILE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.getUser;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test methods/steps related to checking the Lead Conversion for the ProServ Only brands
 * (e.g. Rise America/International, Vodafone Business with RingCentral).
 */
public class ProServOnlyPartnerLeadConvertSteps {
    public final Dataset data;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final SfdcSteps sfdcSteps;
    private final LeadConvertSteps leadConvertSteps;

    private Account partnerAccount;
    public Lead partnerLead;
    private User testSalesRepUser;
    private SubBrandsMapping__c testSubBrandsMapping;

    //  Test data
    public final String businessIdentityName;
    public final String billingCountry;
    private final String serviceName;

    public ProServOnlyPartnerLeadConvertSteps(int index) {
        var datasets = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/Rise_America_Rise_International_Vodafone_with_RingCentral_NB.json",
                Datasets.class);
        data = datasets.dataSets[index];
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        sfdcSteps = new SfdcSteps();
        leadConvertSteps = new LeadConvertSteps(data);

        businessIdentityName = data.getBusinessIdentityName();
        billingCountry = data.getBillingCountry();
        serviceName = data.packageFolders[0].name;
    }

    /**
     * Prepare the test data (get a test user and a Partner Account and create a Partner Lead)
     * for the Partner Lead Conversion without a specified sub-brand.
     *
     * @param testUsersGroup a Public Group name where the test user is a member
     */
    public void preparePartnerLeadConversionSteps(String testUsersGroup) {
        step("Find a user with 'Sales Rep - Lightning' profile and a member of the '" + testUsersGroup + "' Public Group", () -> {
            testSalesRepUser = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withGroupMembership(testUsersGroup)
                    .execute();
        });

        step("Check that Partner Account record exists for '" + data.brandName + "' brand", () -> {
            partnerAccount = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Partner_Contact__c, Permitted_Brands__c, CurrencyIsoCode, Partner_ID__c " +
                            "FROM Account " +
                            "WHERE Partner_ID__c = null " +
                            "AND Permitted_Brands__c = '" + data.brandName + "' " +
                            "AND Type = '" + PARTNER_ACCOUNT_TYPE + "' " +
                            "LIMIT 1",
                    Account.class);
        });

        step("Create test Partner Lead for " + partnerAccount.getName() + " Partner Account " +
                "with Lead.Country__c = '" + billingCountry + "' with via API", () -> {
            partnerLead = createPartnerLeadInSFDC(testSalesRepUser, partnerAccount, leadConvertSteps.tierName);

            partnerLead.setCountry__c(billingCountry);
            enterpriseConnectionUtils.update(partnerLead);
        });

        step("Login as a user with 'Sales Rep - Lightning' profile and a member of the '" + testUsersGroup + "' Public Group", () -> {
            sfdcSteps.initLoginToSfdcAsTestUser(testSalesRepUser);
        });
    }

    /**
     * Prepare the test data (get a test user and a Partner Account and create a Partner Lead)
     * for the Partner Lead Conversion for the provided test sub-brand.
     *
     * @param testUsersGroup a Public Group name where the test user is a member
     * @param testSubBrand   a sub-brand name to check the SubBrandsMapping__c record presence
     */
    public void preparePartnerLeadConversionSteps(String testUsersGroup, String testSubBrand) {
        step("Find a user with 'Sales Rep - Lightning' profile and a member of the '" + testUsersGroup + "' Public Group", () -> {
            testSalesRepUser = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withGroupMembership(testUsersGroup)
                    .execute();
        });

        step("Check that SubBrandsMapping__c record exists for '" + testSubBrand + "' sub-brand", () -> {
            testSubBrandsMapping = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, PartnerID__c " +
                            "FROM SubBrandsMapping__c " +
                            "WHERE Sub_Brand__c = '" + testSubBrand + "'",
                    SubBrandsMapping__c.class);

            assertThat(testSubBrandsMapping.getPartnerID__c())
                    .as("SubBrandsMapping__c.PartnerID__c value for '" + testSubBrand + "' sub-brand")
                    .isNotNull();
        });

        step("Check that Partner Account record exists for '" + testSubBrand + "' sub-brand", () -> {
            partnerAccount = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Partner_Contact__c, Permitted_Brands__c, CurrencyIsoCode, Partner_ID__c " +
                            "FROM Account " +
                            "WHERE Partner_ID__c = '" + testSubBrandsMapping.getPartnerID__c() + "' " +
                            "AND Type = '" + PARTNER_ACCOUNT_TYPE + "' " +
                            "LIMIT 1",
                    Account.class);
        });

        step("Create test Partner Lead for " + partnerAccount.getName() + " Partner Account " +
                "with Lead.Country__c = '" + billingCountry + "' via API", () -> {
            partnerLead = createPartnerLeadInSFDC(testSalesRepUser, partnerAccount, leadConvertSteps.tierName);

            partnerLead.setCountry__c(billingCountry);
            enterpriseConnectionUtils.update(partnerLead);
        });

        step("Login as a user with 'Sales Rep - Lightning' profile and a member of the '" + testUsersGroup + "' Public Group", () -> {
            sfdcSteps.initLoginToSfdcAsTestUser(testSalesRepUser);
        });
    }

    /**
     * <p> - Open Lead Conversion page for the test Partner Lead </p>
     * <p> - Check the selected options in Business Identity and Service picklists </p>
     * <p> - Convert the Lead and check that converted Account's, Opportunity's and Contact's fields
     * are populated with expected values. </p>
     *
     * @param partnerLead a provided test Partner Lead to check and convert
     */
    public void proServOnlyPartnerLeadConvertTestSteps(Lead partnerLead) {
        step("1. Open Lead Convert page for the test lead", () -> {
            leadConvertPage.openPage(partnerLead.getId());
        });

        step("2. Switch the toggle into 'Create new account position'", () ->
                leadConvertPage.newExistingAccountToggle.click()
        );

        step("3. Click 'Edit' in Opportunity Section, check the Brand, Business Identity, and Service field values, " +
                "populate Close Date field and click 'Apply' button", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            //  TODO Known Issue PBC-25470 ('Edit' button in Opportunity section is not clickable due to 'Unhandled error' message, but it should be)
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.brandNameOutputField.shouldHave(exactTextCaseSensitive(data.brandName));
            leadConvertPage.businessIdentityPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(businessIdentityName));
            leadConvertPage.servicePickList.getSelectedOption().shouldHave(exactTextCaseSensitive(serviceName));

            leadConvertPage.closeDateDatepicker.setTomorrowDate();

            leadConvertPage.opportunityInfoApplyButton.click();
        });

        step("4. Press 'Convert' button",
                leadConvertSteps::pressConvertButton
        );

        step("5. Check that the fields on the converted Account, Contact, and Opportunity " +
                "are populated with values from thae converted Lead", () -> {
            var convertedLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ConvertedAccountId, ConvertedOpportunityId, ConvertedContactId " +
                            "FROM Lead " +
                            "WHERE Id = '" + partnerLead.getId() + "'",
                    Lead.class);

            step("Check that Opportunity is created with the same Name, Brand_Name__c and Tier_Name__c field values " +
                    "as on the Lead Conversion page", () -> {
                var opportunityFromLead = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id, Name, Brand_Name__c, Tier_Name__c " +
                                "FROM Opportunity " +
                                "WHERE Id = '" + convertedLead.getConvertedOpportunityId() + "'",
                        Opportunity.class);

                assertThat(opportunityFromLead.getName())
                        .as("Opportunity.Name value")
                        .isEqualTo(partnerLead.getCompany());
                assertThat(opportunityFromLead.getBrand_Name__c())
                        .as("Opportunity.Brand_Name__c value")
                        .isEqualTo(data.brandName);
                assertThat(opportunityFromLead.getTier_Name__c())
                        .as("Opportunity.Tier_Name__c value")
                        .isEqualTo(serviceName);
            });

            step("Check that Account is created with the same Name, RC_Brand__c and BillingAddress field values " +
                    "as on the test Partner Lead", () -> {
                var accountFromLead = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id, Name, RC_Brand__c, BillingAddress " +
                                "FROM Account " +
                                "WHERE Id = '" + convertedLead.getConvertedAccountId() + "'",
                        Account.class);

                assertThat(accountFromLead.getName())
                        .as("Account.Name value")
                        .isEqualTo(partnerLead.getCompany());
                assertThat(accountFromLead.getRC_Brand__c())
                        .as("Account.RC_Brand__c value")
                        .isEqualTo(data.brandName);

                step("Check the Billing Address fields on the Account", () -> {
                    assertThat(accountFromLead.getBillingAddress().getCountry())
                            .as("Account.BillingAddress.Country value")
                            .isEqualTo(partnerLead.getCountry());

                    assertThat(accountFromLead.getBillingAddress().getPostalCode())
                            .as("Account.BillingAddress.PostalCode value")
                            .isEqualTo(partnerLead.getPostalCode());

                    assertThat(accountFromLead.getBillingAddress().getState())
                            .as("Account.BillingAddress.State value")
                            .isEqualTo(partnerLead.getState());

                    assertThat(accountFromLead.getBillingAddress().getStreet())
                            .as("Account.BillingAddress.Street value")
                            .isEqualTo(partnerLead.getStreet());
                });
            });

            step("Check that Contact is created with the same FirstName, LastName, Email and Phone field values " +
                    "as on the test Partner Lead", () -> {
                var contactFromLead = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id, FirstName, LastName, Email, Phone " +
                                "FROM Contact " +
                                "WHERE Id = '" + convertedLead.getConvertedContactId() + "'",
                        Contact.class);

                assertThat(contactFromLead.getFirstName())
                        .as("Contact.FirstName value")
                        .isEqualTo(partnerLead.getFirstName());
                assertThat(contactFromLead.getLastName())
                        .as("Contact.LastName value")
                        .isEqualTo(partnerLead.getLastName());
                assertThat(contactFromLead.getEmail())
                        .as("Contact.Email value")
                        .isEqualTo(partnerLead.getEmail());
                assertThat(contactFromLead.getPhone())
                        .as("Contact.Phone value")
                        .isEqualTo(partnerLead.getPhone());
            });
        });
    }
}
