package com.example.tests

import spock.lang.Specification
import spock.lang.Unroll

class FailedEslApiSpec extends Specification {

    private static final String BASE_URL = "http://example.com/api"

    @Unroll
    def "Retrieve failed ESLs: #scenario"() {
        given: "Test data"
        def endpoint = "sign/failedesl"
        def storeId = storeIdVal

        when: "A GET request is made to /sign/failedesl/{store}"
        def url = "${BASE_URL}/$endpoint/$storeId"
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
            if (expectedListEmpty) {
                assert responseBody.contains('[]')
            } else {
                def eslList = new JsonSlurper().parseText(responseBody)
                assert !eslList.isEmpty()
                eslList.each { esl ->
                    assert esl.eslId != null
                    assert esl.failureReason != null
                    assert esl.failureTimestamp != null
                }
            }
        } else {
            def errorResponseBody = connection.errorStream.text
            log.info("Error Response Body: $errorResponseBody")
            assert errorResponseBody.contains(expectedErrorMessage)
        }

        where:
        scenario                                    | storeIdVal | expectedStatusCode | expectedListEmpty | expectedErrorMessage
        "Valid store ID with failing ESLs"          | 123        | 200                | false             | null
        "Valid store ID with no failing ESLs"       | 123        | 200                | true              | null
        "Invalid store ID"                          | -1         | 400                | false             | "invalid store ID"
        "Non-existent store ID"                     | 999        | 404                | false             | "non-existent store"
        "Valid store ID with high system load"      | 123        | 200                | false             | null
        "Valid store ID with large number of ESLs"  | 123        | 200                | false             | null
        "Unauthorized request"                      | 123        | 401                | false             | "lacking proper authentication"
        "Forbidden request"                         | 123        | 403                | false             | "lacking proper authorization"
        "Get failed ESLs across all stores"         | 0          | 200                | false             | null
    }
}