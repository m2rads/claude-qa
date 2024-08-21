import os
import requests
import json

def read_markdown_file(file_path):
    with open(file_path, 'r') as file:
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
        "X-API-Key": api_key
    }
    data = {
        "model": "claude-3-sonnet-20240229",
        "messages": [{"role": "user", "content": prompt}]
    }
    response = requests.post(url, headers=headers, json=data)
    return response.json()

def process_claude_response(response):
    # Extract the Groovy Spock test specification from Claude's response
    return response['content'][0]['text']

def main():
    markdown_file = input("Enter the path to your Markdown file: ")
    api_key = os.environ.get("CLAUDE_API_KEY")
    
    if not api_key:
        print("Please set the CLAUDE_API_KEY environment variable.")
        return

    content = read_markdown_file(markdown_file)
    data = parse_markdown_table(content)
    prompt = generate_claude_prompt(data)
    claude_response = call_claude_api(prompt, api_key)
    groovy_test = process_claude_response(claude_response)

    print("Generated Groovy Spock Test Specification:")
    print(groovy_test)

    # Optionally, save the generated test to a file
    with open("GeneratedGroovyTest.groovy", "w") as f:
        f.write(groovy_test)
    print("Test specification has been saved to GeneratedGroovyTest.groovy")

if __name__ == "__main__":
    main()