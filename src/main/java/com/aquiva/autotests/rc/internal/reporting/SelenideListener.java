package com.aquiva.autotests.rc.internal.reporting;

import com.aquiva.autotests.rc.internal.proxy.SelenideProxyExtension;
import com.browserup.harreader.model.HttpMethod;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.logevents.LogEvent;
import com.codeborne.selenide.logevents.LogEventListener;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.util.ResultsUtils;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.yandex.qatools.ashot.AShot;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Stream;

import static com.aquiva.autotests.rc.internal.proxy.SelenideProxyExtension.PROXY_IS_HAR;
import static com.aquiva.autotests.rc.internal.proxy.SelenideProxyExtension.PROXY_URLS_TO_INCLUDE_IN_HAR;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.qameta.allure.model.Status.BROKEN;
import static io.qameta.allure.model.Status.PASSED;
import static java.lang.Boolean.parseBoolean;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.openqa.selenium.OutputType.BYTES;
import static org.openqa.selenium.logging.LogType.BROWSER;
import static ru.yandex.qatools.ashot.shooting.ShootingStrategies.viewportPasting;

/**
 * Custom Selenide listener class for Allure reporting:
 * it gets logs from Selenide test execution context and prepares reports for Allure.
 * <p></p>
 * This custom listener adds some features to the standard functionality:
 * password masking, full page screenshots, adding console logs, etc...
 */
public class SelenideListener implements LogEventListener {

    /**
     * RegExp pattern to match URLs with Basic Authentication.
     * E.g. https://login:pass@example.com/
     */
    private static final String BASIC_AUTH_URL_PATTERN = "(.+://.+):(.+)@(.+)";
    private static final String BASIC_AUTH_URL_REPLACEMENT = "$1:***@$3";

    private final boolean isSaveScreenshots = true;
    private final boolean isSaveFullScreenScreenshot = true;
    private final boolean isSavePageSource = true;
    private final boolean isSaveConsoleLog = true;
    private final boolean isSaveVideo = parseBoolean(System.getProperty("selenoid.enableVideo"));

    private final AllureLifecycle lifecycle = Allure.getLifecycle();

    /**
     * Actions to take BEFORE any Selenide action/method/command.
     *
     * @param event event that's created on Selenide action
     *              (e.g. "navigate to url", "click on element", "check a condition")
     */
    @Override
    public void beforeEvent(final @Nonnull LogEvent event) {
        //  Empty implementation
    }

    /**
     * Actions to take AFTER any Selenide action/method/command.
     * <p></p>
     * This method helps add all the additional functionality
     * that is needed in test steps in Allure reports.
     * <p>
     * Note: normally, adding attachments works if step execution has failed
     * (e.g. as a result of a regular assertion error)
     *
     * @param event event that's created on Selenide action
     *              (e.g. "navigate to url", "click on element", "check a condition")
     */
    @Override
    public void afterEvent(final @Nonnull LogEvent event) {
        lifecycle.getCurrentTestCase().ifPresent(uuid -> {
            var stepUUID = UUID.randomUUID().toString();

            lifecycle.startStep(stepUUID, new StepResult()
                    .setName(event.toString())
                    .setStatus(PASSED));

            maskPasswordsInTestSteps(event);

            lifecycle.updateStep(stepResult -> stepResult.setStart(stepResult.getStart() - event.getDuration()));

            if (LogEvent.EventStatus.FAIL.equals(event.getStatus())) {
                if (WebDriverRunner.hasWebDriverStarted()) {
                    var handles = WebDriverRunner.getWebDriver().getWindowHandles();

                    for (var handle : handles) {
                        switchTo().window(handle);

                        var title = WebDriverRunner.getWebDriver().getTitle();

                        if (isSaveScreenshots) {
                            lifecycle.addAttachment("Screenshot - " + title, "image/png", "png",
                                    getScreenshotBytes());
                        }
                        if (isSaveFullScreenScreenshot) {
                            lifecycle.addAttachment("Full page screenshot - " + title, "image/png", "png",
                                    getFullScreenScreenshotBytes());
                        }
                        if (isSavePageSource) {
                            lifecycle.addAttachment("Page source - " + title, "text/html", "html",
                                    getPageSourceBytes());
                        }
                        if (isSaveConsoleLog) {
                            if (WebDriverRunner.isChrome()) {
                                lifecycle.addAttachment("Console log - " + title, "text/plain", "txt",
                                        getConsoleLogsBytes());
                            }
                        }
                        if (isSaveVideo && Configuration.remote != null && !Configuration.remote.isBlank()) {
                            var sessionId = ((RemoteWebDriver) WebDriverRunner.getWebDriver()).getSessionId();
                            var videoUrl = Configuration.remote.replace("/wd/hub", "/video");
                            var videoLink = String.format("%s/%s.mp4", videoUrl, sessionId.toString());

                            lifecycle.addAttachment(videoLink, "text/html", "html",
                                    getVideoLinkFileBytes(videoLink));
                        }

                        lifecycle.addAttachment("Current page's URL", "text/plain", "txt",
                                WebDriverRunner.getWebDriver().getCurrentUrl()
                                        .replaceAll(BASIC_AUTH_URL_PATTERN, BASIC_AUTH_URL_REPLACEMENT)
                                        .getBytes());

                        if (Configuration.proxyEnabled && PROXY_IS_HAR) {
                            lifecycle.addAttachment("Proxy HAR", "application/json", "har", getHarBytes());
                        }
                    }
                }

                lifecycle.updateStep(stepResult -> {
                    var status = ResultsUtils.getStatus(event.getError())
                            .orElse(BROKEN);
                    stepResult.setStatus(status);

                    var details = ResultsUtils.getStatusDetails(event.getError())
                            .orElse(new StatusDetails());
                    stepResult.setStatusDetails(details);
                });
            }

            lifecycle.stopStep(stepUUID);
        });
    }

    /**
     * Mask password values in Allure's test steps.
     *
     * @param event any Selenide's action event
     */
    private void maskPasswordsInTestSteps(LogEvent event) {
        //  Masking password in Basic Authentication URLs in test steps (e.g. https://login:pass@example.com/)
        if (event.getElement().toLowerCase().contains("open") &&
                event.getSubject().matches(BASIC_AUTH_URL_PATTERN)) {
            var basicAuthUrlMasked = event.toString()
                    .replaceAll(BASIC_AUTH_URL_PATTERN, BASIC_AUTH_URL_REPLACEMENT);
            lifecycle.updateStep(stepResult -> stepResult.setName(basicAuthUrlMasked));
        }
    }

    /**
     * Takes screenshot of the currently visible part of the current page.
     *
     * @return raw bytes that represent the taken screenshot.
     */
    private static byte[] getScreenshotBytes() {
        return ((TakesScreenshot) WebDriverRunner.getWebDriver()).getScreenshotAs(BYTES);
    }

    /**
     * Takes screenshot of the entire page that test is currently at.
     *
     * @return raw bytes that represent the taken screenshot.
     */
    private static byte[] getFullScreenScreenshotBytes() {
        var image = new AShot()
                .shootingStrategy(viewportPasting(1_000))
                .takeScreenshot(WebDriverRunner.getWebDriver())
                .getImage();

        var byteStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", byteStream);
        } catch (IOException ignored) {
        }

        return byteStream.toByteArray();
    }

    /**
     * Get HTML page source for the current page.
     *
     * @return raw bytes that represent the page's html code
     */
    private static byte[] getPageSourceBytes() {
        return WebDriverRunner.getWebDriver().getPageSource().getBytes(UTF_8);
    }

    /**
     * Get developer's console logs from the browser.
     * For Google Chrome only!
     *
     * @return raw bytes that represent the logs from the browser's dev console.
     */
    private static byte[] getConsoleLogsBytes() {
        var logEntries = WebDriverRunner.getWebDriver().manage()
                .logs().get(BROWSER).getAll();

        return logEntries.stream()
                .map(Objects::toString)
                .collect(joining("\n"))
                .getBytes(UTF_8);
    }

    /**
     * Get a link to the test's video replay.
     *
     * @param videoLink web link to the test's video replay
     *                  (e.g. "http://path.to.the.video.com/video1365.mp4")
     * @return raw bytes that represent the *.html file with link to the video
     */
    private static byte[] getVideoLinkFileBytes(String videoLink) {
        var videoLinkFile = new File("video.html");
        byte[] videoFileBytes = null;
        try {
            var htmlContent = "<html><body>" +
                    "<a target=\"_blank\" href=\"" + videoLink + "\">Test video replay</a>" +
                    "</body></html>";

            FileUtils.writeStringToFile(videoLinkFile, htmlContent, Charset.defaultCharset());
            videoFileBytes = FileUtils.readFileToByteArray(videoLinkFile);

            videoLinkFile.deleteOnExit();
        } catch (IOException ignored) {
        }

        return videoFileBytes;
    }

    /**
     * Get the HAR (HTTP Archive) of the current browser's session as a byte array.
     * Only the entries with the certain URLs will be included in the final HAR.
     * See {@link SelenideProxyExtension#PROXY_URLS_TO_INCLUDE_IN_HAR}
     *
     * @return HAR as a byte array
     */
    private static byte[] getHarBytes() {
        var har = WebDriverRunner.getSelenideProxy().getProxy().getHar();
        har.getLog().getEntries().removeIf(entry -> {
            var entryMethod = entry.getRequest().getMethod();
            var methodsToRemove = Set.of(HttpMethod.HEAD, HttpMethod.PROPFIND,
                    HttpMethod.OPTIONS, HttpMethod.REPORT, HttpMethod.CONNECT, HttpMethod.TRACE, HttpMethod.CCM_POST);

            var url = entry.getRequest().getUrl();
            var urlsToExclude = Stream.of(".js", ".css", ".png", ".gif", ".svg");
            var urlsToInclude = Arrays.stream(PROXY_URLS_TO_INCLUDE_IN_HAR.split(";"));
            var isUrlToExclude = urlsToExclude.anyMatch(url::contains) || urlsToInclude.noneMatch(url::contains);

            return methodsToRemove.contains(entryMethod) || isUrlToExclude;
        });

        try {
            return har.asBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get HAR as bytes!", e);
        }
    }
}
