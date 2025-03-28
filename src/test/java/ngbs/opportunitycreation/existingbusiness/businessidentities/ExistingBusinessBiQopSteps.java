package ngbs.opportunitycreation.existingbusiness.businessidentities;

import com.aquiva.autotests.rc.page.opportunity.OpportunityCreationPage;

import static base.Pages.opportunityCreationPage;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

/**
 * Test methods related to test cases that check Business identities
 * for Existing Business accounts on {@link OpportunityCreationPage}.
 */
public class ExistingBusinessBiQopSteps {

    /**
     * Open QOP for Existing Business Account, and check 'Business Identity' picklist in 'Select Service Plan' section.
     *
     * @param accountId                   Id of the provided Existing Business Account
     * @param preselectedBusinessIdentity expected preselected value of 'Business Identity' picklist
     */
    public void openQopAndCheckPreselectedBiTestSteps(String accountId, String preselectedBusinessIdentity) {
        step("1. Open QOP for the Existing Business Account", () -> {
            opportunityCreationPage.openPage(accountId);
        });

        step("2. Check that expected BI is preselected in 'Business Identity' picklist", () -> {
            opportunityCreationPage.businessIdentityPicklist.getOptions()
                    .shouldHave(sizeGreaterThan(0), ofSeconds(30));
            opportunityCreationPage.businessIdentityPicklist
                    .getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(preselectedBusinessIdentity));
        });
    }
}
