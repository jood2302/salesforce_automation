package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.sforce.soap.enterprise.sobject.Employee__c;

/**
 * Helper class to facilitate operations on {@link Employee__c} objects.
 */
public class EmployeeHelper extends SObjectHelper {
    //  Default values for main employee's parameters
    private static final String DEFAULT_LOCATION = "Belmont, US";
    private static final String DEFAULT_DEPARTMENT = "Business Development";
    private static final String DEFAULT_DIVISION = "CV";
    private static final String DEFAULT_TEAM = "CSS";

    /**
     * Set default values to basic Employee__c fields that may be useful in tests.
     *
     * @param employee Employee__c instance to set up with default values
     */
    public static void setDefaultFields(Employee__c employee) {
        employee.setLocation__c(DEFAULT_LOCATION);
        employee.setDepartment__c(DEFAULT_DEPARTMENT);
        employee.setDivision__c(DEFAULT_DIVISION);
        employee.setTeam__c(DEFAULT_TEAM);
    }
}
