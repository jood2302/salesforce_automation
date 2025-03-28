package ngbs.opportunitycreation.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.AccountContactRole;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.opportunityCreationPage;
import static com.aquiva.autotests.rc.page.opportunity.OpportunityCreationPage.NO_PRIMARY_OR_SIGNATORY_CONTACT_ON_ACCOUNT;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.INFLUENCER_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.SIGNATORY_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ContactHelper.getFullName;
import static com.codeborne.selenide.CollectionCondition.containExactTextsCaseSensitive;
import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P0")
@Tag("QOP")
public class ContactRolesValidationOnQopTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private AccountContactRole accountContactRole;

    public ContactRolesValidationOnQopTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/newbusiness/RC_MVP_Monthly_NonContract_QOP.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);

        step("Set Account's AccountContactRole record fields IsPrimary = false and Role = 'Influencer' via API", () -> {
            accountContactRole = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM AccountContactRole " +
                            "WHERE AccountId = '" + steps.salesFlow.account.getId() + "' " +
                            "AND ContactId = '" + steps.salesFlow.contact.getId() + "'",
                    AccountContactRole.class);

            accountContactRole.setIsPrimary(false);
            accountContactRole.setRole(INFLUENCER_ROLE);
            enterpriseConnectionUtils.update(accountContactRole);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-21380")
    @DisplayName("CRM-21380 - Primary & Signatory Contact Role is required for Opportunity Creation")
    @Description("Verify that Opportunity can't be created via QOP " +
            "if there's no Primary & Signatory Account Contact Role on the selected Account")
    public void test() {
        step("1. Open QOP for the Account without Primary/Signatory contact role, and check the error message", () -> {
            opportunityCreationPage.openPage(steps.salesFlow.account.getId());

            opportunityCreationPage.notificationBarErrorIcon.shouldBe(visible);
            opportunityCreationPage.notificationBarNotifications
                    .should(containExactTextsCaseSensitive(NO_PRIMARY_OR_SIGNATORY_CONTACT_ON_ACCOUNT));
        });

        step("2. Set Account's AccountContactRole record fields IsPrimary = true and Role = 'Signatory' via API", () -> {
            accountContactRole.setIsPrimary(true);
            accountContactRole.setRole(SIGNATORY_ROLE);
            enterpriseConnectionUtils.update(accountContactRole);
        });

        step("3. Reload the QOP, and verify that Contact is preselected " +
                "and there's no error message about Primary or Signatory Contact Role", () -> {
            refresh();

            var contactFullName = getFullName(steps.salesFlow.contact);
            opportunityCreationPage.contactInputWithSelectedValue
                    .shouldHave(exactText(contactFullName), ofSeconds(60));
            opportunityCreationPage.notificationBarErrorIcon.shouldBe(hidden);
            opportunityCreationPage.notificationBarNotifications.shouldBe(empty);
        });
    }
}
