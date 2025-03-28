package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.sforce.soap.enterprise.sobject.Contract__c;

import java.util.UUID;

/**
 * Helper class to facilitate operations on {@link Contract__c} objects.
 */
public class Contract__cHelper extends SObjectHelper {
    //  Default values for contract's parameters
    private static final String DEFAULT_CONTRACT_NAME = "Autotest Contract";

    //  For 'Available_in_packages__c' field
    public static final String MVP_PACKAGE_CODES = "5,18,19,20,21,22,23,24,25,26,27,28,29,30,1950,1952,1953,1955,1956,1957,1958";

    /**
     * Set default values to basic Contract__c fields that may be useful in tests.
     *
     * @param contract Contract__c instance to set up with default values
     */
    public static void setDefaultFields(Contract__c contract) {
        var contractUniqueName = DEFAULT_CONTRACT_NAME + " " + UUID.randomUUID();
        contract.setName(contractUniqueName);

        contract.setActive__c(true);
    }
}
