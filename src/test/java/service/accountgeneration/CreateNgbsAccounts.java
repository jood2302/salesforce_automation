package service.accountgeneration;

import base.BaseTest;
import com.aquiva.autotests.rc.model.accountgeneration.CreateNgbsAccountsDTO;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.aquiva.autotests.rc.internal.reporting.ServiceTaskLogger.*;
import static io.qameta.allure.Allure.step;

/**
 * Special task for creating Existing Business accounts in NGBS
 * that are ready to be used in automated tests.
 * NGBS accounts are automation-tests-ready when RC Tester Flags are removed on them.
 * <br/>
 * Make sure to provide NGBS account data via system properties:
 * {@code ags.scenarios, ngbs.createAccountsData}.
 * <br/>
 * <b> Note: for 'ags.scenarios' list of AGS scenarios should be separated with a ";" symbol!
 * <br/>
 * E.g.: ngbs(brand=1210,package=1231005v2);ngbs(brand=1210,package=17v1);ngbs(brand=1210,package=301v1,targetPackage=318v1).
 * If 'ags.scenarios' are provided, 'ngbs.createAccountsData' gets ignored!
 * <br/>
 * Note: for 'ngbs.createAccountsData' list of NGBS Accounts Data should be in a JSON format:
 * <pre><code class='json'>
 * [
 *   {
 *     "scenario": "ngbs(brand=1210,package=1231005v2,numberType=TollFree)"
 *   },
 *   {
 *     "scenario": "ngbs(brand=1210,package=1231005v2,numberType=TollFree)",
 *     "contract": {
 *       "contractExtId": "Office",
 *       "contractProduct": {
 *         "name": "DigitalLine Unlimited Standard",
 *         "dataName": "LC_DL-UNL_50",
 *         "group": "Services",
 *         "subgroup": "Main",
 *         "chargeTerm": "Monthly",
 *         "price": "30.99",
 *         "yourPrice": "30.99",
 *         "quantity": 3,
 *         "existingQuantity": 1,
 *         "discount": 0,
 *         "discountType": "%"
 *       }
 *     }
 *   },
 *   {
 *     "scenario": "ngbs(brand=1210,package=1231005v2,numberType=TollFree)",
 *     "discounts": [
 *       {
 *         "name": "Polycom VVX 501 Color Touchscreen Phone with 1 Expansion Module",
 *         "dataName": "LC_HD_139",
 *         "group": "Phones",
 *         "subgroup": "Purchase",
 *         "chargeTerm": "One - Time",
 *         "price": "499.00",
 *         "yourPrice": "449.10",
 *         "quantity": 0,
 *         "discount": 10,
 *         "discountType": "%"
 *       },
 *       {
 *         "name": "Yealink W60P Cordless Phone with 1 Handset",
 *         "dataName": "LC_HD_564",
 *         "group": "Phones",
 *         "subgroup": "Purchase",
 *         "chargeTerm": "One - Time",
 *         "price": "179.00",
 *         "yourPrice": "159.00",
 *         "quantity": 0,
 *         "discount": 20,
 *         "discountType": "USD"
 *       }
 *     ]
 *   },
 *   {
 *     "scenario": "ngbs(brand=1210,package=1231005v2,numberType=TollFree)",
 *     "licensesToOrder": [
 *       {
 *         "catalogId": "LC_DLI_282",
 *         "comment": "Global DigitalLine parent license; set qty to 1-50000 for Global LATAM here",
 *         "billingCycleDuration": "Monthly",
 *         "qty": 30,
 *         "subItems": [
 *           {
 *             "catalogId": "LC_IBO_290",
 *             "comment": "Global LATAM license to be ordered",
 *             "billingCycleDuration": "Monthly",
 *             "qty": 1,
 *             "subItems": [
 *               {
 *                 "catalogId": "LC_IVN_291",
 *                 "billingCycleDuration": "Monthly",
 *                 "qty": 1,
 *                 "subItems": []
 *               }
 *             ]
 *           }
 *         ]
 *       }
 *     ]
 *   },
 *   {
 *     "scenario": "ngbs(brand=1210,package=1231005v2,numberType=TollFree)",
 *     "promoDiscounts": [
 *        {
 *          "code" : "QA-AUTO-DLUNLIMITED-CATEGORY-USD-06-22",
 *          "target" : {
 *              "catalogId" : "1231005",
 *              "version" : "2"
 *          }
 *        }
 *      ]
 *    }
 * ]
 * </code></pre>
 *
 * @see CreateNgbsAccountsDTO
 */
public class CreateNgbsAccounts extends BaseTest {
    private final AccountGenerationSteps accountGenerationSteps;

    public CreateNgbsAccounts() {
        accountGenerationSteps = new AccountGenerationSteps();
    }

    @Test
    @DisplayName("Generate Existing Business account(s) in NGBS")
    public void test() throws IOException {
        var resultsFile = initializeAndGetResultsFile("new_ngbs_accounts");
        var ngbsAccountsInputData = getNgbsAccountsData();

        var processedData = new ArrayList<CreateNgbsAccountsDTO>();
        for (var data : ngbsAccountsInputData) {
            step("Create an Existing Business Account in NGBS for scenario '" + data.scenario + "'", () -> {
                accountGenerationSteps.createAccountInNGBS(data);
                accountGenerationSteps.removeTesterFlagsOnAccountViaSCP(data);

                processedData.add(data);
                updateResultsFile(resultsFile, processedData);
            });
        }

        logResults(resultsFile);
    }

    /**
     * Get a collection of input data objects for creating NGBS accounts from the system property variable.
     *
     * @return list of input data objects to create new NGBS account(s) with
     */
    public List<CreateNgbsAccountsDTO> getNgbsAccountsData() {
        var scenariosInputString = System.getProperty("ags.scenarios");
        if (scenariosInputString != null && !scenariosInputString.isBlank()) {
            var scenariosList = List.of(scenariosInputString
                    .replaceAll("\\s", "")
                    .split(";")
            );

            var ngbsAccountsData = new ArrayList<CreateNgbsAccountsDTO>();
            for (var scenario : scenariosList) {
                var ngbsAccountDTO = new CreateNgbsAccountsDTO();
                ngbsAccountDTO.scenario = scenario;
                ngbsAccountsData.add(ngbsAccountDTO);
            }

            return ngbsAccountsData;
        }

        var ngbsAccountsDataInputString = System.getProperty("ngbs.createAccountsData");
        if (ngbsAccountsDataInputString == null || ngbsAccountsDataInputString.isBlank()) {
            throw new IllegalArgumentException("No AGS scenario(s) or Account Input Data have been provided! " +
                    "Make sure to add AGS scenario(s) for this task via 'ags.scenarios' parameter, " +
                    "or add AGS scenario, contract data (optional), discount data (optional) " +
                    "for this task via 'ngbs.createAccountsData' parameter!");
        }

        var accountsDataParsed = JsonUtils.readJson(ngbsAccountsDataInputString, CreateNgbsAccountsDTO[].class);
        return List.of(accountsDataParsed);
    }
}
