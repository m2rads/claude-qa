import groovy.util.logging.Slf4j
import spock.lang.Specification
import spock.lang.Unroll

@Slf4j
class EslSignTest extends Specification {

    @Unroll
    def "Test ESL sign behaviour: #scenario"() {
        given: "Test data"
        def endpoint = "sign/esl"
        def storeId = storeIdVal
        def deviceBarcode = deviceBarcodeVal
        def user = userVal

        when: "A GET request is made to /sign/esl/{store}"
        def url = "${baseUrl}/$endpoint/${storeId}?deviceBarcode=${deviceBarcode}&user=${user}"
        log.info("Sending GET request to: ${url}")
        def connection = (HttpURLConnection) new URL(url).openConnection()
        connection.requestMethod = "GET"
        connection.connect()

        then: "Verify the response"
        def responseCode = connection.responseCode
        log.info("Response Code: ${responseCode}")
        responseCode == expectedStatusCode

        if (expectedStatusCode == 200) {
            def contentType = connection.contentType
            log.info("Content Type: ${contentType}")
            contentType == expectedContentType

            def responseBody = connection.inputStream.text
            log.info("Response Body: ${responseBody}")
            responseBody != null
            responseBody.contains(expectedResponse)
        } else if (expectedStatusCode == 202) {
            def responseBody = connection.inputStream.text
            log.info("Response Body: ${responseBody}")
            responseBody.contains(expectedResponse)
        } else {
            def errorResponseBody = connection.errorStream.text
            log.info("Error Response Body: ${errorResponseBody}")
            errorResponseBody.contains(expectedErrorMessage)
        }

        where:
        scenario                                        | storeIdVal | deviceBarcodeVal       | userVal    | expectedStatusCode | expectedContentType | expectedResponse                 | expectedErrorMessage
        "Valid request: Existing device"               | 123        | "BARCODE123"           | "testUser" | 200                | "text/plain"        | null                             | null
        "Invalid request: Non-existent device"         | 123        | "NONEXISTENT"          | "testUser" | 404                | null                | null                             | null
        "Valid request: Unactivated device"            | 123        | "UNACTIVATED_DEVICE"   | "testUser" | 202                | null                | "Activation process initiated"   | null
        "Invalid request: Invalid store ID"            | -1         | "BARCODE123"           | "testUser" | 400                | null                | null                             | "Invalid store ID"
        "Valid request: Anonymous access"              | 123        | "BARCODE123"           | null       | 200                | "text/plain"        | "Anonymous access"               | null
        "Rate limiting: Multiple requests in sequence" | 123        | "BARCODE123"           | "testUser" | 200                | "text/plain"        | null                             | null
        "Rate limiting: Multiple requests in sequence" | 123        | "BARCODE123"           | "testUser" | 429                | null                | null                             | "Rate limit exceeded"
    }
}