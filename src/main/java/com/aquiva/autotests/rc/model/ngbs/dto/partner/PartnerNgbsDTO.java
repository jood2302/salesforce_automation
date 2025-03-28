package com.aquiva.autotests.rc.model.ngbs.dto.partner;

import com.aquiva.autotests.rc.model.DataModel;
import com.aquiva.autotests.rc.model.ngbs.dto.AddressDTO;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object with partner information for usage with NGBS API services.
 */
@JsonInclude(value = NON_NULL)
public class PartnerNgbsDTO extends DataModel {
    //  For 'hierarchyType' field
    public static final String DEFAULT_HIERARCHY_TYPE = "Reseller";
    public static final String MASTER_HIERARCHY_TYPE = "Master";
    //  For 'partnerType' field
    public static final String BILL_ON_BEHALF_PARTNER_TYPE = "BillOnBehalf";
    public static final String WHOLESALE_PARTNER_TYPE = "Wholesale";
    //  For 'status' field
    public static final String ACTIVE_STATUS = "Active";
    //  For 'businessIdentityId' field
    public static final int RC_BUSINESS_IDENTITY_ID = 4;

    public Long id;
    public String hierarchyType;
    public String partnerType;
    public String status;
    public String firstName;
    public String lastName;
    public String companyName;
    public AddressDTO[] addresses;
    public InvoiceSettingsDTO invoiceSettings;
    public String externalPartnerAccountId;
    public Integer businessIdentityId;
    public Long parentId;
    public Boolean requiresBillingFeed;
    public Boolean taxExemption;
    public SettlementSettingsDTO settlementSettings;

    /**
     * Inner data structure that stores information about Invoice Settings
     * on partner for usage with NGBS API services.
     */
    @JsonInclude(value = NON_NULL)
    public static class InvoiceSettingsDTO extends DataModel {
        public Boolean invoicingPartnerIdSelf;
    }

    /**
     * Inner data structure that stores information about Settlement Settings
     * on partner for usage with NGBS API services.
     */
    @JsonInclude(value = NON_NULL)
    public static class SettlementSettingsDTO extends DataModel {
        //  For 'settlementType'
        public static final String MARKUP_SETTLEMENT_TYPE = "MarkUp";

        public String settlementType;
        public Long settlementPartnerId;
        public Long settlementRatePartnerId;
        public Boolean settlementPartnerIdSelf;
        public Boolean settlementRatePartnerIdSelf;
    }
}
