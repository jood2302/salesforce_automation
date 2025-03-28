package ngbs.opportunitycreation.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.Account;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.opportunityCreationPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewPartnerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.AVAYA_CA_BUSINESS_IDENTITY_NAME;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.AVAYA_US_BUSINESS_IDENTITY_NAME;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.enabled;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("NGBS")
@Tag("Lambda")
@Tag("Avaya")
public class BusinessIdentityPicklistOnQopForPartnerAccountTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Account customerAccount;
    private Account partnerAccount;

    //  Test data
    private final List<String> avayaAvailableBusinessIdentities;

    public BusinessIdentityPicklistOnQopForPartnerAccountTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/newbusiness/Avaya_Office_Monthly_Contract.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        avayaAvailableBusinessIdentities = List.of(AVAYA_US_BUSINESS_IDENTITY_NAME, AVAYA_CA_BUSINESS_IDENTITY_NAME);
    }

    @BeforeEach
    public void setUpTest() {
        var dealDeskUser = steps.salesFlow.getDealDeskUser();

        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        customerAccount = steps.salesFlow.account;

        step("Create a new Partner Account with a related Contact and AccountContactRole " +
                "for the New Business Account via API", () -> {
            partnerAccount = createNewPartnerAccountInSFDC(dealDeskUser, new AccountData(data));
        });

        step("Set Account.Partner_Account__c and Account.Partner_ID__c fields on the Customer Account via API", () -> {
            customerAccount.setPartner_Account__c(partnerAccount.getId());
            customerAccount.setPartner_ID__c(partnerAccount.getId());
            enterpriseConnectionUtils.update(customerAccount);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-13120")
    @DisplayName("CRM-13120 - Business Identity picklist contains corresponding values to Partner Brand on Account")
    @Description("Verify if the Account has RC_Brand__c equal to: \n" +
            "- Avaya Cloud Office \n" +
            "- Unify Office \n\n" +
            "then on the QOP Business Identity picklist only contains values corresponding to its Brand")
    public void test() {
        step("1. Open QOP for the New Business Account with 'Avaya Cloud Office' brand", () -> {
            opportunityCreationPage.openPage(customerAccount.getId());
        });

        step("2. Check that Business Identity picklist is enabled " +
                "and only Avaya Cloud Office-related business identities are available for selection", () -> {
            opportunityCreationPage.businessIdentityPicklist.shouldBe(enabled);
            opportunityCreationPage.businessIdentityPicklist.getOptions()
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(avayaAvailableBusinessIdentities));
        });
    }
}
