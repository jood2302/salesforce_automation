package ngbs.quotingwizard.newbusiness.proservphasetab;

import base.BaseTest;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.page.salesforce.psorder.SuborderItem;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Order;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import ngbs.quotingwizard.ProServInNgbsSteps;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;

import static base.Pages.proServSuborderPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OrderHelper.LOCKED_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.SuborderHelper.BILLED_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.SuborderHelper.FAILED_STATUS;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitive;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("LTR-121")
@Tag("ProServInNGBS")
public class ChangeOrderAndSuborderStatusTest extends BaseTest {
    private final ProServInNgbsSteps proServInNgbsSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private ProServ_Project__c proServProject;
    private Suborder__c proServSuborderWithBilledStatus;
    private Suborder__c proServSuborderWithFailedStatus;

    //  Test data
    private final List<String> proServProductsNamesSorted;
    private final String testSignoffLink;

    public ChangeOrderAndSuborderStatusTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ProServ_Monthly_Contract.json",
                Dataset.class);

        proServInNgbsSteps = new ProServInNgbsSteps(data, data.packageFolders[1]);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        proServProductsNamesSorted = Arrays.stream(proServInNgbsSteps.proServProductsToAdd)
                .map(product -> product.name)
                .sorted()
                .toList();

        testSignoffLink = "test.com";
    }

    @BeforeEach
    public void setUpTest() {
        proServInNgbsSteps.initSignUpOpportunityWithProServServiceInNgbs();
        proServProject = proServInNgbsSteps.getProServProject(proServInNgbsSteps.account.getId());
        proServInNgbsSteps.populateLocationsOnSuborderLineItems(proServProject.getOrder__c());
        proServInNgbsSteps.createChangeOrderFromProServProject(proServInNgbsSteps.account.getId(), proServProject.getId());
    }

    @Test
    @TmsLink("CRM-37549")
    @TmsLink("CRM-37820")
    @TmsLink("CRM-37821")
    @DisplayName("CRM-37549 - Status Change from 'New' to 'Locked'. \n" +
            "CRM-37820 - Status Change from 'New' to 'Billed'. \n" +
            "CRM-37821 - Status Change from 'New' to 'Failed'")
    @Description("CRM-37549 - Verify that: \n" +
            " - When a change order is initiated, the status of the associated Order transitions is changed from 'New' to 'Locked'\n" +
            " - The status of all related suborders transitions is changed from 'New' to 'Locked'\n" +
            " - Once the status changes to 'Locked', the suborder should be non-editable. \n" +
            "CRM-37820 - Verify that: \n" +
            " - When a Process Suborder is initiated, the status of the suborder transitions from 'New' to 'Billed';\n" +
            " - Once the status changes to 'Billed', the suborder should be non-editable.\n" +
            "CRM-37821 - Verify that: \n" +
            " - When a Process Suborder is initiated and a technical issue is happened, " +
            "the status of the suborder transitions from 'New' to 'Failed'; \n" +
            " - Once the status changes to 'Failed', the suborder should be non-editable")
    public void test() {
        //  CRM-37549, CRM-37821
        step("1. Set Suborder__c.Status__c = 'Billed' for the first ProServ suborder " +
                "and Suborder__c.Status__c = 'Failed' and SignoffLink__c for the second ProServ suborder via API " +
                "and check that ProServ Order has Status = 'Locked'", () -> {
            var proServSuborders = enterpriseConnectionUtils.query(
                    "SELECT Id, Status__c " +
                            "FROM Suborder__c " +
                            "WHERE Order__c = '" + proServProject.getOrder__c() + "'",
                    Suborder__c.class);
            proServSuborderWithBilledStatus = proServSuborders.get(0);
            proServSuborderWithFailedStatus = proServSuborders.get(1);

            proServSuborderWithBilledStatus.setStatus__c(BILLED_STATUS);
            proServSuborderWithFailedStatus.setSignoffLink__c(testSignoffLink);
            proServSuborderWithFailedStatus.setStatus__c(FAILED_STATUS);
            enterpriseConnectionUtils.update(proServSuborderWithBilledStatus, proServSuborderWithFailedStatus);

            var proServOrder = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Status " +
                            "FROM Order " +
                            "WHERE Id = '" + proServProject.getOrder__c() + "'",
                    Order.class);
            assertThat(proServOrder.getStatus())
                    .as("Order.Status value")
                    .isEqualTo(LOCKED_STATUS);
        });

        //  CRM-37549
        step("2. Check Suborder__c.Status__c value for Suborder for each Phase", () -> {
            proServSuborderWithBilledStatus = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Status__c " +
                            "FROM Suborder__c " +
                            "WHERE Id = '" + proServSuborderWithBilledStatus.getId() + "'",
                    Suborder__c.class);
            assertThat(proServSuborderWithBilledStatus.getStatus__c())
                    .as("Suborder__c.Status__c value for the first ProServ suborder")
                    .isEqualTo(BILLED_STATUS);

            proServSuborderWithFailedStatus = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Status__c, Name " +
                            "FROM Suborder__c " +
                            "WHERE Id = '" + proServSuborderWithFailedStatus.getId() + "'",
                    Suborder__c.class);
            assertThat(proServSuborderWithFailedStatus.getStatus__c())
                    .as("Suborder__c.Status__c value for the second ProServ suborder")
                    .isEqualTo(FAILED_STATUS);

            var proServSubordersWithUnchangedStatusValues = enterpriseConnectionUtils.query(
                    "SELECT Id, Status__c, Name " +
                            "FROM Suborder__c " +
                            "WHERE Order__c = '" + proServProject.getOrder__c() + "' " +
                            "AND Id NOT IN ('" + proServSuborderWithBilledStatus.getId() + "', " +
                            "'" + proServSuborderWithFailedStatus.getId() + "')",
                    Suborder__c.class);
            assertThat(proServSubordersWithUnchangedStatusValues.size())
                    .as("Number of the remaining ProServ suborders with 'Locked' status")
                    //  4 suborders in total (from 4 phases) - 1 (with Failed status) - 1 (with Billed status) = 2
                    .isEqualTo(2);

            for (var proServSuborder : proServSubordersWithUnchangedStatusValues) {
                assertThat(proServSuborder.getStatus__c())
                        .as("Suborder__c.Status__c value")
                        .isEqualTo(LOCKED_STATUS);
            }
        });

        //  CRM-37549, CRM-37820, CRM-37821
        step("3. Open the ProServ Order page and check that ProServ Suborders " +
                "in statuses 'Billed', 'Failed' and 'Locked' are not editable", () -> {
            proServSuborderPage.openPage(proServProject.getOrder__c());

            var proServSuborders = enterpriseConnectionUtils.query(
                    "SELECT Id, Name " +
                            "FROM Suborder__c " +
                            "WHERE Order__c = '" + proServProject.getOrder__c() + "'",
                    Suborder__c.class);
            var allProServSubordersNames = proServSuborders.stream()
                    .map(Suborder__c::getName)
                    .toList();

            for (var suborderName : allProServSubordersNames) {
                var suborderItem = new SuborderItem(suborderName);

                for (var suborderProductLineItem : suborderItem.getAllSuborderProductLineItems()) {
                    step("Check that 'Phase/Total Qty' field and 'Add Location' button are disabled " +
                            "and 'Remove Product' button is hidden", () -> {
                        suborderProductLineItem.getPhaseTotalQuantityInput().shouldBe(disabled);
                        suborderProductLineItem.getAddLocationButton().shouldBe(disabled);
                        suborderProductLineItem.getRemoveProductButton().shouldBe(hidden);
                    });
                }

                step("Check that 'Revenue Category' field, 'Add Product' and 'Move All Assigned Items here' buttons are disabled " +
                        "and 'Delete Suborder' button is hidden", () -> {
                    suborderItem.getRevenueCategory().shouldBe(disabled);
                    suborderItem.getAddProductButton().shouldBe(disabled);
                    suborderItem.getMoveAllAssignedItemsHereButton().shouldBe(disabled);
                    suborderItem.getDeleteSuborderButton().shouldBe(hidden);
                });

                if (suborderName.equals(proServSuborderWithFailedStatus.getName())) {
                    step("Check that 'Process Suborder' button is enabled for the ProServ Suborder with 'Failed' status", () -> {
                        suborderItem.getProcessSuborderButton().shouldBe(enabled);
                    });
                } else {
                    step("Check that 'Process Suborder' button is disabled for the ProServ Suborders " +
                            "with 'Billed' and 'Locked' statuses", () -> {
                        suborderItem.getProcessSuborderButton().shouldBe(disabled);
                    });
                }
            }
        });

        //  CRM-37549, CRM-37820, CRM-37821
        step("4. Collapse all Suborders, then expand them back " +
                "and check that all licenses in all suborders are sorted in alphabetical order", () -> {
            proServSuborderPage.getAllSuborders()
                    .forEach(suborder -> {
                        suborder.getExpandCollapseButton().click();
                        suborder.getRevenueCategory().shouldBe(hidden);
                        suborder.getExpandCollapseButton().click();
                    });

            proServSuborderPage.getAllSuborders().forEach(suborder -> {
                suborder.getAllSuborderLicenseNames().shouldHave(exactTextsCaseSensitive(proServProductsNamesSorted));
            });
        });
    }
}
