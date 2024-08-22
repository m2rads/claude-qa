import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.lang.Unroll

class DeviceConfigServiceSpec extends Specification {

    private static final String BASE_URL = "http://example.com"

    @Unroll
    def "Test GetDsDeviceConfig: #scenario"() {
        given: "Test data"
        def endpoint = "/GetDsDeviceConfig"
        def storeId = storeIdVal
        def url = "${BASE_URL}${endpoint}?store=${storeId}"

        when: "A GET request is made to /GetDsDeviceConfig"
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

            def jsonSlurper = new JsonSlurper()
            def deviceConfigs = jsonSlurper.parseText(responseBody)

            deviceConfigs instanceof List
            deviceConfigs.size() == expectedConfigCount

            if (expectedConfigCount > 0) {
                deviceConfigs.every { config ->
                    config.containsKey("SerialNo") &&
                    config.containsKey("AssociatedItem") &&
                    config.containsKey("Location") &&
                    config.containsKey("Template") &&
                    config.containsKey("Page") &&
                    config.containsKey("AssociatedTimeStamp") &&
                    config.containsKey("device_type") &&
                    config.containsKey("rec_crt_usr_id")
                }
            }
        } else {
            def errorResponseBody = connection.errorStream.text
            log.info("Error Response Body: ${errorResponseBody}")
            errorResponseBody.contains(expectedErrorMessage)
        }

        where:
        scenario                                                   | storeIdVal | expectedStatusCode | expectedConfigCount | expectedErrorMessage
        "Valid store ID with more than 100 devices"               | 123        | 200                | 100                 | null
        "Valid store ID with less than 100 devices"               | 456        | 200                | 50                  | null
        "Valid store ID with no devices"                          | 789        | 200                | 0                   | null
        "Invalid store ID"                                        | -1         | 400                | 0                   | "invalid store ID"
        "Non-existent store ID"                                   | 999        | 404                | 0                   | "non-existent store"
        "No store ID provided"                                    | null       | 400                | 0                   | "missing required parameter"
        "Valid store ID with high load"                           | 123        | 200                | 100                 | null
        "Store ID 0 to represent all stores"                      | 0          | 200                | 100                 | null
        "Unauthorized request: No authentication"                 | 123        | 401                | 0                   | "lacking proper authentication"
        "Forbidden request: Unauthorized for specified store"     | 123        | 403                | 0                   | "lacking proper authorization"
    }
}