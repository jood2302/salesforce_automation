package service.postcopy;

import base.BaseTest;
import com.aquiva.autotests.rc.model.postcopy.CustomSettings;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.ToolingConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import com.sforce.soap.tooling.sobject.RemoteProxy;
import org.junit.jupiter.api.*;

import java.util.Map;

import static com.aquiva.autotests.rc.utilities.Constants.CUSTOM_SETTINGS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.RemoteProxyFactory.createRemoteProxy;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("PostCopy")
public class SetRcCustomSettingsAction extends BaseTest {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final ToolingConnectionUtils toolingConnectionUtils;

    //  Data
    private final CustomSettings customSettings;
    private final Map<String, String> remoteSiteSettings;

    public SetRcCustomSettingsAction() {
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        toolingConnectionUtils = ToolingConnectionUtils.getInstance();

        if (CUSTOM_SETTINGS == null) {
            throw new IllegalArgumentException("Custom Settings for SFDC are not provided! " +
                    "Make sure to provide them via system property 'sf.customSettings'.");
        }

        customSettings = JsonUtils.readJson(CUSTOM_SETTINGS, CustomSettings.class);
        remoteSiteSettings = customSettings.collectRemoteSiteSettings();
    }

    @Test
    @DisplayName("Set Custom Settings for RingCentral")
    public void test() {
        step("1. Update GW Settings via API", () -> {
            var gwSettings = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, BaseEndpoint__c " +
                            "FROM GW_Settings__c " +
                            "WHERE SetupOwner.Name = 'RingCentral'",
                    GW_Settings__c.class);

            gwSettings.setBaseEndpoint__c(customSettings.gwSettings.baseEndpoint);
            enterpriseConnectionUtils.update(gwSettings);
        });

        step("2. Update Endpoints Settings via API", () -> {
            var endpointsSettings = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, UQTServerEndpoint__c " +
                            "FROM EndpointsSettings__c " +
                            "WHERE SetupOwner.Name = 'RingCentral'",
                    EndpointsSettings__c.class);
            endpointsSettings.setUQTServerEndpoint__c(customSettings.endpointsSettings.uqtServerEndpoint);
        });

        step("3. Update NGBS Settings via API", () -> {
            var ringCentralNGBSSettings = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, BaseEndpoint__c, Login__c, Password__c, AccountManagerPath__c, InvoiceEnginePath__c, " +
                            "CatalogPath__c, CostCenterNestingLevel__c " +
                            "FROM NGBS_Settings__c " +
                            "WHERE SetupOwner.Name = 'RingCentral'",
                    NGBS_Settings__c.class);

            ringCentralNGBSSettings.setBaseEndpoint__c(customSettings.ngbsSettings.baseEndpoint);
            ringCentralNGBSSettings.setLogin__c(customSettings.ngbsSettings.login);
            ringCentralNGBSSettings.setPassword__c(customSettings.ngbsSettings.password);
            ringCentralNGBSSettings.setAccountManagerPath__c(customSettings.ngbsSettings.accountManagerPath);
            ringCentralNGBSSettings.setInvoiceEnginePath__c(customSettings.ngbsSettings.invoiceEnginePath);
            ringCentralNGBSSettings.setCatalogPath__c(customSettings.ngbsSettings.catalogPath);
            ringCentralNGBSSettings.setCostCenterNestingLevel__c(customSettings.ngbsSettings.costCenterNestingLevel);
            enterpriseConnectionUtils.update(ringCentralNGBSSettings);
        });

        step("4. Update CCB Settings via API", () -> {
            var ringCentralCCBSettings = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, BaseEndpoint__c, Login__c, Password__c, SignUpPath__c, TerminationPath__c " +
                            "FROM CCB_Settings__c " +
                            "WHERE SetupOwner.Name = 'RingCentral'",
                    CCB_Settings__c.class);

            ringCentralCCBSettings.setBaseEndpoint__c(customSettings.ccbSettings.baseEndpoint);
            ringCentralCCBSettings.setLogin__c(customSettings.ccbSettings.login);
            ringCentralCCBSettings.setPassword__c(customSettings.ccbSettings.password);
            ringCentralCCBSettings.setSignUpPath__c(customSettings.ccbSettings.signUpPath);
            ringCentralCCBSettings.setTerminationPath__c(customSettings.ccbSettings.terminationPath);
            enterpriseConnectionUtils.update(ringCentralCCBSettings);
        });

        step("5. Check updated GW Settings", () -> {
            var gwSettings = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, BaseEndpoint__c " +
                            "FROM GW_Settings__c " +
                            "WHERE SetupOwner.Name = 'RingCentral'",
                    GW_Settings__c.class);

            assertThat(gwSettings.getBaseEndpoint__c())
                    .as("GW_Settings__c.BaseEndpoint__c value")
                    .isEqualTo(customSettings.gwSettings.baseEndpoint);
        });

        step("6. Check updated Endpoints Settings", () -> {
            var endpointsSettings = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, UQTServerEndpoint__c " +
                            "FROM EndpointsSettings__c " +
                            "WHERE SetupOwner.Name = 'RingCentral'",
                    EndpointsSettings__c.class);
            assertThat(endpointsSettings.getUQTServerEndpoint__c())
                    .as("EndpointsSettings__c.UQTServerEndpoint__c value")
                    .isEqualTo(customSettings.endpointsSettings.uqtServerEndpoint);
        });

        step("7. Check updated NGBS Settings", () -> {
            var ringCentralNGBSSettings = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, BaseEndpoint__c, Login__c, Password__c, AccountManagerPath__c, InvoiceEnginePath__c, " +
                            "CatalogPath__c, CostCenterNestingLevel__c " +
                            "FROM NGBS_Settings__c " +
                            "WHERE SetupOwner.Name = 'RingCentral'",
                    NGBS_Settings__c.class);

            assertThat(ringCentralNGBSSettings.getBaseEndpoint__c())
                    .as("NGBS_Settings__c.BaseEndpoint__c value")
                    .isEqualTo(customSettings.ngbsSettings.baseEndpoint);
            assertThat(ringCentralNGBSSettings.getLogin__c())
                    .as("NGBS_Settings__c.Login__c value")
                    .isEqualTo(customSettings.ngbsSettings.login);
            assertThat(ringCentralNGBSSettings.getPassword__c())
                    .as("NGBS_Settings__c.Password__c value")
                    .isEqualTo(customSettings.ngbsSettings.password);
            assertThat(ringCentralNGBSSettings.getAccountManagerPath__c())
                    .as("NGBS_Settings__c.AccountManagerPath__c value")
                    .isEqualTo(customSettings.ngbsSettings.accountManagerPath);
            assertThat(ringCentralNGBSSettings.getInvoiceEnginePath__c())
                    .as("NGBS_Settings__c.InvoiceEnginePath__c value")
                    .isEqualTo(customSettings.ngbsSettings.invoiceEnginePath);
            assertThat(ringCentralNGBSSettings.getCatalogPath__c())
                    .as("NGBS_Settings__c.CatalogPath__c value")
                    .isEqualTo(customSettings.ngbsSettings.catalogPath);
            assertThat(ringCentralNGBSSettings.getCostCenterNestingLevel__c())
                    .as("NGBS_Settings__c.CostCenterNestingLevel__c value")
                    .isEqualTo(customSettings.ngbsSettings.costCenterNestingLevel);
        });

        step("8. Check updated CCB Settings", () -> {
            var ringCentralCCBSettings = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, BaseEndpoint__c, Login__c, Password__c, SignUpPath__c, TerminationPath__c " +
                            "FROM CCB_Settings__c " +
                            "WHERE SetupOwner.Name = 'RingCentral'",
                    CCB_Settings__c.class);

            assertThat(ringCentralCCBSettings.getBaseEndpoint__c())
                    .as("CCB_Settings__c.BaseEndpoint__c value")
                    .isEqualTo(customSettings.ccbSettings.baseEndpoint);
            assertThat(ringCentralCCBSettings.getLogin__c())
                    .as("CCB_Settings__c.Login__c value")
                    .isEqualTo(customSettings.ccbSettings.login);
            assertThat(ringCentralCCBSettings.getPassword__c())
                    .as("CCB_Settings__c.Password__c value")
                    .isEqualTo(customSettings.ccbSettings.password);
            assertThat(ringCentralCCBSettings.getSignUpPath__c())
                    .as("CCB_Settings__c.SignUpPath__c value")
                    .isEqualTo(customSettings.ccbSettings.signUpPath);
            assertThat(ringCentralCCBSettings.getTerminationPath__c())
                    .as("CCB_Settings__c.TerminationPath__c value")
                    .isEqualTo(customSettings.ccbSettings.terminationPath);
        });

        step("9. Check and update Remote Site Settings", () -> {
            remoteSiteSettings.forEach(this::processRemoteSiteSetting);
        });
    }

    /**
     * Create a new Remote Site Setting with a given Endpoint URL if there's none.
     *
     * @param siteName    Name of the endpoint that will be set in the new Remote Site Setting
     * @param endpointUrl The URL that will be used for the search and that will be set in the new Remote Site Setting
     */
    private void processRemoteSiteSetting(String siteName, String endpointUrl) {
        step("Processing '" + siteName + "' Remote Site Setting", () -> {
            var remoteProxy = toolingConnectionUtils.query(
                    "SELECT Id " +
                            "FROM RemoteProxy " +
                            "WHERE EndpointUrl = '" + endpointUrl + "'",
                    RemoteProxy.class);
            if (remoteProxy.isEmpty()) {
                var createdRemoteProxy = createRemoteProxy(siteName, endpointUrl);
                assertThat(createdRemoteProxy.getId())
                        .as("Created Remote Site Setting ID for '" + siteName + "'")
                        .isNotNull();
            }
        });
    }
}
