import os
import requests
import json
import re
from dotenv import load_dotenv

def read_markdown_file(file_path):
    full_path = "md-files/" + file_path
    with open(full_path, 'r') as file:
        content = file.read()
    return content

def get_markdown_files(directory):
    return [f for f in os.listdir(directory) if f.endswith('.md')]

def parse_markdown_table(content):
    lines = content.split('\n')
    headers = [header.strip() for header in lines[0].split('|') if header.strip()]
    data = []
    for line in lines[2:]:  # Skip the header separator line
        if '|' in line:
            row = [cell.strip() for cell in line.split('|') if cell.strip()]
            if row:
                data.append(dict(zip(headers, row)))
    return data

def generate_claude_prompt(data):
    prompt = """As a QA assistant, generate a Groovy Spock test specification based on the following Given-When-Then (GWT) scenarios. The test should be data-driven and follow this structure:

    1. Use a descriptive test method name that includes a '#scenario' placeholder
    2. Include a data table with columns for each variable in the scenarios
    3. Use the given-when-then blocks in the test body
    4. Include appropriate assertions and error handling
    5. Use log statements for important steps
    6. Handle different response codes and body content appropriately

    Here are the GWT scenarios:

    """
    for i, scenario in enumerate(data, 1):
        prompt += f"Scenario {i}:\n"
        prompt += f"Given: {scenario['Given']}\n"
        prompt += f"When: {scenario['When']}\n"
        prompt += f"Then: {scenario['Then']}\n\n"

    prompt += """
    Based on these scenarios, create a complete Groovy Spock test specification. The test should be similar in structure to this example:

    def "Test ESL flash behaviour: #scenario"() {
        given: "Test data"
        def endpoint = "sign/esl/flash"
        def storeId = storeIdVal
        def turnOn = turnOnVal
        def flashSkuIdList = skuList
        def secondsTimeout = timeoutVal

        when: "A GET request is made to /sign/esl/flash/{store}"
        def url = "${baseUrl}/$endpoint/${storeId}?turnOn=${turnOn}&flashSkuIdList=${flashSkuIdList.join(',')}&secondsTimeout=${secondsTimeout}"
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
        scenario                                                    | storeIdVal        | turnOnVal             | skuList                      | timeoutVal             | expectedStatusCode        | expectedResponse                   | expectedErrorMessage
        "Valid request: Flash ESLs for given SKUs"                  | 13                | true                  | ["10301846"]                 | 1800                   | 200                       | "success"                          | null
        "Valid request: Stop flashing ESLs for given SKU"           | 123               | false                 | ["SKU1"]                     | 1800                   | 200                       | "success"                          | null
        "Invalid request: Empty SKU list"                           | 123               | true                  | []                           | 1800                   | 400                       | null                               | "Empty SKU list"
        "Invalid request: Invalid store ID"                         | -1                | true                  | ["SKU1", "SKU2"]             | 1800                   | 400                       | null                               | "Invalid store ID"
        "Valid request: Custom timeout for flashing ESLs"           | 123               | true                  | ["SKU1"]                     | 3600                   | 200                       | "success"                          | null
        "Partial success: Non-existent SKU in the list"             | 123               | true                  | ["SKU1", "NONEXISTENT"]      | 1800                   | 200                       | "Partial success"                  | null
        "Invalid request: Invalid turnOn value"                     | 123               | "invalid"             | ["SKU1", "SKU2"]             | 1800                   | 400                       | null                               | "Invalid turnOn value"
        "Invalid request: Invalid timeout value"                    | 123               | true                  | ["SKU1"]                     | -1                     | 400                       | null                               | "Invalid timeout value"
        "Valid request: Flash ESLs for given SKUs in all stores"    | 0                 | true                  | ["SKU1", "SKU2"]             | 1800                   | 200                       | "success"                          | null
        "Unauthorized request: No authentication"                   | 123               | true                  | ["SKU1", "SKU2"]             | 1800                   | 401                       | null                               | "Unauthorized"
    }

    Please generate a similar Spock test specification based on the provided GWT scenarios. Ensure that the test method name, variables, and data table are adjusted to match the given scenarios. The generated test should handle different response codes and include appropriate assertions and error handling as shown in the example."""

    return prompt
  

def call_claude_api(prompt, api_key):
    url = "https://api.anthropic.com/v1/messages"
    headers = {
        "Content-Type": "application/json",
        "x-api-key": api_key,
        "anthropic-version": "2023-06-01"
    }
    data = {
        "model": "claude-3-sonnet-20240229",
        "max_tokens": 4000,
        "messages": [
            {"role": "user", "content": prompt}
        ]
    }
    try:
        response = requests.post(url, headers=headers, json=data)
        response.raise_for_status()
        return response.json()
    except requests.RequestException as e:
        print(f"API Request failed: {e}")
        print(f"Response status code: {e.response.status_code}")
        print(f"Response body: {e.response.text}")
        raise

def process_claude_response(response):
    if isinstance(response, dict) and 'content' in response:
        full_text = response['content'][0]['text']
    elif isinstance(response, list) and len(response) > 0 and 'content' in response[0]:
        full_text = response[0]['content']
    else:
        print(f"Unexpected response structure: {json.dumps(response, indent=2)}")
        raise ValueError("Unexpected response structure from Claude API")
    
    # Extract code between ```groovy and ``` tags
    code_match = re.search(r'```groovy\n(.*?)```', full_text, re.DOTALL)
    if code_match:
        return code_match.group(1).strip()
    else:
        print("No Groovy code found in the response.")
        return full_text  # Return full text if no code block is found

def main():
    load_dotenv()

    api_key = os.getenv("CLAUDE_API_KEY")
    
    if not api_key:
        print("Please set the CLAUDE_API_KEY in your .env file.")
        return

    input_directory = "md-files"
    output_directory = "tests"

    # Create the output directory if it doesn't exist
    os.makedirs(output_directory, exist_ok=True)

    markdown_files = get_markdown_files(input_directory)

    for markdown_file in markdown_files:
        try:
            print(f"Processing file: {markdown_file}")
            content = read_markdown_file(markdown_file)
            data = parse_markdown_table(content)
            prompt = generate_claude_prompt(data)
            
            print("Sending request to Claude API...")
            claude_response = call_claude_api(prompt, api_key)
            
            print("Processing Claude's response...")
            groovy_test = process_claude_response(claude_response)

            print("Generated Groovy Spock Test Specification:")
            print(groovy_test)
            
            output_name = os.path.join(output_directory, f"{os.path.splitext(markdown_file)[0]}.groovy")
            with open(output_name, "w") as f:
                f.write(groovy_test)
            print(f"Test specification has been saved to {output_name}")

        except FileNotFoundError:
            print(f"Error: The file '{markdown_file}' was not found.")
        except requests.RequestException as e:
            print(f"Error calling Claude API: {e}")
        except ValueError as e:
            print(f"Error processing Claude's response: {e}")
        except Exception as e:
            print(f"An unexpected error occurred: {e}")

if __name__ == "__main__":
    main()