package leads;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Case;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.salesforce.cases.modal.CreateCaseModal.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.CaseHelper.DEAL_AND_ORDER_SUPPORT_RECORD_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.LeadHelper.getFullName;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.exactValue;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverConditions.urlContaining;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("CaseManagement")
public class CreateDosCaseFromLeadRecordPageTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public CreateDosCaseFromLeadRecordPageTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createSalesLead(salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-32830")
    @DisplayName("CRM-32830 - Create DOS Case from the Lead page")
    @Description("Verify that Deal and Order Support case can be created from the Lead page by clicking Create a Case button")
    public void test() {
        step("1. Open the test Lead record page, click 'Create a Case' button " +
                "and check that Case creation modal window is opened in a new tab", () -> {
            leadRecordPage.openPage(steps.leadConvert.salesLead.getId());
            leadRecordPage.clickCreateCaseButton();
            switchTo().window(1, ofSeconds(20));

            createCaseModal.waitUntilLoaded();
        });

        step("2. Check pre-populated field values in Case creation modal", () -> {
            createCaseModal.caseRecordTypeOutput.shouldHave(exactTextCaseSensitive(DEAL_AND_ORDER_SUPPORT_RECORD_TYPE));
            createCaseModal.leadLookup.getInput().shouldHave(exactValue(getFullName(steps.leadConvert.salesLead)));
            createCaseModal.caseCategoryPicklist.getInput().shouldHave(exactTextCaseSensitive(SYSTEM_ISSUE_CATEGORY));
            createCaseModal.caseOriginOutput.shouldHave(exactTextCaseSensitive(LEAD_ORIGIN));
            createCaseModal.caseSubcategoryPicklist.getInput().shouldHave(exactTextCaseSensitive(LEAD_SUBCATEGORY));
        });

        step("3. Populate Subject and Description fields, click 'Save' button " +
                "and check that a new Case record is created with the correct Record Type and the Owner", () -> {
            createCaseModal.subjectInput.setValue(TEST_STRING + UUID.randomUUID());
            createCaseModal.description.setValue(TEST_STRING + UUID.randomUUID());
            createCaseModal.saveChanges();

            casePage.waitUntilLoaded();
            //  to get a proper Case record page url that contains 'Case' object name
            refresh();
            webdriver().shouldHave(urlContaining("Case"), ofSeconds(10));
            casePage.waitUntilLoaded();

            var createdCase = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, RecordType.Name, OwnerId " +
                            "FROM Case " +
                            "WHERE Id = '" + casePage.getCurrentRecordId() + "'",
                    Case.class);
            assertThat(createdCase.getRecordType().getName())
                    .as("Case.RecordType.Name value")
                    .isEqualTo(DEAL_AND_ORDER_SUPPORT_RECORD_TYPE);
            assertThat(createdCase.getOwnerId())
                    .as("Case.OwnerId value")
                    .isNotBlank();
        });
    }
}
