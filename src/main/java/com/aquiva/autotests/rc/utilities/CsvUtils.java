package com.aquiva.autotests.rc.utilities;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class that provides methods for working with CSV files.
 * <p></p>
 * Helpful for test cases that need to check the functionality
 * against a set of related test data values (parameterized tests).
 */
public class CsvUtils {
    private static final String DEFAULT_DELIMITER = ",";
    private static final int DEFAULT_LINES_TO_SKIP = 1;

    /**
     * Parse CSV file as a standard Java collection of Lists of Strings.
     * <p></p>
     * This method works with CSV files:
     * <p> - with first line as headers (value names) </p>
     * <p> - data separator as "," (comma) </p>
     *
     * @param filePath relative path to a test data resource
     *                 (e.g. "data/ngbs/discount_data.csv")
     * @return collection of lists of Strings for reference as test data.
     * @see CsvUtils#parseCsvAsList(String filePath, String delimiter, int linesToSkip)
     */
    public static List<List<String>> parseCsvAsList(String filePath) {
        return parseCsvAsList(filePath, DEFAULT_DELIMITER, DEFAULT_LINES_TO_SKIP);
    }

    /**
     * Parse CSV file as a standard Java collection of Lists of Strings.
     * <p></p>
     * Tests that use this method for their test data parsing should iterate over that list and check values
     * using whatever tools they have.
     * Pattern to access data in such collection should be as follows:
     * <p></p>
     * CSV file:
     * <pre>
     *
     * price,discount,expected
     * 10,10,9
     * 100,0,100
     * 5,100,0
     *
     * </pre>
     * <p>
     * Result collection:
     *
     * <pre><code class='java'>
     * // {{10,10,9}, {100,0,100}, {5,100,0}}
     * var resultCollection = parseCsvAsList("path/to/test/data", 1);
     * </code></pre>
     * <p>
     * Then to get <i>price, discount and expected</i> variables user should use indexes:
     *
     * <pre><code class='java'>
     * for (var testCase : resultCollection) {
     *     var price = testCase.get(0);
     *     var discount = testCase.get(1);
     *     var expected = testCase.get(2);
     *
     *     // checks goes here...
     * }
     * </code></pre>
     *
     * @param filePath    relative path to a test data resource
     *                    (e.g. "data/ngbs/discount_data.csv")
     * @param delimiter   delimiter that separates data in the file
     *                    (e.g. ",", ";", etc...)
     * @param linesToSkip number of lines to skip in CSV file.
     *                    Typically, it is '1' (first line is for data headers).
     * @return collection of lists of Strings for reference as test data.
     */
    public static List<List<String>> parseCsvAsList(String filePath, String delimiter, int linesToSkip) {
        List<List<String>> csvList = new ArrayList<>();

        try {
            String absoluteFilePath = CsvUtils.class.getClassLoader().getResource(filePath).toURI().getPath();

            BufferedReader br = new BufferedReader(
                    new FileReader(absoluteFilePath));

            String line;
            while ((line = br.readLine()) != null) {
                if (linesToSkip == 0) {
                    String[] valuesArr = line.split(delimiter);
                    List<String> valuesList = Arrays.stream(valuesArr)
                            .map(value -> value = value.strip())
                            .collect(Collectors.toList());

                    csvList.add(valuesList);
                } else {
                    linesToSkip--;
                }
            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }

        return csvList;
    }
}
