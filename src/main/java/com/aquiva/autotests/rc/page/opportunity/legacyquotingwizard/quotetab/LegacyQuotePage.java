package com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.quotetab;

import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.BaseLegacyQuotingWizardPage;
import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.ProServQuotingWizardPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;
import static com.codeborne.selenide.SetValueOptions.withDate;
import static java.time.Duration.ofSeconds;
import static java.time.LocalDate.now;

/**
 * 'Quote' page: one of the tabs on the Legacy Quote Wizard pipeline.
 * <br/><br/>
 * Can be accessed via Legacy Quote Wizard on the 'Main Quote' and 'ProServ Quote' tabs.
 * <br/><br/>
 * Contains some useful fields and picklists for ProServ Quote.
 *
 * @see BaseLegacyQuotingWizardPage
 * @see ProServQuotingWizardPage
 */
public class LegacyQuotePage extends BaseLegacyQuotingWizardPage {

    //  Picklists default values
    public static final String BUDGETARY_FORECAST_CATEGORY = "Budgetary";
    public static final String CC_BASIC_COMPLEXITY = "CC-Basic";

    private final SelenideElement proServArchitectInput =
            $x("//*[./label[text()='ProServ Architect']]//input");
    private final SelenideElement proServProjectComplexity =
            $x("//*[@name='proServProjectComplexity']");
    private final SelenideElement saveQuoteButton = $x("//button[text()='Save' and @id='saveButton']");
    public final SelenideElement originalSOWQuoteNumberInput =
            $x("//*[./label[text()='Original SOW Quote Number']]//input");
    public final SelenideElement proServUsersInput =
            $x("//div[not(contains(@class,'slds-hide'))]/lightning-input//*[@name='numberOfProServUsers']");
    public final SelenideElement expirationDateInput =
            $x("//label[./span[text()='Expiration Date']]/..//input");
    public final SelenideElement proServForecastedCloseDateCcProServ =
            $x("//lightning-datepicker[.//*[@name='proServForecastedCloseDate2']]");
    public final SelenideElement proServForecastedCloseDateProServInput =
            $x("//label[./span[text()='ProServ Forecasted Close Date']]/..//input");
    public final SelenideElement proServForecastCategorySelect =
            $x("//*[./label='ProServ Forecast Category']//select");
    public final SelenideElement proServSowTypeSelect = $x("//*[./label='ProServ SOW Type']//select");
    public final SelenideElement signedSowInput = $x("//*[./label='Signed SOW' or ./label='SOW Link']//input");
    public final SelenideElement uidFromBizInput = $x("//*[./label='UID from .biz']//input");

    /**
     * Enter provided search query in the 'ProServ Architect' field and select found element from
     * the drop-down list element.
     *
     * @param searchQuery name of the searched ProServ Architect user.
     */
    public void selectProServArchitect(String searchQuery) {
        proServArchitectInput.setValue(searchQuery);
        $x("//*[@data-aura-class='cLookupItemList']//span[./span[text()='" + searchQuery + "']]")
                .click();
    }

    /**
     * Set tomorrow's date value to 'ProServ Forecasted Close Date' field on ProServ Quote.
     */
    public void selectDefaultForecastedCloseDateOnProServQuote() {
        var tomorrowForecastedCloseDate = now().plusDays(1);
        proServForecastedCloseDateProServInput.setValue(withDate(tomorrowForecastedCloseDate));
    }

    /**
     * Set today's date in the 'ProServ Forecasted Close Date' field on CC ProServ Quote.
     */
    public void selectDefaultForecastedCloseDateOnCcProServQuote() {
        proServForecastedCloseDateCcProServ.click();
        proServForecastedCloseDateCcProServ.$(byText("Today")).click();
    }

    /**
     * Set 'CC-Basic' value to 'ProServ Project Complexity' field.
     */
    public void selectDefaultProjectComplexity() {
        proServProjectComplexity.click();
        $(byText(CC_BASIC_COMPLEXITY))
                .shouldBe(visible, ofSeconds(10))
                .click();
    }

    /**
     * Click 'Save' button on the ProServ Quote tab.
     */
    public void saveQuote() {
        saveQuoteButton.click();
        waitUntilLoaded();
    }
}
