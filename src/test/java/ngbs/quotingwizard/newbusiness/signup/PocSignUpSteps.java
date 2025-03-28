package ngbs.quotingwizard.newbusiness.signup;

import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Approval__c;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_APPROVED;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.LINK_TO_SIGNED_EVALUATION_AGREEMENT;

/**
 * Test methods for test cases related to "Sign Up" functionality for POC Quotes.
 */
public class PocSignUpSteps {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    public final String linkToSignedEvaluationAgreement;

    public PocSignUpSteps() {
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        linkToSignedEvaluationAgreement = LINK_TO_SIGNED_EVALUATION_AGREEMENT;
    }

    /**
     * Change POC Approval status to 'Approved' via SFDC API/DB.
     *
     * @param pocQuoteId Salesforce ID for the POC Quote
     */
    public void stepSetPocApprovalStatusToApproved(String pocQuoteId) throws Exception {
        var pocApproval = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id " +
                        "FROM Approval__c " +
                        "WHERE Quote__c = '" + pocQuoteId + "'",
                Approval__c.class);

        pocApproval.setStatus__c(APPROVAL_STATUS_APPROVED);
        enterpriseConnectionUtils.update(pocApproval);
    }
}
