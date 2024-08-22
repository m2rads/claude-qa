import groovy.util.logging.Slf4j
import spock.lang.Specification
import spock.lang.Unroll

@Slf4j
class DeleteAndRecreateQueueSpecification extends Specification {

    @Unroll
    def "#scenario"() {
        given: "Test data"
        String endpoint = "/DeleteAndRecreateDptQueues"
        String token = tokenValue
        String expectedResponse = responseBody
        int expectedStatusCode = responseCodeValue

        when: "A GET request is made to /DeleteAndRecreateDptQueues"
        def url = "${baseUrl}${endpoint}"
        log.info("Sending GET request to: ${url}")
        def connection = (HttpURLConnection) new URL(url).openConnection()
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer ${token}")
        connection.connect()

        then: "Verify the response"
        def responseCode = connection.responseCode
        log.info("Response Code: ${responseCode}")
        responseCode == expectedStatusCode

        if (expectedStatusCode == 200) {
            def responseBody = connection.inputStream.text
            log.info("Response Body: ${responseBody}")
            responseBody.contains(expectedResponse)
            // Add additional assertions for successful scenarios if needed
        } else {
            def errorResponseBody = connection.errorStream.text
            log.info("Error Response Body: ${errorResponseBody}")
            errorResponseBody.contains(expectedErrorMessage)
        }

        where:
        scenario                                                                         | tokenValue                     | responseCodeValue | responseBody | expectedErrorMessage
        "Valid token, successful operation"                                              | "validToken"                   | 200               | "true"       | null
        "Invalid token"                                                                  | "invalidToken"                 | 401               | null         | "Error message about invalid token"
        "Valid token, insufficient permissions"                                          | "validTokenWithoutPermissions" | 403               | null         | "Error message about insufficient permissions"
        "Valid token, queue deletion not allowed"                                        | "validToken"                   | 409               | null         | "Error message about current system state"
        "Valid token, ongoing operation affecting queues"                                | "validToken"                   | 423               | null         | "Error message about ongoing operation"
        "Valid token, Service Bus temporarily unavailable"                               | "validToken"                   | 503               | null         | "Error message about Service Bus unavailability"
        "Valid token, operation fails midway"                                            | "validToken"                   | 500               | null         | "Error message about partial completion"
        "Valid token, existing messages in queues"                                       | "validToken"                   | 200               | "true"       | null
        "Valid token, followed by another identical request"                             | "validToken"                   | 200               | "true"       | "Error message about ongoing operation"
        "No token provided"                                                              | ""                             | 400               | null         | "Error message about missing token"
    }
}