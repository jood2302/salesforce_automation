package com.aquiva.autotests.rc.model.ngbs.testdata;

import com.aquiva.autotests.rc.model.DataModel;

import java.math.BigDecimal;

/**
 * Data object that represents a price for a {@link Product}
 * in the given range.
 * <p> Note: only applicable to the selected products. </p>
 * <p></p>
 * For example:
 * <b>DL Unlimited</b> product's price differs for different quantities.
 * Bigger quantity - lesser price for a single unit.
 * <p> For quantity = 1-99, the price = 40.99; </p>
 * <p> For quantity = 99-999, the price = 35.99; </p>
 * <p> For quantity = 1000+, the price = 32.99 </p>
 * <p> etc... </p>
 */
public class PriceRater extends DataModel {
    public Integer minimumBorder;
    public Integer maximumBorder;
    public BigDecimal raterPrice;
}
