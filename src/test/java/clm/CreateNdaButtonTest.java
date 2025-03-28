package clm;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.AccountContactRole;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.accountRecordPage;
import static com.aquiva.autotests.rc.page.salesforce.account.AccountRecordPage.PRIMARY_SIGNATORY_CONTACT_ROLE_ERROR;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.ACCOUNTS_PAYABLE_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.SIGNATORY_ROLE;
import static com.codeborne.selenide.Condition.exactText;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("FVT")
@Tag("CLM-Phase2")
public class CreateNdaButtonTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private AccountContactRole accountContactRole;

    //  Test data
    private final String brandName;

    public CreateNdaButtonTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_NoPhones.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        brandName = data.brandName;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);

        step("Set Account.RC_Brand__c = '" + brandName + "' via API", () -> {
            steps.salesFlow.account.setRC_Brand__c(brandName);
            enterpriseConnectionUtils.update(steps.salesFlow.account);
        });

        step("Set AccountContactRole.Role = 'Accounts Payable' via API", () -> {
            accountContactRole = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, IsPrimary, Role " +
                            "FROM AccountContactRole " +
                            "WHERE AccountId = '" + steps.salesFlow.account.getId() + "' " +
                            "AND ContactId = '" + steps.salesFlow.contact.getId() + "'",
                    AccountContactRole.class);

            accountContactRole.setRole(ACCOUNTS_PAYABLE_ROLE);
            enterpriseConnectionUtils.update(accountContactRole);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-28743")
    @DisplayName("CRM-28743 - Validate 'Create NDA' button on Account Page Layout is non-functional " +
            "& throws error on-click when Account contact role != Primary + Signatory")
    @Description("Verify that 'Create NDA' button on Account Page Layout is functional only when " +
            "Account contact role = Primary + Signatory")
    public void test() {
        step("1. Open the Account record page, click 'Create NDA' button, and verify the error notification", () -> {
            accountRecordPage.openPage(steps.salesFlow.account.getId());
            accountRecordPage.clickCreateNdaButton();
            accountRecordPage.notificationTitle.shouldHave(exactText(PRIMARY_SIGNATORY_CONTACT_ROLE_ERROR));
            accountRecordPage.notificationCloseButton.click();
        });

        step("2. Set AccountContactRole's Role = 'Signatory' and IsPrimary = false via API, " +
                "click 'Create NDA' button, and verify the error notification", () -> {
            accountContactRole.setRole(SIGNATORY_ROLE);
            accountContactRole.setIsPrimary(false);
            enterpriseConnectionUtils.update(accountContactRole);

            accountRecordPage.clickCreateNdaButton();
            accountRecordPage.notificationTitle.shouldHave(exactText(PRIMARY_SIGNATORY_CONTACT_ROLE_ERROR));
        });
    }
}
