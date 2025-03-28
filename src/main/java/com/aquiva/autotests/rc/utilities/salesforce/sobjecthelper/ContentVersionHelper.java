package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.sforce.soap.enterprise.sobject.ContentDocument;
import com.sforce.soap.enterprise.sobject.ContentVersion;
import com.sforce.ws.ConnectionException;

/**
 * Helper class to facilitate operations on {@link ContentVersion} objects.
 */
public class ContentVersionHelper extends SObjectHelper {

    /**
     * Get {@code ContentVersion.ContentDocumentId} value on the given file attachment.
     * <br/>
     * This field is populated automatically after the file is uploaded to the database,
     * but isn't returned via API "create" call.
     * That's why it's necessary to query for it separately.
     *
     * @param contentVersion SFDC file attachment to get a field's value
     * @return ID for the related {@link ContentDocument} object.
     * @throws ConnectionException in case of errors while accessing API
     */
    public static String getContentDocumentIdOnFile(ContentVersion contentVersion) throws ConnectionException {
        var contentVersionWithContentDocumentId = CONNECTION_UTILS.querySingleRecord(
                "SELECT ContentDocumentId " +
                        "FROM ContentVersion " +
                        "WHERE Id = '" + contentVersion.getId() + "'",
                ContentVersion.class);

        return contentVersionWithContentDocumentId.getContentDocumentId();
    }
}
