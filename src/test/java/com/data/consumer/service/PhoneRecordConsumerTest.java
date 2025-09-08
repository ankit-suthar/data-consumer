package com.data.consumer.service;

import com.data.consumer.repository.PhoneRecordAuditRepoCassandra;
import com.data.consumer.repository.PhoneRecordCassandraRepo;
import com.data.consumer.repository.PhoneRecordElasticRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class PhoneRecordConsumerTest {

    @Mock
    private PhoneRecordCassandraRepo cassandraRepo;

    @Mock
    private PhoneRecordElasticRepo elasticRepo;

    @Mock
    private PhoneRecordAuditRepoCassandra auditRepoCassandra;

    @InjectMocks
    private PhoneRecordConsumer consumer;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mapper = new ObjectMapper();
    }

    static Stream<String> invalidJsonProvider() {
        return Stream.of(
                // Missing e164Number
                "{\"country\": \"IN\", \"state\": \"KA\", \"type\": \"mobile\"}",
                // Invalid phone format
                "{\"e164Number\": \"12345\", \"country\": \"IN\", \"state\": \"KA\", \"type\": \"mobile\"}",
                // Too few fields (size != 4)
                "{\"e164Number\": \"+911234567890\", \"country\": \"IN\"}"
        );
    }

    @ParameterizedTest
    @MethodSource("invalidJsonProvider")
    void testConsumeCSVRecords_invalidInputs(String json, CapturedOutput output) throws Exception {
        JsonNode node = mapper.readTree(json);

        consumer.consumeCSVRecords(node);

        verifyNoInteractions(cassandraRepo, auditRepoCassandra, elasticRepo);

        assertThat(output.getOut()).contains("Invalid record skipped");
    }

    @Test
    void testConsumeCSVRecords_validNewRecord_logsProcessed(CapturedOutput output) throws Exception {
        JsonNode node = mapper.readTree("{\"e164Number\": \"+911234567890\", \"country\": \"IN\", " +
                "\"state\": \"KA\", \"type\": \"mobile\"}");
        when(cassandraRepo.existsById("+911234567890")).thenReturn(false);

        consumer.consumeCSVRecords(node);

        verify(cassandraRepo).save(any());
        verify(auditRepoCassandra).save(any());
        verify(elasticRepo).save(any());

        assertThat(output.getOut()).contains("Processed & stored: +911234567890");
    }

    @Test
    void testConsumeCSVRecords_invalidRecord_logsWarning(CapturedOutput output) throws Exception {
        JsonNode node = mapper.readTree("{\"country\": \"IN\"}");

        consumer.consumeCSVRecords(node);

        verifyNoInteractions(cassandraRepo, auditRepoCassandra, elasticRepo);

        assertThat(output.getOut()).contains("Invalid record skipped");
    }

    @Test
    void testConsumeCSVRecords_duplicateRecord_logsInfo(CapturedOutput output) throws Exception {
        JsonNode node = mapper.readTree("{\"e164Number\": \"+911234567890\", \"country\": \"IN\", " +
                "\"state\": \"KA\", \"type\": \"mobile\"}");
        when(cassandraRepo.existsById("+911234567890")).thenReturn(true);

        consumer.consumeCSVRecords(node);

        verify(cassandraRepo, never()).save(any());
        verify(auditRepoCassandra, never()).save(any());
        verify(elasticRepo, never()).save(any());

        assertThat(output.getOut()).contains("Duplicate record found, skipping");
    }

    @Test
    void testConsumeCSVRecords_exception_logsError(CapturedOutput output) throws Exception {
        JsonNode node = mapper.readTree("{\"e164Number\": \"+911234567890\", \"country\": \"IN\", " +
                "\"state\": \"KA\", \"type\": \"mobile\"}");
        when(cassandraRepo.existsById(anyString())).thenReturn(false);
        doThrow(new RuntimeException("DB error")).when(cassandraRepo).save(any());

        consumer.consumeCSVRecords(node);

        assertThat(output.getOut()).contains("Failed to process message");
    }

    @Test
    void testConsumeCassandraRecordUpdate_validRecord_logsStored(CapturedOutput output) throws Exception {
        JsonNode node = mapper.readTree("{\"e164Number\": \"+911234567890\", \"country\": \"IN\", " +
                "\"state\": \"KA\", \"type\": \"mobile\", \"correlationId\": \"abc123\", \"userId\": \"user1\"}");

        consumer.consumeCassandraRecordUpdate(node);

        verify(auditRepoCassandra).save(any());
        verify(elasticRepo).save(any());

        assertThat(output.getOut()).contains("Stored phone number update");
    }

    @Test
    void testConsumeCassandraRecordUpdate_exception_logsError(CapturedOutput output) throws Exception {
        JsonNode node = mapper.readTree("{\"e164Number\": \"+911234567890\", \"country\": \"IN\", " +
                "\"state\": \"KA\", \"type\": \"mobile\", \"correlationId\": \"abc123\", \"userId\": \"user1\"}");
        doThrow(new RuntimeException("ES error")).when(elasticRepo).save(any());

        consumer.consumeCassandraRecordUpdate(node);

        assertThat(output.getOut()).contains("Failed to process message");
    }
}
