package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.shippingtab;

import com.aquiva.autotests.rc.model.ngbs.testdata.ShippingGroupAddress;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.sleep;
import static java.time.Duration.ofSeconds;

/**
 * 'Shipping Group Details' modal window.
 * <br/>
 * Used to create new Shipping Groups on the {@link ShippingPage} or edit the existing ones.
 * Contains address fields, like city, country, state, etc.
 */
public class ShippingGroupDetailsModal {
    private final SelenideElement shippingGroupDetailsContainer = $("shipping-location-edit-modal");

    public final SelenideElement countrySelect = shippingGroupDetailsContainer.$("#country");
    public final SelenideElement cityInput = shippingGroupDetailsContainer.$("#city");
    public final SelenideElement stateSelectOrInput = shippingGroupDetailsContainer.$("#state");
    public final SelenideElement addressLineInput = shippingGroupDetailsContainer.$("#addressLine");
    public final SelenideElement additionalAddressLineInput = shippingGroupDetailsContainer.$("#additionalAddressLine");
    public final SelenideElement zipCodeInput = shippingGroupDetailsContainer.$("#zipCode");
    public final SelenideElement shipAttentionToInput = shippingGroupDetailsContainer.$("#attentionTo");
    public final SelenideElement shippingMethod = shippingGroupDetailsContainer.$("#shippingMethod");

    public final SelenideElement applyButton = shippingGroupDetailsContainer.$(byText("Apply"));

    /**
     * Populate all the required fields on the modal,
     * and press 'Apply' button.
     */
    public void submitChanges(ShippingGroupAddress shippingGroupAddress) {
        //  without this pause sometimes the form resets all the entered values
        sleep(2_000);

        countrySelect.selectOption(shippingGroupAddress.country);
        cityInput.setValue(shippingGroupAddress.city);

        //  'State/County/Province' field is 'select' if the Country has predefined States, otherwise -- 'input' field
        if (shippingGroupAddress.state.isSelect) {
            stateSelectOrInput.selectOption(shippingGroupAddress.state.value);
        } else {
            stateSelectOrInput.setValue(shippingGroupAddress.state.value);
        }

        addressLineInput.setValue(shippingGroupAddress.addressLine);
        additionalAddressLineInput.setValue(shippingGroupAddress.additionalAddressLine);
        zipCodeInput.setValue(shippingGroupAddress.zipCode);
        shipAttentionToInput.setValue(shippingGroupAddress.shipAttentionTo);
        shippingMethod.selectOption(shippingGroupAddress.shippingMethod);
        applyButton.click();

        shippingGroupDetailsContainer.shouldBe(hidden, ofSeconds(60));
    }
}
