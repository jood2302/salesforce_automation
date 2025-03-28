package com.aquiva.autotests.rc.model.ngbs.testdata;

import com.aquiva.autotests.rc.model.DataModel;

/**
 * Data object that represents options for ProServ's package folder section.
 * <p> - isUcService: select (or deselect) UC checkbox </p>
 * <p> - isCcService: select (or deselect) CC checkbox </p>
 */
public class ProServOptions extends DataModel {
    public Boolean isUcService;
    public Boolean isCcService;

    //  Add other field(s) for "Select service types" multiselect
}
