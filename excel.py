import pandas as pd
import re
import os

def md_to_excel(input_file):
    # Generate output file name
    output_file = os.path.splitext(input_file)[0] + '.xlsx'
    
    # Read the content of the Markdown file
    with open(input_file, 'r', encoding='utf-8') as file:
        content = file.read()
    
    # Find the table in the Markdown content
    table_match = re.search(r'\|(.*?)\|[\r\n]+\|[:-]+\|[:-]+\|[:-]+\|[\r\n]+([\s\S]*?)(?=\n\n|\Z)', content)
    
    if table_match:
        headers = [header.strip() for header in table_match.group(1).split('|') if header.strip()]
        table_content = table_match.group(2)
        
        # Process table rows
        rows = []
        current_row = [''] * len(headers)
        for line in table_content.split('\n'):
            if line.strip().startswith('|'):
                if any(current_row):
                    rows.append(current_row)
                current_row = [''] * len(headers)
                cells = [cell.strip() for cell in line.split('|')[1:-1]]
                for i, cell in enumerate(cells):
                    current_row[i] += cell + '\n'
            else:
                for i, cell in enumerate(current_row):
                    if cell:
                        current_row[i] += line.strip() + '\n'
        
        if any(current_row):
            rows.append(current_row)
        
        # Create a DataFrame
        df = pd.DataFrame(rows, columns=headers)
        
        # Write the DataFrame to an Excel file
        with pd.ExcelWriter(output_file, engine='xlsxwriter') as writer:
            df.to_excel(writer, sheet_name='Table', index=False)
            
            # Get the xlsxwriter workbook and worksheet objects
            workbook = writer.book
            worksheet = writer.sheets['Table']
            
            # Set the column widths and text wrapping
            for i, column in enumerate(df):
                column_width = max(df[column].astype(str).map(len).max(), len(column)) + 2
                cell_format = workbook.add_format({'text_wrap': True, 'valign': 'top'})
                worksheet.set_column(i, i, column_width, cell_format)
        
        print(f"Table from {input_file} has been successfully transferred to {output_file}")
    else:
        print(f"No table found in the Markdown file: {input_file}")

def process_all_md_files():
    current_dir = os.getcwd()
    md_files = [f for f in os.listdir(current_dir) if f.endswith('.md')]
    
    if not md_files:
        print("No Markdown files found in the current directory.")
        return
    
    for md_file in md_files:
        try:
            md_to_excel(md_file)
        except Exception as e:
            print(f"An error occurred while processing {md_file}: {str(e)}")

def main():
    print("Converting all Markdown files in the current directory to Excel...")
    process_all_md_files()
    print("Conversion process completed.")

if __name__ == "__main__":
    main()