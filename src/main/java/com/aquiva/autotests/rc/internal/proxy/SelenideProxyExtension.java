package com.aquiva.autotests.rc.internal.proxy;

import com.browserup.bup.proxy.CaptureType;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.proxy.SelenideProxyServer;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Set;

import static com.aquiva.autotests.rc.utilities.JsonUtils.readResourceAsString;
import static com.codeborne.selenide.Selenide.open;

/**
 * JUnit Extension class that control features in case Selenide's proxy is enabled:
 * provides mock responses for external web services; captures HAR files.
 * <br/><br/>
 * <h2>For the mock requests/responses:</h2>
 * See {@link #PROXY_IS_MOCK}.
 * Right now all mock responses are set up directly in the main callback method.
 * If the number of mock responses starts to grow, then this needs to be refactored.
 * <br/>
 * Should be used for the most test scenarios that don't check integration between CRM and web services.
 * <br/><br/>
 * <h2>For the capturing of HAR files:</h2>
 * See {@link #PROXY_IS_HAR}.
 * The extension controls the type of content that should be captured in the HAR files.
 */
public class SelenideProxyExtension implements BeforeAllCallback {
    /**
     * A flag that indicates whether the mock requests/responses should be used via proxy server.
     */
    public static final boolean PROXY_IS_MOCK = Boolean.parseBoolean(System.getProperty("proxy.isMock", "true"));
    /**
     * A flag that indicates whether the HAR files should be captured via proxy server.
     */
    public static final boolean PROXY_IS_HAR = Boolean.parseBoolean(System.getProperty("proxy.isHar", "false"));
    /**
     * A semicolon-separated list of *parts* of URLs of HTTP requests/responses that should be included in the HAR file.
     * <br/>
     * E.g. for "rclabenv.com" it will include all URLs that contain this part,
     * like "https://rc-gci-armbiams.rclabenv.com/sfdc/account",
     */
    public static final String PROXY_URLS_TO_INCLUDE_IN_HAR = System.getProperty("proxy.urlsToIncludeInHar",
            "rclabenv.com;ApexAction");

    private boolean isRequestForUnsupportedPackage = false;
    private boolean isRequestForRcSwitzerlandLanguages = false;

    /**
     * Open an empty web browser and set up mock responses for external web services.
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
        }

        if (!WebDriverRunner.hasWebDriverStarted()) {
            open();
        }

        var selenideProxy = WebDriverRunner.getSelenideProxy();

        if (PROXY_IS_MOCK) {
            addRequestFilterForUnsupportedPackages(selenideProxy);
            addResponseFilterForSupportedPackages(selenideProxy);
        }

        if (PROXY_IS_HAR) {
            selenideProxy.getProxy().enableHarCaptureTypes(Set.of(
                    CaptureType.REQUEST_HEADERS, CaptureType.REQUEST_CONTENT, CaptureType.REQUEST_BINARY_CONTENT,
                    CaptureType.RESPONSE_HEADERS, CaptureType.RESPONSE_CONTENT, CaptureType.RESPONSE_BINARY_CONTENT)
            );
        }
    }

    /**
     * Filter requests to the Funnel Service.
     * <br/>
     * It is used to identify test cases that...
     * <p> - work with unsupported packages (e.g. "RingCentral Meetings") </p>
     * <p> - work with packages that support multiple preferred languages (e.g. "RingCentral MVP Standard" for Switzerland)</p>
     * ... to provide a proper mock response via {@link #addResponseFilterForSupportedPackages(SelenideProxyServer)}.
     *
     * @param proxy Selenide's proxy server instance
     */
    private void addRequestFilterForUnsupportedPackages(SelenideProxyServer proxy) {
        proxy.addRequestFilter("Funnel Service Request Filter",
                (request, contents, messageInfo) -> {
                    var isPost = request.method().name().equalsIgnoreCase("POST");
                    var isGetCountries = messageInfo.getUrl().contains("/funnel-area-codes/get-countries");
                    var isGetPreferredLanguage = messageInfo.getUrl().contains("/funnel-area-codes/get-preferred-language");
                    var textBody = contents.getTextContents();
                    if (isPost && isGetCountries && !textBody.isBlank()) {
                        //  Any new unsupported packages' IDs should be incorporated into this RegEx
                        isRequestForUnsupportedPackage = textBody.matches(".*\"packageId\":\"([678]|1118)\".*");
                    } else if (isPost && isGetPreferredLanguage && !textBody.isBlank()) {
                        //  Any new packages' IDs that should have different languages should be incorporated into this RegEx
                        isRequestForRcSwitzerlandLanguages = textBody.matches(".*\"packageId\":\"291\".*");
                    }

                    return null;
                });
    }

    /**
     * Filter responses from the Funnel Service for supported packages.
     * It is used to mock responses from the service.
     *
     * @param proxy Selenide's proxy server instance
     */
    private void addResponseFilterForSupportedPackages(SelenideProxyServer proxy) {
        proxy.addResponseFilter("Funnel Service Mock Response Filter", (response,
                                                                        contents, messageInfo) -> {
            var isPost = messageInfo.getOriginalRequest().method().name().equalsIgnoreCase("POST");
            var isFunnelService = messageInfo.getUrl().contains("funnel-area-codes") ||
                    messageInfo.getUrl().contains("package/availability");
            if (!isPost && !isFunnelService) {
                return;
            }

            String newJsonResponse = null;
            if (messageInfo.getUrl().contains("/get-countries")) {
                newJsonResponse = isRequestForUnsupportedPackage ?
                        readResourceAsString("mock/get-countries-unsupported_response.json") :
                        readResourceAsString("mock/get-countries_response.json");
            } else if (messageInfo.getUrl().contains("/get-states")) {
                newJsonResponse = readResourceAsString("mock/get-states_response.json");
            } else if (messageInfo.getUrl().contains("/get-locations")) {
                newJsonResponse = readResourceAsString("mock/get-locations_response.json");
            } else if (messageInfo.getUrl().contains("/get-toll-free-prefixes")) {
                newJsonResponse = readResourceAsString("mock/get-toll-free-prefixes_response.json");
            } else if (messageInfo.getUrl().contains("/check-availability")) {
                newJsonResponse = readResourceAsString("mock/check-availability_response.json");
            } else if (messageInfo.getUrl().contains("/package/availability")) {
                newJsonResponse = readResourceAsString("mock/package-availability_response.json");
            } else if (messageInfo.getUrl().contains("/dam-info")) {
                newJsonResponse = readResourceAsString("mock/dam-info_response.json");
            } else if (messageInfo.getUrl().contains("/get-preferred-language")) {
                newJsonResponse = isRequestForRcSwitzerlandLanguages ?
                        readResourceAsString("mock/get-preferred-language_291_response.json") :
                        readResourceAsString("mock/get-preferred-language_response.json");
            }

            if (newJsonResponse != null) {
                response.headers().remove("Content-Length");
                contents.setTextContents(newJsonResponse);
            }
        });
    }
}
