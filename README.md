# FNOL ACORD Claim Processing Service

Spring Boot 3 (Java 17) backend API that processes ACORD Automobile Loss Notice content from either PDF upload or plain text.

The service extracts claim fields, validates mandatory fields, applies routing rules, and returns:

```json
{
  "extractedFields": {},
  "missingFields": [],
  "recommendedRoute": "FAST_TRACK",
  "reasoning": "..."
}
```

## Tech Stack
- Java 17
- Spring Boot 3.2.x
- Apache PDFBox (PDF text extraction)
- Maven
- JUnit 5

## Run
1. Build and test:
```bash
./mvnw test
```
2. Start service:
```bash
./mvnw spring-boot:run
```

Windows PowerShell:
```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Configured base URL:
- `http://localhost:8085/acord`

## Supported Inputs
- Text-based PDFs via `POST /claims/process`
- Plain text via `POST /claims/process-text`

## Unsupported Inputs
- Scanned/image-only PDFs without OCR

Scanned PDFs without OCR are not supported.

## How to Tell If a PDF Is Scanned
- Open the PDF in a viewer and try selecting/copying text.
- If text cannot be selected or copied, the PDF is likely scanned/image-only.

## Endpoints and cURL Examples

### 1) Process PDF
`POST /claims/process`  
Content-Type: `multipart/form-data`  
Field: `file`

```bash
curl -X POST "http://localhost:8085/acord/claims/process" \
  -F "file=@ACORD-Automobile-Loss-Notice-12.05.16.pdf"
```

### 2) Process Raw Text
`POST /claims/process-text`  
Content-Type: `application/json`

```bash
curl -X POST "http://localhost:8085/acord/claims/process-text" \
  -H "Content-Type: application/json" \
  -d "{\"text\":\"POLICY NUMBER: PL-12345\\nDATE OF LOSS: 01/15/2026\\nLOCATION OF LOSS: 123 Main St, Austin, TX 78701\\nDESCRIPTION OF ACCIDENT: Rear-end collision.\\nESTIMATE AMOUNT: $12000\"}"
```

## Error Behavior
- Invalid request data returns `400 Bad Request`.
- Scanned/image-only PDF detection returns `400 Bad Request` with:

```json
{
  "error": "PDF appears to be scanned/image-only. OCR is not supported. Please upload a text-based PDF or use /claims/process-text."
}
```

## Routing Priority (Implemented in Code)
1. If description contains `fraud`, `inconsistent`, or `staged` -> `INVESTIGATION_FLAG`
2. If injury is indicated -> `SPECIALIST_QUEUE`
3. If any mandatory field is missing -> `MANUAL_REVIEW`
4. Else if `estimateAmount < 25000` -> `FAST_TRACK`
5. Else -> `MANUAL_REVIEW` (safe default)

## Mandatory Fields Validated
- `policyNumber`
- `dateOfLoss`
- `locationOfLoss`
- `descriptionOfAccident`
- `estimateAmount`

## Limitations
- Scanned PDFs without OCR are not supported.
- Extraction is regex/label-heuristic based and may need tuning for unusual document layouts.
