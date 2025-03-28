package com.aquiva.autotests.rc.page.components.packageselector;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.byCssSelector;
import static com.codeborne.selenide.Selectors.byXpath;
import static java.time.Duration.ofSeconds;

/**
 * A single package item in the {@link PackageSelector}:
 * represents one of the packages to select.
 * <br/>
 * A Package has a name (usually, package name + package version, like "RingCentral MVP Standard Version - 1")
 * and an info badge (with currency info, like "USD", or tags from NGBS like "Testing").
 */
public class Package {

    /**
     * Attribute whose value contains offerType, PackageID and Package version
     * in format: "offerType_packageId_packageVersion".
     */
    public static final String PACKAGE_INFO_ATTRIBUTE = "data-ui-auto-package-item";

    private final SelenideElement packageItem;
    private final By name = byCssSelector(".package-info");
    private final By badge = byCssSelector("span.slds-badge");
    private final By priceValue = byCssSelector(".price > span:nth-child(1)");
    private final By priceCurrency = byCssSelector(".price > span:nth-child(2)");
    private final By selectButton = byXpath(".//button[./*='Select']");
    private final By unselectButton = byXpath(".//button[./*='Unselect']");

    /**
     * Constructor with web element as a parameter.
     *
     * @param packageItem SelenideElement that used to locate package item element in DOM.
     */
    public Package(SelenideElement packageItem) {
        this.packageItem = packageItem;
    }

    /**
     * Return actual web element behind Package component.
     * <p></p>
     * Useful if test needs to perform actions on the web element itself
     * via Selenide framework actions (waits, assertions, etc...)
     *
     * @return web element that represents Package in the DOM.
     */
    public SelenideElement getSelf() {
        return packageItem;
    }

    /**
     * Return the name element of the current package.
     *
     * @return web element for the name of the current package.
     */
    public SelenideElement getName() {
        return packageItem.$(name);
    }

    /**
     * Return the badge element of the current package.
     * It displays as a badge on the right,
     * and can contain Currency, source of package
     * or other package info.
     *
     * @return web element for the badge of the current package.
     */
    public SelenideElement getBadge() {
        return packageItem.$(badge);
    }

    /**
     * Return the price's value of the current package.
     * Relates to the price for a single Digital Line in it.
     * (e.g. "30.99")
     *
     * @return web element for the price's value of the current package.
     */
    public SelenideElement getPriceValue() {
        return packageItem.$(priceValue);
    }

    /**
     * Return the price currency of the current package.
     * Relates to the price for a single Digital Line in it.
     * (e.g. "USD")
     *
     * @return web element for the price's currency of the current package.
     */
    public SelenideElement getPriceCurrency() {
        return packageItem.$(priceCurrency);
    }

    /**
     * Return 'Select' button of the package
     * that is going to be selected.
     *
     * @return web element for the 'Select' button of the chosen package.
     */
    public SelenideElement getSelectButton() {
        return packageItem.$(selectButton);
    }

    /**
     * Return 'Unselect' button of the package
     * that is going to be deselected.
     *
     * @return web element for the 'Unselect' button of the chosen package.
     */
    public SelenideElement getUnselectButton() {
        return packageItem.$(unselectButton);
    }

    /**
     * Select the current package.
     * <br/>
     * Note: 'Select'/'Unselect' button is NOT clicked if the package has already been selected
     * (e.g. preselected by default, or otherwise).
     *
     * @return selected/current package item
     */
    public Package selectPackage() {
        if (!isSelected()) {
            getSelectButton().shouldBe(enabled, ofSeconds(10)).click();
        }
        getUnselectButton().shouldBe(visible);
        return this;
    }

    /**
     * Unselect the current package.
     * <br/>
     * Note: 'Select'/'Unselect' button is NOT clicked if the package has already been unselected
     *
     * @return unselected/current package item
     */
    public Package unselectPackage() {
        if (isSelected()) {
            getUnselectButton().shouldBe(enabled, ofSeconds(10)).click();
        }
        getSelectButton().shouldBe(visible);
        return this;
    }

    /**
     * Determine whether the current package selected or not.
     *
     * @return true, if the current package is already selected
     */
    private boolean isSelected() {
        return getSelf().shouldHave(attribute("class"))
                .getAttribute("class")
                .contains("selected");
    }
}
