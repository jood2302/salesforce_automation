package com.aquiva.autotests.rc.model.ngbs.testdata;

import com.aquiva.autotests.rc.model.DataModel;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.shippingtab.*;

import static com.aquiva.autotests.rc.page.components.ShippingAddressForm.GROUND_SHIPPING_OPTION;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;

/**
 * Object for storing test data for the Shipping Group's address:
 * country, state, city, etc.
 *
 * @see ShippingGroup
 */
public class ShippingGroupAddress extends DataModel {
    public String country;
    public String city;
    public State state;
    public String addressLine;
    public String additionalAddressLine;
    public String zipCode;
    public String shipAttentionTo;
    public String shippingMethod;

    /**
     * Constructor to create a new data object with the Shipping Group's address.
     *
     * @param country         country name (e.g. "United States")
     * @param city            city name (e.g. "Los Angeles")
     * @param state           state name (e.g. "California")
     * @param addressLine     address line (e.g. "Apt.123 Mulholland Dr")
     * @param zipCode         zip code (e.g. "11001")
     * @param shipAttentionTo shipping attention (e.g. "John Smith")
     */
    public ShippingGroupAddress(String country, String city, State state, String addressLine, String zipCode,
                                String shipAttentionTo) {
        this(country, city, state, addressLine, EMPTY_STRING, zipCode, shipAttentionTo, GROUND_SHIPPING_OPTION);
    }

    /**
     * Constructor to create a new data object with the Shipping Group's address.
     *
     * @param country               country name (e.g. "United States")
     * @param city                  city name (e.g. "Los Angeles")
     * @param state                 state name (e.g. "California")
     * @param addressLine           address line (e.g. "Apt.123 Mulholland Dr")
     * @param additionalAddressLine additional address line (e.q. "Suite 500 or Building A, Floor 3")
     * @param zipCode               zip code (e.g. "11001")
     * @param shipAttentionTo       shipping attention (e.g. "John Smith")
     */
    public ShippingGroupAddress(String country, String city, State state, String addressLine, String additionalAddressLine,
                                String zipCode, String shipAttentionTo, String shippingMethod) {
        this.country = country;
        this.city = city;
        this.state = state;
        this.addressLine = addressLine;
        this.additionalAddressLine = additionalAddressLine;
        this.zipCode = zipCode;
        this.shipAttentionTo = shipAttentionTo;
        this.shippingMethod = shippingMethod;
    }

    /**
     * Return the address formatted for the header of the Shipping Group on the {@link ShippingPage}
     * (e.g. " App.129 13 Elm Street, Foster City, California 94404, United States").
     */
    public String getAddressFormatted() {
        return this.additionalAddressLine.isBlank() ?
                String.format("%s, %s, %s %s, %s",
                this.addressLine, this.city, this.state.value, this.zipCode, this.country)
                : String.format("%s, %s, %s, %s %s, %s",
                this.addressLine, this.additionalAddressLine, this.city, this.state.value, this.zipCode, this.country);
    }

    /**
     * State for the Shipping Group's Address.
     * Corresponds to the data for 'State/County/Province' field.
     */
    public static class State {
        public String value;
        public Boolean isSelect;

        /**
         * Create a new data for the Shipping Address's State.
         *
         * @param value    any state's value (e.g. "Alabama", "Rheinland-Pfalz")
         * @param isSelect true, if the corresponding state's country has predefined states,
         *                 and they should be selected from the 'select' element;
         *                 false, if there are no predefined states for the country,
         *                 and the state is populated in the 'input' element.
         *                 <br/>
         *                 See {@link ShippingGroupDetailsModal#submitChanges(ShippingGroupAddress)}.
         */
        public State(String value, Boolean isSelect) {
            this.value = value;
            this.isSelect = isSelect;
        }
    }
}
