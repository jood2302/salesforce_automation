package com.aquiva.autotests.rc.internal.proxy;

import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.filters.RequestFilterAdapter;
import com.browserup.bup.filters.ResponseFilterAdapter;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codeborne.selenide.Selenide.open;

/**
 * JUnit Extension class that disables mock requests/responses in the proxy for test execution.
 * Should be executed after {@link SelenideProxyExtension} (to disable it, if necessary).
 * <p></p>
 * Use this extension for E2E test scenarios that check integration of CRM and external web services.
 */
public class SelenideProxyMocksDisabledExtension implements BeforeAllCallback {
    private static final Logger LOG = LoggerFactory.getLogger(SelenideProxyMocksDisabledExtension.class);

    /**
     * Remove any mock requests/responses in the Selenide's proxy.
     * <p></p>
     * Note: this callback is invoked once <em>before</em> all tests in the current
     * container.
     *
     * @param context the current extension context; never {@code null}
     */
    @Override
    public void beforeAll(ExtensionContext context) {
        if (!Configuration.proxyEnabled) {
            return;
        } else if (!WebDriverRunner.getWebDriver().getTitle().isBlank()) {
            throw new RuntimeException("The UI test is in the process! " +
                    "Make sure that your UI test steps start later in the order of JUnit callbacks.");
        }

        if (!WebDriverRunner.hasWebDriverStarted()) {
            open();
        }

        var selenideProxy = WebDriverRunner.getSelenideProxy();
        var filterFactories = ((BrowserUpProxyServer) selenideProxy.getProxy()).getFilterFactories();
        filterFactories.removeIf(httpFilter ->
                httpFilter instanceof ResponseFilterAdapter.FilterSource ||
                        httpFilter instanceof RequestFilterAdapter.FilterSource);

        LOG.info("All mock requests/responses are disabled for the test " +
                context.getRequiredTestClass().getName());
    }
}
