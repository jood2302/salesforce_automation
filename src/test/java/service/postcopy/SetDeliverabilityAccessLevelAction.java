package service.postcopy;

import base.BaseTest;
import org.junit.jupiter.api.*;

import static base.Pages.deliverabilityPage;
import static base.Pages.loginPage;
import static com.aquiva.autotests.rc.page.salesforce.setup.DeliverabilityPage.ALL_EMAIL_OPTION;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("PostCopy")
public class SetDeliverabilityAccessLevelAction extends BaseTest {

    @BeforeEach
    public void setUpTest() {
        step("Open the sandbox's login page, log in to SF as System Administrator user", () -> {
            loginPage.openPage().login();
        });
    }

    @Test
    @DisplayName("Set Deliverability Access level = 'All email' in 'Deliverability'")
    public void test() {
        step("1. Open 'Setup - Email - Deliverability'",
                deliverabilityPage::openPage
        );

        step("2. Select deliverability access level to 'All email' and save changes", () -> {
            deliverabilityPage.accessLevelSelect.shouldBe(visible, ofSeconds(60))
                    .selectOption(ALL_EMAIL_OPTION);
            deliverabilityPage.saveChangesButton.click();
            deliverabilityPage.saveSuccessMessage.shouldBe(visible, ofSeconds(60));
        });

        step("3. Verify that deliverability access level = 'All email'", () -> {
            deliverabilityPage.accessLevelSelect.getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(ALL_EMAIL_OPTION));
        });
    }
}
