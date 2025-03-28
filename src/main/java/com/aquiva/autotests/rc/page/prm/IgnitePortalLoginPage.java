package com.aquiva.autotests.rc.page.prm;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.Cookie;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_PORTAL_URL;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$x;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.SetValueOptions.withText;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static java.time.Duration.ofSeconds;

/**
 * The Ignite PRM Portal page for users to log into.
 */
public class IgnitePortalLoginPage {
    private final SelenideElement usernameInput = $x("//*[@name='usernameInput']");
    private final SelenideElement passwordInput = $x("//*[@name='passwordInput']");
    private final SelenideElement loginButton = $x("//button[@title='Log in']");

    /**
     * Open the Ignite PRM Portal Login page via direct link.
     *
     * @return opened Login Page reference
     */
    public IgnitePortalLoginPage openPage() {
        open(BASE_PORTAL_URL + "/RCPartnerProgram");
        waitUntilLoaded();

        //  to bypass cookie consent modal window
        getWebDriver().manage().addCookie(new Cookie("LSKey-c$Cookie", "1", "/RCPartnerProgram"));

        return this;
    }

    /**
     * Wait until the page is loaded
     * so that the user/test may safely interact with any of its elements after this method is finished.
     */
    public void waitUntilLoaded() {
        loginButton.shouldBe(visible, ofSeconds(10));
    }

    /**
     * Log in to Ignite PRM Portal using the given credentials
     * by submitting username and password via UI (login form).
     */
    public void login(String username, String password) {
        usernameInput.setValue(username);
        passwordInput.setValue(withText(password).withDisplayedText("***"));
        loginButton.click();

        loginButton.shouldBe(hidden, ofSeconds(30));
    }
}
