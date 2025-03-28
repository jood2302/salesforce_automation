package ngbs.opportunitycreation.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartItem;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Opportunity;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.BI_FORMAT;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.String.valueOf;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("P1")
@Tag("PDV")
@Tag("NGBS")
@Tag("QOP")
public class NgbsNewBusinessOpportunityCreationTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private CartItem digitalLineUnlimitedQli;
    private CartItem polycomPhoneQli;

    //  Test data
    private final Product digitalLineUnlimited;
    private final Product phoneToAdd;
    private final AreaCode newLocalAreaCode;

    private final Package testPackage;
    private final String packageFolderName;
    private final String biId;

    public NgbsNewBusinessOpportunityCreationTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/newbusiness/RC_MVP_Annual_Contract_QOP.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        phoneToAdd = data.getProductByDataName("LC_HD_611");
        newLocalAreaCode = new AreaCode("Local", "United States", "New York", EMPTY_STRING, "347");

        packageFolderName = data.packageFolders[0].name;
        testPackage = data.packageFolders[0].packages[0];
        biId = data.businessIdentity.id;
    }

    @BeforeEach
    public void setUpTest() {
        var salesUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesUser);
    }

    @Test
    @TmsLink("CRM-12673")
    @TmsLink("CRM-19657")
    @TmsLink("CRM-19649")
    @TmsLink("CRM-12160")
    @TmsLink("CRM-22733")
    @DisplayName("CRM-12673 - New Business NGBS Opportunity creation. QOP. \n" +
            "CRM-19657 - Already Assigned Phones don't get auto assigned Area Code from Main Area Code. \n" +
            "CRM-19649 - Area Code is automatically added from Main Area Code. \n" +
            "CRM-12160 - Assignment Configurator Case. \n" +
            "CRM-22733 - Business Identity field is filled after Opportunity was created from QOP")
    @Description("CRM-12673 - Verify that the New Business NGBS Opportunity can be created from Quick Opportunity Page. \n" +
            "CRM-19657 - Verify that overwriting Main Area Code doesn't overwrite the Area Code for already assigned phones. \n" +
            "CRM-19649 - Assignment Configurator automatically adds area code to Phones from Main Area Code. \n" +
            "CRM-12160 - To check that Area Codes can be added via Assignment configurator. \n" +
            "CRM-22733 - Verify that Business Identity field is filled after Opportunity was created from QOP")
    public void test() {
        //  CRM-12673
        step("1. Open Quick Opportunity creation Page (QOP) for a test Account", () -> {
            opportunityCreationPage.openPage(steps.salesFlow.account.getId());
            opportunityCreationPage.accountInputWithSelectedValue
                    .shouldHave(exactTextCaseSensitive(steps.salesFlow.account.getName()));
        });

        //  CRM-22733
        step("2. Check the preselected BI and Currency, and Brand values", () -> {
            opportunityCreationPage.businessIdentityPicklist.getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(data.getBusinessIdentityName()), ofSeconds(30));
            opportunityCreationPage.currencyPicklist.getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(data.getCurrencyIsoCode()));
            opportunityCreationPage.brandOutput.shouldHave(exactTextCaseSensitive(data.brandName));
        });

        //  CRM-12673
        step("3. Select Service = 'Office', set the Close Date and new number of DLs, " +
                "and click 'Continue to Opportunity' button", () -> {
            opportunityCreationPage.servicePicklist.getOptions().shouldHave(itemWithText(packageFolderName), ofSeconds(10));
            opportunityCreationPage.servicePicklist.selectOption(packageFolderName);
            opportunityCreationPage.populateCloseDate();
            opportunityCreationPage.newNumberOfDLsInput.setValue(valueOf(digitalLineUnlimited.quantity));

            steps.opportunityCreation.pressContinueToOpp();
        });

        //  CRM-22733
        step("4. Check the 'BusinessIdentity__c' field on the created Opportunity", () -> {
            var expectedBusinessIdentityValue = String.format(BI_FORMAT, data.getCurrencyIsoCode(), biId);

            var createdOpportunityId = opportunityPage.getCurrentRecordId();
            var createdOpportunity = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, BusinessIdentity__c " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + createdOpportunityId + "'",
                    Opportunity.class);

            assertThat(createdOpportunity.getBusinessIdentity__c())
                    .as("Opportunity.BusinessIdentity__c value")
                    .isEqualTo(expectedBusinessIdentityValue);
        });

        step("5. Switch to the Quote Wizard on Opportunity Record Page, " +
                "add a new Sales Quote, select a package for it, and save changes ", () -> {
            opportunityPage.switchToNGBSQW();
            steps.quoteWizard.addNewSalesQuote();
            packagePage.packageSelector.selectPackage(data.chargeTerm, packageFolderName, testPackage);
            packagePage.saveChanges();
        });

        step("6. Check the Select Package tab's content", () -> {
            packagePage.packageSelector.getSelectedPackage().getName()
                    .shouldHave(exactTextCaseSensitive(testPackage.getFullName()));
            packagePage.packageSelector.packageFilter.contractSelectInput.shouldBe(selected);
            packagePage.packageSelector.packageFilter.getChargeTermInput(data.chargeTerm)
                    .shouldBe(selected);
        });

        //  CRM-19657, CRM-19649
        step("7. Open the Add Products tab, and add products", () -> {
            steps.quoteWizard.addProductsOnProductsTab(phoneToAdd);
        });

        //  CRM-19657, CRM-19649
        step("8. Open the Quote Details tab, populate Main Area Code, and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.saveChanges();
        });

        //  CRM-19649, CRM-12160
        step("9. Open the Price tab, open the Assignment Configurator for DigitalLine Unlimited, " +
                "and check Area Code value", () -> {
            cartPage.openTab();
            digitalLineUnlimitedQli = cartPage.getQliFromCartByDisplayName(digitalLineUnlimited.name);

            polycomPhoneQli = cartPage.getQliFromCartByDisplayName(phoneToAdd.name);

            //  CRM-12160 Assignment Configurator check
            digitalLineUnlimitedQli.getNumberAssignmentLineItems().shouldHave(text("0"));
            polycomPhoneQli.getNumberAssignmentLineItems().shouldHave(text("0"));

            digitalLineUnlimitedQli.getDeviceAssignmentButton().click();
            deviceAssignmentPage.getProductItemByName(phoneToAdd.name).getNameElement()
                    .shouldHave(exactText(phoneToAdd.name), ofSeconds(60));

            //  CRM-19649 Area Code value check
            deviceAssignmentPage.getFirstAreaCodeItem(phoneToAdd.name).getAreaCodeSelector()
                    .getSelectedAreaCodeFullName()
                    .shouldHave(exactTextCaseSensitive(steps.quoteWizard.localAreaCode.fullName));
        });

        //  CRM-12160
        step("10. Assign a phone, and check that the values inside of Configurator Icon are changed", () -> {
            //  Assignment Configurator check
            deviceAssignmentPage.getFirstAreaCodeItem(phoneToAdd.name)
                    .getAssignedItemsInput()
                    .shouldHave(value("0"))
                    .setValue(valueOf(phoneToAdd.quantity))
                    .unfocus();
            deviceAssignmentPage.applyButton.click();

            digitalLineUnlimitedQli.getNumberAssignmentLineItems().shouldHave(text("1"));
            polycomPhoneQli.getNumberAssignmentLineItems().shouldHave(text("1"));
        });

        //  CRM-19657
        step("11. Open the Quote Details tab, change Main Area Code, and save changes", () -> {
            quotePage.openTab();
            quotePage.areaCodeSelector.clear();
            quotePage.setMainAreaCode(newLocalAreaCode);
            quotePage.saveChanges();
        });

        //  CRM-19657
        step("12. Open the Price tab, press Assignments button on the DigitalLine Unlimited, and check Area Code value", () -> {
            cartPage.openTab();
            digitalLineUnlimitedQli.getDeviceAssignmentButton().shouldBe(visible, ofSeconds(10)).click();

            deviceAssignmentPage.getProductItemByName(phoneToAdd.name)
                    .getNameElement()
                    .shouldHave(exactText(phoneToAdd.name), ofSeconds(60));
            deviceAssignmentPage.getFirstAreaCodeItem(phoneToAdd.name).getAreaCodeSelector()
                    .getSelectedAreaCodeFullName()
                    .shouldNotHave(text(newLocalAreaCode.fullName))
                    .shouldHave(exactTextCaseSensitive(steps.quoteWizard.localAreaCode.fullName));
        });
    }
}
