package ngbs.opportunitycreation.newbusiness;

import base.SfdcSteps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.codeborne.selenide.WebElementsCondition;
import com.sforce.soap.enterprise.sobject.Account;

import java.util.List;

import static base.Pages.opportunityCreationPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.SYSTEM_ADMINISTRATOR_PROFILE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.getUser;
import static com.codeborne.selenide.CollectionCondition.containExactTextsCaseSensitive;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;

/**
 * Test methods related for checks of preselected and available business identities
 * in Business Identity picklist on Quick Opportunity page
 * depending on the user's profile.
 */
public class AvailableBusinessIdentityOnQopSteps {
    public final Dataset data;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final SfdcSteps sfdcSteps;

    //  Test data
    public final String countryGermany;
    public final String rcGermanyDefaultBusinessIdentity;
    public final String rcFranceBusinessIdentity;
    private final List<String> allCustomMetadataBusinessIdentities;
    private final List<String> otherBusinessIdentities;
    public final String defaultBiMappingLabel;

    public AvailableBusinessIdentityOnQopSteps() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_EU_MVP_Monthly_Contract_RegularAndPOC.json",
                Dataset.class);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        sfdcSteps = new SfdcSteps();

        countryGermany = "Germany";
        rcGermanyDefaultBusinessIdentity = "RingCentral Germany";
        rcFranceBusinessIdentity = data.businessIdentity.name;
        allCustomMetadataBusinessIdentities = List.of(rcGermanyDefaultBusinessIdentity, rcFranceBusinessIdentity);
        otherBusinessIdentities = List.of("RingCentral Singapore", "Avaya Cloud Office", "Unify Office UK", "Amazon US");
        defaultBiMappingLabel = rcGermanyDefaultBusinessIdentity;
    }

    /**
     * Prepare the Customer Account to check BI picklist on QOP.
     *
     * @param account a Customer Account for which QOP is checked
     */
    public void setUpCustomerAccountStep(Account account) {
        step("Set Customer Account.BillingCountry__c = '" + countryGermany + "' " +
                "and RC_Brand__c = '" + data.brandName + "' via API", () -> {
            account.setBillingCountry(countryGermany);
            account.setRC_Brand__c(data.brandName);
            enterpriseConnectionUtils.update(account);
        });
    }

    /**
     * <p> - Open the QOP for the Customer Account and check Brand field </p>
     * <p> - Check that Business Identity is enabled and has preselected value = 'RingCentral Germany' </p>
     * <p> - Check list of business identities and that each business identity is available to select </p>
     *
     * @param account a Customer Account for which QOP is checked
     */
    public void checkBrandAndBiPicklistOnQopForDifferentUsers(Account account) {
        step("1. Open the QOP for the Customer Account, check Brand field value, " +
                "check that BI picklist is enabled and has preselected option = '" + rcGermanyDefaultBusinessIdentity + "', " +
                "check the list of available business identities, " +
                "and that each business identity is available to select", () -> {
            checkBrandAndBiPicklistOnQopForUser(account.getId(), exactTextsCaseSensitiveInAnyOrder(allCustomMetadataBusinessIdentities));
        });

        step("2. Re-login as a user with 'System Administrator' profile and 'Enable Business Identity Mapping' Feature Toggle", () -> {
            var sysAdminUser = getUser().withProfile(SYSTEM_ADMINISTRATOR_PROFILE).execute();
            sfdcSteps.reLoginAsUser(sysAdminUser);
        });

        step("3. Open the QOP for the Customer Account, check Brand field value, " +
                "check that BI picklist is enabled and has preselected option = '" + rcGermanyDefaultBusinessIdentity + "', " +
                "check that all business identities are present in the picklist, " +
                "and that each business identity is available to select", () -> {
            checkBrandAndBiPicklistOnQopForUser(account.getId(), containExactTextsCaseSensitive(otherBusinessIdentities));
        });
    }

    /**
     * <p> - Open the QOP for the Customer Account and check Brand field </p>
     * <p> - Check that Business Identity is enabled and has preselected value = 'RingCentral Germany' </p>
     * <p> - Check list of business identities and that each business identity is available to select </p>
     *
     * @param accountId          ID of the Customer Account for which QOP is checked
     * @param biOptionsCondition a condition to check the list of business identities
     */
    private void checkBrandAndBiPicklistOnQopForUser(String accountId, WebElementsCondition biOptionsCondition) {
        step("Open the QOP for the Customer Account " +
                "and check that Brand field value is equal to '" + data.brandName + "'", () -> {
            opportunityCreationPage.openPage(accountId);

            opportunityCreationPage.brandOutput.shouldHave(exactTextCaseSensitive(data.brandName));
        });

        step("Check that BI picklist is enabled and has preselected value = '" + rcGermanyDefaultBusinessIdentity + "', " +
                "and check list of business identities and that each business identity is available to select", () -> {
            opportunityCreationPage.businessIdentityPicklist
                    .shouldBe(enabled)
                    .getSelectedOption().shouldHave(exactTextCaseSensitive(rcGermanyDefaultBusinessIdentity));
            opportunityCreationPage.businessIdentityPicklist.getOptions().asDynamicIterable()
                    .forEach(businessIdentity -> businessIdentity.shouldBe(enabled));
            opportunityCreationPage.businessIdentityPicklist.getOptions().should(biOptionsCondition);
        });
    }
}
