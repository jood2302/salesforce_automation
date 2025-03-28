package base;

import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import com.sforce.ws.ConnectionException;
import io.qameta.allure.Step;

import java.util.List;

import static base.Pages.loginPage;
import static base.Pages.salesforcePage;
import static com.aquiva.autotests.rc.utilities.Constants.*;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

/**
 * Test methods for the flows related to basic Salesforce actions:
 * e.g. login, logout, open the Lightning App, etc.
 */
public class SfdcSteps {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    /**
     * New instance for the class with the test methods/steps
     * related to basic Salesforce actions.
     */
    public SfdcSteps() {
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    /**
     * Log in as different user in active user's session.
     * <p><b>
     * Note: only works for active SYSTEM ADMINISTRATOR user session!
     * <p>
     * To login as different user in active non-admin user session,
     * please use {@link SfdcSteps#reLoginAsUser(User)}.
     * </p>
     * </b></p>
     *
     * @param user different user to login as
     * @throws Exception in case of malformed DB queries or network failures
     */
    @Step("Log in as different user in current admin user session")
    public void loginAsUser(User user) throws Exception {
        var organization = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id " +
                        "FROM Organization",
                Organization.class);

        var loginAsUserUrl = String.format(BASE_URL +
                        "/servlet/servlet.su" +
                        "?oid=%s" +
                        "&suorgadminid=%s" +
                        "&retURL=/home/home.jsp" +
                        "&targetURL=/home/home.jsp",
                organization.getId(), user.getId()
        );

        open(loginAsUserUrl);
        salesforcePage.systemMessageHeader.shouldBe(visible, ofSeconds(60));

        openDefaultApp();
    }

    /**
     * Open the app with provided name in the current user's session.
     *
     * @param appName name of the application to be opened
     * @throws ConnectionException in case of malformed DB queries or network failures
     */
    @Step("Open App in the current user's session")
    public void openApp(String appName) throws ConnectionException {
        var app = enterpriseConnectionUtils.querySingleRecord(
                "SELECT DurableId " +
                        "FROM AppDefinition " +
                        "WHERE Label = '" + appName + "'" +
                        "AND MasterLabel = '" + appName + "'",
                AppDefinition.class
        );

        var changeAppUrl = String.format(BASE_URL +
                "/lightning/app/%s", app.getDurableId()
        );

        open(changeAppUrl);
        salesforcePage.activeAppName.shouldHave(exactTextCaseSensitive(appName), ofSeconds(20));
    }


    /**
     * Open the default app in the current user's session.
     *
     * @throws ConnectionException in case of malformed DB queries or network failures
     * @see com.aquiva.autotests.rc.utilities.Constants#DEFAULT_APP_LABEL
     */
    @Step("Open the Default App in the current user's session")
    public void openDefaultApp() throws ConnectionException {
        openApp(DEFAULT_APP_LABEL);
    }

    /**
     * Log in as different user in active user's session.
     * <p><b>
     * Note: only works for active NON-ADMIN user session,
     * if it was previously accessed via ADMIN user session.
     * <p>
     * To login as different user in active admin user session,
     * please use {@link SfdcSteps#loginAsUser(User)}.
     * </p>
     * </b></p>
     *
     * @param user different user to re-login as
     * @throws Exception in case of malformed DB queries or network failures
     */
    @Step("Log in as different user in current non-admin user session")
    public void reLoginAsUser(User user) throws Exception {
        logout();
        loginAsUser(user);
    }

    /**
     * Log in as different user in active user's session.
     * <br/>
     * Note: only use it if there are random logout issues in Salesforce
     * that happen when using a regular {@link #reLoginAsUser(User)}.
     *
     * @param user different user to re-login as
     */
    @Step("Log in as different user after resetting the current user session")
    public void reLoginAsUserWithSessionReset(User user) {
        resetSalesforceBrowserSession();
        initLoginToSfdcAsTestUser(user);
    }

    /**
     * Log out from current user's session in Salesforce.
     */
    @Step("Log out from current user's session in Salesforce")
    public void logout() {
        open(LOGOUT_LINK);
    }

    /**
     * Open the SFDC's Login Page, login with the provided user's credentials (as System Administrator),
     * and re-login as a test user (using Login As User functionality of the Admin).
     *
     * @param testUser test user to re-login as (e.g. Sales Rep user; Deal Desk user, etc.)
     */
    public void initLoginToSfdcAsTestUser(User testUser) {
        step("Open test sandbox login page, log in to SFDC as Admin, and re-login as the test user", () -> {
            loginPage.openPage().login();
            loginAsUser(testUser);
        });
    }

    /**
     * Reset the current browser session for the Salesforce resources
     * (clear the cookies and local storage).
     */
    @Step("Reset the current browser's session in Salesforce")
    public void resetSalesforceBrowserSession() {
        var domains = List.of(BASE_URL, BASE_VF_URL);
        for (var url : domains) {
            open(url + "/favicon.ico");

            clearBrowserCookies();
            clearBrowserLocalStorage();
            sessionStorage().clear();
        }
    }
}
