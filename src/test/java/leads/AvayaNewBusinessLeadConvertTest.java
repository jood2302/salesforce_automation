package leads;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.producttab.LegacyProductItem;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityShareFactory;
import com.sforce.soap.enterprise.sobject.QuoteLineItem;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.NEW_ACCOUNT_WILL_BE_CREATED_MESSAGE;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToInteger;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.PROFESSIONAL_SERVICES_LIGHTNING_PROFILE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.getUser;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("PDV")
@Tag("ProServ")
@Tag("Avaya")
@Tag("Lambda")
@Tag("LeadConvert")
public class AvayaNewBusinessLeadConvertTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private String opportunityId;

    //  Test data
    private final String chargeTerm;
    private final String packageFolderName;
    private final Package packageToSelect;

    private final String proServProductCategory;
    private final String proServProductName;
    private final int proServProductQuantity;

    public AvayaNewBusinessLeadConvertTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/Avaya_Office_Monthly_Contract.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        chargeTerm = data.chargeTerm;
        packageFolderName = data.packageFolders[0].name;
        packageToSelect = data.packageFolders[0].packages[0];

        proServProductCategory = "ProServ";
        proServProductName = packageToSelect.products[0].name;
        proServProductQuantity = packageToSelect.products[0].quantity;
    }

    @BeforeEach
    public void setUpTest() {
        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.leadConvert.createPartnerAccountAndLead(dealDeskUser);
        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-19875")
    @TmsLink("CRM-20791")
    @TmsLink("CRM-12960")
    @DisplayName("CRM-19875 - ProServ can be engaged for Avaya Cloud Office Opportunity. \n" +
            "CRM-20791 - Lead Conversion - Partner Lead - Avaya. \n" +
            "CRM-12960 - Lead Conversion Page for Partner brands - only new Account")
    @Description("CRM-19875 - Verify that if a User Engages ProServ on Avaya Cloud Office Brand Opportunity " +
            "then ProServ Quote is created and ProServ items are available on the Product tab on ProServ Quote. \n" +
            "CRM-20791 - Verify that a Partner Lead with a Avaya brand can be converted. \n" +
            "CRM-12960 - Verify that on Lead Conversion Page for Partner Brands there is available only to choose option with creating new Account")
    public void test() {
        step("1. Open Lead Convert page for the Partner test lead", () ->
                leadConvertPage.openDirect(steps.leadConvert.partnerLead.getId())
        );

        //  CRM-20791, CRM-12960
        step("2. Check Account and Opportunity info section and their fields", () -> {
            leadConvertPage.accountInfoSection.shouldBe(visible, ofSeconds(30));

            //  For CRM-12960
            leadConvertPage.newExistingAccountToggle.shouldNot(exist);
            leadConvertPage.accountInfoLabel.shouldHave(exactTextCaseSensitive(NEW_ACCOUNT_WILL_BE_CREATED_MESSAGE), ofSeconds(30));

            //  For CRM-20791
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityNameNonEditable.shouldHave(exactText(steps.leadConvert.partnerLead.getCompany()), ofSeconds(30));
            leadConvertPage.opportunityBrandNonEditable.shouldHave(exactText(steps.leadConvert.brandName), ofSeconds(10));
        });

        step("3. Click 'Edit' button in the Opportunity Info section, populate Close Date and click 'Apply' button", () -> {
            leadConvertPage.opportunityInfoEditButton.click();
            leadConvertPage.closeDateDatepicker.setTomorrowDate();
            leadConvertPage.opportunityInfoApplyButton.click();
        });

        step("4. Press 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        //  CRM-20791
        step("5. Check that Lead is converted", () ->
                steps.leadConvert.checkLeadConversion(steps.leadConvert.partnerLead)
        );

        //  CRM-19875
        step("6. Switch to the Quote Wizard on the Opportunity record page, " +
                "add a new Sales Quote, select a package for it, save changes, " +
                "initiate ProServ from the Primary Quote and submit data for it", () -> {
            opportunityPage.switchToNGBSQW();
            steps.quoteWizard.addNewSalesQuote();
            packagePage.packageSelector.selectPackage(chargeTerm, packageFolderName, packageToSelect);
            packagePage.saveChanges();

            wizardPage.initiateProServ();
        });

        //  CRM-19875
        step("7. Switch to the Opportunity record page, re-login as a user with 'Professional Services Lightning' profile, " +
                "and manually share the Opportunity with this user via API", () -> {
            switchTo().window(0);
            opportunityId = opportunityPage.getCurrentRecordId();

            var proServUser = getUser().withProfile(PROFESSIONAL_SERVICES_LIGHTNING_PROFILE).execute();
            steps.sfdc.reLoginAsUser(proServUser);

            OpportunityShareFactory.shareOpportunity(opportunityId, proServUser.getId());
        });

        //  CRM-19875
        step("8. Switch back to and refresh the Quote Wizard, open the ProServ Quote, open Products tab, and check ProServ items", () -> {
            switchTo().window(1);
            refresh();
            wizardPage.waitUntilLoaded();
            wizardBodyPage.proServTab.click();
            proServWizardPage.waitUntilLoaded();

            legacyProductsPage.openTab();

            var proServCategoryProducts = legacyProductsPage.getProductItemsByCategory(proServProductCategory)
                    .stream()
                    .map(LegacyProductItem::getSelf)
                    .collect(toList());
            $$(proServCategoryProducts).shouldHave(sizeGreaterThan(0));
        });

        //  CRM-19875
        step("9. Add any item to the Cart and check that QLI is created for added item", () -> {
            legacyProductsPage.addProduct(proServProductName);

            legacyCartPage.openTab();
            legacyCartPage.getQliFromCart(proServProductName).getQuantityInput()
                    .shouldHave(value(String.valueOf(proServProductQuantity)));

            var proServProductQLI = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Quantity " +
                            "FROM QuoteLineItem " +
                            "WHERE Quote.Opportunity.Id = '" + opportunityId + "' " +
                            "AND Product2.Name = '" + proServProductName + "'",
                    QuoteLineItem.class);
            assertThat(doubleToInteger(proServProductQLI.getQuantity()))
                    .as("QuoteLineItem.Quantity value for " + proServProductName)
                    .isEqualTo(proServProductQuantity);
        });
    }
}
