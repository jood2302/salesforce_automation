package com.aquiva.autotests.rc.model.accountgeneration;

import com.aquiva.autotests.rc.model.DataModel;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object that contains necessary fields to create Existing Business Contact Center / Engage Account in SFDC/NGBS.
 * Represents the info about Contact Center / Engage Account record: service name, etc...
 * <br/>
 * Used as part of the user's data input in Existing Business Account's Generation task.
 */
@JsonInclude(value = NON_NULL)
public class CreateMultiproductDataInSfdcDTO extends DataModel {
    //  For 'serviceName' field
    public static final String ENGAGE_VOICE_STANDALONE_SERVICE = "Engage Voice Standalone";
    public static final String ENGAGE_DIGITAL_STANDALONE_SERVICE = "Engage Digital Standalone";
    public static final String RC_CONTACT_CENTER_SERVICE = "RingCentral Contact Center";

    public String serviceName;
    public OpportunityData opportunityData;

    //  These variables only get a value after Engage/Contact Center account is created in NGBS to report on its data
    public String accountURL;
    public String billingId;
    public String rcUserId;

    /**
     * Inner data structure for data object to select package and products
     * in New Business Opportunity via Salesforce UI.
     */
    @JsonInclude(value = NON_NULL)
    public static class OpportunityData extends DataModel {
        public Package testPackage;
        public Product[] products;
    }
}
