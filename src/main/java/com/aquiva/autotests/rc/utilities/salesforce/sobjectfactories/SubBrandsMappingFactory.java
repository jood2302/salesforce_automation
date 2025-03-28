package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.sforce.soap.enterprise.sobject.Account;
import com.sforce.soap.enterprise.sobject.SubBrandsMapping__c;
import com.sforce.ws.ConnectionException;

import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomPositiveInteger;
import static java.lang.String.format;

/**
 * Factory class for creating quick instances of {@link SubBrandsMapping__c} class.
 * <br/>
 * All factory methods also insert created objects into the SF database.
 */
public class SubBrandsMappingFactory extends SObjectFactory {

    public static final String SUB_BRANDS_MAPPING_DEFAULT_NAME_PREFIX = "Test SubBrands Mapping";

    /**
     * Create a new {@link SubBrandsMapping__c} Custom Setting record
     * for related Partner Account.
     *
     * @param partnerAccount a related Partner Account record
     * @return a new SubBrands Mapping Custom Setting record
     * @throws ConnectionException in case of errors while accessing API.
     */
    public static SubBrandsMapping__c createNewSubBrandsMapping(Account partnerAccount) throws ConnectionException {
        var subBrandsMapping = new SubBrandsMapping__c();
        var randomInteger = getRandomPositiveInteger();
        subBrandsMapping.setName(format("%s %s", SUB_BRANDS_MAPPING_DEFAULT_NAME_PREFIX, randomInteger));
        subBrandsMapping.setCountry__c(partnerAccount.getBillingCountry());
        subBrandsMapping.setBrand__c(partnerAccount.getRC_Brand__c());
        subBrandsMapping.setPartnerID__c(partnerAccount.getPartner_ID__c());
        subBrandsMapping.setSub_Brand__c("Test SubBrand " + randomInteger);

        CONNECTION_UTILS.insertAndGetIds(subBrandsMapping);

        return subBrandsMapping;
    }
}
