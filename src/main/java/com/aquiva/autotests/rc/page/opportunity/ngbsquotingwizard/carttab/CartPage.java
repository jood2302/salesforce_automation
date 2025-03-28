package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NgbsQuotingWizardFooter;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal.*;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import java.util.List;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.*;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;

/**
 * 'Price' tab in {@link NGBSQuotingWizardPage}
 * that contains a list of Cart Items ({@link CartItem}) with their details (price, discount, quantity);
 * some useful Totals fields; additional buttons for adding taxes or applying promo discounts.
 */
public class CartPage extends NGBSQuotingWizardPage {

    //  Error messages for validations
    public static final String QUANTITY_CANNOT_BE_LOWER_THAN_ERROR = "Quantity cannot be lower than 1";
    public static final String QUANTITY_CANNOT_BE_HIGHER_THAN_ERROR = "Quantity cannot be higher than ";

    //  Text from the Price tab hints
    public static final String PROVISION_TOGGLE_HINT_TEXT = "If Provision toggle is ON then Phone Number area codes will be required. " +
            "If the toggle is OFF Phone Number will not be assigned during signup. " +
            "Provision toggle will be set to OFF automatically if total DL count is 100 or more and " +
            "for all Agent Supported countries.";
    public static final String PROVISIONING_AND_SHIPPING_WILL_BE_DISABLED_MESSAGE =
            "Provisioning and Shipping will be disabled when Professional Services is initiated";
    
    //  Tooltips
    public static final String QUOTE_HAS_DOWNSELL_OR_CONTRACT_EXIT_TOOLTIP = 
            "The quote has downsell or contract exit. PDF cannot be generated until the quote is approved.";

    //  Approval Statuses
    public static final String NOT_REQUIRED_APPROVAL_STATUS = "Not Required";
    public static final String REQUIRED_APPROVAL_STATUS = "Required";
    public static final String APPROVED_APPROVAL_STATUS = "Approved";
    public static final String PENDING_APPROVAL_STATUS = "Pending Approval";
    public static final String PENDING_L1_APPROVAL_STATUS = "Pending L1 Approval";

    //  Approvers
    public static final String IN_CONTRACT_DOWNSELL_APPROVER = "In Contract Downsell";

    //  Approver labels
    public static final String REQUIRED_APPROVER_APPROVER_LABEL = "Required Approver";
    public static final String NEXT_APPROVAL_APPROVER_LABEL = "Next Approval";
    public static final String LAST_APPROVER_APPROVER_LABEL = "Last Approver";

    //  Discretion value
    public static final String ZERO_DISCRETION_VALUE = "0%";

    //  CSS classes for 'dqApproverButton'
    public static final String RED_DQ_APPROVER_BUTTON_CSS_CLASS = "dq-approver-button_destructive";

    //  DQ Approver values on 'DQ Approver' button
    public static final String FINANCE_REVENUE_DQ_APPROVER = "Finance - Revenue";
    public static final String NO_APPROVAL_REQUIRED_DQ_APPROVER = "No Approval Required";

    public final SelenideElement loadingMessage = $("cart").$(byText("loading..."));
    public final SelenideElement notificationBar = $("[data-auto-ui='notification-bar']");
    public final ElementsCollection notifications = $$("[data-auto-ui='notification-text']");
    public final SelenideElement placeholderLoading = $("placeholder-loading");

    //  Cart items
    public final ElementsCollection visibleCartItems = $$("[data-ui-auto-license-is-visible='true']");
    public final ElementsCollection allCartItemElements = $$("[data-ui-auto='cart-item']");
    public final ElementsCollection cartItemNames = $$("[data-ui-auto='cart-item-product-name']");
    public final ElementsCollection taxCartItems = $$x("//*[@data-ui-auto='cart-item'][.//div[text()='Tax']]");
    public final SelenideElement discretionValue = $(".discretion");

    //  Header buttons
    public final SelenideElement saveButton = $("[data-ui-auto='save-cart']");
    public final SelenideElement moreActionsButton = $("[data-ui-auto='price-tab-more-actions']");
    public final SelenideElement addTaxesButton = $("[data-ui-auto='add-taxes']");
    public final SelenideElement removeTaxesButton = $("[data-ui-auto='remove-taxes']");
    public final SelenideElement addPromosButton = $("[data-ui-auto='add-promos']");
    public final SelenideElement submitForApprovalButton = $("[data-ui-auto='submit-for-approval']");
    public final SelenideElement approveRejectButton = $("[data-ui-auto='approve-reject']");
    public final SelenideElement recallApprovalRequestButton = $("[data-ui-auto='recall-approval-request']");
    public final SelenideElement copyQuoteButton = $("[data-ui-auto='copy-quote']");
    public final SelenideElement generatePdfButton = $("[data-ui-auto='generate-pdf']");

    public final SelenideElement approvalStatus = $("[data-ui-auto='quote-approval-status'], #approval-status > span");
    public final SelenideElement approverLabel = $("[data-ui-auto*='approval-status'] button");
    public final SelenideElement selectedApprover = $("#selected-approver");
    public final SelenideElement dqApprovalStatus = $("[data-ui-auto='dq-approval-status']");
    public final SelenideElement dqApproverButton = $("dq-approver-button > button");

    //  Modals
    public final PromotionsManagerModal promosModal = new PromotionsManagerModal();
    public final ReviseDealQualificationModal reviseDealQualificationModal = new ReviseDealQualificationModal();
    public final ViewDqApproversModal viewDqApproversModal = new ViewDqApproversModal();
    public final TargetPriceDetailsModal targetPriceModal = new TargetPriceDetailsModal();
    public final ApproveRejectModal approveRejectModal = new ApproveRejectModal();

    //  Footer
    public final NgbsQuotingWizardFooter footer = new NgbsQuotingWizardFooter();

    /**
     * Get Quote Line Item on the Price tab by its name.
     * <br/>
     * Note: method will return the first <i>visible</i> cart item with a given display name.
     *
     * @param licenseDisplayName displayed name of the item
     * @return composite object to extract other parameters from (quantity, price, discount...)
     */
    public CartItem getQliFromCartByDisplayName(String licenseDisplayName) {
        var cartItemSelector = format(
                "[data-ui-auto-license-display-name='%s'][data-ui-auto-license-is-visible='true']",
                licenseDisplayName);
        return new CartItem($(cartItemSelector));
    }

    /**
     * Get Quote Line Item on the Price tab by its value of 'data-id' attribute.
     * <br/>
     * Note: method will return the first <i>visible</i> cart item with a given 'data-id' attribute value.
     *
     * @param licenseDataId value of 'data-id' attribute of the item
     * @return composite object to extract other parameters from (quantity, price, discount...)
     */
    public CartItem getQliFromCartByDataId(String licenseDataId) {
        var cartItemSelector = format("[data-id='%s'][data-ui-auto-license-is-visible='true']",
                licenseDataId);
        return new CartItem($(cartItemSelector));
    }

    /**
     * Get all items that are visible in the cart as collection of {@link CartItem} elements.
     * <br/>
     * Some Quote Line Items might be on the quote, but not visible in the cart (they're not on the list).
     *
     * @return list of Cart Item objects for further work with its elements
     */
    public List<CartItem> getAllVisibleCartItems() {
        return visibleCartItems.asDynamicIterable().stream().map(CartItem::new).collect(toList());
    }

    /**
     * Get all Tax items in the cart as a collection of {@link CartItem} elements.
     * <br/>
     * These items appear in the cart when user presses 'Add Taxes' button on Price tab.
     * Visually, Tax items have an additional text tag = 'Tax'.
     *
     * @return list of Cart Item objects for further work with its elements
     */
    public List<CartItem> getAllTaxCartItems() {
        return taxCartItems.asDynamicIterable().stream().map(CartItem::new).collect(toList());
    }

    /**
     * Open Price tab by clicking on the tab's button.
     *
     * @return reference to the opened Price tab
     */
    public CartPage openTab() {
        cartTabButton.click();
        waitUntilLoaded();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void waitUntilLoaded() {
        progressBar.shouldBe(hidden, ofSeconds(PROGRESS_BAR_TIMEOUT_AFTER_SAVE));
        placeholderLoading.shouldBe(hidden, ofSeconds(60));

        //  Wait for cart items to rearrange in DOM
        loadingMessage.shouldBe(hidden, ofSeconds(90));
        visibleCartItems.shouldHave(sizeGreaterThan(0), ofSeconds(30));

        errorNotification.shouldBe(hidden);
    }

    /**
     * Set value for 'Quantity' field for Quote Line Item on Price tab.
     *
     * @param qliName  name for Quote Line Item as it displays in the Cart
     * @param quantity quantity value to set in 'Quantity' field
     */
    public void setQuantityForQLItem(String qliName, int quantity) {
        getQliFromCartByDisplayName(qliName)
                .getQuantityInput()
                .shouldBe(enabled, ofSeconds(60))
                .setValue(Integer.toString(quantity))
                .unfocus();
    }

    /**
     * Set value for 'New Quantity' field for Quote Line Item on Price tab.
     *
     * @param qliName     name for Quote Line Item as it displays in the Cart
     * @param newQuantity quantity value to set in 'New Quantity' field
     */
    public void setNewQuantityForQLItem(String qliName, int newQuantity) {
        getQliFromCartByDisplayName(qliName)
                .getNewQuantityInput()
                .shouldBe(enabled, ofSeconds(60))
                .setValue(Integer.toString(newQuantity))
                .unfocus();
    }

    /**
     * Set value for 'Discount' field for Quote Line Item on Price tab.
     *
     * @param qliName  name for Quote Line Item as it displays in the Cart
     * @param discount discount string value to set in 'Discount' field
     */
    public void setDiscountForQLItem(String qliName, int discount) {
        getQliFromCartByDisplayName(qliName)
                .getDiscountInput()
                .shouldBe(enabled, ofSeconds(60))
                .setValue(Integer.toString(discount))
                .unfocus();
    }

    /**
     * Set value for 'Discount Type' field for Quote Line Item on Price tab.
     *
     * @param qliName      name for Quote Line Item as it displays in the Cart
     * @param discountType discount type string value to select in 'Discount Type' field's pick-list
     */
    public void setDiscountTypeForQLItem(String qliName, String discountType) {
        //  Sometimes without the click on the pick-list, the selected value won't change with the action below
        getQliFromCartByDisplayName(qliName)
                .getDiscountTypeSelect()
                .shouldBe(enabled, ofSeconds(60))
                .scrollIntoView("{block: \"center\"}")
                .click();

        getQliFromCartByDisplayName(qliName)
                .getDiscountTypeSelect()
                .selectOptionContainingText(discountType);
    }

    /**
     * Press 'Save Changes' button on the Price Tab of the Quote Wizard.
     */
    public void saveChanges() {
        saveButton.click();

        progressBar.shouldBe(visible, ofSeconds(10));
        waitUntilLoaded();
    }

    /**
     * Click on one of the Service Groups on the Price tab.
     * This action will either expand the section showing all the products in it,
     * or collapse it hiding all the products in it.
     *
     * @param serviceName name of a service (e.g. 'Office', 'Engage Voice Standalone', etc.)
     */
    public void clickCartGroup(String serviceName) {
        $$(".cart-group-button")
                .findBy(text(serviceName))
                .scrollIntoView("{block: \"center\"}")
                .click();
    }

    /**
     * Add taxes to the Cart.
     * <br/>
     * Added taxes appear as additional products (Quote Line Items) in the cart.
     */
    public void addTaxes() {
        addTaxesButton.click();

        waitUntilLoaded();
    }

    /**
     * Apply promotion using its promo code to the products in the Cart.
     * <br/>
     * Note: use it when no promotion is applied yet.
     *
     * @param promoCode Promo Code to be applied (e.g. "QA-AUTO-DL-USD", "NYPROMO2").
     * @see #changeAppliedPromo(String)
     */
    public void applyPromoCode(String promoCode) {
        clickAddPromoButton();
        promosModal.clickApplyPromoButton(promoCode);
        promosModal.submitButton.click();
    }

    /**
     * Remove applied promo and apply specified promo.
     * <br/>
     * Note: use it when there's already a promotion applied.
     *
     * @param promoCode Promo Code to be applied (e.g. "QA-AUTO-DL-USD", "NYPROMO2").
     * @see #applyPromoCode(String)
     */
    public void changeAppliedPromo(String promoCode) {
        clickAddPromoButton();
        promosModal.removeButton.shouldBe(visible, ofSeconds(10)).click();
        promosModal.clickApplyPromoButton(promoCode);
        promosModal.submitButton.click();
    }

    /**
     * Click 'More Actions' - 'Add Promos' button on the Price tab.
     */
    public void clickAddPromoButton() {
        moreActionsButton.hover();
        addPromosButton.click();
    }

    /**
     * Click 'More Actions' - 'Copy Quote' button on the Price tab.
     */
    public void clickCopyQuoteButton() {
        moreActionsButton.hover();
        copyQuoteButton.click();

        progressBar.shouldBe(visible, ofSeconds(10));
        waitUntilLoaded();
    }

    /**
     * Click 'Submit for Approval' button on the Price tab.
     * <br/>
     * Note: only works for situations of the initial submission
     * (i.e. after the click there are no additional modal windows).
     */
    public void submitForApproval() {
        submitForApprovalButton.click();

        progressBar.shouldBe(visible, ofSeconds(10));
        waitUntilLoaded();
    }

    /**
     * Click 'Approve/Reject' button, provide an approval comment in the modal window, and press 'Approve'.
     */
    public void approveQuoteViaApproveRejectModal() {
        approveRejectButton.click();
        approveRejectModal.commentInput.setValue("Approved by CRM QA AutoTest from the UQT");
        approveRejectModal.approveButton.click();

        progressBar.shouldBe(visible, ofSeconds(10));
        waitUntilLoaded();
    }
}
