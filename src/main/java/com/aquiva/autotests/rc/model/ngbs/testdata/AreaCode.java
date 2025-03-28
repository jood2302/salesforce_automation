package com.aquiva.autotests.rc.model.ngbs.testdata;

import com.aquiva.autotests.rc.model.DataModel;
import com.aquiva.autotests.rc.page.components.AreaCodeSelector;

/**
 * Data object that represents test data for client's Area Code.
 * It is used mainly for actions with {@link AreaCodeSelector} component.
 * <p></p>
 * Normally, Area codes are consist of:
 * <p> - type (e.g. "Local", "Toll-Free") </p>
 * <p> - country name (e.g. "United States", "Canada") </p>
 * <p> - state name (e.g. "California", "Nebraska") </p>
 * <p> - city name (e.g. "Los Angeles") </p>
 * <p> - code name (e.g. "888") </p>
 */
public class AreaCode extends DataModel {
    public final String type;
    public final String country;
    public final String state;
    public final String city;
    public final String code;

    public String fullName;

    /**
     * Main parameterized constructor for area code object.
     * <p></p>
     * Note: use empty string instead of the values
     * if they shouldn't be present in the area code
     * (e.g. there's no state for Paris area code).
     *
     * @param type    area code's type (e.g. "Local", "Toll-Free")
     * @param country country name (e.g. "United States", "Canada")
     * @param state   state name (e.g. "California", "Nebraska")
     * @param city    city name (e.g. "Los Angeles", "Beverly Hills")
     * @param code    code name (e.g. "888").
     */
    public AreaCode(String type, String country, String state, String city, String code) {
        this.type = type;
        this.country = country;
        this.state = state;
        this.city = city;
        this.code = code;
    }
}
