package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.sforce.soap.tooling.metadata.RemoteSiteSetting;
import com.sforce.soap.tooling.sobject.RemoteProxy;
import com.sforce.ws.ConnectionException;

/**
 * Factory class for creating quick instances of {@link RemoteProxy} class.
 * <br/>
 * Note: RemoteProxy object represent an entity in the SFDC section 'Setup - Security - Remote Site Settings'.
 * <br/>
 * All factory methods also insert created objects into the SF database.
 */
public class RemoteProxyFactory extends SObjectFactory {

    /**
     * Create new RemoteProxy object and insert it into Salesforce via API.
     * <br/>
     * Note: RemoteProxy object represent an entity in the SFDC section 'Setup - Security - Remote Site Settings'.
     *
     * @param fullName    SiteName value to be set in the created RemoteProxy object
     * @param endpointUrl EndpointUrl value to be set in the created RemoteProxy object
     * @return RemoteProxy object with provided parameters and ID from Salesforce
     * @throws ConnectionException in case of malformed query, DB or network errors.
     */
    public static RemoteProxy createRemoteProxy(String fullName, String endpointUrl) throws ConnectionException {
        var remoteProxy = new RemoteProxy();
        var remoteSiteSetting = new RemoteSiteSetting();

        remoteProxy.setFullName(fullName);
        remoteSiteSetting.setDisableProtocolSecurity(false);
        remoteSiteSetting.setIsActive(true);
        remoteSiteSetting.setUrl(endpointUrl);
        remoteSiteSetting.setDescription(fullName);
        remoteProxy.setMetadata(remoteSiteSetting);

        TOOLING_CONNECTION_UTILS.insertAndGetIds(remoteProxy);

        return remoteProxy;
    }
}
