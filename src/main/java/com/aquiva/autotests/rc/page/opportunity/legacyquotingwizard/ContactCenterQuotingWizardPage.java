package com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard;

import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.modal.EngageContactCenterProServModal;

/**
 * Contact Center Legacy Quoting Wizard page.
 * <p>
 * Can be found in Legacy Quoting Wizard by clicking on 'Contact Center' tab.
 * Used for creating Contact Center quotes.
 * <p/>
 */
public class ContactCenterQuotingWizardPage extends BaseLegacyQuotingWizardPage {
    public static final String CONTACT_CENTER_NOT_AVAILABLE_MESSAGE =
            "CONTACT CENTER IS NOT AVAILABLE FOR THE PACKAGE SELECTED ON YOUR PRIMARY QUOTE";
    public static final String CONTACT_CENTER_QUOTE_CANNOT_BE_CREATED_MESSAGE =
            "CONTACT CENTER QUOTE CANNOT BE CREATED WHEN PRIMARY QUOTE IN AGREEMENT STAGE";

    //  Modal window
    public final EngageContactCenterProServModal engageCcProServDialog = new EngageContactCenterProServModal();
}
