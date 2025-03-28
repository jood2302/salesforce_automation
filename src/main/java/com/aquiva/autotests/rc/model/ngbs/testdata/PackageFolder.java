package com.aquiva.autotests.rc.model.ngbs.testdata;

import com.aquiva.autotests.rc.model.DataModel;

/**
 * Data object that represents a folder with a Package info.
 * It is used to contain a test data for actual test actions
 * (changing the state of SUT; assertions; etc...)
 * <p></p>
 * Normally, this object consists of:
 * <p> - the name of the folder (e.g. "Office", "Meetings", "Fax"...) </p>
 * <p> - one or several packages with specific Package data
 * (e.g. package data for "RingCentral MVP Standard", with monthly charge term,
 * with contract, with specific set of products to add...) </p>
 * <p></p>
 * Note: you may define charge term on this level,
 * and not necessarily on the level of {@link Dataset}.
 */
public class PackageFolder extends DataModel {
    public String name;
    public String chargeTerm;

    public Package[] packages;
}
