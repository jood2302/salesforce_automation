package ngbs.quotingwizard.newbusiness.dqtab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.DQ_Deal_Qualification_Discounts__c;
import com.sforce.soap.enterprise.sobject.QuoteLineItem;
import com.sforce.ws.ConnectionException;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.codeborne.selenide.Condition.exactValue;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.math.RoundingMode.CEILING;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("DealQualificationTab")
public class CurrencyDiscountConvertToPercentDqTest extends BaseTest {
    private final Steps steps;
    private final DealQualificationSteps dqSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Product phone;

    public CurrencyDiscountConvertToPercentDqTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_PhonesAndDLs.json",
                Dataset.class);
        steps = new Steps(data);
        dqSteps = new DealQualificationSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        phone = data.getProductByDataName("LC_HD_523");

        //  the resulting QuoteLineItem.Discount values for the products 
        //  should exceed a 'ceiling' configured in the custom metadata types:
        //  DiscountCeilingCBox__mdt, DiscountCeilingCategory__mdt, DiscountCeilingLicense__mdt
        dqSteps.dlUnlimited.discount = 197; //  for 240 USD, it's 197 / 240 = 0.8208333 (~82%)
        phone.discount = 306; //  for 347 USD, it's 306 / 347 = 0.881844 (~88%)

        dqSteps.dlUnlimited.discountType = data.getCurrencyIsoCode();
        phone.discountType = data.getCurrencyIsoCode();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-31647")
    @DisplayName("CRM-31647 - Currency discount is converted to Percentage on the DQ Discount")
    @Description("Verify that currency discounts are converted into percentage for the License Requirements section")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, select a package for it, " +
                "add Products to the Cart, and save changes on the Price tab", () ->
                steps.cartTab.prepareCartTabViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Quote Details tab, populate Discount Justification and Main Area Code fields, and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.discountJustificationTextArea.setValue(TEST_STRING);
            quotePage.saveChanges();
        });

        step("3. Open the Price tab, set quantities and discounts for the DL Unlimited and the phone, " +
                "and save changes", () -> {
            cartPage.openTab();
            steps.cartTab.setUpQuantities(dqSteps.dlUnlimited, phone);
            steps.cartTab.setUpDiscounts(dqSteps.dlUnlimited, phone);
            cartPage.saveChanges();
        });

        step("4. Submit the quote for approval via 'Submit for Approval' button", () -> {
            cartPage.submitForApproval();
        });

        step("5. Open the Deal Qualification tab, " +
                "verify that 'Ceiling Discount' values for the License Requirements items are populated " +
                "with the same value as QuoteLineItem.Discount field rounded up to 2 decimals", () -> {
            dealQualificationPage.openTab();
            dealQualificationPage.licenseRequirementsSection.shouldBe(visible);

            checkCeilingDiscountAsPercentForCurrencyDiscounts(dqSteps.dlUnlimited);
            checkCeilingDiscountAsPercentForCurrencyDiscounts(phone);
        });
    }

    /**
     * Check 'Ceiling Discount' value for the given product with the currency discount (e.g. "USD")
     * in the License Requirements section on the Deal Qualification tab.
     * Also, check that DQ_Deal_Qualification_Discounts__c.Discount__c is correct as well.
     *
     * @param product test data with the product that the License Requirements item should be checked against
     * @throws ConnectionException in case of errors while accessing API
     */
    private void checkCeilingDiscountAsPercentForCurrencyDiscounts(Product product) throws ConnectionException {
        var productName = product.productName != null ? product.productName : product.name;

        var qli = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id, Discount " +
                        "FROM QuoteLineItem " +
                        "WHERE QuoteId = '" + wizardPage.getSelectedQuoteId() + "' " +
                        "AND Product2.ExtID__c = '" + product.dataName + "'",
                QuoteLineItem.class);
        var qliDiscountRounded = BigDecimal.valueOf(qli.getDiscount()).setScale(2, CEILING);

        var licenseRequirementsItem = dealQualificationPage.getLicenseRequirementsItem(productName);
        licenseRequirementsItem.getSelf().shouldBe(visible);
        licenseRequirementsItem.getCeilingDiscountInput().shouldHave(exactValue(qliDiscountRounded.toString()));

        var dealQualificationDiscount = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id, Discount__c " +
                        "FROM DQ_Deal_Qualification_Discounts__c " +
                        "WHERE Deal_Qualification__r.Quote__c = '" + wizardPage.getSelectedQuoteId() + "' " +
                        "AND Product__r.ExtID__c = '" + product.dataName + "'",
                DQ_Deal_Qualification_Discounts__c.class);
        assertThat(dealQualificationDiscount.getDiscount__c())
                .as("DQ_Deal_Qualification_Discounts__c.Discount__c value of " + product.name)
                .isEqualTo(qliDiscountRounded.doubleValue());
    }
}
