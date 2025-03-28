package com.aquiva.autotests.rc.page.components.packageselector;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.packagetab.PackagePage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import java.util.List;

import static com.aquiva.autotests.rc.utilities.StringHelper.NONE;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$x;
import static java.time.Duration.ofSeconds;

/**
 * Component that is used for Package selection.
 * Can be found on {@link PackagePage}.
 * <br/>
 * User selects packages (e.g. <b>"RingCentral MVP Standard", "RingCentral Meetings Free"</b>, etc...)
 * using filtration in {@link PackageFilter}
 * and creates a new quote with it using this component
 * (or selects a different package for an active quote).
 */
public class PackageSelector {

    /**
     * Text for "badge" on the selected package for opportunities for Existing Business accounts.
     */
    public static final String PACKAGE_FROM_ACCOUNT_BADGE = "Account Package";
    /**
     * Text value of expired package badges
     */
    public static final String EXPIRED_PACKAGE_BADGE_TEXT = "Expired";

    //  Package Filter section
    public final PackageFilter packageFilter = new PackageFilter();

    public final SelenideElement self = $x("//package-select");
    public final ElementsCollection packageFolders = self.$$("package-group > div");
    public final ElementsCollection allPackages = self.$$("package-cpq > li");
    public final SelenideElement selectedPackage = self.$(".selected[data-ui-auto='package-item']");
    public final ElementsCollection selectedPackages = self.$$(".selected[data-ui-auto='package-item']");
    public final ElementsCollection packagesBadges = self.$$("span.slds-badge");
    public final ElementsCollection packagesPriceValues = self.$$(".price > span:nth-child(1)");
    public final ElementsCollection packagesPriceCurrencies = self.$$(".price > span:nth-child(2)");
    public final ElementsCollection packagesNames = self.$$(".package-info > .name");
    public final ElementsCollection packagesVersions = self.$$(".package-info > .version");

    public final SelenideElement packageSpinner = self.$(".loading");
    public final SelenideElement loadingBar = self.$("span.placeholder-loading");
    public final SelenideElement loadingMessage = self.$(byText("loading..."));

    /**
     * Get a Package Folder object by its name.
     * <p>
     * Package folders need to be expanded to get access to its packages.
     * Usually package name corresponds to opportunity's <i>'Tier Name'</i>.
     *
     * @param name name of the folder (e.g. "Office", "Fax", "Meetings", etc...).
     * @return PackageFolder Object that has been found by the name
     */
    public PackageFolder getPackageFolderByName(String name) {
        var packageFolderElement = self.$(byText(name)).closest("package-group/div");
        return new PackageFolder(packageFolderElement);
    }

    /**
     * Get a currently selected package.
     *
     * @return Package Object that is selected
     */
    public Package getSelectedPackage() {
        return new Package(selectedPackage);
    }

    /**
     * Get a list of all selected packages on the Account.
     *
     * @return list of all selected packages on the Account
     */
    public List<Package> getAllSelectedPackages() {
        return selectedPackages.asDynamicIterable().stream()
                .map(Package::new)
                .toList();
    }

    /**
     * Wait until the component loads most of its important elements (package list).
     * User may safely interact with any of the component's elements after this method is finished.
     */
    public void waitUntilLoaded() {
        self.shouldBe(visible, ofSeconds(60));
        loadingMessage.shouldBe(hidden, ofSeconds(20));
        loadingBar.shouldBe(hidden, ofSeconds(10));
        packageSpinner.shouldBe(hidden, ofSeconds(20));
    }

    /**
     * Set 'Contract' checkbox to be selected or not.
     * <br/>
     * This method has to be used instead of {@link SelenideElement#setSelected(boolean)}
     * because {@code input} element for the 'Contract' is rendered invisible using CSS,
     * and {@link SelenideElement#setSelected(boolean)} only works with visible elements.
     *
     * @param isContractToBeSelected true, if the checkbox should be selected,
     *                               otherwise, it should be deselected.
     */
    public void setContractSelected(boolean isContractToBeSelected) {
        packageFilter.contractSelect.setSelected(isContractToBeSelected);
    }

    /**
     * Select a package from the list of available packages
     * using package field values from test data.
     * <p></p>
     * <b> Note: method populates 'Contract' checkbox as a value from test data
     * or skips it if a value from test data is blank. </b>
     *
     * @param chargeTerm        package's charge term (e.g. <b>"Monthly", "Annual"</b>)
     * @param packageFolderName folder name for package (e.g. <b>"Office", "Meetings"</b>...)
     * @param testDataPackage   package test data with package's id, version, type (optional), and contract name
     */
    public void selectPackage(String chargeTerm, String packageFolderName,
                              com.aquiva.autotests.rc.model.ngbs.testdata.Package testDataPackage) {
        var packageFolder = filterPackagesAndExpandFolder(chargeTerm, testDataPackage.contract, packageFolderName);
        packageFolder.getPackageByDataAttribute(testDataPackage.id, testDataPackage.version, testDataPackage.getPackageType())
                .selectPackage();

        //  The number of seats (licenses) on Engage packages should be set 3 or more before saving, see PBC-14141
        if (packageFolderName.contains("Engage")) {
            packageFolder.setNumberOfLicenses(3);
        }

        if (testDataPackage.proServOptions != null) {
            packageFolder.setProServOptions(testDataPackage.proServOptions);
        }
    }

    /**
     * Unselect a package using package field values from test data.
     *
     * @param packageFolderName folder name for package (e.g. <b>"Office", "Meetings"</b>...)
     * @param testDataPackage   package test data with package's id, version, type (optional)
     */
    public void unselectPackage(String packageFolderName,
                                com.aquiva.autotests.rc.model.ngbs.testdata.Package testDataPackage) {
        getPackageFolderByName(packageFolderName)
                .expandFolder()
                .getPackageByDataAttribute(testDataPackage.id, testDataPackage.version, testDataPackage.getPackageType())
                .unselectPackage();
    }

    /**
     * Select a package from the list of available packages without 'Number of Seats' field setting
     * (in case of Engage New/Existing Business Multi-Product Quote creation).
     *
     * @param chargeTerm        package's charge term (e.g. <b>"Monthly", "Annual"</b>)
     * @param packageFolderName folder name for package (e.g. <b>"Office", "Meetings"</b>...)
     * @param testDataPackage   package test data with package's id, version, type (optional), and contract name
     */
    public void selectPackageWithoutSeatsSetting(String chargeTerm, String packageFolderName,
                                                 com.aquiva.autotests.rc.model.ngbs.testdata.Package testDataPackage) {
        var packageFolder = filterPackagesAndExpandFolder(chargeTerm, testDataPackage.contract, packageFolderName);
        packageFolder.getPackageByDataAttribute(testDataPackage.id, testDataPackage.version, testDataPackage.getPackageType())
                .selectPackage();
    }

    /**
     * Filter the packages by charge term, contract value, and expand the package folder.
     *
     * @param chargeTerm        package's charge term (e.g. <b>"Monthly", "Annual"</b>)
     * @param packageContract   contract name for the package (e.g. <b>"Office Contract", "Meetings Contract"</b>)
     * @param packageFolderName folder name for package (e.g. <b>"Office", "Meetings"</b>...)
     * @return expanded package folder with the package to select
     */
    private PackageFolder filterPackagesAndExpandFolder(String chargeTerm, String packageContract, String packageFolderName) {
        if (!chargeTerm.isBlank()) {
            packageFilter.selectChargeTerm(chargeTerm);
        }

        var isContractToBeSelected = packageContract != null && !packageContract.isBlank() && !packageContract.equals(NONE);
        setContractSelected(isContractToBeSelected);

        var packageFolder = getPackageFolderByName(packageFolderName);
        packageFolder.getSelf().shouldBe(visible, ofSeconds(20));
        packageFolder.expandFolder();

        return packageFolder;
    }
}
