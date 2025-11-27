package com.withdrawal.support.repository;

import com.withdrawal.support.model.CaseDocument;
import com.withdrawal.support.model.CaseStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CaseRepository extends MongoRepository<CaseDocument, String> {
    
    Optional<CaseDocument> findByCaseId(String caseId);
    
    List<CaseDocument> findByStatus(CaseStatus status);
    
    List<CaseDocument> findByStatusAndLastUpdatedBefore(CaseStatus status, LocalDateTime dateTime);
}





