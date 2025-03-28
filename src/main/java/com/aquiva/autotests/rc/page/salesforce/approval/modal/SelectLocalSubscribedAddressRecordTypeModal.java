package com.aquiva.autotests.rc.page.salesforce.approval.modal;

import com.aquiva.autotests.rc.page.salesforce.approval.KycApprovalPage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.LocalSubscribedAddress__c;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

/**
 * Modal window in {@link KycApprovalPage} activated by clicking on 'New' button
 * from Local Subscribed Addresses related list.
 *
 * <p> Used for selecting a type of {@link LocalSubscribedAddress__c} record to be created. </p>
 */
public class SelectLocalSubscribedAddressRecordTypeModal {
    private final SelenideElement visiblePageContainer = $(".windowViewMode-normal");

    private final SelenideElement dialogContainer = visiblePageContainer
            .$x(".//c-lsa-creation//div[@class='slds-modal__container']");

    private final SelenideElement recordTypesContainer = dialogContainer.$x(".//lightning-radio-group");
    public final ElementsCollection recordTypesList = recordTypesContainer.$$x(".//span[@class='slds-form-element__label']");
    public final SelenideElement submitButton = dialogContainer.$(byText("Submit"));

    /**
     * Select type of Local Subscribed Address record to be created.
     *
     * @param localSubscribedAddressType name of the Local Subscribed Address record type
     *                                   (e.g. 'Local Subscribed Address of Company')
     */
    public void selectLocalSubscribedAddressType(String localSubscribedAddressType) {
        recordTypesContainer.$(byText(localSubscribedAddressType)).click();
    }
}
