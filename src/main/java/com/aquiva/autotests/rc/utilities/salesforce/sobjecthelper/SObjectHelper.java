package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;

/**
 * Common helper class that contains instance of Enterprise connection utility for usage in child classes.
 */
public abstract class SObjectHelper {
    /**
     * Enterprise connection utility to interact with SFDC API in helper classes.
     */
    protected static final EnterpriseConnectionUtils CONNECTION_UTILS = EnterpriseConnectionUtils.getInstance();
}
