package com.aquiva.autotests.rc.page;

import com.codeborne.selenide.SelenideElement;

import static com.aquiva.autotests.rc.utilities.Constants.*;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.SetValueOptions.withText;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

/**
 * Login page where user logs in to Salesforce org using username and password.
 */
public class LoginPage {

    public final SelenideElement logo = $("#logo");
    public final SelenideElement usernameInput = $("#username");
    public final SelenideElement passwordInput = $("#password");
    public final SelenideElement loginButton = $("#Login");

    /**
     * Open the Login page via direct link.
     *
     * @return opened Login Page reference
     */
    public LoginPage openPage() {
        open(BASE_URL + "/?login");
        waitUntilLoaded();
        return this;
    }

    /**
     * Wait until the page is loaded
     * so that the user/test may safely interact with any of its elements after this method is finished.
     */
    public void waitUntilLoaded() {
        logo.shouldBe(visible, ofSeconds(10));
        usernameInput.shouldBe(visible, enabled);
        passwordInput.shouldBe(visible, enabled);
    }

    /**
     * Login to Salesforce org using default credentials
     * by submitting username and password via UI (login form).
     * <p> Note: default credentials are usually provided via system properties. </p>
     */
    public void login() {
        usernameInput.setValue(USER);
        passwordInput.setValue(withText(PASSWORD).withDisplayedText("***"));

        //  for some reason on Chrome 107+ version the login *might* happen right after the user types the password,
        //  and there's no login/password to send the 'Enter' key to, or 'Login' button to click. 
        step("Click the 'Login' button", () -> {
            try {
                executeJavaScript("arguments[0].click()", loginButton);
            } catch (Throwable ignored) {
            }
        });

        loginButton.shouldBe(hidden, ofSeconds(20));
    }
}
