package com.aquiva.autotests.rc.model.ags;

import com.aquiva.autotests.rc.model.DataModel;
import com.aquiva.autotests.rc.utilities.ags.AGSRestApiClient;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object for account information retrieved from AGS.
 * <p></p>
 * Useful data structure for parsing responses from AGS
 * (see {@link AGSRestApiClient} for a reference).
 */
@JsonInclude(value = NON_NULL)
public class AccountAgsDTO extends DataModel {
    public List<Object> forwardingNumbers;
    public String externalBillingId;
    public String signupCoockie;
    public List<Object> devices;
    public List<Object> digitalLines;
    public String mainPhoneNumber;
    public String rcUserId;
    public List<MailboxesItem> mailboxes;
    public List<PhoneNumbersItem> phoneNumbers;
    public Long jobId;
    public String password;
    public Integer tierId;
    public String scenario;
    public Integer brandId;
    public Integer id;
    public String partnerId;
    public String subset;
    public Long sysMailboxId;
    public String status;

    /**
     * Get billing ID for existing business account after account generation via AGS.
     *
     * @return billing ID for further usage in tests and elsewhere (e.g. "3551258002")
     */
    public String getAccountBillingId() {
        return externalBillingId.split("\\|")[0];
    }

    /**
     * Get package ID for existing business account after account generation via AGS.
     *
     * @return billing ID for further usage in tests and elsewhere (e.g. "3531377002")
     */
    public String getAccountPackageId() {
        return externalBillingId.split("\\|")[1];
    }

    /**
     * Inner data structure for Account data object.
     * Represents user's personal data: name, email, password, etc...
     */
    @JsonInclude(value = NON_NULL)
    public static class MailboxesItem extends DataModel {
        public String ivrPin;
        public String firstName;
        public String lastName;
        public Integer extensionTypeId;
        public String password;
        public Integer accessLevelId;
        public String pin;
        public Long rcMailboxId;
        public Integer id;
        public String email;
    }

    /**
     * Inner data structure for Account data object.
     * Represents user's phone data: area code, phone number, etc...
     */
    @JsonInclude(value = NON_NULL)
    public static class PhoneNumbersItem extends DataModel {
        public Integer paymentTypeId;
        public String phoneNumber;
        public String mailboxId;
        public Long rcPhoneNumberId;
        public Integer phoneTypeId;
        public Integer id;
        public String label;
    }
}