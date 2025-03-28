package com.aquiva.autotests.rc.model.ngbs.dto.packages;

import com.aquiva.autotests.rc.model.DataModel;
import com.aquiva.autotests.rc.page.components.packageselector.PackageSelector;
import com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient;
import com.fasterxml.jackson.annotation.*;

import java.util.Optional;
import java.util.stream.Stream;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Arrays.stream;

/**
 * Data object with general package information for usage with NGBS API services.
 * <p></p>
 * Useful data structure for parsing responses from NGBS API package service
 * (see {@link NgbsRestApiClient} for a reference).
 */
@JsonInclude(value = NON_NULL)
public class PackageNgbsDTO extends DataModel {
    public String id;
    public String version;
    public String displayName;
    public String productName;
    public String currency;
    public String offerType;
    public Labels labels;
    public BusinessIdentity businessIdentity;
    public Cbox[] cboxes;
    public License[] licenses;

    /**
     * Get full name for the package.
     * Normally, it includes the display name and the version.
     * <p>
     * Useful when working with {@link PackageSelector}.
     * </p>
     *
     * @return package's full name (e.g. "RingCentral MVP Standard - v.1")
     */
    public String getFullName() {
        return displayName + " - v." + version;
    }

    /**
     * Get the brand name in the 'labels' object of the package info.
     *
     * @return main brand name for the package (e.g. "RingCentral", "Avaya Cloud Office", etc...)
     */
    public String getLabelsBrandName() {
        return labels.brand[0].brandName;
    }

    /**
     * Inner data structure for NGBS Package Info data object.
     * Represents data for package's labels that are used in sales flows
     * (package selection, signing up, etc...).
     */
    @JsonInclude(value = NON_NULL)
    public static class Labels extends DataModel {
        @JsonProperty("Brand")
        public Brand[] brand;

        /**
         * Inner data structure that stores Brand label values from NGBS Account's Package.
         */
        @JsonInclude(value = NON_NULL)
        public static class Brand extends DataModel {
            public String brandName;

            /**
             * Parameterized constructor for data mapper.
             *
             * @param brandName name of the brand to sell (e.g. "RingCentral")
             */
            public Brand(String brandName) {
                this.brandName = brandName;
            }
        }
    }

    /**
     * Inner data structure that represents Business Identity information
     * for the concrete Package in NGBS.
     * <br/>
     * Normally, this object consists of:
     * <p> - id (e.g. "4") </p>
     * <p> - name (e.g. "RingCentral Inc.") </p>
     */
    @JsonInclude(value = NON_NULL)
    public static class BusinessIdentity extends DataModel {
        public String id;
        public String name;
    }

    /**
     * Inner data structure that represents CBox information
     * for the concrete Package in NGBS.
     * <br/>
     * The difference between this one and {@link License}
     * is that Cbox is more like a group of licenses.
     * <br/>
     * Normally, this object consists of:
     * <p> - elementID (e.g. "CB_65") </p>
     * <p> - name (e.g. "Phones") </p>
     * <p> - licenses (inner licenses inside the cbox group), see {@link License} </p>
     */
    @JsonInclude(value = NON_NULL)
    public static class Cbox extends DataModel {
        public String elementID;
        public String name;
        public PackageNgbsDTO.License[] licenses;
    }

    /**
     * Inner data structure that represents License/Product information
     * for the concrete Package in NGBS.
     * <br/>
     * Normally, this object consists of:
     * <p> - elementID (e.g. "LC_DL-UNL_50", "LC_HD_564") </p>
     * <p> - displayName (e.g. "DigitalLine Unlimited Standard", "Yealink W60P Cordless Phone with 1 Handset") </p>
     */
    @JsonInclude(value = NON_NULL)
    public static class License extends DataModel {
        public String elementID;
        public String displayName;
        public String name;
        public Labels labels;

        /**
         * Inner data structure that contains data
         * about the license's group, subgroup, license type, GOA data
         */
        @JsonInclude(value = NON_NULL)
        public static class Labels extends DataModel {
            public String[] quotingGroup;
            public String[] quotingSubGroup;

            /**
             * Get the main quoting group.
             * <br/>
             * Normally, there's only one group, and this is what we get here.
             *
             * @return name of the quoting group (e.g. "Services")
             */
            @JsonIgnore
            public String getMainQuotingGroup() {
                return quotingGroup[0];
            }

            /**
             * Get the main quoting subgroup.
             * <br/>
             * Normally, there's only one subgroup, and this is what we get here.
             *
             * @return name of the quoting subgroup (e.g. "Purchase")
             */
            @JsonIgnore
            public String getMainQuotingSubGroup() {
                return quotingSubGroup[0];
            }
        }
    }

    /**
     * Get the data for the license from the package.
     * <br/>
     * The data is searched in "cboxes" and "licenses" collections in the current package.
     *
     * @param licenseID ID of the license in NGBS (e.g. "LC_DL-UNL_50" for the DigitalLine Unlimited)
     * @return data about the license from the current package (with id, name, group, subgroup, etc.)
     */
    @JsonIgnore
    public Optional<License> getLicenseDataByID(String licenseID) {
        var licenseStream = licenses != null
                ? stream(licenses)
                : Stream.<License>empty();
        var licenseCboxesStream = cboxes != null
                ? stream(cboxes).flatMap(cbox -> cbox.licenses != null ? stream(cbox.licenses) : Stream.empty())
                : Stream.<License>empty();

        return Stream.concat(licenseStream, licenseCboxesStream)
                .filter(license -> license.elementID.equals(licenseID))
                .findFirst();
    }
}
