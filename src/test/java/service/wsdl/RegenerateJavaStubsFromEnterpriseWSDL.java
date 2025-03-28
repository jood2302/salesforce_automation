package service.wsdl;

import base.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Special task for regenerating JAR library with SOAP API bindings from Enterprise WSDL.
 */
public class RegenerateJavaStubsFromEnterpriseWSDL extends BaseTest {
    private final RegenerateJavaStubsFromWsdlSteps regenerateJavaStubsFromWsdlSteps;

    public RegenerateJavaStubsFromEnterpriseWSDL() {
        regenerateJavaStubsFromWsdlSteps = new RegenerateJavaStubsFromWsdlSteps();
    }

    @Test
    @DisplayName("Regenerate Enterprise API JAR library from the Salesforce org")
    public void regenerateEnterpriseApiJar() {
        regenerateJavaStubsFromWsdlSteps.regenerateApiJar(regenerateJavaStubsFromWsdlSteps.enterpriseType);
    }
}
