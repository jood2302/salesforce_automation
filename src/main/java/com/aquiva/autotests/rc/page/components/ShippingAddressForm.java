package com.aquiva.autotests.rc.page.components;

import com.aquiva.autotests.rc.page.opportunity.OpportunityCreationPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selectors.byCssSelector;
import static com.codeborne.selenide.Selenide.$;

/**
 * Component that is used to fill in data about the Shipment Address.
 * Can be found on {@link OpportunityCreationPage} and {@link QuotePage}.
 */
public class ShippingAddressForm {
    //  For 'Shipping Option' picklist
    public static final String OVERNIGHT_SHIPPING_OPTION = "Overnight";
    public static final String GROUND_SHIPPING_OPTION = "Ground";

    private final SelenideElement shippingFormComponent;
    private final By customerName = byCssSelector("#customerName");
    private final By country = byCssSelector("#country");
    private final By city = byCssSelector("#city");
    private final By state = byCssSelector("#state");
    private final By addressLine = byCssSelector("#addressLine");
    private final By zipCode = byCssSelector("#zipCode");
    private final By additionalAddressLine = byCssSelector("#additionalAddressLine");
    private final By multipleShippingLocationsCheckbox = byCssSelector("#multipleLocations");
    private final By shipAttentionTo = byCssSelector("#shipAttentionTo");
    private final By shippingOptionPicklist = byCssSelector("#shippingOption");
    private final By applyButton = byCssSelector("[type='submit'] button");
    private final By cancelButton = byCssSelector("[type='button'] button");

    /**
     * Constructor for the component with initialization of
     * its parent element.
     */
    public ShippingAddressForm() {
        shippingFormComponent = $("shipping-form");
    }

    /**
     * Return web element for 'Customer Name' input field.
     *
     * @return 'Customer Name' input field
     */
    public SelenideElement getCustomerName() {
        return shippingFormComponent.$(customerName);
    }

    /**
     * Return web element for 'Country' input field.
     *
     * @return 'Country' input field
     */
    public SelenideElement getCountry() {
        return shippingFormComponent.$(country);
    }

    /**
     * Return web element for 'City' input field.
     *
     * @return 'City' input field
     */
    public SelenideElement getCity() {
        return shippingFormComponent.$(city);
    }

    /**
     * Return web element for 'State' input field.
     *
     * @return 'State' input field
     */
    public SelenideElement getState() {
        return shippingFormComponent.$(state);
    }

    /**
     * Return web element for 'Address Line' input field.
     *
     * @return 'Address Line' input field
     */
    public SelenideElement getAddressLine() {
        return shippingFormComponent.$(addressLine);
    }

    /**
     * Return web element for 'ZIP Code' input field.
     *
     * @return 'ZIP Code' input field
     */
    public SelenideElement getZipCode() {
        return shippingFormComponent.$(zipCode);
    }

    /**
     * Return web element for 'Additional Address Line' input field.
     *
     * @return 'Additional Address Line' input field
     */
    public SelenideElement getAdditionalAddressLine() {
        return shippingFormComponent.$(additionalAddressLine);
    }

    /**
     * Return web element for 'Multiple Shipping Locations' checkbox.
     *
     * @return 'Multiple Shipping Locations' checkbox
     */
    public SelenideElement getMultipleShippingLocations() {
        return shippingFormComponent.$(multipleShippingLocationsCheckbox);
    }

    /**
     * Return web element for 'Ship Attention To' input field.
     *
     * @return 'Ship Attention To' input field
     */
    public SelenideElement getShipAttentionTo() {
        return shippingFormComponent.$(shipAttentionTo);
    }

    /**
     * Return web element for 'Shipping Options' picklist.
     *
     * @return 'Shipping Options' picklist.
     */
    public SelenideElement getShippingOptionPicklist() {
        return shippingFormComponent.$(shippingOptionPicklist);
    }

    /**
     * Click 'Apply' button on the form.
     * It saves all the entered values and closes the form.
     */
    public void applyShippingForm() {
        shippingFormComponent.$(applyButton).click();
    }

    /**
     * Click 'Cancel' button on the form.
     * It discards all the changes and closes the form.
     */
    public void cancelShippingForm() {
        shippingFormComponent.$(cancelButton).click();
    }
}
