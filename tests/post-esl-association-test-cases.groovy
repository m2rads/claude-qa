import groovy.util.logging.Slf4j
import spock.lang.Specification
import spock.lang.Unroll

@Slf4j
class ESLAssociationSpec extends Specification {

    @Unroll
    def "Associate/Unassociate ESL: #scenario"() {
        given: "Test data"
        def endpoint = "sign/esl"
        def storeId = storeIdVal
        def eslBarcode = eslBarcodeVal
        def operation = operationVal
        def userId = userIdVal
        def itemId = itemIdVal
        def template = templateVal

        when: "A POST request is made to /sign/esl/{store}"
        def url = "${baseUrl}/$endpoint/${storeId}"
        def requestBody = [
                eslBarcode: eslBarcode,
                operation : operation,
                userId     : userId,
                itemId     : itemId,
                template   : template
        ]
        log.info("Sending POST request to: ${url} with body: ${requestBody}")
        def connection = (HttpURLConnection) new URL(url).openConnection()
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        def writer = new OutputStreamWriter(connection.outputStream)
        writer.write(requestBody.toString())
        writer.flush()
        writer.close()
        connection.connect()

        then: "Verify the response"
        def responseCode = connection.responseCode
        log.info("Response Code: ${responseCode}")
        responseCode == expectedStatusCode

        if (expectedStatusCode == 200) {
            def responseBody = connection.inputStream.text
            log.info("Response Body: ${responseBody}")
            responseBody.contains(expectedResponse)
            // Additional assertions for successful association/unassociation
        } else {
            def errorResponseBody = connection.errorStream.text
            log.info("Error Response Body: ${errorResponseBody}")
            errorResponseBody.contains(expectedErrorMessage)
        }

        where:
        scenario                                                                           | storeIdVal | eslBarcodeVal | operationVal | userIdVal | itemIdVal       | templateVal | expectedStatusCode | expectedResponse | expectedErrorMessage
        "Successful association of ESL with item"                                          | 123        | "ESL123"      | "PAIR"       | "user1"   | "ITEM001"       | "valid"     | 200                | "success"        | null
        "Successful unassociation of ESL from item"                                        | 123        | "ESL123"      | "UNPAIR"     | "user1"   | "ITEM001"       | null        | 200                | "success"        | null
        "Non-existent ESL barcode"                                                         | 123        | "NONEXISTENT" | "PAIR"       | "user1"   | "ITEM001"       | "valid"     | 404                | null             | "Non-existent ESL"
        "Invalid store ID"                                                                  | -1         | "ESL123"      | "PAIR"       | "user1"   | "ITEM001"       | "valid"     | 400                | null             | "Invalid store ID"
        "Invalid operation"                                                                 | 123        | "ESL123"      | "INVALID_OP" | "user1"   | "ITEM001"       | "valid"     | 400                | null             | "Invalid operation"
        "Non-existent item ID"                                                              | 123        | "ESL123"      | "PAIR"       | "user1"   | "NONEXISTENT_ITEM" | "valid"  | 404                | null             | "Non-existent item"
        "Invalid user ID"                                                                   | 123        | "ESL123"      | "PAIR"       | ""        | "ITEM001"       | "valid"     | 400                | null             | "Invalid user ID"
        "Invalid template"                                                                  | 123        | "ESL123"      | "PAIR"       | "user1"   | "ITEM001"       | "invalid"   | 400                | null             | "Invalid template"
        "Association of ESL with new item, removing old association"                       | 123        | "ESL123"      | "PAIR"       | "user1"   | "ITEM002"       | "valid"     | 200                | "success"        | null
    }
}