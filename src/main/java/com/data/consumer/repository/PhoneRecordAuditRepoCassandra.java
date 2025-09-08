package com.data.consumer.repository;

import com.common.models.model.PhoneRecordAuditCassandra;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhoneRecordAuditRepoCassandra extends CassandraRepository<PhoneRecordAuditCassandra, String> {
    List<PhoneRecordAuditCassandra> findByE164Number(String number);
}