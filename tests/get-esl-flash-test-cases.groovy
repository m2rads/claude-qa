import groovy.util.logging.Slf4j
import spock.lang.Specification
import spock.lang.Unroll

@Slf4j
class ESLFlashBehaviorSpec extends Specification {

    private static final String BASE_URL = "http://example.com"

    @Unroll
    def "Test ESL flash behaviour: #scenario"() {
        given: "Test data"
        def endpoint = "sign/esl/flash"
        def storeId = storeIdVal
        def turnOn = turnOnVal
        def flashSkuIdList = skuList
        def secondsTimeout = timeoutVal

        when: "A GET request is made to /sign/esl/flash/{store}"
        def url = "${BASE_URL}/$endpoint/${storeId}?turnOn=${turnOn}&flashSkuIdList=${flashSkuIdList.join(',')}&secondsTimeout=${secondsTimeout}"
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
        scenario                                                    | storeIdVal | turnOnVal | skuList                      | timeoutVal | expectedStatusCode | expectedResponse     | expectedErrorMessage
        "Valid request: Flash ESLs for given SKUs"                  | 123        | true      | ["SKU1", "SKU2"]             | 1800       | 200                | "success"            | null
        "Valid request: Stop flashing ESLs for given SKU"           | 123        | false     | ["SKU1"]                     | 1800       | 200                | "success"            | null
        "Invalid request: Empty SKU list"                           | 123        | true      | []                           | 1800       | 400                | null                 | "Empty SKU list"
        "Invalid request: Invalid store ID"                         | -1         | true      | ["SKU1", "SKU2"]             | 1800       | 400                | null                 | "Invalid store ID"
        "Valid request: Custom timeout for flashing ESLs"           | 123        | true      | ["SKU1"]                     | 3600       | 200                | "success"            | null
        "Partial success: Non-existent SKU in the list"             | 123        | true      | ["SKU1", "NONEXISTENT"]      | 1800       | 200                | "Partial success"    | null
        "Invalid request: Invalid turnOn value"                     | 123        | "invalid" | ["SKU1", "SKU2"]             | 1800       | 400                | null                 | "Invalid turnOn value"
        "Invalid request: Invalid timeout value"                    | 123        | true      | ["SKU1"]                     | -1         | 400                | null                 | "Invalid timeout value"
        "Valid request: Flash ESLs for given SKUs in all stores"    | 0          | true      | ["SKU1", "SKU2"]             | 1800       | 200                | "success"            | null
        "Unauthorized request: No authentication"                   | 123        | true      | ["SKU1", "SKU2"]             | 1800       | 401                | null                 | "Unauthorized"
    }
}