package com.data.consumer.repository;

import com.common.models.model.CustomerRecordAuditCassandra;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRecordAuditRepoCassandra extends CassandraRepository<CustomerRecordAuditCassandra, String> {
}