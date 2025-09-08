package com.data.consumer.repository;

import com.common.models.model.PhoneRecordElastic;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhoneRecordElasticRepo extends ElasticsearchRepository<PhoneRecordElastic, String> {}

