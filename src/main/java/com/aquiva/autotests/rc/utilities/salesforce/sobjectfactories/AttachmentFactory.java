package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.sforce.soap.enterprise.sobject.*;
import org.apache.commons.io.FileUtils;

import static com.aquiva.autotests.rc.utilities.FileUtils.getAttachmentResourceFile;

/**
 * Factory class for quick creation of {@link Attachment} object instances.
 * <br/>
 * This class is used to add file attachments to Salesforce records (as Attachments).
 * All factory methods also insert created objects into the SF database.
 * <br/><br/>
 * Note: {@link Attachment} is a legacy object!
 * It should not be mistaken with new {@link ContentVersion}/{@link ContentDocument} objects!
 * {@link Attachment} is for 'Notes & Attachments' related list only,
 * while {@link ContentVersion}/{@link ContentDocument} are for 'Files' related list,
 * and (as of Summer'21 edition) it duplicates the attachment to 'Notes & Attachments' list as well
 * (but not to the {@link Attachment} object).
 */
public class AttachmentFactory extends SObjectFactory {

    /**
     * Create a new {@link Attachment} record and insert it into Salesforce via API.
     * The new record will be attached to the given SObject in Salesforce.
     *
     * @param sObject  Salesforce Object for which an attachment is created
     * @param fileName name of a file that's about to be attached to the SObject
     *                 (should be in the test resources' attachment folder!)
     * @throws Exception in case of I/O exception, incorrectly formatted URL from file path or errors while accessing API
     */
    public static Attachment createAttachmentForSObject(SObject sObject, String fileName) throws Exception {
        var file = getAttachmentResourceFile(fileName);
        var fileAsBytes = FileUtils.readFileToByteArray(file);

        var attachment = new Attachment();
        attachment.setParentId(sObject.getId());
        attachment.setName(fileName);
        attachment.setBody(fileAsBytes);

        CONNECTION_UTILS.insertAndGetIds(attachment);

        return attachment;
    }
}
