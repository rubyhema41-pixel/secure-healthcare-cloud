package com.healthcare.repo;
import com.healthcare.model.EhrRecord;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EhrRecordRepository extends JpaRepository<EhrRecord, Long> {}
