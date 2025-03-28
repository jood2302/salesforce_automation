package service.limits;

import base.BaseTest;
import com.aquiva.autotests.rc.model.salesforce.Limits;
import com.aquiva.autotests.rc.utilities.salesforce.SalesforceRestApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.qameta.allure.Allure.step;
import static java.lang.Double.parseDouble;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Special task for monitoring and checking the current Salesforce Org limits.
 */
public class LimitsCheck extends BaseTest {
    private Limits sfdcLimits;

    //  Data
    private final Double dataStorageThresholdPercent;

    public LimitsCheck() {
        dataStorageThresholdPercent = parseDouble(System.getProperty("sf.dataStorageThresholdPercent", "90.0"));
    }

    @Test
    @DisplayName("Check the current limits in the Salesforce org")
    public void test() {
        step("1. Get all the current Org Limits via Salesforce REST API", () -> {
            sfdcLimits = SalesforceRestApiClient.getOrgCurrentLimits();
        });

        step("2. Check that the threshold for the Data Storage is not exceeded", () -> {
            var consumedMB = sfdcLimits.dataStorageMB.max - sfdcLimits.dataStorageMB.remaining;
            var consumedPercent = consumedMB / sfdcLimits.dataStorageMB.max * 100;

            assertThat(consumedPercent)
                    .as("The consumed Data Storage in % of the overall available storage")
                    .isLessThan(dataStorageThresholdPercent);
        });
    }
}
