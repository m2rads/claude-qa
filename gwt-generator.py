import os
import requests
import json
from dotenv import load_dotenv

def read_markdown_file(file_path):
    full_path = "md-files/" + file_path
    with open(full_path, 'r') as file:
        content = file.read()
    return content

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
    prompt = "Generate a Groovy Spock test specification for the following API scenarios:\n\n"
    for scenario in data:
        prompt += f"Scenario:\n"
        prompt += f"Given: {scenario['Given']}\n"
        prompt += f"When: {scenario['When']}\n"
        prompt += f"Then: {scenario['Then']}\n\n"
    prompt += "Please create a complete Groovy Spock test class that covers all these scenarios. Include appropriate setup and cleanup methods if necessary."
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
        return response['content'][0]['text']
    elif isinstance(response, list) and len(response) > 0 and 'content' in response[0]:
        return response[0]['content']
    else:
        print(f"Unexpected response structure: {json.dumps(response, indent=2)}")
        raise ValueError("Unexpected response structure from Claude API")

def main():
    load_dotenv()

    markdown_file = input("Enter the path to your Markdown file: ")
    api_key = os.getenv("CLAUDE_API_KEY")
    
    if not api_key:
        print("Please set the CLAUDE_API_KEY in your .env file.")
        return

    try:
        content = read_markdown_file(markdown_file)
        data = parse_markdown_table(content)
        prompt = generate_claude_prompt(data)
        
        print("Sending request to Claude API...")
        claude_response = call_claude_api(prompt, api_key)
        
        print("Processing Claude's response...")
        groovy_test = process_claude_response(claude_response)

        print("Generated Groovy Spock Test Specification:")
        print(groovy_test)

        with open("GeneratedGroovyTest.groovy", "w") as f:
            f.write(groovy_test)
        print("Test specification has been saved to GeneratedGroovyTest.groovy")

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