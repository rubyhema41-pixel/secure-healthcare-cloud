package com.healthcare.repo;
import com.healthcare.model.AccessRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {
    List<AccessRequest> findByStatus(AccessRequest.Status status);
    boolean existsByRecordIdAndUserIdAndStatus(Long recordId, Long userId, AccessRequest.Status status);
}
