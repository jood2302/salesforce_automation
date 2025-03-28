package base;

import com.aquiva.autotests.rc.page.AccountViewerPage;
import com.aquiva.autotests.rc.page.LoginPage;
import com.aquiva.autotests.rc.page.lead.LeadCreationPage;
import com.aquiva.autotests.rc.page.lead.LeadRecordPage;
import com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage;
import com.aquiva.autotests.rc.page.opportunity.*;
import com.aquiva.autotests.rc.page.opportunity.closewizard.CloseWizardPage;
import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.ContactCenterQuotingWizardPage;
import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.ProServQuotingWizardPage;
import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.carttab.LegacyCartPage;
import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.phasetab.PhasePage;
import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.producttab.LegacyProductsPage;
import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.quotetab.LegacyQuotePage;
import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.serviceplanstab.ServicePlansPage;
import com.aquiva.autotests.rc.page.opportunity.modal.*;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.*;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.*;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.dealqualificationtab.DealQualificationPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.packagetab.PackagePage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.producttab.ProductsPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.psphasestab.PsPhasesPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.shippingtab.ShippingPage;
import com.aquiva.autotests.rc.page.prm.*;
import com.aquiva.autotests.rc.page.prm.lead.LeadsListPage;
import com.aquiva.autotests.rc.page.prm.lead.PortalLeadRecordPage;
import com.aquiva.autotests.rc.page.salesforce.LocalSubscribedAddressRecordPage;
import com.aquiva.autotests.rc.page.salesforce.SalesforcePage;
import com.aquiva.autotests.rc.page.salesforce.account.AccountHighlightsPage;
import com.aquiva.autotests.rc.page.salesforce.account.AccountRecordPage;
import com.aquiva.autotests.rc.page.salesforce.account.modal.ApprovalCreationForChannelOperationsModal;
import com.aquiva.autotests.rc.page.salesforce.account.modal.BobWholesalePartnerConfirmationModal;
import com.aquiva.autotests.rc.page.salesforce.approval.*;
import com.aquiva.autotests.rc.page.salesforce.approval.modal.NewLocalSubscribedAddressCreationModal;
import com.aquiva.autotests.rc.page.salesforce.approval.modal.SelectLocalSubscribedAddressRecordTypeModal;
import com.aquiva.autotests.rc.page.salesforce.cases.CaseRecordPage;
import com.aquiva.autotests.rc.page.salesforce.cases.modal.CreateCaseModal;
import com.aquiva.autotests.rc.page.salesforce.contact.ContactRecordPage;
import com.aquiva.autotests.rc.page.salesforce.psorder.ProServProjectRecordPage;
import com.aquiva.autotests.rc.page.salesforce.psorder.ProServSuborderPage;
import com.aquiva.autotests.rc.page.salesforce.setup.DeliverabilityPage;
import com.aquiva.autotests.rc.page.salesforce.setup.MyDomainPage;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.LocalSubscribedAddressHelper.LOCAL_SUBSCRIBED_ADDRESS_RECORD_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.LocalSubscribedAddressHelper.REGISTERED_ADDRESS_RECORD_TYPE;

/**
 * Collection of all the Page Objects used in the project.
 */
public class Pages {

    //  SFDC
    public static LoginPage loginPage = new LoginPage();
    public static SalesforcePage salesforcePage = new SalesforcePage();

    //  Lead
    public static LeadCreationPage leadCreationPage = new LeadCreationPage();
    public static LeadRecordPage leadRecordPage = new LeadRecordPage();
    public static LeadConvertPage leadConvertPage = new LeadConvertPage();

    //  Account
    public static AccountRecordPage accountRecordPage = new AccountRecordPage();
    public static AccountViewerPage accountViewer = accountRecordPage.accountViewerModal.accountViewer;
    public static AccountHighlightsPage accountHighlightsPage = accountRecordPage.accountHighlightsPage;

    //  Contact
    public static ContactRecordPage contactRecordPage = new ContactRecordPage();

    //  Opportunity
    public static OpportunityRecordPage opportunityPage = new OpportunityRecordPage();
    public static OpportunityCreationPage opportunityCreationPage = new OpportunityCreationPage();
    public static OpportunityRecordTypeSelectionModal opportunityRecordTypeSelectionModal = new OpportunityRecordTypeSelectionModal();
    public static ProcessOrderModal processOrderModal = opportunityPage.processOrderModal;
    public static CloseWizardPage closeWizardPage = new CloseWizardPage();

    //  Case
    public static CaseRecordPage casePage = new CaseRecordPage();
    public static CreateCaseModal createCaseModal = new CreateCaseModal();

    //  Quote Wizard container and Quote Selector (Landing Page) 
    public static WizardBodyPage wizardBodyPage = new WizardBodyPage();
    public static NgbsQuoteSelectionQuoteWizardPage quoteSelectionWizardPage = wizardBodyPage.mainQuoteSelectionWizardPage;

    //  Quote Wizard tabs and components
    public static NGBSQuotingWizardPage wizardPage = wizardBodyPage.mainQuoteWizardPage;
    public static DealQualificationPage dealQualificationPage = new DealQualificationPage();
    public static PackagePage packagePage = new PackagePage();
    public static ProductsPage productsPage = new ProductsPage();
    public static CartPage cartPage = new CartPage();
    public static ShippingPage shippingPage = new ShippingPage();
    public static PsPhasesPage psPhasesPage = new PsPhasesPage();
    public static QuotePage quotePage = new QuotePage();
    public static DeviceAssignmentPage deviceAssignmentPage = new DeviceAssignmentPage();
    public static AreaCodePage areaCodePage = new AreaCodePage();
    public static NgbsQuotingWizardFooter quotingWizardFooter = new NgbsQuotingWizardFooter();

    //  Legacy Wizard tabs
    public static ContactCenterQuotingWizardPage contactCenterWizardPage = wizardBodyPage.contactCenterWizardPage;
    public static ProServQuotingWizardPage proServWizardPage = wizardBodyPage.proServWizardPage;
    public static ServicePlansPage servicePlansPage = new ServicePlansPage();
    public static LegacyProductsPage legacyProductsPage = new LegacyProductsPage();
    public static LegacyCartPage legacyCartPage = new LegacyCartPage();
    public static LegacyQuotePage legacyQuotePage = new LegacyQuotePage();
    public static PhasePage phasePage = new PhasePage();

    //  Approvals
    public static ApprovalPage approvalPage = new ApprovalPage();
    public static KycApprovalPage kycApprovalPage = new KycApprovalPage();
    public static KycApprovalCreationModal kycApprovalCreationModal = new KycApprovalCreationModal();
    public static TaxExemptApprovalPage teaApprovalPage = new TaxExemptApprovalPage();
    public static TaxExemptionManagerPage taxExemptionManagerPage = new TaxExemptionManagerPage();
    public static InvoicingRequestApprovalCreationModal invoiceApprovalCreationModal =
            opportunityPage.invoicingRequestApprovalCreationModal;
    public static DirectDebitApprovalCreationModal directDebitApprovalCreationModal =
            opportunityPage.directDebitApprovalCreationModal;

    //  Bill-on-Behalf
    public static BobWholesalePartnerConfirmationModal bobWholesalePartnerConfirmationModal =
            accountRecordPage.removeBobWholesalePartnerModal;
    public static ApprovalCreationForChannelOperationsModal bobApprovalCreationModal =
            new ApprovalCreationForChannelOperationsModal();

    //  Local Subscribed Address
    public static SelectLocalSubscribedAddressRecordTypeModal selectRecordTypeModal =
            new SelectLocalSubscribedAddressRecordTypeModal();
    public static NewLocalSubscribedAddressCreationModal localSubscribedAddressOfCompanyCreationModal =
            new NewLocalSubscribedAddressCreationModal(LOCAL_SUBSCRIBED_ADDRESS_RECORD_TYPE);
    public static NewLocalSubscribedAddressCreationModal registeredAddressOfCompanyCreationModal =
            new NewLocalSubscribedAddressCreationModal(REGISTERED_ADDRESS_RECORD_TYPE);
    public static LocalSubscribedAddressRecordPage localSubscribedAddressRecordPage =
            new LocalSubscribedAddressRecordPage();

    //  Setup pages
    public static DeliverabilityPage deliverabilityPage = new DeliverabilityPage();
    public static MyDomainPage myDomainPage = new MyDomainPage();

    //  PRM Portal
    public static IgnitePortalLoginPage ignitePortalLoginPage = new IgnitePortalLoginPage();
    public static PortalGlobalNavBar portalGlobalNavBar = new PortalGlobalNavBar();
    public static DealRegistrationListPage dealRegistrationListPage = new DealRegistrationListPage();
    public static LeadsListPage leadsListPage = new LeadsListPage();
    public static DealRegistrationCreationPage dealRegistrationCreationPage = new DealRegistrationCreationPage();
    public static DealRegistrationRecordPage dealRegistrationRecordPage = new DealRegistrationRecordPage();
    public static PortalEditLeadPage portalEditLeadPage = new PortalEditLeadPage();
    public static PortalLeadRecordPage portalLeadRecordPage = new PortalLeadRecordPage();
    public static PortalNewCasePage portalNewCasePage = new PortalNewCasePage();
    public static PortalCustomerDetailsPage portalCustomerDetailsPage = new PortalCustomerDetailsPage();
    public static PortalOpportunityDetailsPage portalOpportunityDetailsPage = new PortalOpportunityDetailsPage();

    //  ProServ Order
    public static ProServSuborderPage proServSuborderPage = new ProServSuborderPage();
    public static ProServProjectRecordPage proServProjectRecordPage = new ProServProjectRecordPage();
}
