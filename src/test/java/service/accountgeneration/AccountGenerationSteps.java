package service.accountgeneration;

import base.NgbsSteps;
import com.aquiva.autotests.rc.model.accountgeneration.CreateNgbsAccountsDTO;
import com.aquiva.autotests.rc.model.ngbs.dto.discounts.PromotionDiscountNgbsDTO;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.ags.AGSRestApiClient;
import io.qameta.allure.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.aquiva.autotests.rc.model.scp.ScpOperationRequestDTO.Variables.TesterFlagsItem.*;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.createPromoDiscountInNGBS;
import static com.aquiva.autotests.rc.utilities.scp.ScpRestApiClient.getTesterFlagsOnAccount;
import static com.aquiva.autotests.rc.utilities.scp.ScpRestApiClient.removeTesterFlagsOnAccount;
import static io.qameta.allure.Allure.step;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test methods related to creating Accounts in NGBS and SFDC.
 * <br/>
 * Make sure to provide System Configuration Portal REST API base URL (optional)
 * via system properties: {@code scp.rest.baseUrl}.
 */
public class AccountGenerationSteps {
    private static final Logger LOGGER = LoggerFactory.getLogger("AccountGeneration");

    private final NgbsSteps ngbsSteps;

    /**
     * New instance for the class with the test methods/steps
     * related to creating Accounts in NGBS and SFDC.
     */
    public AccountGenerationSteps() {
        ngbsSteps = new NgbsSteps(new Dataset());
    }

    /**
     * Generate a new account in NGBS via AGS.
     * <br/>
     * This method also handles additional actions:
     * <p> - creating discount(s) in NGBS (if user has provided discounts' data) </p>
     * <p> - creating contract in NGBS (if user has provided contract's data) </p>
     * <p> - purchasing additional licenses in NGBS (if user has provided license's data) </p>
     *
     * @param accountData data object parsed from user's input parameter
     *                    for creating Account in NGBS
     */
    public void createAccountInNGBS(CreateNgbsAccountsDTO accountData) {
        if (accountData.scenario == null || accountData.scenario.isBlank()) {
            throw new IllegalArgumentException("AGS Scenario is not provided in the current Account data object! \n" +
                    "Account data: " + accountData);
        }

        updateAgsScenarioForDurableAccount(accountData);
        var accountAGS = AGSRestApiClient.createAccount(accountData.scenario);

        accountData.billingId = accountAGS.getAccountBillingId();
        accountData.packageId = accountAGS.getAccountPackageId();
        accountData.rcUserId = accountAGS.rcUserId;

        if (accountData.licensesToOrder != null && accountData.licensesToOrder.length != 0) {
            ngbsSteps.purchaseAdditionalLicensesInNGBS(accountData.billingId, accountData.packageId, asList(accountData.licensesToOrder));
        }

        if (accountData.contract != null &&
                accountData.contract.contractExtId != null && accountData.contract.contractProduct != null) {
            ngbsSteps.stepCreateContractInNGBS(
                    accountData.billingId, accountData.packageId,
                    accountData.contract.contractExtId, accountData.contract.contractProduct
            );
        }

        if (accountData.discounts != null && accountData.discounts.length != 0) {
            ngbsSteps.stepCreateDiscountsInNGBS(accountData.billingId, accountData.packageId, accountData.discounts);
        }

        if (accountData.promoDiscounts != null) {
            for (var promoDiscount : accountData.promoDiscounts) {
                var promoDiscountDTO = new PromotionDiscountNgbsDTO(promoDiscount.code, promoDiscount.target);
                createPromoDiscountInNGBS(accountData.billingId, accountData.packageId, promoDiscountDTO);
            }
        }
    }

    /**
     * Connect to System Configuration Portal via REST API, remove RC tester flags on the NGBS account,
     * and check that this change is successful.
     *
     * @param ngbsAccountData account data with NGBS billing ID, package ID, RC User ID, etc...
     */
    @Step("Remove RC Tester flags on the NGBS account via System Configuration Portal API")
    public void removeTesterFlagsOnAccountViaSCP(CreateNgbsAccountsDTO ngbsAccountData) {
        if (!ngbsAccountData.scenario.contains("autoDelete=false")) {
            var flagsNotRemovedMessage = "RC Tester Flags were not removed for NGBS account with billingId = " + ngbsAccountData.billingId;
            step(flagsNotRemovedMessage);
            LOGGER.info(flagsNotRemovedMessage);
            return;
        }

        var rcUserId = ngbsAccountData.rcUserId;

        step("Remove Tester Flags ('Tester', 'Auto-delete', 'Send real request to Zoom', 'Send Real Request to Distributor') " +
                "on the NGBS Account", () -> {
            var removeTesterFlagsResponse = removeTesterFlagsOnAccount(rcUserId);

            assertThat(removeTesterFlagsResponse.errors)
                    .as("Errors in the response on changing Tester Flags (should not exist)")
                    .isNull();
            assertThat(removeTesterFlagsResponse.data.account.testerFlags)
                    .as("List of RC Tester Flags in the response on changing Tester Flags")
                    .contains(NO_EMAIL_NOTIFICATIONS)
                    .doesNotContain(TESTER, AUTO_DELETE, SEND_REAL_REQUESTS_TO_ZOOM, SEND_REAL_REQUESTS_TO_DISTRIBUTOR);
        });

        step("Check the list of the Tester Flags on the NGBS account after the change", () -> {
            var getTesterFlagsResponse = getTesterFlagsOnAccount(rcUserId);

            assertThat(getTesterFlagsResponse.errors)
                    .as("Errors in the response on getting Tester Flags (should not exist)")
                    .isNull();
            assertThat(getTesterFlagsResponse.data.account.accountInfo.serviceInfo.testerFlags)
                    .as(String.format("List of RC Tester Flags on NGBS Account with User ID = %s after the change", rcUserId))
                    .contains(NO_EMAIL_NOTIFICATIONS)
                    .doesNotContain(TESTER, AUTO_DELETE, SEND_REAL_REQUESTS_TO_ZOOM, SEND_REAL_REQUESTS_TO_DISTRIBUTOR);
        });

        LOGGER.info("RC Tester Flags are removed successfully for NGBS account with billingId = " + ngbsAccountData.billingId);
    }

    /**
     * Update AGS scenario in the data object for creating a "durable" account:
     * the one that won't be automatically deleted after a certain period of time (usually 1 day).
     * Normally, account generation scripts here will remove tester flags on such accounts
     * in a separate action.
     * <br/>
     * Result will be something like this:
     * <pre><code>
     *     //   This scenario...
     *     "ngbs(brand=1210,package=1231005v2,numberType=TollFree)"
     *     //   ... will become this one...
     *     "ngbs(brand=1210,package=1231005v2,numberType=TollFree,autoDelete=false)"
     *  </code></pre>
     *
     * <b> Note: if the scenario already contains any "autoDelete" parameter, the scenario won't be updated! </b>
     *
     * @param data data object parsed from user's input parameter
     *             for creating Account in NGBS
     */
    public void updateAgsScenarioForDurableAccount(CreateNgbsAccountsDTO data) {
        data.scenario = data.scenario.contains("autoDelete")
                ? data.scenario
                : data.scenario.replaceAll("\\)", ",autoDelete=false)");
    }
}
