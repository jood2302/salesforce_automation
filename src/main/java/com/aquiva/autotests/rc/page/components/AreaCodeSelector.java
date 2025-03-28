package com.aquiva.autotests.rc.page.components;

import com.aquiva.autotests.rc.model.ngbs.testdata.AreaCode;
import com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.AreaCodePage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.DeviceAssignmentPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage;
import com.aquiva.autotests.rc.page.opportunity.OpportunityCreationPage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.byCssSelector;
import static com.codeborne.selenide.Selectors.withText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;
import static java.time.Duration.ofSeconds;

/**
 * Represent component that is used to fill in data about client's Area Code.
 * <p></p>
 * There are two types of Area Codes:
 * <p> - Local Area Codes
 * <p> - Toll-Free Area Codes
 * <p></p>
 * All Area Codes are selected sequentially.
 * <p></p>
 * For Local Area Code, first the country is selected (e.g. "United States", "Canada", "Puerto Rico"),
 * then the state or province (e.g. "California", "Alberta", etc.), then the city with a digital telephone code
 * (e.g. "Alpine (619)", "Calgary (403)", etc.). If there are no states in the country, then after selecting the country,
 * the city with the code is selected.
 * <p></p>
 * For Toll-Free Are Code, first the country is selected (e.g. "United States", "Canada", "Puerto Rico"),
 * then a digital telephone code (e.g. "800", "888", etc.).
 * <p></p>
 * Can be found on {@link OpportunityCreationPage}, {@link LeadConvertPage}, {@link DeviceAssignmentPage},
 * {@link AreaCodePage}, and {@link QuotePage}.
 */
public class AreaCodeSelector {
    public static final String AREA_CODES_ARE_NOT_REQUIRED_MESSAGE = "Area Codes are not required for selected package";
    public static final String START_TYPING_TO_SEARCH_MESSAGE = "Start typing to search Country";
    public static final String SEARCH_CODES_MESSAGE = "Search Codes";
    public static final String REQUIRED_AREA_CODE_ERROR = "Area Code is required";

    /**
     * Parent web element.
     */
    private final SelenideElement areaCodeComponent;

    private final By input = byCssSelector("input");
    private final By selectedAreaCodeFullName = byCssSelector("icon[iconname='custom64'] ~ span");
    private final By clearButton = byCssSelector("[iconname='close'] button");
    private final By removeOptionButton = byCssSelector("[title='Remove']");
    private final By defaultAreaCodeSection = byCssSelector("[datauiautoinput='main-area-code']");

    /**
     * Constructor for Area Code Selector component with default locator for parent web element.
     */
    public AreaCodeSelector() {
        areaCodeComponent = $x("//*[@data-ui-auto='lookupCombobox']");
    }

    /**
     * Constructor for Area Code Selector component with web element as a parameter for its parent.
     *
     * @param areaCodeComponent SelenideElement that used to locate Area Code Selector element in DOM
     */
    public AreaCodeSelector(SelenideElement areaCodeComponent) {
        this.areaCodeComponent = areaCodeComponent;
    }

    /**
     * Return actual web element behind Area Code Selector component.
     * <p></p>
     * Useful if test needs to perform actions on the web element itself
     * via Selenide framework actions (waits, assertions, etc.).
     *
     * @return SelenideElement that represents Area Code Selector in the DOM
     */
    public SelenideElement getSelf() {
        return areaCodeComponent;
    }

    /**
     * Return the list of search results after initiating a search.
     *
     * @return list of the available search results
     */
    public ElementsCollection getSearchResults() {
        areaCodeComponent.$("div[role='option']")
                .should(matchText("^.+$"), ofSeconds(60));
        return areaCodeComponent.$$("div[role='option']");
    }

    /**
     * Return the text of the full name of the current Area Code Selector.
     * <br/>
     * Useful for checks of full selected area code text
     * (e.g. "United States, Alaska, St George (907)")
     *
     * @return the current Area Code Selector text element
     */
    public SelenideElement getSelectedAreaCodeFullName() {
        return areaCodeComponent.$(selectedAreaCodeFullName);
    }

    /**
     * Return section element of the current Area Code Selector.
     * <p></p>
     * Useful for checks of validation errors texts.
     *
     * @return the current Area Code Selector section element
     */
    public SelenideElement getDefaultAreaCodeSection() {
        return $(defaultAreaCodeSection);
    }

    /**
     * Return input field element.
     *
     * @return input field of current Area Code Selector
     */
    public SelenideElement getInputElement() {
        return areaCodeComponent.$(input);
    }

    /**
     * Wait for Area Code selector to be visible and ready for input.
     * Useful for slow/unstable integrations.
     *
     * @return same invoked AreaCodeSelector object
     */
    public AreaCodeSelector waitUntilEnabled() {
        areaCodeComponent.shouldBe(visible, ofSeconds(60));
        getInputElement().shouldHave(or("Area Code placeholder text",
                        attribute("placeholder", START_TYPING_TO_SEARCH_MESSAGE),
                        attribute("placeholder", SEARCH_CODES_MESSAGE)),
                ofSeconds(90));

        return this;
    }

    /**
     * Select an option available for selection by provided text in the current Area Code Selector
     * (e.g. Country, State, City, etc.).
     *
     * @param option an option available for selection
     */
    public void selectOption(String option) {
        if (option != null && !option.isBlank()) {
            getInputElement().shouldBe(enabled, ofSeconds(60))
                    .click();
            areaCodeComponent.$(withText(option))
                    .shouldBe(visible, ofSeconds(10))
                    .hover()
                    .click();
        }
    }

    /**
     * Wait for the current Area Code Selector to be enabled, then select provided Area Code data in it.
     *
     * @param areaCode object that represents test data for client's Area Code
     */
    public void selectCode(AreaCode areaCode) {
        waitUntilEnabled();
        selectOption(areaCode.country);
        checkThatOptionSelected(areaCode.country);
        selectOption(areaCode.state);
        checkThatOptionSelected(areaCode.state);
        selectOption(areaCode.city);
        selectOption(areaCode.code);
        areaCode.fullName = getSelectedAreaCodeFullName().getText();
    }

    /**
     * Check that the Area Code selector is filled in, if so - clears it.
     */
    public void clear() {
        getInputElement().shouldBe(enabled, ofSeconds(30));
        areaCodeComponent.$(clearButton).click();
    }

    /**
     * Remove option with the provided text.
     * <p>
     * In Area Code Selector options are either Countries or States (e.g. Countries: "United States", "Canada";
     * States: "California" (for the US), "Ontario" (for Canada)).
     * <p>
     * <b>Important Note:</b>
     * If you remove the selected State, then the selected Country stays selected.
     * If you remove the selected Country with the selected State, then both are removed.
     * <p>
     * You can't remove the selected City (or Toll-Free code) by this method, because once it's selected the whole selector
     * loses the "input" part, and turns into the selected area code with its full name, although the "input" part stays
     * in the DOM after selection, you still can't really enter anything in the same way as you could before, for that case you can use
     * {@link AreaCodeSelector#clear()} method.
     *
     * @param selectedOption value that will be removed
     */
    public void removeSelectedOption(String selectedOption) {
        getInputElement().shouldBe(enabled, ofSeconds(30));
        areaCodeComponent.$(withText(selectedOption))
                .parent()
                .$(removeOptionButton)
                .click();
    }

    /**
     * Check that option with the provided text is selected in Area Code Selector input.
     * <p>
     * In Area Code Selector options are either Countries or States (e.g. Countries: "United States", "Canada";
     * States: "California" (for the US), "Ontario" (for Canada)).
     * <p>
     * <b>Important Note:</b>
     * You can't check the selected City (or Toll-Free code) by this method, because once it's selected the whole selector
     * loses the "input" part, and turns into the selected area code with its full name, although the "input" part stays
     * in the DOM after selection, you still can't really enter anything in the same way as you could before.
     *
     * @param selectedOption value that will be checked
     */
    public void checkThatOptionSelected(String selectedOption) {
        getInputElement().shouldBe(enabled, ofSeconds(30));
        areaCodeComponent.$(withText(selectedOption))
                .shouldBe(visible, ofSeconds(20));
    }
}