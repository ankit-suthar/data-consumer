package com.data.consumer.service;

import com.common.models.enums.PhoneNumberStatus;
import com.common.models.util.PhoneRecordMapper;
import com.data.consumer.repository.PhoneRecordAuditRepoCassandra;
import com.data.consumer.repository.PhoneRecordCassandraRepo;
import com.data.consumer.repository.PhoneRecordElasticRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PhoneRecordConsumer {

    @Autowired
    private PhoneRecordCassandraRepo cassandraRepo;

    @Autowired
    private PhoneRecordElasticRepo elasticRepo;

    @Autowired
    private PhoneRecordAuditRepoCassandra auditRepoCassandra;

    private static final Logger log = LoggerFactory.getLogger(PhoneRecordConsumer.class);

    @KafkaListener(topics = "csv-records", groupId = "csv-group")
    public void consumeCSVRecords(JsonNode node) {
        try {
            // Basic validation
            if (node.size() != 4 || node.get("e164Number") == null || !node.get("e164Number").asText().matches("\\+\\d{10,15}")) {
                log.warn("Invalid record skipped: {}", node);
                return;
            }

            // Check duplicate by primary key (E164Number)
            if (cassandraRepo.existsById(node.get("e164Number").asText())) {
                log.info("Duplicate record found, skipping: {}", node.get("e164Number").asText());
                return;
            }

            String e164Number = node.get("e164Number").asText();
            String country = node.get("country").asText();
            String state = node.get("state").asText();
            String type = node.get("type").asText();
            long now = System.currentTimeMillis();

            Map<String, String> data = new HashMap<>();
            data.put("e164Number", e164Number);
            data.put("country", country);
            data.put("state", state);
            data.put("type", type);
            data.put("status", PhoneNumberStatus.AVAILABLE.toValue());
            data.put("version", "1");
            data.put("eventTime", Long.toString(now));

            JsonNode inputData = new ObjectMapper().valueToTree(data);

            // Save in Cassandra
            cassandraRepo.save(PhoneRecordMapper.toCassandra(data));
            // Save in Cassandra for Audit
            auditRepoCassandra.save(PhoneRecordMapper.toCassandraAudit(data));
            // Index in Elasticsearch
            elasticRepo.save(PhoneRecordMapper.toElastic(inputData));

            log.info("Processed & stored: {}", e164Number);
        } catch (Exception e) {
            log.error("Failed to process message: {}", node);
            e.printStackTrace();
        }
    }

    @KafkaListener(topics = "post-processing", groupId = "post-processing-group")
    public void consumeCassandraRecordUpdate(JsonNode node) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("e164Number", node.get("e164Number").asText());
            data.put("country", node.get("country").asText());
            data.put("state", node.get("state").asText());
            data.put("type", node.get("type").asText());
            data.put("status", PhoneNumberStatus.AVAILABLE.toValue());
            data.put("version", "1");
            data.put("eventTime", Long.toString(System.currentTimeMillis()));
            data.put("correlationId", node.get("correlationId").asText());
            data.put("userId", node.get("userId").asText());

            // Save in Cassandra for Audit
            auditRepoCassandra.save(PhoneRecordMapper.toCassandraAudit(data));
            JsonNode inputData = new ObjectMapper().valueToTree(data);
            // Index in Elasticsearch
            elasticRepo.save(PhoneRecordMapper.toElastic(inputData));

            log.info("Stored phone number update: {}", node);
        } catch (Exception e) {
            log.error("Failed to process message: {}", node);
            e.printStackTrace();
        }
    }
}
