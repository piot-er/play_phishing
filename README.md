# PHISHING APP

Assumptions taken:
1. For now, sms input from a file, in the future it can be a broker/producer from kafka
2. For now, consent given on API, to simulate external app providing consent SMS interpretation. Consents stored in Postgres, can be changed to different DB
3. For now basic logging, to be changed to log4j or something later
4. For now, no security/authentication. I would use OAuth2.0 or JWT tokens for API, with passwords and secrets stored for example in vault.
5. For now - no real integration with googleAPIs AND a cache in-memory. I would use redis for cache and cassandra/mongo for long term storage of URLs with the highest certainty of danger.

## Description
Structure of the project:

Consent API (consent-service) - REST API to give consent for phone number, stores in Postgres and compacted topic "consent.state" in Kafka
SMS input (sms.jsonl file in sms-ingest/data)
 -> SMS Ingestor (sms-ingest) - reads sms from file, sends to Kafka topic "sms.raw"
 -> Consent Gate - (consent-gate) - reads from "sms.raw", checks consent in kafka, if consented sends to "sms.filtered"
 -> URL reputation orchestrator (url-reputation-orchestrator) - reads from "sms.filtered", extracts URLs, checks cache (in-memory for now), if not in cache checks Google API, stores in cache, sends results to "sms.processed"

To run: 
```
docker compose -f 'docker-compose.yml' up -d --build
```

To grant consent (in a docker network):
```
curl -X POST http://consent-service:8080/consents/inbound-sms \
  -H "Content-Type: application/json" \
  -d '{"receiver": "234", "text": "TAK"}'
```