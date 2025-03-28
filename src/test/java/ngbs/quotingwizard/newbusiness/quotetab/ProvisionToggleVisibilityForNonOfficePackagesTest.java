package ngbs.quotingwizard.newbusiness.quotetab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.packagePage;
import static base.Pages.quotePage;
import static com.codeborne.selenide.Condition.hidden;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("NGBS")
@Tag("LBO")
public class ProvisionToggleVisibilityForNonOfficePackagesTest extends BaseTest {
    private final Dataset data;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final Steps steps;

    //  Test data
    private final PackageFolder faxPackageFolder;
    private final Package faxPackage;
    private final String faxService;

    public ProvisionToggleVisibilityForNonOfficePackagesTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_AllPackageFolders_Annual_Contract_NoProducts.json",
                Dataset.class);

        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        steps = new Steps(data);

        faxPackageFolder = data.packageFolders[5];
        faxPackage = faxPackageFolder.packages[0];
        faxService = faxPackageFolder.name;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);

        //  to make Fax package preselected instead of Office one
        step("Set Opportunity.Tier_Name__c = 'Fax' via API", () -> {
            steps.quoteWizard.opportunity.setTier_Name__c(faxService);
            enterpriseConnectionUtils.update(steps.quoteWizard.opportunity);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-20903")
    @DisplayName("CRM-20903 - Provision toggle should be shown only for LBO services")
    @Description("To verify that LBO functionality is not shown for non-LBO products")
    public void test() {
        step("1. Open the Quote Wizard to add a new Sales Quote, select a package for it and save changes", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            packagePage.packageSelector.selectPackage(data.chargeTerm, faxPackageFolder.name, faxPackage);
            packagePage.saveChanges();
        });

        step("2. Open the Quote Details tab and check that Provision toggle is not displayed", () -> {
            quotePage.openTab();
            quotePage.provisionToggle.shouldBe(hidden);
        });
    }
}
