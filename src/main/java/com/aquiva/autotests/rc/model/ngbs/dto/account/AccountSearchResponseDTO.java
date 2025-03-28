package com.aquiva.autotests.rc.model.ngbs.dto.account;

import com.aquiva.autotests.rc.model.DataModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object with response information for the search results for Accounts via NGBS API services.
 */
@JsonInclude(value = NON_NULL)
public class AccountSearchResponseDTO extends DataModel {
    public AccountSummary[] result;
    public Integer total;
}
