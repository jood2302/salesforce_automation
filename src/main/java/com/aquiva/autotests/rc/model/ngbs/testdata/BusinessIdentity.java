package com.aquiva.autotests.rc.model.ngbs.testdata;

import com.aquiva.autotests.rc.model.DataModel;

/**
 * Test data object that represents Business Identity information.
 * It is used to contain a test data for actual test actions
 * (changing the state of SUT; assertions; etc...)
 * <p></p>
 * Normally, this object consists of:
 * <p> - id (e.g. "4") </p>
 * <p> - name (e.g. "RingCentral Inc.") </p>
 */
public class BusinessIdentity extends DataModel {
    public String id;
    public String name;
}
