# Data Consumer

The **Data Consumer** service listens to **Kafka topics** and processes telephone number records.  
It ensures records are validated, deduplicated, and consistently stored across multiple data stores with full audit tracking.

---

## 🚀 Features
- **Kafka Listeners**
  - `csv-records` → Ingests raw telephone data from CSV ingestion pipeline.
  - `post-processing` → Handles updates after booking/transition.

- **Validation**
  - Ensures phone number format matches **E.164** (`+\d{10,15}`).
  - Skips invalid or duplicate records.

- **Data Persistence**
  - **Postgres** → Latest state of each phone number.
  - **Cassandra Audit** → Audit history per phone number.
  - **Cassandra Customer Audit** → Audit history per customer.
  - **ElasticSearch** → Optimized index for fast search.

- **Idempotency & Deduplication**
  - Checks Cassandra for existing record by primary key before insert.

---

## 🏗️ Processing Flow
1. **Consume message from Kafka (`csv-records`).**
   - Validate and skip duplicates.
   - Save latest state in Postgres.
   - Save audit trail in Cassandra.
   - Index in ElasticSearch.

2. **Consume message from Kafka (`post-processing`).**
   - Store audit in Cassandra (per number and per customer).
   - Update ElasticSearch index.

---

## 📊 Example Message
**Incoming (`csv-records`):**
```json
{
  "e164Number": "+917980883532",
  "country": "IN",
  "state": "TN",
  "type": "FIXED"
}

Transformed & Stored:

{
  "e164Number": "+917980883532",
  "country": "IN",
  "state": "TN",
  "type": "FIXED",
  "status": "AVAILABLE",
  "version": "1",
  "eventTime": 1757393437690
}

Incoming (post-processing):

{
  "e164Number": "+917980883532",
  "country": "IN",
  "state": "TN",
  "type": "FIXED",
  "status": "RESERVED",
  "version": "2",
  "correlationId": "62ad...",
  "userId": "user123"
}
