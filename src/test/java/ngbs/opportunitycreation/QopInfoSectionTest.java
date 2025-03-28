package ngbs.opportunitycreation;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.time.LocalDate;

import static base.Pages.opportunityCreationPage;
import static com.aquiva.autotests.rc.page.components.lookup.AngularLookupComponent.NO_RESULTS;
import static com.aquiva.autotests.rc.page.opportunity.OpportunityCreationPage.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountContactRoleFactory.createAccountContactRole;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewCustomerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.BUSINESS_USER_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.INFLUENCER_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.getPrimaryContactOnAccount;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ContactHelper.getFullName;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.SetValueOptions.withDate;
import static io.qameta.allure.Allure.step;
import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("QOP")
public class QopInfoSectionTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Account existingBusinessAccount;
    private Account newBusinessAccount;
    private Contact newBusinessAccountContact;
    private String newBusinessAccountContactFullName;

    //  Test data
    private final LocalDate closeDateInThePast;
    private final String existingNumberOfDLs;
    private final String numberOfDLsForDownsell;
    private final String serviceName;

    public QopInfoSectionTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_Contract_163073013.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        closeDateInThePast = LocalDate.of(2020, 1, 10);
        existingNumberOfDLs = valueOf(data.packageFolders[0].packages[0].productsFromBilling[2].existingQuantity);
        numberOfDLsForDownsell = valueOf(parseInt(existingNumberOfDLs) - 1);
        serviceName = data.packageFolders[0].name;
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
            steps.ngbs.stepCreateContractInNGBS();
        }

        var salesUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesUser);
        existingBusinessAccount = steps.salesFlow.account;

        step("Create New Business account with related Contact and AccountContactRole in SFDC via API", () -> {
            newBusinessAccount = createNewCustomerAccountInSFDC(salesUser, new AccountData(data));
            newBusinessAccountContact = getPrimaryContactOnAccount(newBusinessAccount);
            newBusinessAccountContactFullName = getFullName(newBusinessAccountContact);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesUser);
    }

    @Test
    @TmsLink("CRM-12724")
    @TmsLink("CRM-12832")
    @TmsLink("CRM-13046")
    @TmsLink("CRM-13067")
    @DisplayName("CRM-12724 - Contact is preselected from Primary Contact Role. \n" +
            "CRM-12832 - Contact is not preselected if there is no Primary Contact Role on Account. \n" +
            "CRM-13046 - Close Date validations on QOP. \n" +
            "CRM-13067 - Provisioning details Validations on QOP.")
    @Description("CRM-12724 - Verify that if an Account is selected with a Contact in Primary Contact Role then this Contact is preselected. \n" +
            "CRM-12832 - Verify that no Contact is preselected if there's no Primary Signatory Contact Role on Account " +
            "and a list of Primary or Signatory Contacts is shown. \n" +
            "CRM-13046 - Verify that Close Date field has validations. \n" +
            "CRM-13067 - Verify that Provisioning details are required on Downsell on QOP.")
    public void test() {
        //  For CRM-12724
        step("1. Open QOP for New Business Account with Primary 'Influencer' Contact Role, " +
                "and check that contact is preselected from 'Influencer' contact role on Account", () -> {
            step("Set Account Contact Role to be Primary 'Influencer' via API", () -> {
                var accountContactRole = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id " +
                                "FROM AccountContactRole " +
                                "WHERE AccountId = '" + newBusinessAccount.getId() + "' " +
                                "AND ContactId = '" + newBusinessAccountContact.getId() + "' ",
                        AccountContactRole.class);
                accountContactRole.setRole(INFLUENCER_ROLE);
                accountContactRole.setIsPrimary(true);
                enterpriseConnectionUtils.update(accountContactRole);
            });

            opportunityCreationPage.openPage(newBusinessAccount.getId());

            opportunityCreationPage.contactInputWithSelectedValue
                    .shouldHave(exactText(newBusinessAccountContactFullName), ofSeconds(10));
        });

        //  For CRM-13046
        step("2. Check the preselected Business Identity, select 'Office' service, " +
                "and make sure that Close Date field is empty", () -> {
            opportunityCreationPage.businessIdentityPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(data.getBusinessIdentityName()));
            opportunityCreationPage.servicePicklist.getOptions().shouldHave(itemWithText(serviceName), ofSeconds(20));
            opportunityCreationPage.servicePicklist.selectOption(serviceName);

            opportunityCreationPage.closeDateTextInput.shouldBe(empty);
        });

        //  For CRM-13046
        step("3. Click 'Continue to Opportunity' button and check validation of Close Date input", () -> {
            opportunityCreationPage.continueToOppButton.click();
            opportunityCreationPage.closeDateSection.shouldHave(textCaseSensitive(CLOSE_DATE_IS_REQUIRED_ERROR));
        });

        //  For CRM-13046
        step("4. Select a date in the past and check validation of Close Date input", () -> {
            opportunityCreationPage.closeDateTextInput.setValue(withDate(closeDateInThePast));
            opportunityCreationPage.closeDateSection.shouldHave(textCaseSensitive(CLOSE_DATE_IN_THE_PAST_ERROR));
        });

        //  For CRM-12832
        step("5. Open QOP for New Business Account with a non-primary 'Business User' " +
                "and 'Influencer' contact roles, and check that 'Primary Contact' field is not preselected", () -> {
            step("Set 'Influencer' Account Contact Role 'IsPrimary' flag to 'false', " +
                    "and create non-primary 'Business User' Account Contact Role via API", () -> {
                var influencerAccountContactRole = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id " +
                                "FROM AccountContactRole " +
                                "WHERE AccountId = '" + newBusinessAccount.getId() + "' " +
                                "AND ContactId = '" + newBusinessAccountContact.getId() + "' " +
                                "AND Role = '" + INFLUENCER_ROLE + "' ",
                        AccountContactRole.class);
                influencerAccountContactRole.setIsPrimary(false);
                enterpriseConnectionUtils.update(influencerAccountContactRole);

                createAccountContactRole(newBusinessAccount, newBusinessAccountContact, BUSINESS_USER_ROLE, false);
            });

            opportunityCreationPage.openPage(newBusinessAccount.getId());

            opportunityCreationPage.contactLookupInput.getInput().shouldBe(empty).click();
            opportunityCreationPage.contactLookupInput.getSearchResults()
                    .findBy(exactTextCaseSensitive(NO_RESULTS))
                    .shouldBe(visible);
        });

        //  For CRM-13067
        step("6. Select the Existing Business account, populate Close Date, " +
                "reduce the number of DLs in 'New Number of DLs', press 'Continue to Opportunity', " +
                "and check Provisioning details validation message on QOP", () -> {
            opportunityCreationPage.accountComboboxInput.selectItemInCombobox(existingBusinessAccount.getName());

            opportunityCreationPage.populateCloseDate();

            //  to avoid returning of previous value in 'New Number of DLs' input
            opportunityCreationPage.existingNumberOfDLsOutput.shouldHave(exactValue(existingNumberOfDLs), ofSeconds(30));
            opportunityCreationPage.newNumberOfDLsInput
                    .shouldBe(visible, ofSeconds(60))
                    .setValue(numberOfDLsForDownsell);
            opportunityCreationPage.continueToOppButton.click();

            opportunityCreationPage.provisioningDetailsError
                    .shouldHave(exactTextCaseSensitive(PROVISIONING_DETAILS_ARE_REQUIRED_FOR_DOWNSELL_ERROR));
        });
    }
}
