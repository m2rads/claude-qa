import groovy.util.logging.Slf4j
import spock.lang.Specification
import spock.lang.Unroll

@Slf4j
class ESLRefreshSpec extends Specification {

    @Unroll
    def "Test ESL refresh behaviour: #scenario"() {
        given: "Test data"
        def endpoint = "/api/ESL/Refresh"
        def storeId = storeIdVal
        def itemId = itemIdVal
        def effectiveStoreTime = effectiveStoreTimeVal

        when: "A POST request is made to /api/ESL/Refresh/{store}/{itemID}"
        def url = "${baseUrl}$endpoint/${storeId}/${itemId}?effectiveStoreTime=${effectiveStoreTime}"
        log.info("Sending POST request to: ${url}")
        def connection = (HttpURLConnection) new URL(url).openConnection()
        connection.requestMethod = "POST"
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
        scenario                                                                                     | storeIdVal | itemIdVal  | effectiveStoreTimeVal | expectedStatusCode | expectedResponse | expectedErrorMessage
        "Successful refresh for a valid store, item, and effective time"                             | 123        | "ITEM001"  | "2023-05-01T10:00"    | 200                | "true"           | null
        "Successful refresh for all stores with a valid item and effective time"                     | 0          | "ITEM001"  | "2023-05-01T10:00"    | 200                | "true"           | null
        "Successful immediate refresh for a valid store and item without effective time"             | 123        | "ITEM001"  | null                  | 200                | "true"           | null
        "Invalid store ID"                                                                            | -1         | "ITEM001"  | "2023-05-01T10:00"    | 400                | null             | "invalid store ID"
        "Non-existent item ID"                                                                        | 123        | "NONEXISTENT" | "2023-05-01T10:00"    | 404                | null             | "non-existent item"
        "Invalid datetime format"                                                                     | 123        | "ITEM001"  | "invalid-date"        | 400                | null             | "invalid datetime format"
        "Invalid (past) datetime"                                                                     | 123        | "ITEM001"  | "2023-01-01T10:00"    | 400                | null             | "invalid (past) datetime"
        "No associated ESL"                                                                           | 123        | "ITEM001"  | "2023-05-01T10:00"    | 404                | null             | "no associated ESL"
        "Successful refresh for multiple items in a store with valid effective time"                 | 123        | "ITEM001,ITEM002" | "2023-05-01T10:00"    | 200                | "true"           | null
    }
}