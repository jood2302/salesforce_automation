package com.aquiva.autotests.rc.page.components.packageselector;

import com.aquiva.autotests.rc.model.ngbs.testdata.ProServOptions;
import com.aquiva.autotests.rc.page.components.ProServOptionsSection;
import com.codeborne.selenide.*;
import org.openqa.selenium.By;

import java.util.List;

import static com.aquiva.autotests.rc.page.components.packageselector.Package.PACKAGE_INFO_ATTRIBUTE;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.byCssSelector;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;

/**
 * Represents an expandable folder with a name ("Office", "Fax", "Meetings", etc...),
 * and with packages (see {@link Package}) inside.
 * Can be found in the {@link PackageSelector}.
 * <br/><br/>
 * Note: packages inside the folder don't exist and are not visible in the DOM
 * if the folder is not expanded. Make sure that the folder is expanded first
 * before calling any UI actions/assertions with package elements inside.
 */
public class PackageFolder {
    private final SelenideElement packageFolder;
    private final By expandButton = byCssSelector("c-button");
    private final By childPackages = byCssSelector("package-cpq > li");
    private final By childPackagesFullNames = byCssSelector(".package-info");
    private final By numberOfLicensesInput = byCssSelector("[formcontrolname='newDLs']");

    public static final WebElementCondition beExpanded = have(cssClass("slds-is-open"));

    /**
     * Constructor with a web element as a parameter.
     *
     * @param packageFolder SelenideElement that is used to locate the package folder element in the DOM
     */
    public PackageFolder(SelenideElement packageFolder) {
        this.packageFolder = packageFolder;
    }

    /**
     * Return the actual web element behind the Package component.
     * <p></p>
     * Useful if the test needs to perform actions on the web element itself
     * via Selenide framework actions (waits, assertions, etc...).
     *
     * @return SelenideElement that represents a Package in the DOM
     */
    public SelenideElement getSelf() {
        return packageFolder;
    }

    /**
     * Return the button that expands/collapses the current Package folder.
     *
     * @return expand button of the current Package folder.
     */
    public SelenideElement getExpandButton() {
        return packageFolder.$(expandButton);
    }

    /**
     * Return the 'Number of Licenses'/'Number of Seats' input field of the current Package folder.
     *
     * @return 'Number of Licenses'/'Number of Seats' input field of the current Package folder.
     */
    public SelenideElement getNumberOfLicensesInput() {
        return packageFolder.$(numberOfLicensesInput);
    }

    /**
     * Return the packages in the current package folder
     * as a collection of web elements.
     * <br/>
     * Note: make sure that the folder is expanded
     * before calling UI-actions/assertions for these elements!
     *
     * @return child packages as a collection of web elements
     */
    public ElementsCollection getPackagesElements() {
        return packageFolder.$$(childPackages);
    }

    /**
     * Return the packages' full names in the current package folder
     * as a collection of web elements.
     * E.g. "RingCentral MVP Standard\nVersion 1".
     * <br/>
     * Note: make sure that the folder is expanded
     * before calling UI-actions/assertions for these elements!
     *
     * @return child packages' full names as a collection of web elements
     */
    public ElementsCollection getPackageFullNameElements() {
        return packageFolder.$$(childPackagesFullNames);
    }

    /**
     * Get a child package in the current package folder
     * using its display name.
     * <br/>
     * Note: make sure that the folder is expanded
     * before calling UI-actions/assertions for this package!
     *
     * @param packageName full name of the package to find (e.g. "RingCentral MVP Standard - v.1")
     * @return child package with a given full name
     */
    public Package getChildPackageByName(String packageName) {
        var packageElement = getPackageFullNameElements()
                .findBy(exactTextCaseSensitive(packageName))
                .ancestor("package-cpq/li");
        return new Package(packageElement);
    }

    /**
     * Get a child package in the current package folder
     * using its 'data-ui-auto-package-item' attribute value
     * (e.g. <b>"Regular_1127001_1"</b> for "RingCentral MVP Standard Version - 1")
     * <br/>
     * Note: make sure that the folder is expanded
     * before calling UI-actions/assertions for this package!
     *
     * @param packageId      id of the package that will be selected
     * @param packageVersion version of the package that will be selected
     * @param packageType    type of the package that will be selected
     *                       (e.g. 'Regular', 'POC', 'Trial')
     * @return package that will be selected
     */
    public Package getPackageByDataAttribute(String packageId, String packageVersion, String packageType) {
        var packageInfoAttributeValue = String.join("_", packageType, packageId, packageVersion);
        var packageElement = packageFolder.$(format("[%s='%s']",
                PACKAGE_INFO_ATTRIBUTE, packageInfoAttributeValue));

        return new Package(packageElement);
    }

    /**
     * Get a full list of packages in a current package folder.
     *
     * @return list of packages in a current package folder
     */
    public List<Package> getAllPackages() {
        return getPackagesElements().asDynamicIterable()
                .stream()
                .map(Package::new)
                .collect(toList());
    }

    /**
     * Get the section with ProServ options selector
     * (for the 'Professional Services' package folder).
     */
    public ProServOptionsSection getProServOptionsSection() {
        return new ProServOptionsSection();
    }

    /**
     * Expand the current package folder.
     *
     * @return current package folder
     */
    public PackageFolder expandFolder() {
        if (!isExpanded()) {
            getExpandButton().scrollIntoView("{block: \"center\"}").click();
            getPackagesElements().shouldHave(sizeGreaterThan(0), ofSeconds(10));
        }
        return this;
    }

    /**
     * Set the new value in the 'Number of Licenses'/'Number of Seats' input field
     * for the current package folder.
     *
     * @param numberOfLicenses any valid integer number of licenses/seats (e.g. 30)
     */
    public void setNumberOfLicenses(Integer numberOfLicenses) {
        // workaround to avoid resetting a new value to default value = '1'
        getNumberOfLicensesInput().clear();
        getNumberOfLicensesInput()
                .setValue(valueOf(numberOfLicenses))
                .unfocus();
    }

    /**
     * Set the options specific for the 'Professional Services' packages folder.
     *
     * @param proServOptions different options for the selected ProServ package
     *                       (e.g. set "UC" checkbox).
     */
    public void setProServOptions(ProServOptions proServOptions) {
        var selectorSection = getProServOptionsSection();
        selectorSection.ucCaseCheckbox.setSelected(proServOptions.isUcService);
        selectorSection.ccCaseCheckbox.setSelected(proServOptions.isCcService);
    }

    /**
     * Check if the current package folder is expanded or not.
     *
     * @return true if folder is expanded, false if not.
     */
    private boolean isExpanded() {
        packageFolder.shouldHave(attribute("class"));
        return packageFolder.getAttribute("class").contains("slds-is-open");
    }
}
