package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.quoteSelectionWizardPage;
import static base.Pages.wizardPage;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("P1")
@Tag("PDV")
@Tag("UQT")
@Tag("NGBS")
public class MakePrimaryButtonTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private String firstQuoteName;
    private String firstQuoteId;
    private String secondQuoteId;

    public MakePrimaryButtonTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_2TypesOfDLs_RegularAndPOC.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-1453")
    @TmsLink("CRM-26431")
    @DisplayName("CRM-1453 - Make Primary button makes Quote Primary. \n" +
            "CRM-26431 - Make Primary and Delete buttons' availability on the Quote selector.")
    @Description("CRM-1453 - Check that after pressing 'Make Primary' button, new Quote will be marked as Primary. \n" +
            "CRM-26431 - Verify that Make Primary and Delete buttons on the Quote selection page are disabled for the current Primary Quote.")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
            steps.quoteWizard.selectPackageFromTestDataAndCreateQuote();

            firstQuoteId = wizardPage.getSelectedQuoteId();
            closeWindow();
        });

        //  CRM-1453
        step("2. Switch back to and refresh the Quote Selection page, " +
                "and check that the current quote is shown as Primary there", () -> {
            switchTo().window(0);
            refresh();
            quoteSelectionWizardPage.waitUntilLoaded();

            quoteSelectionWizardPage.getPrimaryQuoteIcon(firstQuoteId).shouldBe(visible);
            firstQuoteName = quoteSelectionWizardPage.getQuoteName(firstQuoteId).getText();
        });

        step("3. Add a new Sales Quote, select a package for it, and save changes", () -> {
            steps.quoteWizard.addNewSalesQuote();
            steps.quoteWizard.selectPackageFromTestDataAndCreateQuote();
            secondQuoteId = wizardPage.getSelectedQuoteId();
        });

        //  CRM-1453
        step("4. Reload the Quote Selection page and check that the new quote is not primary " +
                "and has different name from the first one", () -> {
            switchTo().window(0);
            refresh();
            quoteSelectionWizardPage.waitUntilLoaded();
            quoteSelectionWizardPage.salesQuotes.shouldHave(size(2));

            //  New Quote should not be primary and has different name from the first one
            quoteSelectionWizardPage.getQuoteName(secondQuoteId).shouldNotHave(exactTextCaseSensitive(firstQuoteName));
            quoteSelectionWizardPage.getPrimaryQuoteIcon(secondQuoteId).shouldNot(exist);

            //  Additionally, check the second quote in SF
            var newQuoteBefore = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, isPrimary__c " +
                            "FROM Quote " +
                            "WHERE Id = '" + secondQuoteId + "' ",
                    Quote.class);

            assertThat(newQuoteBefore.getIsPrimary__c())
                    .as("New Quote.IsPrimary__c value")
                    .isFalse();
        });

        //  CRM-26431
        step("5. Check that the 'Make Primary' and 'Delete' buttons are disabled for the primary quote " +
                "and enabled for the non-primary quote", () -> {
            quoteSelectionWizardPage.getMakePrimaryButton(firstQuoteId).shouldBe(disabled);
            quoteSelectionWizardPage.getDeleteButton(firstQuoteId).shouldBe(disabled);
            quoteSelectionWizardPage.getMakePrimaryButton(secondQuoteId).shouldBe(enabled);
            quoteSelectionWizardPage.getDeleteButton(secondQuoteId).shouldBe(enabled);
        });

        //  CRM-1453
        step("6. Click the 'Make Primary' button on the new Quote and verify that this Quote has become Primary", () -> {
            quoteSelectionWizardPage.makeQuotePrimary(secondQuoteId);
            quoteSelectionWizardPage.getPrimaryQuoteIcon(secondQuoteId).shouldBe(visible);

            var newQuoteAfter = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, isPrimary__c " +
                            "FROM Quote " +
                            "WHERE Id = '" + secondQuoteId + "' ",
                    Quote.class);

            assertThat(newQuoteAfter.getIsPrimary__c())
                    .as("New Quote.isPrimary__c value")
                    .isTrue();
        });

        //  CRM-26431
        step("7. Check that Primary Quote moved on top of the Quote list, " +
                "and the 'Make Primary' and 'Delete' buttons are disabled for the new Primary Quote " +
                "and enabled for the non-primary quote", () -> {
            quoteSelectionWizardPage.salesQuotes.first()
                    .shouldHave(attribute("data-ui-quote-id", secondQuoteId));

            quoteSelectionWizardPage.getMakePrimaryButton(firstQuoteId).shouldBe(enabled);
            quoteSelectionWizardPage.getDeleteButton(firstQuoteId).shouldBe(enabled);
            quoteSelectionWizardPage.getMakePrimaryButton(secondQuoteId).shouldBe(disabled);
            quoteSelectionWizardPage.getDeleteButton(secondQuoteId).shouldBe(disabled);
        });
    }
}
