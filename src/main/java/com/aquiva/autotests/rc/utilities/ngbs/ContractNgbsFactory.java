package com.aquiva.autotests.rc.utilities.ngbs;

import com.aquiva.autotests.rc.model.ngbs.dto.contracts.ContractNgbsDTO;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.sforce.soap.enterprise.sobject.Contract__c;

import java.time.Clock;

/**
 * Factory for generating instances of {@link ContractNgbsDTO} objects.
 */
public class ContractNgbsFactory {

    /**
     * Create a contract "object" for one selected license with default values.
     * Such object could be used later in NGBS REST request
     * for creating contract on existing account.
     * <br/>
     * Note: a contract will be created for the current package on the NGBS account.
     *
     * @param contractExtId special ID for a contract to map it to the custom SFDC contract object
     *                      (e.g. "Office", "Autotest_Contract_42").
     *                      See {@link Contract__c#getExtID__c()}.
     *                      <br/><br/>
     * @param contractItem  test data for the account's contracted item
     *                      (e.g. "DigitalLine Unlimited Standard").
     *                      Note: it should contain contract's quantity
     *                      and product's data name in NGBS (e.g. "LC_DL-UNL_50")!
     * @return contract object to pass on in NGBS REST API request methods.
     */
    public static ContractNgbsDTO createContractForOneLicense(String contractExtId, Product contractItem) {
        var contract = new ContractNgbsDTO();

        contract.startDate = Clock.systemUTC().instant().toString();
        contract.term = 24;
        contract.renewalTerm = 24;
        contract.autoRenewal = true;
        contract.description = "{\"contractExtId\":\"" + contractExtId + "\"}";

        var license = new ContractNgbsDTO.License();
        license.catalogId = contractItem.dataName;
        license.contractualQty = contractItem.existingQuantity != null ?
                contractItem.existingQuantity :
                contractItem.quantity;

        contract.licenses = new ContractNgbsDTO.License[]{license};

        return contract;
    }
}
