package leads;

import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.*;
import com.sforce.ws.ConnectionException;
import io.qameta.allure.Step;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewPartnerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.LeadFactory.createCustomerLeadInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.LeadFactory.createPartnerLeadInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ContactHelper.getFullName;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.sleep;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test methods for the flows related to Lead Convert functionality:
 * e.g. create new Sales/Partner Leads, converting Leads on the LC page, etc.
 */
public class LeadConvertSteps {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public Lead salesLead;
    public Lead partnerLead;
    public Account partnerAccount;

    //  Test data
    public final String brandName;
    public final String tierName;
    public final String currencyIsoCode;
    public final String billingCountry;

    private final String testFirstName;
    private final String testLastName;
    private final String testCompany;
    private final String leadSource;
    private final String numberOfEmployees;
    private final String industry;
    private final String website;

    /**
     * New instance for the class with the test methods/steps related to the Lead Convert functionality.
     *
     * @param data object parsed from the JSON files with the test data
     */
    public LeadConvertSteps(Dataset data) {
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        brandName = data.getBrandName();
        tierName = data.packageFolders[0].name;
        currencyIsoCode = data.getCurrencyIsoCode();
        billingCountry = data.getBillingCountry();

        testFirstName = "TestFName_" + getRandomPositiveInteger();
        testLastName = "TestLastName_" + getRandomPositiveInteger();
        testCompany = "TestCompany_" + getRandomPositiveInteger();
        leadSource = "ZoomInfo";
        numberOfEmployees = "1-19";
        industry = "Banking";
        website = "https://example.com/page";
    }

    /**
     * Create a new Sales Lead via API.
     *
     * @param ownerUser user intended to be the owner of the record
     */
    public void createSalesLead(User ownerUser) {
        step("Create test Sales Lead via API", () -> {
            salesLead = createCustomerLeadInSFDC(ownerUser);
        });
    }

    /**
     * Create a new Sales Lead via Lead Creation page.
     *
     * @return created Lead object
     */
    public Lead createSalesLeadViaLeadCreationPage() {
        step("Open the Lead Creation page to create a new Sales Lead", () ->
                leadCreationPage.openPage()
        );

        step("Populate necessary fields, click 'Search' button and click 'Create New Lead' button", () -> {
            leadCreationPage.firstNameInput.setValue(testFirstName);
            leadCreationPage.lastNameInput.setValue(testLastName);
            leadCreationPage.companyInput.setValue(testCompany);
            leadCreationPage.emailInput.setValue(getRandomEmail());
            leadCreationPage.leadSourcePicklist.selectOption(leadSource);
            leadCreationPage.numberOfEmployeesPicklist.selectOption(numberOfEmployees);
            leadCreationPage.industryPicklist.selectOption(industry);
            leadCreationPage.websiteInput.setValue(website);
            leadCreationPage.contactPhoneNumberInput.setValue(getRandomCanadaPhone());

            leadCreationPage.searchValidInputs();
            leadCreationPage.clickCreateNewLeadButton();
        });

        return step("Check that the Lead is created correctly", () -> {
            leadRecordPage.waitUntilLoaded();

            return enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Company, LastName, Email " +
                            "FROM Lead " +
                            "WHERE Id = '" + leadRecordPage.getCurrentRecordId() + "'",
                    Lead.class);
        });
    }

    /**
     * Create a new Partner Lead with related Partner Account via API.
     *
     * @param ownerUser user intended to be the owner of the records
     */
    public void createPartnerAccountAndLead(User ownerUser) {
        step("Create test Partner Account with related Contact and AccountContactRole via API", () -> {
            partnerAccount = createNewPartnerAccountInSFDC(ownerUser,
                    new AccountData()
                            .withCurrencyIsoCode(currencyIsoCode)
                            .withRcBrand(brandName)
                            .withPermittedBrands(brandName)
                            .withBillingCountry(billingCountry)
            );
        });

        step("Create test Partner Lead via API", () -> {
            partnerLead = createPartnerLeadInSFDC(ownerUser, partnerAccount, tierName);
        });
    }

    /**
     * Test steps to make account available for search on Partner Lead Conversion page.
     *
     * @param account Account to be updated.
     * @param contact Contact to get phone for Lead update.
     */
    public void preparePartnerLeadTestSteps(Account account, Contact contact) {
        step("Set Account.Partner_ID__c = Lead.LeadPartnerID__c for the test Account via API", () -> {
            account.setPartner_ID__c(partnerLead.getLeadPartnerID__c());
            enterpriseConnectionUtils.update(account);
        });

        step("Set Lead.Phone = Account's Contact.Phone for the test Lead via API", () -> {
            partnerLead.setPhone(contact.getPhone());
            enterpriseConnectionUtils.update(partnerLead);
        });
    }

    /**
     * Press 'Convert' button on the Lead Convert page
     * and wait for a converted Opportunity's record page to load.
     * <p></p>
     * Note: only works for the positive flow
     * (where a Lead is supposed to be successfully converted into an Opportunity).
     */
    @Step("Press 'Convert' button on the Lead Convert page")
    public void pressConvertButton() {
        leadConvertPage.convertButton.click();

        leadConvertPage.spinner.shouldBe(hidden, ofSeconds(120));
        //  'Trying to connect with NGBS...' might be shown, 
        //  and user has to click 'Convert' again after the response is received
        //  Right now, there's no way to avoid this using UI elements, hence this hack
        if (leadConvertPage.tryingToConnectWithNgbsError.isDisplayed()) {
            sleep(5_000);
            leadConvertPage.opportunityLoadingBar.shouldBe(hidden, ofSeconds(30));
            leadConvertPage.convertButton.click();
        }

        leadConvertPage.switchFromIFrame();
        opportunityPage.entityTitle.shouldBe(visible, ofSeconds(120));
        opportunityPage.waitUntilLoaded();
    }

    /**
     * Check that the Lead has been successfully converted
     * (i.e. Account, Contact and Opportunity were created from the Lead).
     *
     * @param lead Lead object that was converted
     * @return converted Lead object with IDs of and links to the converted Account, Contact and Opportunity
     * @throws ConnectionException in case of errors while accessing API
     */
    @Step("Check that the Lead has been successfully converted")
    public Lead checkLeadConversion(Lead lead) throws ConnectionException {
        var convertedLead = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id, ConvertedAccountId, ConvertedOpportunityId, ConvertedContactId, " +
                        "ConvertedAccount.Id, ConvertedOpportunity.Id, ConvertedContact.Id " +
                        "FROM Lead " +
                        "WHERE Id = '" + lead.getId() + "'",
                Lead.class);

        assertThat(convertedLead.getConvertedOpportunityId())
                .as("Converted Lead.ConvertedOpportunityId value")
                .isNotNull();
        assertThat(convertedLead.getConvertedAccountId())
                .as("Converted Lead.ConvertedAccountId value")
                .isNotNull();
        assertThat(convertedLead.getConvertedContactId())
                .as("Converted Lead.ConvertedContactId value")
                .isNotNull();

        return convertedLead;
    }

    /**
     * Check the selected contact in "Contact" section on the Lead Convert page.
     *
     * @param contact contact object to be checked
     */
    @Step("Check the selected contact in 'Contact' section on the Lead Convert page")
    public void checkSelectedContact(Contact contact) {
        leadConvertPage.matchedContactsTableRadioButtons
                .findBy(selected)
                .shouldHave(exactValue(contact.getId()));

        leadConvertPage.contactInfoSelectedContactFullName
                .shouldHave(exactText(getFullName(contact)));
    }
}
