package ngbs.opportunitycreation.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Default_Business_Identity_Mapping__mdt;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Ignite")
public class AvailableBusinessIdentitiesOnQopTest extends BaseTest {
    private final AvailableBusinessIdentityOnQopSteps availableBiOnQopSteps;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public AvailableBusinessIdentitiesOnQopTest() {
        availableBiOnQopSteps = new AvailableBusinessIdentityOnQopSteps();
        steps = new Steps(availableBiOnQopSteps.data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        availableBiOnQopSteps.setUpCustomerAccountStep(steps.salesFlow.account);

        step("Check that 'Default Business Identity Mapping' record of custom metadata type exists " +
                "for 'RingCentral Germany' Default Business Identity " +
                "and Available_Business_Identities__c = 'RingCentral, Ltd (France)'", () -> {
            var rcUsDefaultBiMappingCustomMetadataRecords = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Default_Business_Identity_Mapping__mdt " +
                            "WHERE Sub_Brand__c = null " +
                            "AND Brand__c = '" + availableBiOnQopSteps.data.brandName + "' " +
                            "AND Country__c = '" + availableBiOnQopSteps.countryGermany + "' " +
                            "AND Default_Business_Identity__c = '" + availableBiOnQopSteps.rcGermanyDefaultBusinessIdentity + "' " +
                            "AND Available_Business_Identities__c = '" + availableBiOnQopSteps.rcFranceBusinessIdentity + "'",
                    Default_Business_Identity_Mapping__mdt.class);
            assertThat(rcUsDefaultBiMappingCustomMetadataRecords.size())
                    .as("Quantity of Default_Business_Identity_Mapping__mdt records for 'RingCentral Germany' " +
                            "and 'RingCentral, Ltd (France)' business identities")
                    .isEqualTo(1);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-34727")
    @DisplayName("CRM-34727 - Availability of 'Available Business Identity' from 'Business Identity Mapping' Custom MetaData " +
            "on Quick Opportunity Page for Customer Account")
    @Description("Verify that 'Available Business Identity' is retrieved from 'Business Identity Mapping' Custom MetaData " +
            "on Quick Opportunity Page for Customer Account")
    public void test() {
        availableBiOnQopSteps.checkBrandAndBiPicklistOnQopForDifferentUsers(steps.salesFlow.account);
    }
}
