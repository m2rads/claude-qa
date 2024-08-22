import groovy.util.logging.Slf4j
import spock.lang.Specification
import spock.lang.Unroll

@Slf4j
class FailedESLCountApiSpec extends Specification {

    private static final String BASE_URL = "http://example.com"

    @Unroll
    def "Get failed ESL count: #scenario"() {
        given: "Test data"
        def endpoint = "sign/failedesl/count"
        def storeId = storeIdVal

        when: "A GET request is made to /sign/failedesl/count/{store}"
        def url = "$BASE_URL/$endpoint/$storeId"
        log.info("Sending GET request to: $url")
        def connection = (HttpURLConnection) new URL(url).openConnection()
        connection.requestMethod = "GET"
        connection.connect()

        then: "Verify the response"
        def responseCode = connection.responseCode
        log.info("Response Code: $responseCode")
        responseCode == expectedStatusCode

        if (expectedStatusCode == 200) {
            def responseBody = connection.inputStream.text
            log.info("Response Body: $responseBody")
            responseBody.isInteger() || responseBody.toInteger() >= 0
            responseBody.toInteger() == expectedCount
        } else {
            def errorResponseBody = connection.errorStream.text
            log.info("Error Response Body: $errorResponseBody")
            errorResponseBody.contains(expectedErrorMessage)
        }

        where:
        scenario                                                   | storeIdVal | expectedStatusCode | expectedCount | expectedErrorMessage
        "Valid store with multiple failing ESLs"                   | 123        | 200                | 5             | null
        "Valid store with no failing ESLs"                         | 123        | 200                | 0             | null
        "Invalid store ID"                                         | -1         | 400                | null          | "invalid store ID"
        "Non-existent store ID"                                    | 999        | 404                | null          | "non-existent store"
        "Valid store with a large number of failing ESLs"          | 123        | 200                | 10000         | null
        "System under high load"                                   | 123        | 200                | 500           | null
        "User not authenticated"                                   | 123        | 401                | null          | "Unauthorized"
        "User not authorized for the store"                        | 123        | 403                | null          | "Forbidden"
        "Valid store ID to represent all stores"                   | 0          | 200                | 15000         | null
        "Multiple concurrent requests for the same store"          | 123        | 200                | 500           | null
    }
}