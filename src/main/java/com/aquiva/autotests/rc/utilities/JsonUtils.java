package com.aquiva.autotests.rc.utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.fasterxml.jackson.module.mrbean.MrBeanModule;
import org.apache.commons.io.IOUtils;
import org.json.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.lang.String.format;

/**
 * Utility class for working with JSON files and strings.
 * <p></p>
 * This class contains various useful methods for:
 * <p> - serialization of Java objects into JSON strings/files </p>
 * <p> - deserialization of JSON strings/files into Java objects </p>
 * <p></p>
 * Useful for parsing test data into Java objects; parsing REST API requests/responses, etc...
 */
public class JsonUtils {
    /**
     * Main object mapper.
     * Used for both serialization and deserialization.
     */
    private static final ObjectMapper MAPPER;

    //  Error message patterns
    private static final String CONFIG_READ_ERROR = "Unable to read configuration '%s'. \n Error details: %s";
    private static final String JSON_READ_ERROR = "Unable to read JSON file: '%s'. \n Error details: %s";
    private static final String OBJECT_WRITE_ERROR = "Unable to write JSON file for object: '%s'. \n Error details: %s";
    private static final String NULL_PATH_ERROR = "Path to the JSON file is NULL!";

    static {
        MAPPER = new ObjectMapper();
        MAPPER.registerModule(new MrBeanModule());
        MAPPER.registerModule(new BlackbirdModule());
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Deserialize the given JSON file as the Java object.
     * This method searches JSON files in project's "resources" folders.
     * <p></p>
     * <b>Note: useful to locate and parse test data files
     * from <u>src/test/resources</u> folder</b>.
     *
     * @param filePathName a file path to the JSON file to deserialize
     *                     (inside the resources folder).
     *                     <p> E.g.: <i>"data/ngbs/newbusiness/RC_Meetings_Monthly_Contract.json"</i></p>
     *                     <p></p>
     * @param valueType    Java class as a target for deserialization.
     *                     Object of this class should be created as a result.
     *                     <p></p>
     * @param <T>          any valid type of the Java class that contains proper structure
     *                     (variables, constructor...) for mapping
     * @return Java object that encapsulates the content of JSON file
     * @see JsonUtils#readJson(String, Class)
     */
    public static <T> T readConfigurationResource(String filePathName, Class<T> valueType) {
        try {
            var fileURL = JsonUtils.class.getClassLoader().getResource(filePathName);
            if (fileURL != null) {
                return MAPPER.readValue(fileURL, valueType);
            } else {
                throw new RuntimeException(format(CONFIG_READ_ERROR, filePathName, NULL_PATH_ERROR));
            }
        } catch (IOException e) {
            throw new RuntimeException(format(CONFIG_READ_ERROR, filePathName, e.getMessage()), e);
        }
    }

    /**
     * Deserialize the given JSON file as the List of Java objects.
     * This method searches JSON files in project's "resources" folders.
     * <p> Helpful when incoming JSON file contains an array of similar objects. </p>
     * <p></p>
     * <b>Note: useful to locate and parse test data files
     * from <u>src/test/resources</u> folder</b>.
     *
     * @param filePathName a file path to the JSON file to deserialize
     *                     (inside the resources folder).
     *                     <p> E.g.: <i>"data/ngbs/newbusiness/RC_Meetings_Monthly_Contract.json"</i></p>
     *                     <p></p>
     * @param valueType    Java class as a target for deserialization.
     *                     Object of this class should be created as every element of the resulted list.
     *                     <p></p>
     * @param <T>          any valid type of the Java class that contains proper structure
     *                     (variables, constructor...) for mapping
     * @return List of Java objects that encapsulate the contents of JSON file
     * @see JsonUtils#readJson(String, Class)
     */
    public static <T> List<T> readConfigurationResourceAsList(String filePathName, Class<T> valueType) {
        try {
            var fileURL = JsonUtils.class.getClassLoader().getResource(filePathName);
            if (fileURL != null) {
                return MAPPER.readValue(fileURL, MAPPER.getTypeFactory().constructCollectionType(List.class, valueType));
            } else {
                throw new RuntimeException(format(CONFIG_READ_ERROR, filePathName, NULL_PATH_ERROR));
            }
        } catch (IOException e) {
            throw new RuntimeException(format(CONFIG_READ_ERROR, filePathName, e.getMessage()), e);
        }
    }

    /**
     * Read the given JSON file as a String object.
     * This method searches JSON files in project's "resources" folders.
     * <p></p>
     * <b>Note: useful to locate and parse any JSON files
     * from <u>src/test/resources</u> folder</b>.
     *
     * @param filePathName a file path to the JSON file to deserialize
     *                     (inside the resources folder).
     *                     <p> E.g.: <i>"mock/get-countries_response.json"</i></p>
     *                     <p></p>
     * @return String representation of the given JSON file
     */
    public static String readResourceAsString(String filePathName) {
        try {
            var fileURL = JsonUtils.class.getClassLoader().getResource(filePathName);
            if (fileURL != null) {
                return IOUtils.toString(fileURL, Charset.defaultCharset());
            } else {
                throw new RuntimeException(format(JSON_READ_ERROR, filePathName, NULL_PATH_ERROR));
            }
        } catch (IOException e) {
            throw new RuntimeException(format(JSON_READ_ERROR, filePathName, e.getMessage()), e);
        }
    }

    /**
     * Deserialize the given JSON string as the List of Java objects.
     * <p></p>
     * Helpful when incoming JSON string contains an array of similar objects.
     *
     * @param json      string representation of the contest of JSON file
     * @param valueType Java class as a target for deserialization.
     *                  Object of this class should be created as every element of the resulted list.
     * @param <T>       any valid type of the Java class that contains proper structure
     *                  (variables, constructor...) for mapping
     * @return List of Java objects that encapsulate the contents of JSON file
     * @see JsonUtils#readJson(String, Class)
     */
    public static <T> List<T> readJsonAsList(String json, Class<T> valueType) {
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory().constructCollectionType(List.class, valueType));
        } catch (IOException e) {
            throw new RuntimeException(format(JSON_READ_ERROR, json, e.getMessage()), e);
        }
    }

    /**
     * Deserialize the given JSON string as the Java object.
     * <p></p>
     * <p> For example, given class Car:</p>
     * <pre><code class='java'>
     * public class Car {
     *     public String brand;
     *     public int price;
     *
     *     public String toString() {
     *         return "Car{" +
     *         "brand='" + brand + "',"
     *         "price='" + price + "',"
     *         "}";
     *     }
     * }
     * </code></pre>
     * <p>
     * and JSON string:
     * <pre><code class='json'>
     * {
     *      "brand":"BMW",
     *      "price":40000
     * }
     * </code></pre>
     * Then with the following code for getting Car object:
     * <pre><code class='java'>
     * ObjectMapper mapper = new ObjectMapper();
     * Car carFromJson = mapper.readValue(jsonString, Car.class);
     *
     * // prints "Car{brand='BMW',price='4000'}"
     * System.out.println(carFromJson);
     * // prints "BMW"
     * System.out.println(carFromJson.brand);
     * // prints "4000"
     * System.out.println(carFromJson.price);
     * </code></pre>
     *
     * @param json      string representation of the contents of JSON file
     * @param valueType Java class as a target for deserialization.
     *                  Object of this class should be created as a result.
     * @param <T>       any valid type of the Java class that contains proper structure
     *                  (variables, constructor...) for mapping
     * @return Java object that encapsulates the contents of JSON file
     */
    public static <T> T readJson(String json, Class<T> valueType) {
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory().constructType(valueType));
        } catch (IOException e) {
            throw new RuntimeException(format(JSON_READ_ERROR, json, e.getMessage()), e);
        }
    }

    /**
     * Serialize a given Java Object into JSON string.
     * <p></p>
     * <p> For example, given class Car:</p>
     * <pre><code class='java'>
     * public class Car {
     *     public String brand;
     *     public int price;
     * }
     * ...
     * ObjectMapper mapper = new ObjectMapper();
     * Car car = new Car("BMW", 40000);
     * String jsonString = mapper.writeValueAsString(car);
     * </code></pre>
     * The JSON string for 'jsonString' will be:
     * <pre><code class='json'>
     * {
     *      "brand":"BMW",
     *      "price":40000
     * }
     * </code></pre>
     *
     * @param object Java Object to create a JSON string from
     * @param <T>    any valid type of the Java class that contains proper structure
     *               (variables, constructor...) for mapping
     * @return JSON string from Java Object
     */
    public static <T> String writeJsonAsString(T object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException(format(OBJECT_WRITE_ERROR, object.toString(), e.getMessage()), e);
        }
    }

    /**
     * Recursively traverse the provided JSONObject and add all the values of the found keys to the provided list.
     * <br/><br/>
     * This method recursively explores the keys and values of the given JSONObject.
     * <p> - If the value is an object, the method calls itself recursively. </p>
     * <p> - If the value is an array, the method calls itself recursively for each element of the array. </p>
     * <p> - If the key = a given {@code keyName}, it adds the key's value the provided list. </p>
     *
     * @param jsonObject The JSONObject obtained from reading the contents of a JSON file
     * @param valuesList the list that aggregates key's values from all processed JSON files
     */
    public static void findValuesForKeyInJsonObject(String keyName, JSONObject jsonObject, List<String> valuesList) {
        for (var key : jsonObject.keySet()) {
            var value = jsonObject.get(key);

            if (value instanceof JSONObject jsonObjectInValue) {
                findValuesForKeyInJsonObject(keyName, jsonObjectInValue, valuesList);
            } else if (value instanceof JSONArray jsonArray) {
                for (var arrayElement : jsonArray) {
                    if (arrayElement instanceof JSONObject jsonObjectInArray) {
                        findValuesForKeyInJsonObject(keyName, jsonObjectInArray, valuesList);
                    }
                }
            } else if (key.equals(keyName)) {
                valuesList.add(String.valueOf(value));
            }
        }
    }

    /**
     * Determine whether the provided string value is a valid JSON object.
     *
     * @param jsonAsString string that represents a value that might or might not be JSON object.
     * @return true, if the source string value is a valid JSON Object
     * @see JSONObject
     */
    public static boolean isJsonObject(String jsonAsString) {
        try {
            new JSONObject(jsonAsString);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }
}
