package base;

import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import leads.LeadConvertSteps;
import ngbs.SalesFlowSteps;
import ngbs.approvals.KycApprovalSteps;
import ngbs.opportunitycreation.OpportunityCreationSteps;
import ngbs.quotingwizard.*;
import ngbs.quotingwizard.billonbehalf.BillOnBehalfSteps;
import ngbs.quotingwizard.engage.EngageSteps;
import ngbs.quotingwizard.sync.SyncWithNgbsSteps;

/**
 * Collection of some of the most common "test steps" classes.
 */
public class Steps {
    public NgbsSteps ngbs;

    public SfdcSteps sfdc;
    public SalesFlowSteps salesFlow;
    public LeadConvertSteps leadConvert;
    public OpportunityCreationSteps opportunityCreation;

    public QuoteWizardSteps quoteWizard;
    public CartTabSteps cartTab;
    public LboSteps lbo;
    public ProServSteps proServ;

    public EngageSteps engage;
    public SyncWithNgbsSteps syncWithNgbs;

    public KycApprovalSteps kycApproval;
    public BillOnBehalfSteps billOnBehalf;

    /**
     * Create a new collection for the most common "test steps" classes
     * (e.g. Quote Wizard, Lead Convert, etc.)
     *
     * @param data data object usually parsed from the JSON files with the test data for tests
     */
    public Steps(Dataset data) {
        this.ngbs = new NgbsSteps(data);

        this.sfdc = new SfdcSteps();
        this.salesFlow = new SalesFlowSteps(data);
        this.leadConvert = new LeadConvertSteps(data);
        this.opportunityCreation = new OpportunityCreationSteps();

        this.quoteWizard = new QuoteWizardSteps(data);
        this.cartTab = new CartTabSteps(data);
        this.lbo = new LboSteps();
        this.proServ = new ProServSteps();

        this.engage = new EngageSteps();
        this.syncWithNgbs = new SyncWithNgbsSteps(data);

        this.kycApproval = new KycApprovalSteps();
        this.billOnBehalf = new BillOnBehalfSteps(data);
    }
}
