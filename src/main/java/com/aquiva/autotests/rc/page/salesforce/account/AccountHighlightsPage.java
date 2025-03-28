package com.aquiva.autotests.rc.page.salesforce.account;

import com.aquiva.autotests.rc.page.salesforce.IframePage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$x;

/**
 * Account Highlights page on the {@link AccountRecordPage}.
 * <br/>
 * It is used for displaying different summarized info/warning messages about the account,
 * depending on the business context, account's fields, etc.
 * <br/>
 * See {@code AccountPop.page} VF page in SFDC.
 */
public class AccountHighlightsPage extends IframePage {

    public static final String CRITICAL_ACCOUNT_BANNER_MESSAGE =
            "Critical account, do not touch, contact the Deal Management team for assistance with opportunities.";

    public final SelenideElement miscHighlightsWarningMessage =
            $x("//form[contains(@id,'accountFom')]//tbody//table//td[2]");

    /**
     * Constructor for the Account Highlights page's iframe
     * on the Account record page.
     */
    public AccountHighlightsPage() {
        super($x("//article[.//*[@title='Account Highlights']]//iframe"));
    }
}
