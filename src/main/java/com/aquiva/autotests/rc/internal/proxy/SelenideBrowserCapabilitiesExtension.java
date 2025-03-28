package com.aquiva.autotests.rc.internal.proxy;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.Map;

import static java.lang.Boolean.parseBoolean;

/**
 * JUnit Extension class that sets up browser's capabilities for Selenide.
 * After the capabilities are statically set up, all subsequent calls to {@code Selenide.open()}
 * will create a browser configured with capabilities from this class.
 */
public class SelenideBrowserCapabilitiesExtension implements BeforeAllCallback {

    /**
     * Set up some browser's capabilities specific for a browser (e.g. Chrome or Firefox),
     * or type of web driver (e.g. local or remote).
     * <br/>
     * Note: invoked once <em>before</em> all tests in the current container.
     */
    @Override
    public void beforeAll(ExtensionContext context) {
        var capabilities = new MutableCapabilities();

        if (WebDriverRunner.isChrome()) {
            var options = new ChromeOptions();
            options.addArguments("--disable-features=site-per-process");
            options.addArguments("--disable-notifications");
            capabilities = capabilities.merge(options);
        }

        if (Configuration.remote != null) {
            var selenoidOptions = Map.<String, Object>of(
                    "name", System.getProperty("selenoid.name", "default"),
                    "enableVNC", parseBoolean(System.getProperty("selenoid.enableVNC", "true")),
                    "sessionTimeout", System.getProperty("selenoid.sessionTimeout", "10m"),
                    "screenResolution", System.getProperty("selenoid.screenResolution", "3200x1800x24"),
                    "enableVideo", parseBoolean(System.getProperty("selenoid.enableVideo"))
            );
            capabilities.setCapability("selenoid:options", selenoidOptions);
        }

        Configuration.browserCapabilities = capabilities;
    }
}
