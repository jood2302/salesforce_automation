package com.aquiva.autotests.rc.utilities;

import io.qameta.allure.Step;
import org.apache.http.*;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.aquiva.autotests.rc.utilities.RestApiAuthentication.usingNoAuthentication;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generic version of the REST API client.
 * <br/>
 * It contains a generic implementation for the useful HTTP methods,
 * like GET, POST, PUT, DELETE, etc...
 */
public class RestApiClient {
    //  REST API constants
    public static final String APPLICATION_JSON = "application/json";
    public static final String ACCEPT_HEADER = "Accept";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String ACCEPT_ENCODING_HEADER = "Accept-Encoding";

    private final RestApiAuthentication authentication;
    private final String exceptionPrefix;
    private final List<Header> additionalHeaders = new ArrayList<>();

    /**
     * Constructor for the basic REST API client.
     *
     * @param authentication  object with authentication data (username and password for Basic Authentication;
     *                        token key/value pair for authorization with Token Header...)
     * @param exceptionPrefix string prefix for possible exceptions in API methods.
     *                        Should be a string that goes first in all the exception messages for the class
     *                        (e.g. "XYZ Rest API Client has failed with an exception! Here's the details: ")
     */
    public RestApiClient(RestApiAuthentication authentication, String exceptionPrefix) {
        this.authentication = authentication;
        this.exceptionPrefix = exceptionPrefix;
    }

    /**
     * No-args constructor for a default implementation of REST API client.
     */
    public RestApiClient() {
        this(usingNoAuthentication(), "REST API Client has failed with an exception! Here are the details: ");
    }

    /**
     * Add/set additional headers to all the requests of the given client.
     *
     * @param headers list of headers to add to all the requests
     *                (e.g. [{"Connection": "keep-alive"}, {"User-Agent": "CustomAgent_123"}])
     */
    public void addHeaders(List<Header> headers) {
        additionalHeaders.addAll(headers);
    }

    /**
     * Invoke a GET request using the given endpoint.
     *
     * @param url endpoint URL to send a request to
     * @return raw response from the service after the request
     * (usually, in the JSON format).
     */
    @Step
    public String get(String url) {
        return httpRequest(new HttpGet(url));
    }

    /**
     * Invoke a GET request using the given endpoint.
     *
     * @param url                endpoint URL to send a request to
     * @param classObjectToParse any valid type of the Java class
     *                           that contains a proper structure (variables, constructor...)
     *                           for mapping the response to
     * @return mapped response from the service in the form of the Java object
     * that was provided as an argument for the method
     * @see JsonUtils#readJson(String, Class)
     */
    @Step
    public <T> T get(String url, Class<T> classObjectToParse) {
        var responseJson = httpRequest(new HttpGet(url));
        return JsonUtils.readJson(responseJson, classObjectToParse);
    }

    /**
     * Invoke a GET request using the given endpoint.
     *
     * @param url                endpoint URL to send a request to
     * @param classObjectToParse any valid type of the Java class
     *                           that contains a proper structure (variables, constructor...)
     *                           for mapping the response to
     * @return mapped response from the service in the form of the List of Java objects
     * that was provided as an argument for the method
     * @see JsonUtils#readJsonAsList(String, Class)
     */
    @Step
    public <T> List<T> getAsList(String url, Class<T> classObjectToParse) {
        var responseJson = httpRequest(new HttpGet(url));
        return JsonUtils.readJsonAsList(responseJson, classObjectToParse);
    }

    /**
     * Invoke a POST request using the given endpoint and the payload.
     *
     * @param url                endpoint URL to send a request to
     * @param bodyObject         any data object used as a payload (body) for the request
     * @param classObjectToParse any valid type of the Java class
     *                           that contains a proper structure (variables, constructor...)
     *                           for mapping the response to
     * @return mapped response from the service in the form of the Java object
     * that was provided as an argument for the method
     * @see JsonUtils#readJson(String, Class)
     */
    @Step
    public <PAYLOAD, RESPONSE> RESPONSE post(String url, PAYLOAD bodyObject, Class<RESPONSE> classObjectToParse) {
        var jsonBody = JsonUtils.writeJsonAsString(bodyObject);
        var responseJson = post(url, jsonBody);
        return JsonUtils.readJson(responseJson, classObjectToParse);
    }

    /**
     * Invoke a POST request using the given endpoint and the payload.
     *
     * @param url      endpoint URL to send a request to
     * @param jsonBody payload (request's body) in the form of JSON string
     * @return raw response from the service after the request
     * (usually, in the JSON format).
     */
    @Step
    public String post(String url, String jsonBody) {
        return httpRequest(new HttpPost(url), jsonBody);
    }

    /**
     * Invoke a POST request using the given endpoint and NO payload.
     *
     * @param url endpoint URL to send a request to
     * @return raw response from the service after the request
     * (usually, in the JSON format).
     */
    @Step
    public String post(String url) {
        return httpRequest(new HttpPost(url));
    }

    /**
     * Invoke a PUT request using the given endpoint and the payload.
     *
     * @param url                endpoint URL to send a request to
     * @param bodyObject         any data object used as a payload (body) for the request
     * @param classObjectToParse any valid type of the Java class
     *                           that contains a proper structure (variables, constructor...)
     *                           for mapping the response to
     *                           <p> (Note: the type should be the same as for {@code bodyObject}!)</p>
     * @return mapped response from the service in the form of the Java object
     * that was provided as an argument for the method
     * @see JsonUtils#readJson(String, Class)
     */
    @Step
    public <T> T put(String url, T bodyObject, Class<T> classObjectToParse) {
        var jsonBody = JsonUtils.writeJsonAsString(bodyObject);
        var responseJson = httpRequest(new HttpPut(url), jsonBody);
        return JsonUtils.readJson(responseJson, classObjectToParse);
    }

    /**
     * Invoke a PUT request using the given endpoint and the payload.
     *
     * @param url        endpoint URL to send a request to
     * @param bodyObject any data object used as a payload (body) for the request
     */
    @Step
    public <T> void put(String url, T bodyObject) {
        var jsonBody = JsonUtils.writeJsonAsString(bodyObject);
        httpRequest(new HttpPut(url), jsonBody);
    }

    /**
     * Invoke a DELETE request using the given endpoint.
     *
     * @param url endpoint URL to send a request to
     */
    @Step
    public void delete(String url) {
        httpRequest(new HttpDelete(url));
    }

    /**
     * Execute a simple HTTP request and get a response.
     * <p>
     * This method should be used for GET and DELETE methods.
     *
     * @param request HTTP request that should be invoked
     * @return raw response from the service after the request
     * (usually, in the JSON format).
     */
    private String httpRequest(HttpRequestBase request) {
        try (var client = createCustomHttpClient()) {
            setHeaders(request);

            return executeAndGetResponse(client, request);
        } catch (IOException | AuthenticationException e) {
            throw new RuntimeException(exceptionPrefix + e.getMessage());
        }
    }

    /**
     * Execute an HTTP request with a payload (body) and get a response.
     * <p>
     * This method should be used for POST and PUT methods.
     *
     * @param request  HTTP request that should be invoked
     * @param jsonBody payload for the request in the form of JSON string
     * @return raw response from the service after the request
     * (usually, in the JSON format).
     */
    private String httpRequest(HttpEntityEnclosingRequest request, String jsonBody) {
        try (var client = createCustomHttpClient()) {
            setHeaders(request);

            request.setEntity(new StringEntity(jsonBody));

            return executeAndGetResponse(client, (HttpUriRequest) request);
        } catch (IOException | AuthenticationException e) {
            throw new RuntimeException(exceptionPrefix + e.getMessage());
        }
    }

    /**
     * Execute an HTTP request and get a response from the service.
     *
     * @param client  any valid HTTP client that executes requests and reads responses
     * @param request any HTTP request that needs to be send to the service
     * @return raw response from the service after the request
     * (usually, in the JSON format).
     * @throws IOException in case of a problem or the connection was aborted
     */
    private String executeAndGetResponse(CloseableHttpClient client, HttpUriRequest request) throws IOException {
        String responseJson;
        try (var response = client.execute(request)) {
            responseJson = EntityUtils.toString(response.getEntity());
            assertThat(response.getStatusLine().getStatusCode())
                    .as("Status code for the response with contents: \n" + responseJson)
                    .isBetween(200, 299);
        }
        return responseJson;
    }

    /**
     * Set basic headers for the HTTP request.
     *
     * @param request HTTP request to set headers to
     * @throws AuthenticationException in case of the incorrect credentials
     */
    private void setHeaders(HttpRequest request) throws AuthenticationException {
        request.setHeader(ACCEPT_HEADER, APPLICATION_JSON);
        request.setHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON);
        request.addHeader(getAuthorizationHeader(request));
        additionalHeaders.forEach(header -> request.setHeader(header));
    }

    /**
     * Get an HTTP Header with the authentication data (username/password, token, etc.).
     * Used for HTTP requests for services that require an authorization.
     *
     * @param httpRequest any HTTP request for the service that needs an authorization
     * @return HTTP header field for the HTTP request,
     * or {@code null} if no authorization is required.
     * @throws AuthenticationException in case of the incorrect credentials
     */
    private Header getAuthorizationHeader(HttpRequest httpRequest) throws AuthenticationException {
        switch (authentication.method) {
            case BASIC_AUTH:
                var credentials = new UsernamePasswordCredentials(authentication.username, authentication.password);
                return new BasicScheme().authenticate(credentials, httpRequest, null);
            case API_KEY:
                return new BasicHeader(authentication.tokenKey, authentication.tokenValue);
            case NONE:
            default:
                return null;
        }
    }

    /**
     * Create a custom HTTP client with the custom request configuration.
     * <br/>
     * Some custom settings are needed in order to deal with problematic REST API services
     * (e.g. services that may hang the connection, or have a long response time).
     */
    private CloseableHttpClient createCustomHttpClient() {
        var clientRequestConfig = RequestConfig.custom()
                .setConnectTimeout(10_000)
                .setSocketTimeout(30_000)
                .setConnectionRequestTimeout(10_000)
                .build();

        return HttpClients.custom()
                .setDefaultRequestConfig(clientRequestConfig)
                .build();
    }
}
