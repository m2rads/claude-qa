import groovy.util.logging.Slf4j
import spock.lang.Specification
import spock.lang.Unroll

@Slf4j
class EslRecoverySpecification extends Specification {

    def baseUrl = "http://example.com"

    @Unroll
    def "Test ESL recovery behavior: #scenario"() {
        given: "Test data"
        def endpoint = "sign/esl/Recover"
        def storeId = storeIdVal
        def reassociateTags = reassociaateTagsVal

        when: "A POST request is made to /sign/esl/Recover/{store}"
        def url = "${baseUrl}/$endpoint/${storeId}?reassociateTags=${reassociateTags}"
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
        scenario                                                                                  | storeIdVal | reassociaateTagsVal | expectedStatusCode | expectedResponse                                    | expectedErrorMessage
        "Valid request: Recover ESLs without re-association"                                      | 123        | false               | 200                | "successful recovery initiation"                    | null
        "Valid request: Recover ESLs with re-association"                                         | 123        | true                | 200                | "successful recovery initiation"                    | null
        "Invalid request: Invalid store ID"                                                       | -1         | false               | 400                | null                                                | "invalid store ID"
        "Invalid request: Non-existent store ID"                                                  | 999        | false               | 404                | null                                                | "non-existent store"
        "Valid request: Recover ESLs without specifying reassociateTags (default behavior)"      | 123        | null                | 200                | "successful recovery initiation"                    | null
        "Valid request: Recover ESLs with re-association when no tags need re-association"       | 123        | true                | 200                | "successful recovery"                               | null
        "Valid request: Handle consecutive recovery requests"                                     | 123        | false               | 200                | "ongoing recovery process"                          | null
        "Invalid request: Database corruption"                                                    | 123        | true                | 500                | null                                                | "failure due to database corruption"
        "Invalid request: Unauthorized (no authentication)"                                       | 123        | false               | 401                | null                                                | "lacking proper authentication"
        "Invalid request: Forbidden (no authorization for the specified store)"                  | 123        | false               | 403                | null                                                | "lacking proper authorization"
    }
}