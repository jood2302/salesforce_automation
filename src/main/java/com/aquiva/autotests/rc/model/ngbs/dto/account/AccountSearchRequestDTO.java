package com.aquiva.autotests.rc.model.ngbs.dto.account;

import com.aquiva.autotests.rc.model.DataModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object with request information to search for Accounts via NGBS API services.
 */
@JsonInclude(value = NON_NULL)
public class AccountSearchRequestDTO extends DataModel {
    public AccountSearchQuery query;
    public Integer limit;

    /**
     * Create Account Search request with related Contact's Last Name.
     *
     * @param lastName related contact's last name (e.g. "Smith")
     * @return search request object to be used in NGBS API requests
     */
    public static AccountSearchRequestDTO byLastName(String lastName) {
        var searchRequest = new AccountSearchRequestDTO();
        var query = new AccountSearchRequestDTO.AccountSearchQuery();

        query.lastName = lastName;
        searchRequest.query = query;

        return searchRequest;
    }
    
    /**
     * Create Account Search request with related Contact's Last Name.
     *
     * @param billingId ID for the NGBS account (e.g. "235714001")
     * @return search request object to be used in NGBS API requests
     */
    public static AccountSearchRequestDTO byBillingId(String billingId) {
        var searchRequest = new AccountSearchRequestDTO();
        var query = new AccountSearchRequestDTO.AccountSearchQuery();

        query.accountid = billingId;
        searchRequest.query = query;

        return searchRequest;
    }

    /**
     * Search query object that contains various account's fields
     * that can be used to find an account(s).
     */
    @JsonInclude(value = NON_NULL)
    public static class AccountSearchQuery extends DataModel {
        /**
         * Billing ID (e.g. "82717013").
         */
        public String accountid;
        /**
         * Account's Last Name (e.g. "Smith").
         */
        public String lastName;
        /**
         * Service's name (e.g. "Office", "Fax", "Meetings", "Engage Voice Standalone"...).
         */
        public String productname;
        /**
         * Account's status (e.g. "Initial", "Active", "Disabled", "Suspended", "Deleted").
         */
        public String status;
        /**
         * RC User ID / Enterprise Account ID (e.g. "401387890061").
         */
        public String enterpriseid;
        /**
         * Account's Business Identity (e.g. "RingCentral Inc.", "RingCentral Canada", "Avaya Cloud Office EU"...).
         */
        public String businessidentity;
    }
}
