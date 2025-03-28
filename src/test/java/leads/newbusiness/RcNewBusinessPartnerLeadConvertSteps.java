package leads.newbusiness;

import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Account;
import com.sforce.soap.enterprise.sobject.Lead;
import leads.LeadConvertSteps;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.NEW_ACCOUNT_WILL_BE_CREATED_MESSAGE;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test methods related to the checks for Partner Lead Conversion flow for RingCentral New Business accounts.
 */
public class RcNewBusinessPartnerLeadConvertSteps {
    private final LeadConvertSteps leadConvertSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String ringCentralBrand;
    private final String ringCentralBI;
    private final String serviceName;

    /**
     * New instance for the class with the test methods/steps related to
     * the checks for Partner Lead Conversion flow for RingCentral New Business accounts.
     */
    public RcNewBusinessPartnerLeadConvertSteps(Dataset data) {
        leadConvertSteps = new LeadConvertSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        ringCentralBrand = data.brandName;
        ringCentralBI = data.getBusinessIdentityName();
        serviceName = data.packageFolders[0].name;
    }

    /**
     * <p> - Open Lead Conversion page for the Partner Lead and choose creating a new Account or using existing one. </p>
     * <p> - Select Business Identity and Service on {@link LeadConvertPage} and convert the Lead. </p>
     * <p> - Check that the Lead is converted, Account, Contact and Opportunity from the Lead are created. </p>
     *
     * @param partnerLead Partner Lead for Lead Conversion
     * @param account     Account object, to convert the Lead into an existing (in SFDC) Account;
     *                    or {@code null}, to convert the Lead into a new Account
     */
    protected void newBusinessPartnerLeadConvertTestSteps(Lead partnerLead, Account account) {
        step("1. Open Lead Convert page for the test Partner Lead", () -> {
            leadConvertPage.openPage(partnerLead.getId());

            leadConvertPage.newExistingAccountToggle.shouldBe(enabled, ofSeconds(60));
            leadConvertPage.existingAccountSearchInput.getSelf().shouldBe(visible);
        });

        if (account == null) {
            step("2. Switch the toggle into 'Create New Account' position", () -> {
                leadConvertPage.newExistingAccountToggle.click();

                leadConvertPage.opportunityInfoSection.shouldBe(visible);
                leadConvertPage.newExistingAccountToggle.$x(".//input").shouldBe(disabled);
                leadConvertPage.accountInfoLabel
                        .shouldHave(exactTextCaseSensitive(NEW_ACCOUNT_WILL_BE_CREATED_MESSAGE));

                leadConvertPage.waitUntilOpportunitySectionIsLoaded();
                leadConvertPage.opportunityNameNonEditable.shouldHave(exactText(partnerLead.getCompany()));
                leadConvertPage.opportunityBrandNonEditable.shouldHave(exactText(ringCentralBrand));
            });
        } else {
            step("2. Select the account that exists in SFDC (from the 'Matched Accounts' table), " +
                    "click 'Apply' button in Account info section " +
                    "and check Opportunity Name and Brand fields in the Opportunity Info section", () -> {
                leadConvertPage.selectMatchedAccount(account.getId());

                leadConvertPage.waitUntilOpportunitySectionIsLoaded();
                leadConvertPage.opportunityNameNonEditable.shouldHave(exactText(account.getName()));
                leadConvertPage.opportunityBrandNonEditable.shouldHave(exactText(ringCentralBrand));
            });
        }

        step("3. Click 'Edit' in Opportunity Section, check that Business Identity = 'RingCentral Inc.', " +
                "select 'Office' in Service picklist, populate Close Date field and press 'Apply' button", () -> {
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.businessIdentityPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(ringCentralBI));
            leadConvertPage.selectService(serviceName);
            leadConvertPage.closeDateDatepicker.setTomorrowDate();

            leadConvertPage.opportunityInfoApplyButton.click();
        });

        step("4. Press 'Convert' button", () ->
                leadConvertSteps.pressConvertButton()
        );

        step("5. Check that the Lead is converted", () -> {
            leadConvertSteps.checkLeadConversion(partnerLead);

            if (account != null) {
                step("Check that the converted account for the Lead = selected account on the LC page", () -> {
                    var convertedLead = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Id, ConvertedAccountId " +
                                    "FROM Lead " +
                                    "WHERE Id = '" + partnerLead.getId() + "'",
                            Lead.class);
                    assertThat(convertedLead.getConvertedAccountId())
                            .as("ConvertedLead.ConvertedAccountId value")
                            .isEqualTo(account.getId());
                });
            }
        });
    }
}