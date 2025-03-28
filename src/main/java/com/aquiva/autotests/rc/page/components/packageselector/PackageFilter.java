package com.aquiva.autotests.rc.page.components.packageselector;

import com.aquiva.autotests.rc.page.components.ContractSelector;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

/**
 * Component that is used for Package selection.
 * Can be found on {@link PackageSelector}.
 * <br/>
 * User filters packages using different fields (e.g. 'Service' picklist and 'Payment Plan').
 */
public class PackageFilter {

    //  For 'Service' picklist
    public static final String ALL_SERVICE_OPTION = "All";
    
    public final ContractSelector contractSelect = new ContractSelector();
    public final SelenideElement contractSelectInput = contractSelect.lwcCheckboxElement.getInput();
    public final SelenideElement numberOfLicensesInput = $("[data-ui-auto='number-of-licenses']");

    //  Picklists
    public final SelenideElement servicePicklist = $("#select-service-filter");

    /**
     * Return a charge term option for selection.
     * <p>
     * Useful in case of selecting and checking the state (checked/unchecked).
     *
     * @param chargeTerm name of the Charge Term (e.g. <b>"Monthly", "Annual"</b>)
     */
    public SelenideElement getChargeTermInput(String chargeTerm) {
        return $("input[formcontrolname='chargeTerm'][value='" + chargeTerm + "']");
    }

    /**
     * Select the charge term with a given name.
     *
     * @param chargeTerm expected option to be selected (e.g. <b>"Monthly", "Annual"</b>)
     */
    public void selectChargeTerm(String chargeTerm) {
        getChargeTermInput(chargeTerm).sibling(0).click();
    }
}
