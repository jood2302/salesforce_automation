package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.psphasestab;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;
import static java.time.Duration.ofSeconds;

/**
 * PS Phases tab in {@link NGBSQuotingWizardPage}
 * that contains a list of Products which was(were) added to the Phases.
 */
public class PsPhasesPage extends NGBSQuotingWizardPage {

    public static final String PHASE_SECTION_NAME_FORMAT = "Phases (%d)";

    //  Header buttons
    private final SelenideElement saveChangesButton = $("#save-changes");

    //  'Cart Items' section
    public final SelenideElement cartItemsSection = $("pro-serv-product-list");
    public final ElementsCollection allCartItems = cartItemsSection.$$("tbody tr");

    //  'Phases' section
    public final SelenideElement phasesSectionName = $("#phase-section-name");
    public final ElementsCollection phases = $$("pro-serv-phase");
    public final SelenideElement addPhaseButton = $("#add-phase-button");

    public final PsPhaseAddProductModal addProductModal = new PsPhaseAddProductModal();

    /**
     * Get the Phase Item from the list via its index.
     *
     * @param index index of the Phase to return (0..N)
     */
    public PhaseItem getPhaseByIndex(int index) {
        return new PhaseItem(phases.get(index));
    }

    /**
     * Get the Phase Item from the list via its auto-number.
     *
     * @param phaseAutoNumber phase's auto-number (e.g. "001239")
     */
    public PhaseItem getPhaseByAutoNumber(String phaseAutoNumber) {
        return new PhaseItem($x("//pro-serv-phase[.//*=' " + phaseAutoNumber + " ']"));
    }

    /**
     * Get one of the ProServ Cart Items from the 'Cart Items' section.
     *
     * @param name product name for the ProServ Cart Item (e.g. 'Additional Data Collection Session')
     */
    public PsCartItem getPsCartItem(String name) {
        return new PsCartItem(cartItemsSection.$x(".//tr[.//*[@id='name'][text()='" + name + "']]"));
    }

    /**
     * Open PS Phases tab by clicking on the tab's button.
     *
     * @return reference to the opened PS Phases page
     */
    public PsPhasesPage openTab() {
        proServPhasesTabButton.click();
        waitUntilLoaded();
        return this;
    }

    /**
     * Press 'Save Changes' button on the PS Phases tab of the Quote Wizard.
     */
    public void saveChanges() {
        saveChangesButton.click();

        progressBar.shouldBe(visible, ofSeconds(10));
        waitUntilLoaded();
    }

    /**
     * Add a given product to the Phase.
     *
     * @param phaseListIndex  index of the Phase to add the product to (0..N)
     * @param productName     name of the product to add
     * @param productQuantity quantity of the product to add
     */
    public void addProductToPhase(int phaseListIndex, String productName, int productQuantity) {
        getPhaseByIndex(phaseListIndex).getAddProductButton().click();

        addProductModal.productNamePicklist.selectOption(productName);
        addProductModal.quantityInput.setValue(Integer.toString(productQuantity));
        addProductModal.applyButton.click();
    }
}
