package com.aquiva.autotests.rc.utilities.salesforce.sobjectutils;

import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.EmployeeHelper;
import com.sforce.soap.enterprise.sobject.*;
import com.sforce.ws.ConnectionException;
import org.json.JSONObject;

import java.util.*;

import static com.aquiva.autotests.rc.utilities.StringHelper.getStringListAsString;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

/**
 * Utility class that provides {@link User} objects.
 * <p>
 * All methods also create an {@link Employee__c} record for a User record in SFDC, if it doesn't exist yet.
 * </p>
 */
public class UserUtils {

    //  Profile Names
    public static final String SALES_REP_LIGHTNING_PROFILE = "Sales Rep - Lightning";
    public static final String DEAL_DESK_LIGHTNING_PROFILE = "Deal Desk Lightning";
    public static final String PROFESSIONAL_SERVICES_LIGHTNING_PROFILE = "Professional Services Lightning";
    public static final String CHANNEL_OPERATIONS_LIGHTNING_PROFILE = "Channel Operations - Lightning";
    public static final String SALES_ENGINEER_LIGHTNING_PROFILE = "Sales Engineer Lightning";
    public static final String SYSTEM_ADMINISTRATOR_PROFILE = "System Administrator";
    public static final String FINANCE_ADMIN_EU_RESTRICTED_PROFILE = "Finance Admin - EU Restricted";

    //  Permission Sets
    public static final String ALLOW_PROCESS_ORDER_WITHOUT_SHIPPING_PS = "AllowProcessOrderWithoutShipping";
    public static final String KYC_APPROVAL_EDIT_PS = "KYC_Approval_Edit";
    public static final String PROCESS_ENGAGE_LEGACY_PS = "Process_Engage_Legacy";
    public static final String RINGCENTRAL_WITH_VERIZON_PS = "RingCentral_with_Verizon";
    public static final String ENABLE_DOCUSIGN_CLM_ACCESS_PS = "EnableDocuSignCLMAccess";
    public static final String EDIT_STATUS_ON_QUOTE_PS = "Edit_Status_on_Quote";
    public static final String LBO_QUOTE_ENABLE_PS = "LBOQuoteEnable";
    public static final String EDIT_ACCOUNT_CONTACT_ROLE_PS = "EditAccountContactRole";
    public static final String EDIT_OPPORTUNITY_CONTACT_PS = "EditOpportunityContact";
    public static final String ALLOW_FULL_MRS_FOR_FSC_CALCULATION_FOR_EXISTING_CUSTOMERS_PS =
            "Allow_to_user_full_MRS_for_FSC_Calculation_for_Existing_Customers";
    public static final String UI_LESS_SIGN_UP_PREVIEW_FOR_ADMINS_PS = "UILessSignUpPreviewForAdmins";
    public static final String ENABLE_SUPER_USER_PROSERV_IN_UQT_PS = "EnableSuperUserProServInUQT";

    //  Feature Toggles
    public static final String ENABLE_PROMOTIONS_FT = "Enable_Promotions__c";
    public static final String ENABLE_MULTIPRODUCT_UNIFIED_BILLING_FT = "EnableMultiProductUnifiedBilling__c";
    public static final String PROSERV_IN_NGBS_FT = "ProServInNGBS__c";

    //  Group Names
    public static final String NON_GSP_GROUP = "Non GSP";
    public static final String BRIGHTSPEED_USERS_GROUP = "Brightspeed Users";
    public static final String OPTUS_USERS_GROUP = "Optus users";
    public static final String RISE_INTERNATIONAL_USERS_GROUP = "Rise International Users";
    public static final String COX_USERS_GROUP = "Cox Users";
    public static final String ZAYO_USERS_GROUP = "Zayo Users";
    public static final String RISE_AMERICA_USERS_GROUP = "Rise America Users";
    public static final String VERIZON_USERS_GROUP = "Verizon Users Group";
    public static final String FINANCE_INVOICE_APPROVAL_GROUP = "Finance Invoice Approval";
    public static final String TREASURY_CREDIT_CHECK_GROUP = "Treasury Credit Check";
    public static final String INVOICE_APPROVAL_QUEUE_GROUP = "Invoice Approval Queue";

    //  Special User's Full Names
    public static final String D_N_B_CREDIT_CHECK_USER_FULL_NAME = "D&B Credit Check";

    //  Segments
    public static final String SOHO_SEGMENT = "SOHO";

    private static final String DEFAULT_COMMON_FEATURE_TOGGLES = "" +
            "{" +
            "\"" + ENABLE_MULTIPRODUCT_UNIFIED_BILLING_FT + "\": true, " +
            "\"" + PROSERV_IN_NGBS_FT + "\": false" +
            "}";

    /**
     * Enterprise connection utility to interact with SFDC API.
     */
    private static final EnterpriseConnectionUtils CONNECTION_UTILS = EnterpriseConnectionUtils.getInstance();

    /**
     * Entry point in getting a test User record with the given criteria.
     *
     * @return builder instance to start constructing SOQL query
     * @see UserQueryBuilder#execute
     */
    public static UserQueryBuilder getUser() {
        return new UserQueryBuilder();
    }

    /**
     * Main Query Builder/Executor.
     * <br/>
     * Build your SOQL query to get a user using the methods of this class,
     * terminating the chain with {@link UserQueryBuilder#execute()} method.
     */
    public static class UserQueryBuilder {
        private final List<String> filters = new ArrayList<>();
        /**
         * Feature toggles that are overridden by the user in the initial query.
         * These FTs will override all the "common" FTs.
         *
         * @see #DEFAULT_COMMON_FEATURE_TOGGLES
         */
        private final Map<String, Boolean> overriddenFeatureToggles = new HashMap<>();

        private String limit;

        /**
         * Add a filter to get a user with given profile's name.
         *
         * @param profileName any valid existing profile name (e.g. "Sales Rep - Lightning")
         */
        public UserQueryBuilder withProfile(String profileName) {
            filters.add("Profile.Name = '" + profileName + "'");
            return this;
        }

        /**
         * Add a filter to get a user that has one of the given full names.
         *
         * @param userFullNames list of the user's full names (e.g. ["John Smith", "Jane Doe"])
         */
        public UserQueryBuilder withFullNames(List<String> userFullNames) {
            filters.add("Name IN " + getStringListAsString(userFullNames));
            return this;
        }

        /**
         * Add a filter to get a user with the given Username.
         *
         * @param username username of the User (e.g. "jsmith@ringcentral.com.gci")
         */
        public UserQueryBuilder withUsername(String username) {
            filters.add("Username = '" + username + "'");
            return this;
        }

        /**
         * Add a filter to get a user that has a given permission set.
         *
         * @param permissionSetName permission set's name on the user (e.g. "RingCentral_with_Verizon")
         */
        public UserQueryBuilder withPermissionSet(String permissionSetName) {
            filters.add("Id IN " +
                    "(SELECT AssigneeId " +
                    "FROM PermissionSetAssignment " +
                    "WHERE PermissionSet.Name = '" + permissionSetName + "' " +
                    ")");
            return this;
        }

        /**
         * Add a filter to get a user that does NOT have a given permission set.
         *
         * @param permissionSetName permission set's name on the user (e.g. "RingCentral_with_Verizon")
         */
        public UserQueryBuilder withoutPermissionSet(String permissionSetName) {
            filters.add("Id NOT IN " +
                    "(SELECT AssigneeId " +
                    "FROM PermissionSetAssignment " +
                    "WHERE PermissionSet.Name = '" + permissionSetName + "' " +
                    ")");
            return this;
        }

        /**
         * Add a filter to get a user that has enabled feature toggles (set to true).
         *
         * @param featureToggleNames list of <b>enabled</b> feature toggle's for the user
         *                           (e.g. ["EnableUILessProcessOrderMVP__c", "EnableUILessProcessOrderEngage__c"])
         * @see Feature_Toggle__c
         */
        public UserQueryBuilder withFeatureToggles(List<String> featureToggleNames) {
            var featureToggleMapping = featureToggleNames
                    .stream()
                    .collect(toMap(ftNameAsKey -> ftNameAsKey, ftValue -> true));
            return withFeatureToggles(featureToggleMapping);
        }

        /**
         * Add a filter to get a user that has feature toggles set up according to the given mapping.
         *
         * @param featureToggles feature toggle's mapping for the user
         *                       (e.g.
         *                       {
         *                       "EnableUILessProcessOrderMVP__c": false,
         *                       "EnableUILessProcessOrderEngage__c": true
         *                       })
         * @see Feature_Toggle__c
         */
        public UserQueryBuilder withFeatureToggles(Map<String, Boolean> featureToggles) {
            overriddenFeatureToggles.putAll(featureToggles);
            
            /*
            - "ToggleName1__c = true"
            - "ToggleName1__c = true AND ToggleName2__c = false"
            - "ToggleName1__c = true AND ToggleName2__c = false AND ToggleName3__c = true"
            etc.
            */
            var featureToggleFilter = featureToggles.entrySet().stream()
                    .map(ft -> ft.getKey() + " = " + ft.getValue())
                    .collect(joining(" AND "));
            if (!featureToggleFilter.isBlank()) {
                filters.add("Id IN " +
                        "(SELECT SetupOwnerId " +
                        "FROM Feature_Toggle__c " +
                        "WHERE " + featureToggleFilter + " " +
                        ")");
            }
            return this;
        }

        /**
         * Add a filter to get a user that has one of the IDs of the given Users.
         *
         * @param targetUsers list of Users to get IDs for filtering
         */
        public UserQueryBuilder withUserIds(List<User> targetUsers) {
            var targetUsersIds = targetUsers.stream()
                    .map(User::getId)
                    .collect(joining("','", "('", "')"));
            if (!targetUsersIds.isBlank()) {
                filters.add("Id IN " + targetUsersIds);
            }
            return this;
        }

        /**
         * Add a filter to get a user that does NOT have a given custom permissions set.
         *
         * @param customPermissionApiName custom permission's API Name (e.g. "Can_Sync_With_NGBS")
         */
        public UserQueryBuilder withoutCustomPermission(String customPermissionApiName) throws ConnectionException {
            var setupEntityAccessList = CONNECTION_UTILS.query(
                    "SELECT Id, ParentId " +
                            "FROM SetupEntityAccess " +
                            "WHERE SetupEntityId IN (" +
                            "SELECT Id " +
                            "FROM CustomPermission " +
                            "WHERE DeveloperName = '" + customPermissionApiName + "')",
                    SetupEntityAccess.class);

            var permissionSetIdList = setupEntityAccessList
                    .stream()
                    .map(SetupEntityAccess::getParentId)
                    .collect(joining("','", "('", "')"));
            if (!permissionSetIdList.isBlank()) {
                filters.add("Id NOT IN (" +
                        "SELECT AssigneeId " +
                        "FROM PermissionSetAssignment " +
                        "WHERE PermissionSetId IN " + permissionSetIdList + " " +
                        ")");
            }
            return this;
        }

        /**
         * Add a filter to get a user that is a part of given Public Group.
         *
         * @param groupName public group's name that the user belongs to (e.g. "Verizon Users Group")
         */
        public UserQueryBuilder withGroupMembership(String groupName) {
            filters.add("Id IN " +
                    "(SELECT UserOrGroupId " +
                    "FROM GroupMember " +
                    "WHERE Group.Name = '" + groupName + "' " +
                    ")");
            return this;
        }

        /**
         * Add a filter to get a user that is either GSP or non-GSP User.
         * <br/>
         * Might be valuable to bypass certain validation errors.
         *
         * @param isGspUser true to get GSP User, false to get non-GSP User
         */
        public UserQueryBuilder withGspUserValue(boolean isGspUser) {
            filters.add("GSP_User__c = " + isGspUser);
            return this;
        }

        /**
         * Set a LIMIT query parameter to limit the number of found users.
         *
         * @param limit any non-zero valid number (e.g. 1)
         */
        public UserQueryBuilder withLimit(int limit) {
            this.limit = String.valueOf(limit);
            return this;
        }

        /**
         * Add a filter to get a user from a given Segment.
         *
         * @param segment Segment's name (e.g. "SOHO")
         */
        public UserQueryBuilder withSegment(String segment) {
            filters.add("SegmentPicklist__c = '" + segment + "'");
            return this;
        }

        /**
         * Execute a resulting SOQL query to find all the User records for the previously given requirements.
         * <br/>
         * Note: This method starts a new query with a filter to get a user from one of the found ones (see {@link #withUserIds(List)}).
         * <br/>
         * Use this method as an "intermediate", if it's not possible to get a single, target User record with one query
         * (e.g. there are too many sub-selects, the query is too long, or any other SOQL violation).
         * <br/>
         * Make sure to follow it with any additional filters and terminate with {@link #execute()}.
         *
         * @throws ConnectionException in case of malformed query, DB or network errors.
         */
        public UserQueryBuilder and() throws ConnectionException {
            var users = getAllUsersWithCurrentFilter();
            filters.clear();

            return withUserIds(users);
        }

        /**
         * Return a SOQL query to find the User(s)
         * using the combined requirements (filters).
         */
        public String buildString() {
            var query = new StringBuilder()
                    .append("SELECT Id, FirstName, LastName, Name, PID__c, Email, Phone ")
                    .append("FROM User ")
                    .append("WHERE IsActive = true ")
                    .append("AND Email LIKE '%.invalid' "); //  to avoid using 'real' users

            if (!filters.isEmpty()) {
                query.append("AND ")
                        .append(join(" AND ", filters))
                        .append(" ");
            }

            if (limit != null && !limit.isBlank()) {
                query.append(format("LIMIT %s", limit));
            }

            return query.toString();
        }

        /**
         * Execute a resulting SOQL query to find a test User.
         * <br/>
         * Note: optionally, the method creates {@code Employee} record for the User, if it doesn't exist.
         *
         * @return Salesforce User with the given requirements
         * @throws ConnectionException in case of malformed query, DB or network errors.
         */
        public User execute() throws ConnectionException {
            var users = getAllUsersWithCurrentFilter();
            var user = getFinalUserWithCommonFilter(users);

            getEmployeeRecord(user);

            return user;
        }

        /**
         * Execute the SOQL query with the all the current filters
         * to find all the User records for it.
         *
         * @return list of the Users found in the current SOQL query with filters
         * @throws ConnectionException in case of malformed query, DB or network errors.
         */
        public List<User> getAllUsersWithCurrentFilter() throws ConnectionException {
            var usersQuery = buildString();
            return CONNECTION_UTILS.query(usersQuery, User.class);
        }

        /**
         * Apply a common filtering on the list of test Users.
         *
         * @param targetUsers list of the Users to filter
         * @return a single Salesforce User that complies with the common filter
         * @throws ConnectionException in case of malformed query, DB or network errors.
         */
        private User getFinalUserWithCommonFilter(List<User> targetUsers) throws ConnectionException {
            var ftSystemProperty = System.getProperty("sf.userCommonFeatureToggles");
            var featureTogglesNonEmpty = ftSystemProperty != null && !ftSystemProperty.isBlank()
                    ? ftSystemProperty
                    : DEFAULT_COMMON_FEATURE_TOGGLES;

            var featureTogglesMap = new HashMap<String, Boolean>();
            new JSONObject(featureTogglesNonEmpty).toMap().forEach((key, value) -> {
                if (overriddenFeatureToggles.get(key) == null) {
                    featureTogglesMap.put(key, (Boolean) value);
                }
            });

            var filteredUserQuery = new UserQueryBuilder()
                    .withUserIds(targetUsers)
                    .withFeatureToggles(featureTogglesMap)
                    .withLimit(1)
                    .buildString();
            return CONNECTION_UTILS.querySingleRecord(filteredUserQuery, User.class);
        }
    }

    /**
     * Get Employee record for the provided User.
     * This Employee record is necessary for non-admin SF users
     * to be able to create standard objects, like Opportunity.
     * <p></p>
     * Note: if an Employee record doesn't exist, then it's created using
     * provided User object.
     *
     * @param user Salesforce User to link to Employee record
     * @throws ConnectionException in case of malformed query, DB or network errors.
     */
    private static void getEmployeeRecord(User user) throws ConnectionException {
        var employeeList = CONNECTION_UTILS.query(
                "SELECT Id " +
                        "FROM Employee__c " +
                        "WHERE User__c = '" + user.getId() + "'",
                Employee__c.class);

        if (employeeList.isEmpty()) {
            var employee = new Employee__c();
            employee.setFirst_Name__c(user.getFirstName());
            employee.setLast_Name__c(user.getLastName());
            employee.setIs_Active__c(true);
            employee.setEmail__c(user.getEmail());
            employee.setSPID__c(user.getPID__c());
            employee.setUser__c(user.getId());
            EmployeeHelper.setDefaultFields(employee);

            CONNECTION_UTILS.insertAndGetIds(employee);
        }
    }
}
