import groovy.util.logging.Slf4j
import spock.lang.Specification
import spock.lang.Unroll

@Slf4j
class EslTemplateControllerSpec extends Specification {

    def baseUrl = "http://example.com"

    @Unroll
    def "Get ESL templates: #scenario"() {
        given: "Test data"
        def endpoint = "sign/esl/template"
        def storeId = storeIdVal
        def eslBarcode = eslBarcodeVal
        def itemId = itemIdVal
        def signTypeId = signTypeIdVal

        when: "A GET request is made to /sign/esl/template/{store}"
        def url = "${baseUrl}/${endpoint}/${storeId}?eslBarcode=${eslBarcode}&itemId=${itemId}&signTypeId=${signTypeId}"
        log.info("Sending GET request to: ${url}")
        def connection = (HttpURLConnection) new URL(url).openConnection()
        connection.requestMethod = "GET"
        connection.connect()

        then: "Verify the response"
        def responseCode = connection.responseCode
        log.info("Response Code: ${responseCode}")
        responseCode == expectedStatusCode

        if (expectedStatusCode == 200) {
            def responseBody = connection.inputStream.text
            log.info("Response Body: ${responseBody}")
            responseBody.contains(expectedResponse)
        } else {
            def errorResponseBody = connection.errorStream.text
            log.info("Error Response Body: ${errorResponseBody}")
            errorResponseBody.contains(expectedErrorMessage)
        }

        where:
        scenario                                                                                | storeIdVal | eslBarcodeVal  | itemIdVal | signTypeIdVal || expectedStatusCode | expectedResponse                  | expectedErrorMessage
        "Valid request with existing ESL, item, and default sign type"                         | 123        | "ESL123"       | "ITEM001" | 0             || 200                | "list of available templates"     | null
        "Valid request with existing ESL, item, and specific sign type"                        | 123        | "ESL123"       | "ITEM001" | 1             || 200                | "list of available templates"     | null
        "Non-existent ESL barcode"                                                              | 123        | "NONEXISTENT"  | "ITEM001" | 0             || 404                | null                              | "non-existent ESL"
        "Non-existent item ID"                                                                  | 123        | "ESL123"       | "NONEXISTENT" | 0         || 404                | null                              | "non-existent item"
        "Invalid store ID"                                                                      | -1         | "ESL123"       | "ITEM001" | 0             || 400                | null                              | "invalid store ID"
        "Invalid sign type ID"                                                                  | 123        | "ESL123"       | "ITEM001" | -1            || 400                | null                              | "invalid sign type ID"
        "Non-existent sign type ID"                                                             | 123        | "ESL123"       | "ITEM001" | 999           || 404                | null                              | "non-existent sign type"
        "No templates available"                                                                | 123        | "ESL123"       | "ITEM001" | 0             || 200                | "empty list of templates"         | null
        "ESL not associated with any item, but templates available based on ESL type and item" | 123        | "ESL123"       | "ITEM001" | 0             || 200                | "list of available templates"     | null
    }
}