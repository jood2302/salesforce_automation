package ngbs.quotingwizard.existingbusiness.packagetab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.packagePage;
import static base.Pages.wizardPage;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("PackageTab")
@Tag("UQT")
public class MeetingsServiceSwitchToOfficeTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String chargeTerm;
    private final String meetingsService;
    private final String officeService;
    private final Package officePackage;

    public MeetingsServiceSwitchToOfficeTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_Meetings_Free_MVP_Monthly_NonContract_181715013.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        chargeTerm = data.chargeTerm;
        meetingsService = data.packageFolders[0].name;
        officeService = data.packageFoldersUpgrade[0].name;
        officePackage = data.packageFoldersUpgrade[0].packages[0];
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();

        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-28023")
    @DisplayName("CRM-28023 - Meetings can be switched to Office on Select Package tab")
    @Description("Verify that Meetings service can be switched to Office on Select Package tab")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity, verify value of Service picklist " +
                "and that Service picklist is not disabled", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());

            packagePage.packageSelector.packageFilter.servicePicklist
                    .shouldBe(enabled)
                    .getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(meetingsService));
        });

        step("2. Click on Service picklist and check that Meetings and Office options are available in the picklist", () -> {
            packagePage.packageSelector.packageFilter.servicePicklist.click();

            packagePage.packageSelector.packageFilter.servicePicklist.getOptions()
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(meetingsService, officeService));
        });

        step("3. Select an Office Package, save changes and check that Quote.Enabled_LBO__c = true", () -> {
            packagePage.packageSelector.packageFilter.servicePicklist.selectOption(officeService);
            packagePage.packageSelector.selectPackage(chargeTerm, officeService, officePackage);
            packagePage.saveChanges();

            var quote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Enabled_LBO__c " +
                            "FROM Quote " +
                            "WHERE Id = '" + wizardPage.getSelectedQuoteId() + "'",
                    Quote.class);
            assertThat(quote.getEnabled_LBO__c())
                    .as("Quote.Enabled_LBO__c value")
                    .isTrue();
        });
    }
}
