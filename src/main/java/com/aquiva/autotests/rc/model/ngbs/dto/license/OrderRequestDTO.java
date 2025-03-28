package com.aquiva.autotests.rc.model.ngbs.dto.license;

import com.aquiva.autotests.rc.model.DataModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object for the requests to change licenses for the NGBS account.
 * (add, remove, update, etc.)
 * To be used with NGBS API services.
 */
@JsonInclude(value = NON_NULL)
public class OrderRequestDTO extends DataModel {
    public List<CatalogItem> itemsToAdd;
    public List<RemovalRequestItem> itemsToRemove;

    /**
     * Default no-arg constructor.
     * It's needed for data serialization with Jackson data mapper.
     */
    public OrderRequestDTO() {
        itemsToAdd = new ArrayList<>();
        itemsToRemove = new ArrayList<>();
    }

    /**
     * Create new request to add licenses to the NGBS account's package.
     * Used with NGBS API services.
     *
     * @param itemsToAdd list of licenses to order for the account's package
     * @return new request to add licenses to the account's package
     */
    public static OrderRequestDTO createOrderRequestToAddLicenses(List<CatalogItem> itemsToAdd) {
        var orderRequest = new OrderRequestDTO();
        orderRequest.itemsToAdd.addAll(itemsToAdd);
        return orderRequest;
    }

    /**
     * Create new request to remove licenses from the NGBS account's package.
     * Used with NGBS API services.
     *
     * @param itemsToRemove list of licenses to remove from the account's package
     * @return new request to remove licenses from the account's package
     */
    public static OrderRequestDTO createOrderRequestToRemoveLicenses(List<RemovalRequestItem> itemsToRemove) {
        var orderRequest = new OrderRequestDTO();
        orderRequest.itemsToRemove.addAll(itemsToRemove);
        return orderRequest;
    }
}
