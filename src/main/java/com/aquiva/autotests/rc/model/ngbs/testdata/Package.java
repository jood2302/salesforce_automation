package com.aquiva.autotests.rc.model.ngbs.testdata;

import com.aquiva.autotests.rc.model.DataModel;
import com.aquiva.autotests.rc.page.components.packageselector.PackageSelector;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.lang.String.format;

/**
 * Data object that represents a specific package data.
 * It is used to contain a test data for actual test actions
 * (changing the state of SUT; assertions; etc...)
 * <p></p>
 * Normally, this object consists of:
 * <p> - name of the package (e.g. "RingCentral MVP Standard") </p>
 * <p> - package id and version (e.g. "18" and "1") </p>
 * <p> - contract name (e.g. "Office Contract", "None") </p>
 * <p> - products to add to the quote (e.g. DigitalLine, various phones...) </p>
 * <p> - products from NGBS (for Existing Business Customers) </p>
 * <p> - other products </p>
 */
@JsonInclude(value = NON_NULL)
public class Package extends DataModel {

    //  for 'type' variable
    public static final String REGULAR_TYPE = "Regular";

    public String name;
    /**
     * Catalog ID for the package (e.g. "2088005" for "RingEX Core™").
     */
    public String id;
    public String version;
    public String type;
    public String contract;
    public String contractExtId;
    public ContractTerms contractTerms;
    public ProServOptions proServOptions;
    /**
     * Unique ID for the Account's package in NGBS (for Existing Business Accounts).
     */
    public String ngbsPackageId;

    public Product[] products;
    public Product[] productsFromBilling;
    public Product[] productsDefault;
    public Product[] productsOther;
    public Product[] taxes;
    public Promotion[] promotions;

    /**
     * Get full name for the package.
     * Normally, it includes the display name and the version.
     * <p>
     * Useful when working with {@link PackageSelector}.
     * </p>
     *
     * @return package's full name (e.g. "RingCentral MVP Standard\nVersion - 1")
     */
    @JsonIgnore
    public String getFullName() {
        return format("%s\nVersion - %s", name, version);
    }

    /**
     * Get package edition by extracting it from the package name
     * (normally, the last word in it).
     *
     * @return package edition (e.g. "Standard", "Essentials", "Ultimate")
     */
    @JsonIgnore
    public String getEdition() {
        return name.substring(name.lastIndexOf(" ") + 1);
    }

    /**
     * Get type of the package.
     * <p>
     * In most cases it is equal to 'Regular',
     * otherwise is taken from 'type' field of test data file.
     * </p>
     *
     * @return package type value (e.g. 'Regular', 'POC', etc.)
     */
    public String getPackageType() {
        return type != null ? type : REGULAR_TYPE;
    }
}
