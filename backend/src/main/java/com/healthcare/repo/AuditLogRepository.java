package com.healthcare.repo;
import com.healthcare.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> { List<AuditLog> findAllByOrderByIdDesc(); }
