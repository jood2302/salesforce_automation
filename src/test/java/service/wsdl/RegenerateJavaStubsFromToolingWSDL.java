package service.wsdl;

import base.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Special task for regenerating JAR library with SOAP API bindings from Tooling WSDL.
 */
public class RegenerateJavaStubsFromToolingWSDL extends BaseTest {
    private final RegenerateJavaStubsFromWsdlSteps regenerateJavaStubsFromWsdlSteps;

    public RegenerateJavaStubsFromToolingWSDL() {
        regenerateJavaStubsFromWsdlSteps = new RegenerateJavaStubsFromWsdlSteps();
    }

    @Test
    @DisplayName("Regenerate Tooling API JAR library from the Salesforce org")
    public void test() {
        regenerateJavaStubsFromWsdlSteps.regenerateApiJar(regenerateJavaStubsFromWsdlSteps.toolingType);
    }
}
