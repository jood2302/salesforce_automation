package com.aquiva.autotests.rc.model.ngbs.dto.account;

import com.aquiva.autotests.rc.model.DataModel;
import com.aquiva.autotests.rc.model.ngbs.dto.AddressDTO;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object with service location address information for usage with NGBS API services.
 */
@JsonInclude(value = NON_NULL)
public class ServiceLocationAddressDTO extends DataModel {
    public Long id;
    public AddressDTO address;
}
