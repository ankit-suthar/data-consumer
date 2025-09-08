package com.data.consumer.repository;

import com.common.models.model.PhoneRecordCassandra;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhoneRecordCassandraRepo extends CassandraRepository<PhoneRecordCassandra, String> {}

