package com.aquiva.autotests.rc.page.salesforce.psorder;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import java.util.List;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_URL;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;
import static java.time.Duration.ofSeconds;

/**
 * Page Object for the ProServ Suborder page in Salesforce.
 * <p>
 * This page is located inside ProServ Order record page
 * and contains Suborder for each ProServ Order phase.
 */
public class ProServSuborderPage {
    private final ElementsCollection allSuborders = $$("pro-serv-phase");

    private final SelenideElement subordersSectionName = $("#phase-section-name");

    /**
     * Get a list of all Suborders from the page.
     *
     * @return list of {@link SuborderItem} objects
     */
    public List<SuborderItem> getAllSuborders() {
        return allSuborders.stream().map(SuborderItem::new).toList();
    }

    /**
     * Open the ProServ Suborder page by the given ProServ Order ID.
     *
     * @param proServOrderId ID of the ProServ Order to open the Suborder page for
     */
    public void openPage(String proServOrderId) {
        open(BASE_URL + "/apex/ProServSuborder?id=" + proServOrderId);
        subordersSectionName.shouldBe(visible, ofSeconds(90));
    }
}
