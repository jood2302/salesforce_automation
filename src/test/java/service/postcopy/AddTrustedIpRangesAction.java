package service.postcopy;

import base.BaseTest;
import com.aquiva.autotests.rc.utilities.salesforce.ToolingConnectionUtils;
import com.sforce.soap.tooling.sobject.IpRange;
import org.junit.jupiter.api.*;

import java.util.List;

import static io.qameta.allure.Allure.step;
import static java.util.Collections.emptyList;

@Tag("PostCopy")
public class AddTrustedIpRangesAction extends BaseTest {
    private final ToolingConnectionUtils toolingConnectionUtils;

    //  Data
    private final List<String> ipAddressesList;

    public AddTrustedIpRangesAction() {
        toolingConnectionUtils = ToolingConnectionUtils.getInstance();

        var trustedIpsInput = System.getProperty("sf.trustedips");
        ipAddressesList = trustedIpsInput != null && !trustedIpsInput.isBlank()
                ? List.of(trustedIpsInput.replaceAll("\\s", "").split(";"))
                : emptyList();
    }

    @Test
    @DisplayName("Add Trusted IP Ranges to 'Network Access'")
    public void test() {
        step("Collect all the Trusted IPs from the system property and insert them in SFDC via API", () -> {
            var ipRangeList = ipAddressesList.stream().map(ip -> {
                var ipRange = new IpRange();
                ipRange.setStart(ip);
                ipRange.setEnd(ip);
                ipRange.setDescription("Aquiva AQA Team");
                return ipRange;
            }).distinct().toArray(IpRange[]::new);

            toolingConnectionUtils.insertAndGetIds(ipRangeList);
        });
    }
}
