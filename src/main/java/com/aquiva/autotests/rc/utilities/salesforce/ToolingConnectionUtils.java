package com.aquiva.autotests.rc.utilities.salesforce;

import com.sforce.soap.tooling.ToolingConnection;
import com.sforce.soap.tooling.sobject.SObject;
import com.sforce.ws.ConnectionException;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Utility class that encapsulates the logic for working with SFDC using Tooling connection SOAP API.
 * The main usage for Tooling Connection is to invoke Apex scripts via API calls.
 * <p></p>
 * The class is designed using Singleton pattern
 * to be the "single point of contact" for users of Tooling connection API.
 */
public class ToolingConnectionUtils {

    //  Single instance of the class
    private static final ToolingConnectionUtils INSTANCE = new ToolingConnectionUtils();

    //  Connection object for accessing SFDC via SOAP API
    private final ToolingConnection toolingConnection;

    /**
     * Class constructor.
     * Contains the logic to establish an Enterprise connection with SFDC via SOAP API
     * and obtain a corresponding connection object.
     */
    private ToolingConnectionUtils() {
        try {
            toolingConnection = ConnectionFactory.getDefaultToolingConnection();
        } catch (ConnectionException e) {
            throw new RuntimeException("Unable to create tooling connection! Details: " + e, e);
        }
    }

    /**
     * Get the instance of ToolingConnectionUtils object.
     * Useful for classes that contain Tooling API connection's operations logic.
     *
     * @return instance of ToolingConnectionUtils with the current session's data.
     */
    public static ToolingConnectionUtils getInstance() {
        return INSTANCE;
    }

    /**
     * Execute the provided Apex code.
     * <p></p>
     * Note: Apex code is run using SF user that authenticated via Tooling connection
     * (see {@link ConnectionFactory#getDefaultToolingConnection()}).
     *
     * @param apexSnippet string with Apex code to invoke via API
     * @throws ConnectionException in case of errors while accessing API
     * @throws RuntimeException    if Apex code fails to compile, or fails at runtime
     */
    public void executeAnonymousApex(String apexSnippet) throws ConnectionException {
        var executeAnonymousResult = toolingConnection.executeAnonymous(apexSnippet);

        if (!executeAnonymousResult.getSuccess()) {
            if (!executeAnonymousResult.getCompiled()) {
                throw new RuntimeException("The provided Apex code failed to compile! " +
                        "Code: \n" + apexSnippet + "\n" +
                        "Compilation error: " + executeAnonymousResult.getCompileProblem());
            } else {
                throw new RuntimeException("The provided Apex code failed to run successfully! " +
                        "Code: \n" + apexSnippet + "\n" +
                        "Exception message: " + executeAnonymousResult.getExceptionMessage() + "\n" +
                        "Exception stacktrace: " + executeAnonymousResult.getExceptionStackTrace());
            }
        }
    }

    /**
     * Insert the provided tooling SObject(-s) into the Salesforce DB and return the list of its/their resulting ID(-s).
     * <p></p>
     * This method also assigns resulting IDs to their corresponding provided tooling SObjects.
     *
     * @param objects array of tooling SObjects (or a single SObject) to insert into the database
     * @return list of the IDs for all provided SObjects after inserting them into the database
     * @throws ConnectionException in case of errors while accessing API
     */
    public List<String> insertAndGetIds(SObject... objects) throws ConnectionException {
        var insertResult = SalesforceUtils.create(toolingConnection, objects);

        for (int i = 0; i < insertResult.size(); i++) {
            objects[i].setId(insertResult.get(i));
        }

        return insertResult;
    }

    /**
     * Query the Salesforce database with the provided SOQL expression and
     * map the result to the provided tooling SObject's type.
     *
     * <pre><code class='java'>
     * var connectionUtils = ToolingConnectionUtils.getInstance();
     * var contacts = connectionUtils.query(
     *         "SELECT ID, EndpointUrl " +
     *         "FROM RemoteProxy " +
     *         "WHERE SiteName != null",
     *     RemoteProxy.class);
     * </code>
     *
     * Then, these 'RemoteProxy's' might look like this:
     * - RemoteProxy{Id='0031k00000bhPQeAAM', EndpointUrl='http://example.com'}
     * - RemoteProxy{Id='0031k00000bWaLcAAK', EndpointUrl='https://test.com'}
     * - etc...
     * </pre>
     *
     * <b> Note: all resulting objects will only contain values in the queried fields ("SELECT" part)!
     * Non-null (in the DB) fields that weren't queried will remain null on the Java object!
     * </b>
     *
     * @param queryString SOQL expression that queries one or several fields for any SObject
     *                    (e.g. <i>"SELECT ID, EndpointUrl FROM RemoteProxy WHERE SiteName != null"</i>)
     * @param valueType   any valid standard tooling SObject type
     *                    (e.g. RemoteProxy...)
     * @return list of tooling SObjects that were found using the provided query
     * @throws ConnectionException in case of errors while accessing API
     */
    public <T extends SObject> List<T> query(String queryString, Class<T> valueType) throws ConnectionException {
        var records = toolingConnection.query(queryString).getRecords();

        return Arrays.stream(records)
                .map(valueType::cast)
                .collect(toList());
    }
}
