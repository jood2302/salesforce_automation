package service.postcopy;

import base.BaseTest;
import org.junit.jupiter.api.*;

import static base.Pages.loginPage;
import static base.Pages.myDomainPage;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("PostCopy")
public class UncheckLoginPolicyAction extends BaseTest {

    @BeforeEach
    public void setUpTest() {
        step("Open the sandbox's login page, log in to SF as System Administrator user", () -> {
            loginPage.openPage().login();
        });
    }

    @Test
    @DisplayName("Uncheck 'Login Policy - Prevent login from https://test.salesforce.com' checkbox in 'My Domain'")
    public void test() {
        step("1. Open 'Setup - Company Settings - My Domain'",
                myDomainPage::openPage
        );

        step("2. Uncheck 'Login Policy - Prevent login from https://test.salesforce.com' checkbox in 'Routing and Policies' section", () -> {
            myDomainPage.editRoutingAndPoliciesButton.click();
            myDomainPage.preventLoginFromTestCheckbox.setSelected(false);
            myDomainPage.savePolicyChangesButton.click();
            myDomainPage.savePolicyChangesButton.shouldBe(hidden, ofSeconds(10));
        });

        step("3. Verify that 'Login Policy - Prevent login from https://test.salesforce.com' checkbox is unchecked", () -> {
            refresh();
            myDomainPage.switchToIFrame();
            myDomainPage.waitUntilLoaded();
            myDomainPage.preventLoginFromTestCheckbox.shouldNotBe(checked);
        });
    }
}
