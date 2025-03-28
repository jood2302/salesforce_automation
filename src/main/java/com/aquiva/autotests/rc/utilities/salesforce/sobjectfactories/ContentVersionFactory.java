package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.sforce.soap.enterprise.sobject.*;
import org.apache.commons.io.FileUtils;

import static com.aquiva.autotests.rc.utilities.FileUtils.getAttachmentResourceFile;

/**
 * Factory class for creating quick instances of {@link ContentVersion} class.
 * <br/>
 * This class is used to add file attachments to Salesforce records (as Files).
 * All factory methods also insert created objects into the SF database.
 * <br/><br/>
 * Note: {@link ContentVersion} objects are READ-ONLY!
 * In order to <b>delete</b> the created attachment from SFDC, delete a parent {@link ContentDocument} object.
 * Its ID is in {@link ContentVersion#getContentDocumentId()}.
 * It will delete ALL versions of the file if there's more than one.
 * <br/><br/>
 * Note: not to be mistaken with legacy {@link Attachment} object!
 * {@link Attachment} is for 'Notes & Attachments' related list only,
 * while {@link ContentVersion}/{@link ContentDocument} are for 'Files' related list,
 * and (as of Summer'21 edition) it duplicates the attachment to 'Notes & Attachments' list as well
 * (but not to the {@link Attachment} object).
 */
public class ContentVersionFactory extends SObjectFactory {
    //  For 'ContentLocation' field
    private static final String SALESFORCE_CONTENT_LOCATION = "S";

    /**
     * Create a new file attachment and add it to the given SObject.
     * This method inserts the record into SFDC,
     * and the file attachment shows up on the record
     * in the 'Files' and 'Notes & Attachments' related lists.
     *
     * @param sObjectId unique Salesforce record Id (e.g. "0061900000BXZNgAAP")
     * @param fileName  name of the actual file in the test "resources/attachment" folder
     * @return ContentVersion object with default parameters and ID from Salesforce
     * @throws Exception in case of I/O exceptions, DB or network errors.
     */
    public static ContentVersion createAttachmentForSObject(String sObjectId, String fileName) throws Exception {
        var file = getAttachmentResourceFile(fileName);

        var contentVersion = new ContentVersion();
        contentVersion.setContentLocation(SALESFORCE_CONTENT_LOCATION);
        contentVersion.setPathOnClient(file.getName());
        contentVersion.setTitle(file.getName());
        contentVersion.setFirstPublishLocationId(sObjectId);

        var fileAsBytes = FileUtils.readFileToByteArray(file);
        contentVersion.setVersionData(fileAsBytes);

        CONNECTION_UTILS.insertAndGetIds(contentVersion);

        return contentVersion;
    }
}
