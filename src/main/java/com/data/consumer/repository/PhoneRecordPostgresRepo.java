package com.data.consumer.repository;

import com.common.models.model.PhoneRecordPostgres;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhoneRecordPostgresRepo extends JpaRepository<PhoneRecordPostgres, String> {
}
