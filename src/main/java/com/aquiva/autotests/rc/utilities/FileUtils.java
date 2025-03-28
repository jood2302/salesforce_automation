package com.aquiva.autotests.rc.utilities;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.aquiva.autotests.rc.utilities.JsonUtils.findValuesForKeyInJsonObject;
import static com.aquiva.autotests.rc.utilities.JsonUtils.isJsonObject;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;

/**
 * Utility class that provides methods for working with files and attaching it to SObjects.
 * <p></p>
 * Helpful for test cases that need to work with files, e.g. attach it to SObject.
 */
public class FileUtils {

    //  Default resource location for attachments
    private static final String ATTACHMENTS_RESOURCE_PATH = "attachment";
    //  Directory with JSON files with the test data
    private static final String DATA_RESOURCE_PATH = "data";

    /**
     * Return the standard File object from the given file name
     * <i>located in the attachment's resource folder.</i>
     *
     * @param fileName path to file (should be in the src/test/resources/attachment folder!)
     * @return File instance from the path to resource file
     */
    public static File getAttachmentResourceFile(String fileName) {
        var filePath = ATTACHMENTS_RESOURCE_PATH + "/" + fileName;
        var fileURL = FileUtils.class.getClassLoader().getResource(filePath);
        if (fileURL != null) {
            try {
                return new File(fileURL.toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Unable to read a resource file at '" + filePath + "'. " +
                    "The resource is NOT found in the project directory!");
        }
    }

    /**
     * Retrieves a list of Billing IDs from JSON files with test data.
     * <br/><br/>
     * This method walks through the test data directory, processes each JSON file,
     * and extracts Billing IDs from them.
     *
     * @return a list of Billing IDs obtained from the JSON files
     * @throws URISyntaxException if the URL for the test data directory
     *                            is not formatted strictly according to RFC2396
     *                            and cannot be converted to a URI
     * @throws RuntimeException   if an I/O error is thrown when accessing JSON files
     */
    public static List<String> getBillingIdsFromTestDataFiles() throws URISyntaxException {
        var billingIds = new ArrayList<String>();
        var dataFolderURI = Objects.requireNonNull(FileUtils.class.getClassLoader().getResource(DATA_RESOURCE_PATH)).toURI();
        var billingIdKeyName = "billingId";

        try (var walk = Files.walk(Paths.get(dataFolderURI))) {
            walk.toList().parallelStream().forEach(path -> {
                try {
                    var isJsonFile = path.toString().toLowerCase().endsWith(".json");
                    var fileContent = Files.isRegularFile(path) ? Files.readString(path) : EMPTY_STRING;
                    if (isJsonFile && isJsonObject(fileContent) && fileContent.contains(billingIdKeyName)) {
                        findValuesForKeyInJsonObject(billingIdKeyName, new JSONObject(fileContent), billingIds);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error occurred while reading a JSON file and processing Billing IDs", e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while reading JSON files and processing Billing IDs", e);
        }

        return billingIds;
    }
}
