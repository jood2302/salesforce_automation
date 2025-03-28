package service.wsdl;

import com.aquiva.autotests.rc.utilities.salesforce.SalesforceRestApiClient;
import org.apache.commons.io.FileUtils;

import java.io.File;

import static com.aquiva.autotests.rc.internal.reporting.ServiceTaskLogger.logResults;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static kotlin.text.Charsets.UTF_8;

/**
 * Methods for special task for regenerating JAR libraries with SOAP API bindings
 * from Enterprise and Tooling WSDL.
 */
public class RegenerateJavaStubsFromWsdlSteps {
    //  Data
    public final String enterpriseType;
    public final String toolingType;

    public RegenerateJavaStubsFromWsdlSteps() {
        enterpriseType = "enterprise";
        toolingType = "tooling";
    }

    /**
     * Regenerate Java "stubs" for the given WSDL type.
     *
     * @param wsdlType type of WSDL that Java binding are generated for
     *                 (e.g. "enterprise", "tooling")
     */
    public void regenerateApiJar(String wsdlType) {
        var wsdlXmlFile = new File(format("force-%s-api.wsdl.xml", wsdlType));

        step("1. Regenerate " + wsdlType + " WSDL file from Salesforce org via REST API", () -> {
            var wsdlAsString = wsdlType.equals(enterpriseType) ?
                    SalesforceRestApiClient.getEnterpriseWSDL() :
                    SalesforceRestApiClient.getToolingWSDL();

            //  Temp Workaround for an issue with Tooling WSDL's standard object 
            //  https://github.com/forcedotcom/wsc/issues/334
            if (wsdlType.equals(toolingType)) {
                wsdlAsString = wsdlAsString.replaceAll(
                        "xsd:enumeration value=\"([0-9].*)\"",
                        "xsd:enumeration value=\"_$1\""
                );
            }

            FileUtils.writeStringToFile(wsdlXmlFile, wsdlAsString, UTF_8);
        });

        step("2. Generate JAR library from the generated WSDL file", () -> {
            var pathToWsdlFile = wsdlXmlFile.getAbsolutePath();
            var pathToResultJar = format("force-%s-api.jar", wsdlType);
            var args = new String[]{pathToWsdlFile, pathToResultJar};

            com.sforce.ws.tools.wsdlc.main(args);

            logResults(new File(pathToResultJar), "application/java-archive", "jar");
        });

        step("3. Delete the " + wsdlType + " WSDL file", () -> {
            FileUtils.delete(wsdlXmlFile);
        });
    }
}
