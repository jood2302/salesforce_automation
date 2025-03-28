package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$x;

/**
 * Modal window in {@link NGBSQuotingWizardPage} ("Main Quote" tab)
 * activated by clicking on "Engage ProServ" button.
 * <p>
 * After engaging "professional services", a <b>ProServ Quote</b> is created
 * and can be accessed via Quote Wizard ("ProServ Quote" tab)
 * by user with profile = <b>"Professional Services"</b>.
 * </p>
 */
public class EngageProServModal {

    //  String constants used on the form
    public static final String INITIATE_PROFESSIONAL_SERVICES_HEADER = "Initiate Professional Services";
    public static final String LABEL = "Please select all of the following options that apply to your request:";
    public static final String PLEASE_SELECT_ALL_FOLLOWING_OPTIONS_MESSAGE = 
            "Please select all of the following options that apply to your request:";

    //  PDF Templates
    public static final String IMPLEMENTATION_SERVICES_QUOTE_TEMPLATE = "Implementation Services Quote / SOW";
    public static final String AFTERMARKET_PS_SUPPORT_TEMPLATE = "Aftermarket PS Support";
    public static final String MANAGED_RECURRING_SERVICES_ADVANCED_SUPPORT_TEMPLATE = "Managed Services, Recurring Services, Advanced Support";
    public static final String VIDEO_ROOMS_IN_A_BOX_TEMPLATE = "Video / Rooms in a Box";

    //  Page elements
    private final SelenideElement dialogContainer = $x("//div[contains(@class,'slds-modal__container')]");
    public final SelenideElement header = dialogContainer.$x(".//h2"); 
    public final SelenideElement proServOptionsLegend = dialogContainer.$x(".//fieldset/legend");
    public final ElementsCollection proServOptions = dialogContainer.$$x(".//*[@class='slds-checkbox']");
    public final ElementsCollection proServOptionCheckboxes = dialogContainer.$$x(".//*[@class='slds-checkbox']/input");

    //  Buttons
    public final SelenideElement closeButton = dialogContainer.$("[title='Close']");
    public final SelenideElement cancelButton = dialogContainer.$(byText("Cancel"));
    public final SelenideElement submitButton = dialogContainer.$(byText("Submit"));

    /**
     * Select a checkbox with a provided name of PDF template.
     *
     * @param pdfCheckboxName the name of the PDF template to select
     */
    public void selectPdfTemplate(String pdfCheckboxName) {
        var pdfTemplateCheckbox =
                dialogContainer.$x(".//*[@class='slds-checkbox'][.//span='" + pdfCheckboxName + "']");
        pdfTemplateCheckbox.$("label").click();
        pdfTemplateCheckbox.$("input").shouldBe(checked);
    }
}
