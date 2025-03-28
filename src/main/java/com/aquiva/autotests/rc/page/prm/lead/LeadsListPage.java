package com.aquiva.autotests.rc.page.prm.lead;

import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Lead;

import static com.codeborne.selenide.Selenide.$x;
import static java.lang.String.format;

/**
 * The page to see all the existing Leads records in the PRM Portal.
 * <br/>
 * User can open the existing records from the list.
 */
public class LeadsListPage {
    public static final String LEAD_ITEM_FORMAT = "//tr[.//a='%s']";

    public final SelenideElement searchBySelect = $x("//select[@name='searchBy']");
    public final SelenideElement searchKeywordInput = $x("//div[./*='Keyword']//input");
    public final SelenideElement searchButton = $x("//button[text()='Search']");

    /**
     * Search the lead among all the existing ones using its Company name.
     *
     * @param companyName Company name of the Lead
     * @see Lead#getName()
     */
    public void searchLeadByCompanyName(String companyName) {
        searchBySelect.selectOption("Name");
        searchKeywordInput.setValue(companyName);
        searchButton.click();
    }

    /**
     * Get lead item element from the Lead's list.
     *
     * @param companyName Company name of the Lead
     * @return SelenideElement that represents Lead item in the DOM.
     */
    public SelenideElement getLeadItemElement(String companyName) {
        return $x(format(LEAD_ITEM_FORMAT, companyName));
    }
}
