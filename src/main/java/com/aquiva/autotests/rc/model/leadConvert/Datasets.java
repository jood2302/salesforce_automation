package com.aquiva.autotests.rc.model.leadConvert;

import com.aquiva.autotests.rc.model.DataModel;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;

/**
 * Main test data object for Lead Convert-related tests (see test/java/leads/BaseLeadConvertTest).
 * <p></p>
 * Normally, test data gets loaded from JSON files via {@link JsonUtils} into the objects of this type.
 * After that, actual test interacts with this object.
 * <p></p>
 * This object only contains data sets with test data for different Lead Convert scenarios.
 */
public class Datasets extends DataModel {
    public String description;

    public Dataset[] dataSets;
}
