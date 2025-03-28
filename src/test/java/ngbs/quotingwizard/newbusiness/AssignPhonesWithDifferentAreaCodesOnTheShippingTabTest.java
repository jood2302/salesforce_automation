package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.CustomAddressAssignments__c;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import javax.annotation.Nullable;
import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.shippingtab.ShippingDevice.DEVICES_LEFT_REGEX;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToIntToString;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.codeborne.selenide.CollectionCondition.*;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("ShippingTab")
public class AssignPhonesWithDifferentAreaCodesOnTheShippingTabTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Product phoneOne;
    private final Product phoneTwo;
    private final Product phoneThree;
    private final Product dlUnlimited;
    private final Product dlBasic;
    private final Product globalMvpAPAC;

    private final AreaCode areaCodeOne;
    private final AreaCode areaCodeTwo;
    private final AreaCode areaCodeThree;
    private final AreaCode areaCodeFour;
    private final AreaCode areaCodeFive;
    private final AreaCode areaCodeSeven;
    private final AreaCode areaCodeSix;
    private final AreaCode areaCodeEight;
    private final AreaCode areaCodeNine;

    private final DigitalLinePhoneAreaCode dlUnlimitedPhoneOneAreaCodeOne;
    private final DigitalLinePhoneAreaCode dlBasicPhoneOneAreaCodeTwo;
    private final DigitalLinePhoneAreaCode globalMvpApacPhoneOneAreaCodeThree;

    private final DigitalLinePhoneAreaCode dlUnlimitedPhoneTwoAreaCodeFour;
    private final DigitalLinePhoneAreaCode dlBasicPhoneTwoAreaCodeFive;
    private final DigitalLinePhoneAreaCode globalMvpApacPhoneTwoAreaCodeSix;
    private final int phoneTwoUnassignedTotalQuantity;
    private final int phoneTwoUnassignedShippingQuantity;

    private final DigitalLinePhoneAreaCode dlUnlimitedPhoneThreeAreaCodeSeven;
    private final DigitalLinePhoneAreaCode dlBasicPhoneThreeAreaCodeEight;
    private final DigitalLinePhoneAreaCode globalMvpApacPhoneThreeAreaCodeNine;
    private final int phoneThreeUnassignedTotalQuantity;

    private final int initUnassignedDevicesOnShippingTab;
    private final int expectedNumberOfCustomAddressAssignments;

    private final ShippingGroupAddress shippingGroupAddress;
    private final String shippingAddressFormatted;

    public AssignPhonesWithDifferentAreaCodesOnTheShippingTabTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_2TypesOfDLs_RegularAndPOC.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        phoneOne = data.getProductByDataName("LC_HD_936");
        phoneTwo = data.getProductByDataName("LC_HD_959");
        phoneThree = data.getProductByDataName("LC_HD_523");
        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        dlBasic = data.getProductByDataName("LC_DL-BAS_178");
        globalMvpAPAC = data.getProductByDataName("LC_IBO_288");

        //  United States, California, Alpine (619)
        areaCodeOne = new AreaCode("Local", "United States", "California", EMPTY_STRING, "619");
        //  Canada, Nova Scotia, Dartmouth (902)
        areaCodeTwo = new AreaCode("Local", "Canada", "Nova Scotia", EMPTY_STRING, "902");
        //  Australia, Queensland, Cleveland (728)
        areaCodeThree = new AreaCode("Local", "Australia", "Queensland", EMPTY_STRING, "728");
        //  United States, Connecticut, Kent (860)
        areaCodeFour = new AreaCode("Local", "United States", "Connecticut", EMPTY_STRING, "860");
        //  Canada, Alberta, Grande Prairie (780)
        areaCodeFive = new AreaCode("Local", "Canada", "Alberta", EMPTY_STRING, "780");
        //  Australia, Victoria, Eltham (394)
        areaCodeSix = new AreaCode("Local", "Australia", "Victoria", EMPTY_STRING, "394");
        //  Puerto Rico, San Juan (787)
        areaCodeSeven = new AreaCode("Local", "Puerto Rico", EMPTY_STRING, "San Juan", "787");
        //  United States, Hawaii, Hilo (808)
        areaCodeEight = new AreaCode("Local", "United States", "Hawaii", EMPTY_STRING, "808");
        //  Australia, New South Wales, Lower Hunter (240)
        areaCodeNine = new AreaCode("Local", "Australia", "New South Wales", EMPTY_STRING, "240");

        phoneOne.quantity = 10;
        dlUnlimitedPhoneOneAreaCodeOne = new DigitalLinePhoneAreaCode(dlUnlimited, phoneOne, areaCodeOne, 1);
        dlBasicPhoneOneAreaCodeTwo = new DigitalLinePhoneAreaCode(dlBasic, phoneOne, areaCodeTwo, 2);
        globalMvpApacPhoneOneAreaCodeThree = new DigitalLinePhoneAreaCode(globalMvpAPAC, phoneOne, areaCodeThree, 3);
        var phoneOneUnassignedTotalQuantity = 4; //  unused, but kept for the overall data consistency
        dlBasicPhoneOneAreaCodeTwo.shippingQuantity = dlBasicPhoneOneAreaCodeTwo.quantity; //  no unassigned devices left for shipping

        phoneTwo.quantity = 26;
        dlUnlimitedPhoneTwoAreaCodeFour = new DigitalLinePhoneAreaCode(dlUnlimited, phoneTwo, areaCodeFour, 5);
        dlBasicPhoneTwoAreaCodeFive = new DigitalLinePhoneAreaCode(dlBasic, phoneTwo, areaCodeFive, 6);
        globalMvpApacPhoneTwoAreaCodeSix = new DigitalLinePhoneAreaCode(globalMvpAPAC, phoneTwo, areaCodeSix, 7);
        phoneTwoUnassignedTotalQuantity = 8;
        phoneTwoUnassignedShippingQuantity = 5;

        phoneThree.quantity = 45;
        dlUnlimitedPhoneThreeAreaCodeSeven = new DigitalLinePhoneAreaCode(dlUnlimited, phoneThree, areaCodeSeven, 9);
        dlBasicPhoneThreeAreaCodeEight = new DigitalLinePhoneAreaCode(dlBasic, phoneThree, areaCodeEight, 11);
        globalMvpApacPhoneThreeAreaCodeNine = new DigitalLinePhoneAreaCode(globalMvpAPAC, phoneThree, areaCodeNine, 12);
        phoneThreeUnassignedTotalQuantity = 13;
        dlUnlimitedPhoneThreeAreaCodeSeven.shippingQuantity = 3; //  some unassigned devices left for shipping

        dlUnlimited.quantity = 15;
        dlBasic.quantity = 19;
        globalMvpAPAC.quantity = 22;

        //  9 Area Codes + 3 phones that still have some unassigned quantity = 12
        initUnassignedDevicesOnShippingTab = 12;
        expectedNumberOfCustomAddressAssignments = 4;

        shippingGroupAddress = new ShippingGroupAddress("United States", "Findlay",
                new ShippingGroupAddress.State("Ohio", true),
                "3644 Cedarstone Drive", "45840", "QA Automation");
        shippingAddressFormatted = shippingGroupAddress.getAddressFormatted();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-33649")
    @DisplayName("CRM-33649 - Divide the Phones by DL and Area Code on the Shipping Tab, " +
            "with the ability to assign them to the Shipping Groups")
    @Description("Verify that the Shipping tab has been modified to show Phones available for shipping in Groups:\n" +
            "- Phone + Digital Line it was assigned to + Area Code + the exact number of phones in such groups (devices left)\n" +
            "- Phones can be with or without Area Codes, and still be valid for shipping\n" +
            "- When the user drags phones to the Shipping Group, the maximum number is allocated by default. " +
            "The user may decrease this quantity, and the remaining devices should be displayed on the left.\n" +
            "- The sorting order is based on the Area Code")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Add Products tab, add the 3 phones and 2 additional digital lines", () -> {
            steps.quoteWizard.addProductsOnProductsTab(phoneOne, phoneTwo, phoneThree, dlBasic, globalMvpAPAC);
        });

        step("3. Open the Price tab, set up quantities for the added phones and DLs, " +
                "assign some of the phones' quantities to the DLs, and save changes", () -> {
            cartPage.openTab();

            steps.cartTab.setUpQuantities(phoneOne, phoneTwo, phoneThree, dlUnlimited, dlBasic, globalMvpAPAC);

            steps.cartTab.assignDevicesToDL(dlUnlimitedPhoneOneAreaCodeOne);
            steps.cartTab.assignDevicesToDL(dlBasicPhoneOneAreaCodeTwo);
            steps.cartTab.assignDevicesToDL(globalMvpApacPhoneOneAreaCodeThree);

            steps.cartTab.assignDevicesToDL(dlUnlimitedPhoneTwoAreaCodeFour);
            steps.cartTab.assignDevicesToDL(dlBasicPhoneTwoAreaCodeFive);
            steps.cartTab.assignDevicesToDL(globalMvpApacPhoneTwoAreaCodeSix);

            steps.cartTab.assignDevicesToDL(dlUnlimitedPhoneThreeAreaCodeSeven);
            steps.cartTab.assignDevicesToDL(dlBasicPhoneThreeAreaCodeEight);
            steps.cartTab.assignDevicesToDL(globalMvpApacPhoneThreeAreaCodeNine);

            cartPage.saveChanges();
        });

        step("4. Open the Shipping tab, " +
                "check all the unassigned devices on the left, " +
                "check that the list has alphabetic sort by area code's country, " +
                "and check the initial available number of the unassigned devices on the elements ('xyz devices left')", () -> {
            shippingPage.openTab();

            shippingPage.listOfDevices.shouldHave(size(initUnassignedDevicesOnShippingTab));

            var expectedAreaCodesList = List.of(
                    areaCodeThree.fullName, //  Australia
                    areaCodeSix.fullName,   //  Australia
                    areaCodeNine.fullName,  //  Australia
                    areaCodeTwo.fullName,   //  Canada
                    areaCodeFive.fullName,  //  Canada
                    areaCodeSeven.fullName, //  Puerto Rico
                    areaCodeOne.fullName,   //  United States
                    areaCodeFour.fullName,  //  United States
                    areaCodeEight.fullName  //  United States
            );
            shippingPage.areaCodesOnDevices.shouldHave(exactTexts(expectedAreaCodesList));

            var dlsToPhonesToAreaCodesList = List.of(
                    dlUnlimitedPhoneOneAreaCodeOne, dlBasicPhoneOneAreaCodeTwo, globalMvpApacPhoneOneAreaCodeThree,
                    dlUnlimitedPhoneTwoAreaCodeFour, dlBasicPhoneTwoAreaCodeFive, globalMvpApacPhoneTwoAreaCodeSix,
                    dlUnlimitedPhoneThreeAreaCodeSeven, dlBasicPhoneThreeAreaCodeEight, globalMvpApacPhoneThreeAreaCodeNine
            );
            dlsToPhonesToAreaCodesList.forEach((dlPhoneAreaCode) -> {
                var productName = dlPhoneAreaCode.phone.productName;
                var areaCodeName = dlPhoneAreaCode.areaCode.fullName;
                var numberOfDevicesLeft = dlPhoneAreaCode.quantity;

                step("Check the initial devices left for the area code = " + areaCodeName + ", device =  " + productName, () -> {
                    shippingPage.getShippingDevice(productName, areaCodeName)
                            .getNumberOfDevicesLeft()
                            .should(matchText(numberOfDevicesLeft + DEVICES_LEFT_REGEX));
                });
            });
        });

        step("5. Add a new Shipping Group", () -> {
            shippingPage.addNewShippingGroup(shippingGroupAddress);
        });

        //  Shipping Group Device 1: Phone + Area Code + DL, shipping quantity = SOME available devices
        step("6. Assign " + dlUnlimitedPhoneThreeAreaCodeSeven.phone.name + " device to the new Shipping Group, " +
                "check the default shipping quantity, check that it's impossible to set more devices than available, " +
                "set the new quantity of assigned devices, and check the value of devices left", () -> {
            checkPartialShippingDeviceAssignment(dlUnlimitedPhoneThreeAreaCodeSeven.phone, dlUnlimitedPhoneThreeAreaCodeSeven.areaCode,
                    dlUnlimitedPhoneThreeAreaCodeSeven.quantity, dlUnlimitedPhoneThreeAreaCodeSeven.shippingQuantity);
        });

        //  Shipping Group Device 2: Phone + Area Code + DL, shipping quantity = ALL available devices
        step("7. Assign " + dlBasicPhoneOneAreaCodeTwo.phone.name + " device to the new Shipping Group, " +
                "and check that there are no devices left (on the left side)", () -> {
            var shippingDevice = shippingPage.getShippingDevice(
                    dlBasicPhoneOneAreaCodeTwo.phone.productName,
                    dlBasicPhoneOneAreaCodeTwo.areaCode.fullName);
            shippingPage.assignDeviceToShippingGroup(shippingDevice, shippingAddressFormatted, shippingGroupAddress.shipAttentionTo);

            shippingDevice.getSelf().shouldNot(exist);
        });

        //  Shipping Group Device 3: Phone + No Area Code and DL, shipping quantity = SOME available devices
        step("8. Assign " + phoneTwo.name + " device to the new Shipping Group (with no area codes), " +
                "check the default shipping quantity, check that it's impossible to set more devices than available, " +
                "set the new quantity of assigned devices, and check the value of devices left", () -> {
            checkPartialShippingDeviceAssignment(phoneTwo, null,
                    phoneTwoUnassignedTotalQuantity, phoneTwoUnassignedShippingQuantity);
        });

        //  Shipping Group Device 4: Phone + No Area Code and DL, shipping quantity = ALL available devices
        step("9. Assign " + phoneThree.name + " device to the new Shipping Group (with no area codes), " +
                "and check that there are no devices left (on the left side)", () -> {
            var shippingDevice = shippingPage.getShippingDevice(phoneThree.productName, EMPTY_STRING);
            shippingPage.assignDeviceToShippingGroup(shippingDevice, shippingAddressFormatted, shippingGroupAddress.shipAttentionTo);

            shippingDevice.getSelf().shouldNot(exist);
        });

        step("10. Save changes on the Shipping tab", () -> {
            shippingPage.saveChanges();

            shippingPage.getShippingGroup(shippingAddressFormatted, shippingGroupAddress.shipAttentionTo)
                    .getAllShippingDevicesNames()
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(dlUnlimitedPhoneThreeAreaCodeSeven.phone.name,
                            dlBasicPhoneOneAreaCodeTwo.phone.name, phoneTwo.name, phoneThree.name), ofSeconds(30));
        });

        step("11. Check values of CustomAddressAssignments__c.CustomAddress__r.Name, " +
                "CustomAddressAssignments__c.QuoteLineItem__r.Display_Name__c, AssignmentLineItem__r.AreaCode__r.Country__c, " +
                "CustomAddressAssignments__c.Quantity__c and AssignmentLineItem__r.AreaCode__r.Area_Code__c", () -> {
            var customAddressAssignments = enterpriseConnectionUtils.query(
                    "SELECT Id, CustomAddress__r.Name, QuoteLineItem__r.Display_Name__c," +
                            "AssignmentLineItem__r.AreaCode__r.Country__c, AssignmentLineItem__r.AreaCode__r.Area_Code__c, " +
                            "Quantity__c " +
                            "FROM CustomAddressAssignments__c " +
                            "WHERE QuoteLineItem__r.QuoteId = '" + wizardPage.getSelectedQuoteId() + "'",
                    CustomAddressAssignments__c.class);
            assertThat(customAddressAssignments.size())
                    .as("Number of created CustomAddressAssignments__c records")
                    .isEqualTo(expectedNumberOfCustomAddressAssignments);

            step("Check the CustomAddressAssignments__c for the Phone + Area Code + DL, with shipping quantity < max quantity)", () -> {
                checkCustomAddressAssignments(customAddressAssignments,
                        dlUnlimitedPhoneThreeAreaCodeSeven.phone.name, dlUnlimitedPhoneThreeAreaCodeSeven.areaCode,
                        dlUnlimitedPhoneThreeAreaCodeSeven.shippingQuantity);
            });

            step("Check the CustomAddressAssignments__c for the Phone + Area Code + DL, with shipping quantity = max quantity)", () -> {
                checkCustomAddressAssignments(customAddressAssignments,
                        dlBasicPhoneOneAreaCodeTwo.phone.name, dlBasicPhoneOneAreaCodeTwo.areaCode,
                        dlBasicPhoneOneAreaCodeTwo.shippingQuantity);
            });

            step("Check the CustomAddressAssignments__c for the Phone + No Area Code and DL, with shipping quantity < max quantity)", () -> {
                checkCustomAddressAssignments(customAddressAssignments,
                        phoneTwo.name, null, phoneTwoUnassignedShippingQuantity);
            });

            step("Check the CustomAddressAssignments__c for the Phone + No Area Code and DL, with shipping quantity = max quantity)", () -> {
                checkCustomAddressAssignments(customAddressAssignments,
                        phoneThree.name, null, phoneThreeUnassignedTotalQuantity);
            });
        });
    }

    /**
     * Assign a device to the shipping group, and check its default (max) quantity.
     * Set shipping quantity less than max quantity, and check the unassigned device on the left side.
     *
     * @param phone            phone to assign to the Shipping Group
     * @param areaCode         {@code AreaCode} test data record with Area Code info (country, state, city, code)
     *                         associated with the phone (if it was assigned on the Price tab),
     *                         or {@code null} if some given phones were left unassigned (no area codes)
     * @param totalQuantity    total quantity of the given phone in the cart
     * @param shippingQuantity part of the total quantity for the given phone
     *                         that will be shipped via the shipping group
     */
    private void checkPartialShippingDeviceAssignment(Product phone, @Nullable AreaCode areaCode,
                                                      int totalQuantity, int shippingQuantity) {
        var shippingGroup = shippingPage.getShippingGroup(shippingAddressFormatted, shippingGroupAddress.shipAttentionTo);

        var shippingDevice = shippingPage.getShippingDevice(
                phone.productName,
                areaCode != null ? areaCode.fullName : EMPTY_STRING);
        shippingPage.assignDeviceToShippingGroup(shippingDevice, shippingAddressFormatted, shippingGroupAddress.shipAttentionTo);

        //  check the default shipping quantity (max available)
        shippingGroup.getAssignedDevice(phone.name)
                .getQuantityInput()
                .shouldHave(exactValue(valueOf(totalQuantity)));

        //  set up quantity over the max (=> impossible)
        shippingGroup.setQuantityForAssignedDevice(phone.name, totalQuantity + 1);
        shippingGroup.getAssignedDevice(phone.name)
                .getQuantityInput()
                .shouldHave(exactValue(valueOf(totalQuantity)));

        //  set up quantity less than the max (=> possible + some devices remain to the left)                 
        shippingGroup.setQuantityForAssignedDevice(phone.name, shippingQuantity);
        var expectedNumberOfDevicesLeft = valueOf(totalQuantity - shippingQuantity);
        shippingDevice.getNumberOfDevicesLeft().should(matchText(expectedNumberOfDevicesLeft + DEVICES_LEFT_REGEX));
    }

    /**
     * Find and check the {@code CustomAddressAssignments__c} records created after saving the state on the Shipping tab.
     *
     * @param customAddressAssignments all the available CustomAddressAssignments__c records to check
     * @param phoneName                phone's name as it appears on its related {@code QuoteLineItem}
     * @param areaCode                 {@code AreaCode} test data record with Area Code info (country, state, city, code)
     *                                 associated with the phone (if it was assigned on the Price tab),
     *                                 or {@code null} if some given phones were left unassigned (no area codes)
     * @param shippingQuantity         expected quantity that was set on the phone assigned to the given Shipping Group
     */
    private void checkCustomAddressAssignments(List<CustomAddressAssignments__c> customAddressAssignments,
                                               String phoneName, @Nullable AreaCode areaCode, int shippingQuantity) {
        var recordToCheck = customAddressAssignments.stream()
                .filter(caa -> caa.getQuoteLineItem__r() != null && caa.getQuoteLineItem__r().getDisplay_Name__c().equals(phoneName) &&
                        (areaCode != null
                                ? caa.getAssignmentLineItem__r() != null &&
                                caa.getAssignmentLineItem__r().getAreaCode__r() != null &&
                                doubleToIntToString(caa.getAssignmentLineItem__r().getAreaCode__r().getArea_Code__c()).equals(areaCode.code)
                                : caa.getAssignmentLineItem__r() == null
                        ))
                .findFirst()
                .orElseThrow(() -> new AssertionError(format("CustomAddressAssignments__c record is not found " +
                        "for phone = '%s', area code = '%s', shipping quantity = '%s'!", phoneName, areaCode, shippingQuantity)));

        assertThat(recordToCheck.getQuoteLineItem__r().getDisplay_Name__c())
                .as("CustomAddressAssignments__c.QuoteLineItem__r.Display_Name__c value")
                .isEqualTo(phoneName);
        assertThat(recordToCheck.getCustomAddress__r().getName())
                .as("CustomAddressAssignments__c.CustomAddress__r.Name value")
                //  e.g. 'United States, Ohio, 45840, Findlay, 3644 Cedarstone Drive'
                .isEqualTo(format("%s, %s, %s, %s, %s",
                        shippingGroupAddress.country, shippingGroupAddress.state.value,
                        shippingGroupAddress.zipCode, shippingGroupAddress.city, shippingGroupAddress.addressLine));

        if (areaCode != null) {
            assertThat(recordToCheck.getAssignmentLineItem__r().getAreaCode__r().getCountry__c())
                    .as("CustomAddressAssignments__c.AssignmentLineItem__r.AreaCode__r.Country__c value")
                    .isEqualTo(areaCode.country);
            assertThat(doubleToIntToString(recordToCheck.getAssignmentLineItem__r().getAreaCode__r().getArea_Code__c()))
                    .as("CustomAddressAssignments__c.AssignmentLineItem__r.AreaCode__r.Area_Code__c value")
                    .isEqualTo(areaCode.code);
        } else {
            assertThat(recordToCheck.getAssignmentLineItem__c())
                    .as("CustomAddressAssignments__c.AssignmentLineItem__c value")
                    .isNull();
        }

        assertThat(recordToCheck.getQuantity__c())
                .as("CustomAddressAssignments__c.Quantity__c value")
                .isEqualTo(shippingQuantity);
    }
}
