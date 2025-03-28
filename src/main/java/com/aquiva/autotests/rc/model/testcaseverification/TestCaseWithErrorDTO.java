package com.aquiva.autotests.rc.model.testcaseverification;

import com.aquiva.autotests.rc.model.DataModel;

/**
 * Data object with information about the test case that does not pass verification for automated test cases.
 */
public class TestCaseWithErrorDTO extends DataModel {
    public String id;
    public String name;
    public String error;

    public TestCaseWithErrorDTO(String id, String name, String error) {
        this.id = id;
        this.name = name;
        this.error = error;
    }
}
