import groovy.util.logging.Slf4j
import spock.lang.Specification
import spock.lang.Unroll

@Slf4j
class ESLDeactivationSpec extends Specification {

    @Unroll
    def "Deactivate ESL: #scenario"() {
        given: "Test data"
        def endpoint = "sign/esl/{store}"
        def storeId = storeIdVal
        def eslBarcode = eslBarcodeVal
        def deactivateReason = deactivateReasonVal
        def userId = userIdVal

        when: "A DELETE request is made to /sign/esl/{store} to deactivate the ESL"
        def url = "${baseUrl}/$endpoint/${storeId}?eslBarcode=${eslBarcode}&deactivateReason=${deactivateReason}&userId=${userId}"
        log.info("Sending DELETE request to: ${url}")
        def connection = (HttpURLConnection) new URL(url).openConnection()
        connection.requestMethod = "DELETE"
        connection.connect()

        then: "Verify the response"
        def responseCode = connection.responseCode
        log.info("Response Code: ${responseCode}")
        responseCode == expectedStatusCode

        if (expectedStatusCode == 200) {
            def responseBody = connection.inputStream.text
            log.info("Response Body: ${responseBody}")
            responseBody.contains(expectedResponse)
            // Validate ESL deactivation in the system
            validateESLDeactivation(eslBarcode, deactivateReasonText)
        } else {
            def errorResponseBody = connection.errorStream.text
            log.info("Error Response Body: ${errorResponseBody}")
            errorResponseBody.contains(expectedErrorMessage)
        }

        where:
        scenario                                                                            | storeIdVal | eslBarcodeVal | deactivateReasonVal | userIdVal | expectedStatusCode | expectedResponse | expectedErrorMessage                        | deactivateReasonText
        "Valid deactivation: Existing ESL, Missing reason"                                  | 123        | "ESL123"      | 1                   | "user1"   | 200                | "success"        | null                                        | "Missing"
        "Valid deactivation: Existing ESL, Broken reason"                                   | 123        | "ESL123"      | 2                   | "user1"   | 200                | "success"        | null                                        | "Broken"
        "Invalid deactivation: Non-existent ESL"                                            | 123        | "NONEXISTENT" | 1                   | "user1"   | 404                | null             | "Non-existent ESL"                          | null
        "Invalid deactivation: Invalid store ID"                                            | -1         | "ESL123"      | 1                   | "user1"   | 400                | null             | "Invalid store ID"                          | null
        "Invalid deactivation: Invalid deactivate reason"                                   | 123        | "ESL123"      | 3                   | "user1"   | 400                | null             | "Invalid deactivate reason"                 | null
        "Invalid deactivation: Invalid user ID"                                             | 123        | "ESL123"      | 1                   | ""        | 400                | null             | "Invalid user ID"                           | null
        "Invalid deactivation: ESL already deactivated"                                     | 123        | "ESL123"      | 1                   | "user1"   | 400                | null             | "ESL already deactivated"                   | null
        "Valid deactivation: ESL associated with item, remove associations"                 | 123        | "ESL123"      | 1                   | "user1"   | 200                | "success"        | null                                        | "Missing"
        "Invalid deactivation: Missing deactivate reason"                                   | 123        | "ESL123"      | null                | "user1"   | 400                | null             | "Missing deactivate reason"                 | null

    }

    void validateESLDeactivation(String eslBarcode, String deactivateReasonText) {
        // Implement the logic to validate ESL deactivation in the system
        // This might involve checking the ESL status and deactivate reason in the database or other data store
        log.info("Validating ESL deactivation for barcode: ${eslBarcode}, Reason: ${deactivateReasonText}")
    }
}