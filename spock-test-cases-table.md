| Given | When | Then |
|-------|------|------|
| A valid store ID (123), device barcode ("BARCODE123"), and user ("testUser") | A GET request is made to /sign/esl/{store} | - Response status code is 200<br>- Content type is "text/plain"<br>- Response body contains non-null data |
| A valid store ID (123), non-existent device barcode ("NONEXISTENT"), and user ("testUser") | A GET request is made to /sign/esl/{store} | Response status code is 404 |
| A valid store ID (123), unactivated device barcode ("UNACTIVATED_DEVICE"), and user ("testUser") | A GET request is made to /sign/esl/{store} | - Response status code is 202<br>- Response body contains message about activation process initiation |
| An invalid store ID (-1), device barcode ("BARCODE123"), and user ("testUser") | A GET request is made to /sign/esl/{store} | - Response status code is 400<br>- Response body contains error message about invalid store ID |
| A valid store ID (123), device barcode ("BARCODE123"), and no user (null) | A GET request is made to /sign/esl/{store} | - Response status code is 200<br>- Response body contains non-null data<br>- Response indicates anonymous access (if applicable) |
| A valid store ID (123), device barcode ("BARCODE123"), and user ("testUser") | Multiple GET requests are made in quick succession to /sign/esl/{store} | - First few responses have status code 200<br>- Later responses have status code 429 (rate limiting) |
