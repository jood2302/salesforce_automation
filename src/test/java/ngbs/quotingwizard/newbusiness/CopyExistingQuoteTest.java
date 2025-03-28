package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper;
import com.sforce.soap.enterprise.sobject.CustomAddressAssignments__c;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.Calendar;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.components.ShippingAddressForm.OVERNIGHT_SHIPPING_OPTION;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToIntToString;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.switchTo;
import static com.codeborne.selenide.Selenide.webdriver;
import static com.codeborne.selenide.WebDriverConditions.urlContaining;
import static io.qameta.allure.Allure.step;
import static java.lang.String.valueOf;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("UQT")
public class CopyExistingQuoteTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private String startDateAsString;
    private Calendar startDateAsCalendar;

    //  Test data
    private final String packageFullName;

    private final Product phoneToAdd;
    private final Product dlUnlimited;

    private final String specialTerms;
    private final String initialTerm;
    private final String renewalTerm;
    private final String discountJustification;
    private final String paymentMethodUI;
    private final String paymentMethodDB;

    private final String shippingCountry;
    private final String shippingCity;
    private final String shippingState;
    private final String shippingAddressLine;
    private final String shippingZipCode;
    private final String shippingAdditionalAddress;
    private final String shippingAttentionTo;
    private final String shippingOption;
    private final ShippingGroupAddress shippingGroupAddress;
    private final String shippingGroupAddressFormatted;

    public CopyExistingQuoteTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_PhonesAndDLs.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        packageFullName = data.packageFolders[0].packages[0].getFullName();

        phoneToAdd = data.getProductByDataName("LC_HD_523");
        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");

        specialTerms = "6 Free Months of Service";
        var contractTerms = data.packageFolders[0].packages[0].contractTerms;
        initialTerm = contractTerms.initialTerm[0];
        renewalTerm = contractTerms.renewalTerm;
        discountJustification = TEST_STRING;
        paymentMethodUI = QuotePage.CREDIT_CARD_PAYMENT_METHOD;
        paymentMethodDB = QuoteHelper.CREDITCARD_PAYMENT_METHOD;

        //  shipping address
        shippingCountry = "United States";
        shippingCity = "Miami Beach";
        shippingState = "Florida";
        shippingAddressLine = "2100 Collins Ave";
        shippingZipCode = "33139";
        shippingAdditionalAddress = "Collins Park";
        shippingAttentionTo = "John Wayne (Attention To field)";
        shippingOption = OVERNIGHT_SHIPPING_OPTION;
        shippingGroupAddress = new ShippingGroupAddress(
                shippingCountry, shippingCity,
                new ShippingGroupAddress.State(shippingState, true),
                shippingAddressLine, shippingAdditionalAddress, shippingZipCode,
                shippingAttentionTo, shippingOption);
        shippingGroupAddressFormatted = shippingGroupAddress.getAddressFormatted();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-26894")
    @DisplayName("CRM-26894 - User can copy existing Sales Quote in Quote Wizard. New business")
    @Description("Verify that 'Copy' button in Quote Wizard creates new Quote with information copied from selected existing Quote")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Add the phones on the Add Products tab, open the Price tab, " +
                "set up discounts and quantities, assign phones to DLs, and save changes", () -> {
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());

            cartPage.openTab();
            steps.cartTab.setUpQuantities(phoneToAdd, dlUnlimited);
            steps.cartTab.setUpDiscounts(dlUnlimited);

            steps.cartTab.assignDevicesToDLAndSave(phoneToAdd.name, dlUnlimited.name, steps.quoteWizard.localAreaCode,
                    dlUnlimited.quantity);
        });

        step("3. Open the Shipping tab, add a new Shipping Group, assign device to it, and save changes", () -> {
            shippingPage.openTab();
            shippingPage.addNewShippingGroup(shippingGroupAddress);
            shippingPage.assignDeviceToShippingGroup(phoneToAdd.productName, shippingGroupAddressFormatted, shippingAttentionTo);
            shippingPage.saveChanges();
        });

        step("4. Open the Quote Details tab, set up the contract terms, Free Service Credit, Area Codes, " +
                "Justification/Description, Payment Method and save changes", () -> {
            quotePage.openTab();

            quotePage.selectPaymentMethod(paymentMethodUI);
            quotePage.initialTermPicklist.selectOption(initialTerm);
            quotePage.renewalTermPicklist.selectOption(renewalTerm);
            quotePage.discountJustificationTextArea.setValue(discountJustification);

            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.setDefaultStartDate();

            //  the quote's state is saved here
            quotePage.applyNewSpecialTerms(specialTerms);

            //  to check this value on the copied quote later
            startDateAsString = quotePage.startDateInput.getValue();
            var quoteUpdated = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Start_Date__c " +
                            "FROM Quote " +
                            "WHERE Id = '" + wizardPage.getSelectedQuoteId() + "'",
                    Quote.class);
            startDateAsCalendar = quoteUpdated.getStart_Date__c();
        });

        step("5. Open the Price tab, press 'Copy Quote' button, " +
                "and check that the new Quote is created and opened in the Quote Wizard", () -> {
            cartPage.openTab();
            cartPage.clickCopyQuoteButton();

            switchTo().window(1);
            packagePage.waitUntilLoaded();
            webdriver().shouldHave(urlContaining("id=" + steps.quoteWizard.opportunity.getId()));
        });

        step("6. Check the preselected package, Contract checkbox, and the Charge Term on the Select Package tab", () -> {
            packagePage.packageSelector.getSelectedPackage().getName()
                    .shouldHave(exactTextCaseSensitive(packageFullName), ofSeconds(10));
            packagePage.packageSelector.packageFilter.contractSelectInput.shouldBe(checked);
            packagePage.packageSelector.packageFilter.getChargeTermInput(data.chargeTerm).shouldBe(selected);
        });

        step("7. Open the Price tab, and check its contents", () -> {
            cartPage.openTab();

            var dlUnlimitedItem = cartPage.getQliFromCartByDisplayName(dlUnlimited.name);
            dlUnlimitedItem.getQuantityInput().shouldHave(exactValue(valueOf(dlUnlimited.quantity)));
            dlUnlimitedItem.getDiscountInput().shouldHave(exactValue(valueOf(dlUnlimited.discount)));
            dlUnlimitedItem.getDiscountTypeSelect().getSelectedOption().shouldHave(exactText(valueOf(dlUnlimited.discountType)));
            //  "1" means that the 1 device is already assigned
            dlUnlimitedItem.getNumberAssignmentLineItems().shouldHave(text("1"));

            cartPage.getQliFromCartByDisplayName(phoneToAdd.name)
                    .getQuantityInput()
                    .shouldHave(exactValue(valueOf(phoneToAdd.quantity)));
        });

        step("8. Open the Shipping tab, and check Shipping Address", () -> {
            shippingPage.openTab();
            shippingPage.getFirstShippingGroup().getEditButton().click();
            shippingPage.shippingGroupDetailsModal.countrySelect.getSelectedOption().shouldHave(exactTextCaseSensitive(shippingCountry));
            shippingPage.shippingGroupDetailsModal.cityInput.shouldHave(exactValue(shippingCity));
            shippingPage.shippingGroupDetailsModal.stateSelectOrInput.getSelectedOption().shouldHave(exactTextCaseSensitive(shippingState));
            shippingPage.shippingGroupDetailsModal.addressLineInput.shouldHave(exactValue(shippingAddressLine));
            shippingPage.shippingGroupDetailsModal.shipAttentionToInput.shouldHave(exactValue(shippingAttentionTo));
            shippingPage.shippingGroupDetailsModal.additionalAddressLineInput.shouldHave(exactValue(shippingAdditionalAddress));
            shippingPage.shippingGroupDetailsModal.shippingMethod.getSelectedOption().shouldHave(exactTextCaseSensitive(shippingOption));
            shippingPage.shippingGroupDetailsModal.applyButton.click();
        });

        step("9. Open the Quote Details tab, and check its contents", () -> {
            quotePage.openTab();
            quotePage.startDateInput.shouldHave(exactValue(startDateAsString));
            quotePage.initialTermPicklist.getSelectedOption().shouldHave(exactText(initialTerm));
            quotePage.renewalTermPicklist.getSelectedOption().shouldHave(exactText(renewalTerm));
            quotePage.discountJustificationTextArea.shouldHave(exactValue(discountJustification));
            quotePage.paymentMethodPicklist.getSelectedOption().shouldHave(exactText(paymentMethodUI));
            quotePage.mainAreaCodeText.shouldHave(exactTextCaseSensitive(steps.quoteWizard.localAreaCode.fullName));
            quotePage.faxAreaCodeText.shouldHave(exactTextCaseSensitive(steps.quoteWizard.localAreaCode.fullName));
            quotePage.freeServiceCreditNumberOfMonths.shouldHave(exactTextCaseSensitive(specialTerms));
        });

        step("10. Check the new created quote in SFDC DB", () -> {
            var newQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, OpportunityId, " +
                            "Initial_Term_months__c, Term_months__c, Start_Date__c, Special_Terms__c, " +
                            "PaymentMethod__c, JustificationandDescription__c, " +
                            "AreaCode__r.Country__c, AreaCode__r.State__c, AreaCode__r.Area_Code__c, " +
                            "FaxAreaCode__r.Country__c, FaxAreaCode__r.State__c, FaxAreaCode__r.Area_Code__c " +
                            "FROM Quote " +
                            "WHERE Id = '" + wizardPage.getSelectedQuoteId() + "'",
                    Quote.class);

            assertThat(newQuote.getOpportunityId())
                    .as("New Quote.OpportunityId")
                    .isEqualTo(steps.quoteWizard.opportunity.getId());

            //  Contract and Special terms
            assertThat(newQuote.getInitial_Term_months__c())
                    .as("New Quote.Initial_Term_months__c")
                    .isEqualTo(initialTerm);
            assertThat(newQuote.getTerm_months__c())
                    .as("New Quote.Term_months__c")
                    .isEqualTo(renewalTerm);
            assertThat(newQuote.getStart_Date__c())
                    .as("New Quote.Start_Date__c")
                    .isEqualTo(startDateAsCalendar);
            assertThat(newQuote.getSpecial_Terms__c())
                    .as("New Quote.Special_Terms__c")
                    .isEqualTo(specialTerms);

            //  Other fields
            assertThat(newQuote.getJustificationandDescription__c())
                    .as("New Quote.JustificationandDescription__c")
                    .isEqualTo(discountJustification);
            assertThat(newQuote.getPaymentMethod__c())
                    .as("New Quote.PaymentMethod__c")
                    .isEqualTo(paymentMethodDB);

            //  Main Area Code
            assertThat(newQuote.getAreaCode__r().getCountry__c())
                    .as("New Quote.AreaCode__r.Country__c")
                    .isEqualTo(steps.quoteWizard.localAreaCode.country);
            assertThat(newQuote.getAreaCode__r().getState__c())
                    .as("New Quote.AreaCode__r.State__c")
                    .isEqualTo(steps.quoteWizard.localAreaCode.state);
            assertThat(doubleToIntToString(newQuote.getAreaCode__r().getArea_Code__c()))
                    .as("New Quote.AreaCode__r.Area_Code__c")
                    .isEqualTo(steps.quoteWizard.localAreaCode.code);

            //  Fax Area Code
            assertThat(newQuote.getFaxAreaCode__r().getCountry__c())
                    .as("New Quote.FaxAreaCode__r.Country__c")
                    .isEqualTo(steps.quoteWizard.localAreaCode.country);
            assertThat(newQuote.getFaxAreaCode__r().getState__c())
                    .as("New Quote.FaxAreaCode__r.State__c")
                    .isEqualTo(steps.quoteWizard.localAreaCode.state);
            assertThat(doubleToIntToString(newQuote.getFaxAreaCode__r().getArea_Code__c()))
                    .as("New Quote.FaxAreaCode__r.Area_Code__c")
                    .isEqualTo(steps.quoteWizard.localAreaCode.code);
        });

        step("11. Check CustomAddress record at the created quote in SFDC DB", () -> {
            var customAddress = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, AttentionTo__c, CustomAddress__r.Country__c, CustomAddress__r.PostalCode__c, " +
                            "CustomAddress__r.City__c, CustomAddress__r.AddressLine__c, " +
                            "CustomAddress__r.AdditionalAddressLine__c, Type__c " +
                            "FROM CustomAddressAssignments__c " +
                            "WHERE QuoteLineItem__r.QuoteId = '" + wizardPage.getSelectedQuoteId() + "'",
                    CustomAddressAssignments__c.class);

            assertThat(customAddress.getAttentionTo__c())
                    .as("CustomAddressAssignments__c.AttentionTo__c value")
                    .isEqualTo(shippingAttentionTo);

            assertThat(customAddress.getType__c())
                    .as("CustomAddressAssignments__c.Type__c value")
                    .isEqualTo(shippingOption);

            assertThat(customAddress.getCustomAddress__r().getCountry__c())
                    .as("CustomAddressAssignments__c.CustomAddress__r.Country__c value")
                    .isEqualTo(shippingCountry);

            assertThat(customAddress.getCustomAddress__r().getPostalCode__c())
                    .as("CustomAddressAssignments__c.CustomAddress__r.PostalCode__c value")
                    .isEqualTo(shippingZipCode);

            assertThat(customAddress.getCustomAddress__r().getCity__c())
                    .as("CustomAddressAssignments__c.CustomAddress__r.City__c value")
                    .isEqualTo(shippingCity);

            assertThat(customAddress.getCustomAddress__r().getAddressLine__c())
                    .as("CustomAddressAssignments__c.CustomAddress__r.AddressLine__c value")
                    .isEqualTo(shippingAddressLine);

            assertThat(customAddress.getCustomAddress__r().getAdditionalAddressLine__c())
                    .as("CustomAddressAssignments__c.CustomAddress__r.AdditionalAddressLine__c value")
                    .isEqualTo(shippingAdditionalAddress);
        });
    }
}
