package com.aquiva.autotests.rc.internal.reporting;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDateTime;
import java.util.List;

import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

/**
 * Utility class for logging and reporting on results of service-type scripts,
 * e.g. creating NGBS accounts, verifying automated cases in TMS, etc...
 * <br/>
 * It contains methods to create and update file reports for script's results,
 * send results to console and Allure report.
 */
public class ServiceTaskLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTaskLogger.class);

    /**
     * Initialize a new file for writing scripts results.
     * The file can be found in the project's base folder
     * with a name like "file_name_prefix_[current_date_time].json".
     * <br/>
     * E.g. <b>"new_ngbs_users_2021-08-09T16:44:10.926334.json"</b>.
     *
     * @param fileNamePrefix prefix to use in the file name (e.g. "new_ngbs_users")
     * @return new empty results file
     * @throws IOException in case of an I/O error
     */
    public static File initializeAndGetResultsFile(String fileNamePrefix) throws IOException {
        var currentDateTimeFormatted = LocalDateTime.now().format(ISO_LOCAL_DATE_TIME);
        var fileName = String.format("%s_%s.json", fileNamePrefix, currentDateTimeFormatted);
        var resultsFile = new File(fileName);
        FileUtils.write(resultsFile, EMPTY_STRING, UTF_8);

        LOGGER.info("The results will be written to the file: " + resultsFile.getAbsolutePath());
        return resultsFile;
    }

    /**
     * Update the results file with a freshly processed data.
     *
     * @param file          results file to write
     * @param processedData list of processed data objects
     * @throws IOException in case of an I/O error
     */
    @Step("Update the results file with all the processed data")
    public static void updateResultsFile(File file, List<?> processedData) throws IOException {
        FileUtils.write(file, processedData.toString(), UTF_8);

        var latestData = processedData.get(processedData.size() - 1);
        var logMessage = "New item added to the results file: " + latestData;
        Allure.step(logMessage);
        LOGGER.info(logMessage);
    }

    /**
     * Add all the results of service script to the report (in the form of JSON file).
     * Should be invoked at the end of the task.
     *
     * @param resultsFile results file to write as *.json file
     * @throws IOException in case of an I/O error
     */
    public static void logResults(File resultsFile) throws IOException {
        logResults(resultsFile, "application/json", "json");

        LOGGER.info("Results file's contents: \n " + FileUtils.readFileToString(resultsFile, UTF_8));
    }

    /**
     * Add all the results of service script to the report.
     * Should be invoked at the end of the processing.
     *
     * @param resultsFile   results file to write
     * @param fileType      MIME type of the results file (e.g. "application/json")
     * @param fileExtension extension of the results file (e.g. "txt")
     * @throws IOException in case of an I/O error
     */
    public static void logResults(File resultsFile, String fileType, String fileExtension) throws IOException {
        var logMessage = "All data has been processed. Results can be found in the file: " +
                resultsFile.getAbsolutePath();
        Allure.step(logMessage);
        Allure.addAttachment(resultsFile.getName(), fileType,
                new ByteArrayInputStream(FileUtils.readFileToByteArray(resultsFile)), fileExtension);
        LOGGER.info(logMessage);
    }
}
