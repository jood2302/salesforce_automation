package com.aquiva.autotests.rc.model.testcaseverification;

import com.aquiva.autotests.rc.model.DataModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object with test case information for usage with TestIt API services.
 * The test case's data includes a name, priority, execution type, folder structure for test case location, etc...
 */
@JsonInclude(value = NON_NULL)
public class TestCaseDTO extends DataModel {
    //  For 'priority' field
    public static final int P0_PRIORITY = 0;
    public static final int P1_PRIORITY = 1;
    //  For 'executionType' field
    public static final int AUTOMATED_EXECUTION_TYPE = 1;

    public String name;
    public long externalId;
    public String prefix;
    public int priority;
    public int executionType;
    public Ascendant[] ascendants;
    public String message;

    /**
     * Inner data structure that stores information about folders
     * where the test case is stored.
     */
    public static class Ascendant extends DataModel {
        //  For 'name' field
        public static final String JUNK_FOLDER_NAME = "JUNK";

        public String name;
    }

    /**
     * Generate and return test case ID in TMS link format(e.g. CRM-22459, CRM-19703, etc.).
     *
     * @return test case ID in TMS link format(e.g. CRM-22459, CRM-19703, etc.)
     */
    public String getTmsLinkId() {
        return String.format("%s-%d", prefix, externalId);
    }

}
