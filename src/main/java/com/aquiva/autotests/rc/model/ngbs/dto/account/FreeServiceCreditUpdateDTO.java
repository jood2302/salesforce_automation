package com.aquiva.autotests.rc.model.ngbs.dto.account;

import com.aquiva.autotests.rc.model.DataModel;

/**
 * Data object to update information about Free Service Credit on account
 * for usage with NGBS API services.
 */
public class FreeServiceCreditUpdateDTO extends DataModel {
    public double newAmount;

    /**
     * Constructor for Free Service Credit data object with amount as parameter.
     *
     * @param newAmount amount of Free Service Credit that will be created
     */
    public FreeServiceCreditUpdateDTO(double newAmount) {
        this.newAmount = newAmount;
    }
}
