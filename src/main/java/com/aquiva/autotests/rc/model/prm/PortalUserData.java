package com.aquiva.autotests.rc.model.prm;

import com.aquiva.autotests.rc.model.DataModel;

import static com.aquiva.autotests.rc.utilities.Constants.PRM_PASSWORD;
import static com.aquiva.autotests.rc.utilities.Constants.QA_SANDBOX_NAME;

/**
 * Data object for Portal User's data.
 * Contains a data related to the User and its Contact,
 * e.g. username, profile, role, etc.
 */
public class PortalUserData extends DataModel {
    //  for filenames with user's test data (see data/prm)
    public static final String IGNITE_PORTAL = "ignite";

    //	for 'hierarchy'
    public static final String PARTNER_HIERARCHY_LEVEL = "Partner Level";
    public static final String IGNITE_TOP_HIERARCHY_LEVEL = "Ignite Top Level";

    //	for 'persona'
    public static final String PARTNER_FULL_ACCESS_PERSONA = "Partner - Full Access";
    public static final String PARTNER_LIMITED_ACCESS_PERSONA = "Partner - Limited Access";

    public String hierarchy;
    public String username;
    public String password;
    public String contactName;
    public String profile;
    public String role;
    public String accountName;
    public Integer partnerId;
    public String persona;

    /**
     * Get the username value for the PRM Portal on the current sandbox.
     */
    public String getUsernameSandbox() {
        return username.endsWith("." + QA_SANDBOX_NAME)
                ? username
                : username + "." + QA_SANDBOX_NAME;
    }

    /**
     * Get the user's password to access the PRM Portal.
     */
    public String getPassword() {
        return password == null || password.isBlank()
                ? PRM_PASSWORD
                : password;
    }
}
