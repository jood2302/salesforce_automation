package com.aquiva.autotests.rc.utilities.ngbs;

import com.aquiva.autotests.rc.model.ngbs.dto.AddressDTO;
import com.aquiva.autotests.rc.model.ngbs.dto.partner.PartnerNgbsDTO;

import java.util.UUID;

import static com.aquiva.autotests.rc.model.ngbs.dto.AddressDTO.*;
import static com.aquiva.autotests.rc.model.ngbs.dto.partner.PartnerNgbsDTO.*;
import static com.aquiva.autotests.rc.model.ngbs.dto.partner.PartnerNgbsDTO.SettlementSettingsDTO.MARKUP_SETTLEMENT_TYPE;
import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomEmail;
import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomUSPhone;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.getPartnersInNGBS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Factory for generating instances of {@link PartnerNgbsDTO} objects.
 */
public class PartnerNgbsFactory {
    //  Default values for partner's parameters
    private static final String FIRSTNAME_PREFIX = "FirstName ";
    private static final String LASTNAME_PREFIX = "LastName ";
    private static final String COMPANY_NAME_PREFIX = "TestCompanyName ";

    /**
     * Create a 'Bill on Behalf' partner "object" with default values.
     * Such object could be used later in NGBS REST request for creating partner in NGBS.
     *
     * @return partner object to pass on in NGBS REST API request methods.
     */
    public static PartnerNgbsDTO createBillOnBehalfPartner() {
        return createPartner(BILL_ON_BEHALF_PARTNER_TYPE);
    }

    /**
     * Create a 'Wholesale' partner "object" with default values.
     * Such object could be used later in NGBS REST request for creating partner in NGBS.
     *
     * @return partner object to pass on in NGBS REST API request methods.
     */
    public static PartnerNgbsDTO createWholesalePartner() {
        var partner = createPartner(WHOLESALE_PARTNER_TYPE);

        partner.taxExemption = false;

        var settlementSettings = new PartnerNgbsDTO.SettlementSettingsDTO();
        settlementSettings.settlementType = MARKUP_SETTLEMENT_TYPE;
        settlementSettings.settlementPartnerIdSelf = true;
        settlementSettings.settlementRatePartnerIdSelf = true;
        partner.settlementSettings = settlementSettings;

        return partner;
    }

    /**
     * Create a partner "object" with default values.
     *
     * @param partnerType type of partner (e.g. "BillOnBehalf")
     * @return partner object to pass on in NGBS REST API request methods.
     */
    private static PartnerNgbsDTO createPartner(String partnerType) {
        var partner = new PartnerNgbsDTO();

        var address = new AddressDTO();
        address.label = ACCOUNTS_PAYABLE_LABEL;
        address.firstName = FIRSTNAME_PREFIX + UUID.randomUUID();
        address.lastName = LASTNAME_PREFIX + UUID.randomUUID();
        address.country = DEFAULT_COUNTRY;
        address.state = DEFAULT_STATE;
        address.city = DEFAULT_CITY;
        address.street1 = DEFAULT_STREET;
        address.zip = DEFAULT_ZIP;
        address.phone = getRandomUSPhone();
        address.email = getRandomEmail();
        partner.addresses = new AddressDTO[]{address};

        var invoiceSettings = new PartnerNgbsDTO.InvoiceSettingsDTO();
        invoiceSettings.invoicingPartnerIdSelf = true;
        partner.invoiceSettings = invoiceSettings;

        partner.hierarchyType = DEFAULT_HIERARCHY_TYPE;
        partner.partnerType = partnerType;
        partner.status = ACTIVE_STATUS;
        partner.firstName = FIRSTNAME_PREFIX + UUID.randomUUID();
        partner.lastName = LASTNAME_PREFIX + UUID.randomUUID();
        partner.companyName = COMPANY_NAME_PREFIX + UUID.randomUUID();
        partner.externalPartnerAccountId = UUID.randomUUID().toString();
        partner.businessIdentityId = RC_BUSINESS_IDENTITY_ID;
        partner.parentId = getParentPartnerId(partnerType);
        partner.requiresBillingFeed = false;

        return partner;
    }

    /**
     * Get ID of parent partner with provided partner type.
     *
     * @param partnerType type of parent partner (e.g. "BillOnBehalf")
     * @return ID of parent partner (e.g. 11001)
     */
    private static Long getParentPartnerId(String partnerType) {
        var allPartners = getPartnersInNGBS();
        var parentPartner = allPartners.stream()
                .filter(partner -> partner.hierarchyType.equals(MASTER_HIERARCHY_TYPE) &&
                        partner.partnerType.equals(partnerType))
                .findFirst();
        assertThat(parentPartner)
                .as("Parent Partner with type = " + partnerType)
                .isPresent();

        return parentPartner.get().id;
    }
}
