package com.aquiva.autotests.rc.page.prm;

import com.codeborne.selenide.SelenideElement;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_PORTAL_URL;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$x;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.SetValueOptions.withText;
import static java.time.Duration.ofSeconds;

/**
 * The page to log in to the Avaya or Atos PRM Portal.
 * Contains methods to open the login page, wait until it is loaded, and log in with the given credentials.
 */
public class AvayaAtosPortalLoginPage {
    private final SelenideElement usernameInput = $x("//div[@class='usernamewrap']/input");
    private final SelenideElement passwordInput = $x("//div[@class='passwordwrap']/input");
    private final SelenideElement submitButton = $x("//input[@value='Submit']");

    /**
     * Open the Avaya PRM Portal Login page via direct link.
     *
     * @return opened Login Page reference
     */
    public AvayaAtosPortalLoginPage openAvayaPortalLoginPage() {
        open(BASE_PORTAL_URL + "/partner");
        waitUntilLoaded();

        return this;
    }

    /**
     * Open the Atos PRM Portal Login page via direct link.
     *
     * @return opened Login Page reference
     */
    public AvayaAtosPortalLoginPage openAtosPortalLoginPage() {
        open(BASE_PORTAL_URL + "/atosunify");
        waitUntilLoaded();

        return this;
    }

    /**
     * Wait until the page is loaded
     * so that the user/test may safely interact with any of its elements after this method is finished.
     */
    public void waitUntilLoaded() {
        submitButton.shouldBe(visible, ofSeconds(10));
    }

    /**
     * Log in to Avaya/Atos PRM Portal using the given credentials
     * by submitting username and password via UI (login form).
     */
    public void login(String username, String password) {
        usernameInput.setValue(username);
        passwordInput.setValue(withText(password).withDisplayedText("***"));
        submitButton.click();

        submitButton.shouldBe(hidden, ofSeconds(30));
    }
}
