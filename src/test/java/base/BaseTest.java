package base;

import com.aquiva.autotests.rc.internal.proxy.SelenideBrowserCapabilitiesExtension;
import com.aquiva.autotests.rc.internal.proxy.SelenideProxyExtension;
import com.aquiva.autotests.rc.internal.reporting.JUnitLoggerExtension;
import com.aquiva.autotests.rc.internal.reporting.SelenideListener;
import com.aquiva.autotests.rc.internal.util.ElementHighlighting;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.junit5.BrowserStrategyExtension;
import com.codeborne.selenide.logevents.SelenideLogger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.aquiva.autotests.rc.internal.util.ElementHighlighting.IS_HIGHLIGHT_ELEMENTS;

/**
 * Base test class for all the other tests in the current framework.
 * Serves as a main parent class for every test in the framework.
 * <br/><br/>
 * The class defines some technical features for other tests:
 * web driver capabilities, Allure's report listener, basic commandline logging settings, etc...
 */
@ExtendWith({BrowserStrategyExtension.class, JUnitLoggerExtension.class, SelenideBrowserCapabilitiesExtension.class})
@ExtendWith(SelenideProxyExtension.class)
public abstract class BaseTest {

    @BeforeAll
    public void setUpBaseTestAll() {
        SelenideLogger.addListener("allure", new SelenideListener());
        if (IS_HIGHLIGHT_ELEMENTS) {
            WebDriverRunner.addListener(new ElementHighlighting());
        }
    }

    @AfterAll
    public void tearDownBaseTestAll() {
        SelenideLogger.removeAllListeners();
    }
}
