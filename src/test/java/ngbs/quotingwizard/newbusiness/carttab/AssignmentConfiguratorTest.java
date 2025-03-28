package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.*;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.exactValue;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("NGBS")
public class AssignmentConfiguratorTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private String opportunityId;

    //  Test data
    private final Product ciscoPhone;
    private final Product digitalLineUnlimited;
    private final Product commonPhone;
    private final Integer ciscoPhonesQuantityToAssign;
    private final String remainingCiscoPhonesQuantityToAssign;

    public AssignmentConfiguratorTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_AllTypesOfDLs.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        ciscoPhone = data.getProductByDataName("LC_HD_523");
        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        commonPhone = data.getProductByDataName("LC_DL-HDSK_177");
        //  user cannot assign more phones on the DL than there are DLs
        ciscoPhonesQuantityToAssign = digitalLineUnlimited.quantity;
        remainingCiscoPhonesQuantityToAssign = String.valueOf(ciscoPhone.quantity - ciscoPhonesQuantityToAssign);
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        opportunityId = steps.quoteWizard.opportunity.getId();
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-12161")
    @TmsLink("CRM-12162")
    @TmsLink("CRM-12164")
    @DisplayName("CRM-12161 - Assignment Configurator Allows User to Assign Phones to different DLs. \n" +
            "CRM-12162 - Assignment Configurator Items are created in SFDC after saving changes on the Price Tab. \n" +
            "CRM-12164 - Assignment Configurator does not allow to enter more than allowed quantities")
    @Description("CRM-12161 - To check that different Phones can be assigned to different types of DLs. \n" +
            "CRM-12162 - To check that AssignmentLineItem__c are created with correct values after saving changes on the Price Tab. \n" +
            "CRM-12164 - To check that Assignment Configurator does not allow to enter more than allowed quantities")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales quote, " +
                "select a package for it, add some products, and save changes on the Price tab", () ->
                steps.cartTab.prepareCartTabViaQuoteWizardVfPage(opportunityId)
        );

        step("2. Open the Quote Details tab, set Main Area Code and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.saveChanges();
        });

        step("3. Open the Price tab, set up quantities of the added phone, DigitalLine Unlimited, and Common Phone, " +
                "press on Configurator icon for the DL Unlimited " +
                "and check that quantity of available Phones = " + ciscoPhone.quantity, () -> {
            cartPage.openTab();
            steps.cartTab.setUpQuantities(ciscoPhone, digitalLineUnlimited, commonPhone);
            cartPage.getQliFromCartByDisplayName(digitalLineUnlimited.name).getDeviceAssignmentButton().click();

            deviceAssignmentPage.getProductItemByName(ciscoPhone.name).getAvailableNumber()
                    .shouldHave(exactTextCaseSensitive(ciscoPhone.quantity.toString()));
        });

        step("4. Assign " + ciscoPhonesQuantityToAssign + " Phones to DL Unlimited and press Apply button", () -> {
            deviceAssignmentPage.getFirstAreaCodeItem(ciscoPhone.name).getAssignedItemsInput()
                    .setValue(ciscoPhonesQuantityToAssign.toString());
            deviceAssignmentPage.getFirstAreaCodeItem(ciscoPhone.name).getAssignedItemsInput()
                    .shouldHave(exactValue(ciscoPhonesQuantityToAssign.toString()));
            deviceAssignmentPage.applyButton.click();
        });

        //  CRM-12161
        step("5. Click on the Device Assignment button for the Common Phone " +
                "and check that allowed to assign quantity of Phones is decreased by " + ciscoPhonesQuantityToAssign, () -> {
            cartPage.getQliFromCartByDisplayName(commonPhone.name).getDeviceAssignmentButton().click();
            deviceAssignmentPage.getProductItemByName(ciscoPhone.name).getAvailableNumber()
                    .shouldHave(exactTextCaseSensitive(remainingCiscoPhonesQuantityToAssign));
        });

        //  CRM-12164
        step("6. Increase quantity of phones for the Common phones more than " + remainingCiscoPhonesQuantityToAssign + ", " +
                "verify that phones quantity is automatically set to maximum available quantity, " +
                "apply changes on the modal, and save changes on the Price tab", () -> {
            deviceAssignmentPage.getFirstAreaCodeItem(ciscoPhone.name).getAssignedItemsInput()
                    .setValue(ciscoPhone.quantity.toString()).unfocus();

            deviceAssignmentPage.getFirstAreaCodeItem(ciscoPhone.name).getAssignedItemsInput()
                    .shouldHave(exactValue(remainingCiscoPhonesQuantityToAssign));

            deviceAssignmentPage.applyButton.click();
            cartPage.saveChanges();
        });

        //  CRM-12162
        step("7. Verify that AssignmentLineItem__c records were created for the DL Unlimited and Common Phone " +
                "with expected values for its fields: Parent__c, Child__c, AreaCode__c", () -> {
            for (var dlParent : List.of(digitalLineUnlimited, commonPhone)) {
                step("Check AssignmentLineItem__c for the " + dlParent.name, () -> {
                    var dlParentQLI = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Id " +
                                    "FROM QuoteLineItem " +
                                    "WHERE Quote.OpportunityId = '" + opportunityId + "' " +
                                    "AND Product2.ExtID__c = '" + dlParent.dataName + "'",
                            QuoteLineItem.class);
                    var ciscoQLI = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Id " +
                                    "FROM QuoteLineItem " +
                                    "WHERE Quote.OpportunityId = '" + opportunityId + "' " +
                                    "AND Product2.ExtID__c = '" + ciscoPhone.dataName + "'",
                            QuoteLineItem.class);

                    var assignmentLineItem = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Id, Child__c, AreaCode__c " +
                                    "FROM AssignmentLineItem__c " +
                                    "WHERE Parent__c = '" + dlParentQLI.getId() + "'",
                            AssignmentLineItem__c.class);

                    assertThat(assignmentLineItem.getChild__c())
                            .as("AssignmentLineItem__c.Child__c value")
                            .isEqualTo(ciscoQLI.getId());

                    var assignedAreaCode = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Name, City__c, Country__c, State__c " +
                                    "FROM Area_Codes__c " +
                                    "WHERE Id = '" + assignmentLineItem.getAreaCode__c() + "'",
                            Area_Codes__c.class);

                    // actual full area code should be like "United States, California, Beverly Hills (320)"
                    var actualAreaCodeFull = String.format("%s, %s, %s (%s)",
                            assignedAreaCode.getCountry__c(), assignedAreaCode.getState__c(),
                            assignedAreaCode.getCity__c(), assignedAreaCode.getName()
                    );
                    assertThat(actualAreaCodeFull)
                            .as("Area_Codes__c's Country__c, State__c, City__c, Name values " +
                                    "for the Assignment Line Item")
                            .isEqualTo(steps.quoteWizard.localAreaCode.fullName);
                });
            }
        });
    }
}
