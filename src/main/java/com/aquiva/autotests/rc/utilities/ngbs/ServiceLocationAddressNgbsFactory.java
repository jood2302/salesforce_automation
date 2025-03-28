package com.aquiva.autotests.rc.utilities.ngbs;

import com.aquiva.autotests.rc.model.ngbs.dto.AddressDTO;
import com.aquiva.autotests.rc.model.ngbs.dto.account.ServiceLocationAddressDTO;

import static com.aquiva.autotests.rc.model.ngbs.dto.AddressDTO.*;

/**
 * Factory for generating instances of {@link ServiceLocationAddressDTO} objects.
 */
public class ServiceLocationAddressNgbsFactory {

    /**
     * Create a service location address "object" with default values.
     *
     * @return service location address object to pass on in NGBS REST API request methods.
     */
    public static ServiceLocationAddressDTO createServiceLocationWithDefaultAddress() {
        var serviceLocation = new ServiceLocationAddressDTO();
        var address = new AddressDTO();

        address.street1 = DEFAULT_STREET;
        address.zip = DEFAULT_ZIP;
        address.country = DEFAULT_COUNTRY;
        address.state = DEFAULT_STATE;
        address.city = DEFAULT_CITY;

        serviceLocation.address = address;

        return serviceLocation;
    }
}
