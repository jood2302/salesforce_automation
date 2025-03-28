package com.aquiva.autotests.rc.page.salesforce.approval;

import com.codeborne.selenide.SelenideElement;

/**
 * A single step in the {@link ApprovalHistoryRelatedListPage}.
 * <br/>
 * Contains the fields like Name, Assigned To, Current Approver, Status.
 */
public class ApprovalHistoryStep {
    private final SelenideElement stepWebElement;

    /**
     * Constructor for the Approval History step.
     *
     * @param stepWebElement the web element that defines the step's location in the DOM
     */
    public ApprovalHistoryStep(SelenideElement stepWebElement) {
        this.stepWebElement = stepWebElement;
    }

    /**
     * Get the web element of the whole step.
     */
    public SelenideElement getSelf() {
        return stepWebElement;
    }

    /**
     * Get the web element of the name of the step
     * (e.g. "Approval Request Submitted", "Final Approval").
     */
    public SelenideElement getStepName() {
        return stepWebElement.$x("./*[2]");
    }

    /**
     * Get the web element of the status of the step
     * (e.g. "Pending", "Approved").
     */
    public SelenideElement getStatus() {
        return stepWebElement.$x("./*[4]");
    }

    /**
     * Get the web element of the entity that the step is assigned to.
     * Can be a specific user, or a group/queue.
     */
    public SelenideElement getAssignedTo() {
        return stepWebElement.$x("./*[5]");
    }
}
