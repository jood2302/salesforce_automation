package com.aquiva.autotests.rc.page.components;

import static com.codeborne.selenide.Selenide.$;

/**
 * The component that is used to select different options
 * for the packages of 'Professional Services' service.
 */
public class ProServOptionsSection {
    public final LightningCheckbox ucCaseCheckbox = new LightningCheckbox($("#uc-case-checkbox"));
    public final LightningCheckbox ccCaseCheckbox = new LightningCheckbox($("#cc-case-checkbox"));
}
