import groovy.util.logging.Slf4j
import spock.lang.Specification
import spock.lang.Unroll

@Slf4j
class EslFailureReasonApiSpec extends Specification {

    def baseUrl = "http://example.com/api"

    @Unroll
    def "Get ESL failure reasons: #scenario"() {
        given: "Test data"
        def endpoint = "/sign/failedesl/reason"
        def isSystemOperational = systemOperational
        def isAuthenticationProvided = authenticationProvided
        def isAuthorizationProvided = authorizationProvided
        def isEndpointAvailable = endpointAvailable
        def isMultipleRequests = multipleRequests
        def unsupportedHttpMethod = httpMethod != "GET"

        when: "A request is made to /sign/failedesl/reason"
        def url = "${baseUrl}${endpoint}"
        log.info("Sending ${httpMethod} request to: ${url}")
        def connection = (HttpURLConnection) new URL(url).openConnection()
        connection.requestMethod = httpMethod
        if (authenticationProvided) {
            connection.setRequestProperty("Authentication", "Bearer valid_token")
        }
        connection.connect()

        then: "Verify the response"
        def responseCode = connection.responseCode
        log.info("Response Code: ${responseCode}")
        responseCode == expectedStatusCode

        if (expectedStatusCode == 200) {
            def responseBody = connection.inputStream.text
            log.info("Response Body: ${responseBody}")
            verifyResponseBody(responseBody, expectedResponseBody)
        } else {
            def errorResponseBody = connection.errorStream.text
            log.info("Error Response Body: ${errorResponseBody}")
            errorResponseBody.contains(expectedErrorMessage)
        }

        where:
        scenario                                     | systemOperational | authenticationProvided | authorizationProvided | endpointAvailable | multipleRequests | httpMethod | expectedStatusCode | expectedResponseBody                                     | expectedErrorMessage
        "System operational, endpoint accessible"    | true              | true                   | true                  | true              | false             | "GET"      | 200                | { it.length() > 0 && isListOfReasons(it) }              | null
        "No failure reasons configured"              | true              | true                   | true                  | true              | false             | "GET"      | 200                | { it == "[]" }                                          | null
        "System under high load"                     | true              | true                   | true                  | true              | false             | "GET"      | 200                | { it.length() > 0 && isListOfReasons(it) }              | null
        "Lack of authentication"                     | true              | false                  | true                  | true              | false             | "GET"      | 401                | null                                                    | "Unauthorized"
        "Lack of authorization"                      | true              | true                   | false                 | true              | false             | "GET"      | 403                | null                                                    | "Forbidden"
        "Endpoint temporarily unavailable"           | true              | true                   | true                  | false             | false             | "GET"      | 503                | null                                                    | "Service Unavailable"
        "Multiple concurrent requests"               | true              | true                   | true                  | true              | true              | "GET"      | 200                | { it.length() > 0 && isListOfReasons(it) }              | null
        "Unsupported HTTP method"                    | true              | true                   | true                  | true              | false             | "POST"     | 405                | null                                                    | "Method Not Allowed"

    }

    private boolean isListOfReasons(String responseBody) {
        def reasons = new JsonSlurper().parseText(responseBody)
        reasons.every { it.containsKey("id") && it.containsKey("text") }
    }

    private void verifyResponseBody(String responseBody, Closure<Boolean> condition) {
        assert condition(responseBody), "Response body did not match the expected condition"
    }
}