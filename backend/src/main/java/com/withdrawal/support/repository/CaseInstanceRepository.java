package com.withdrawal.support.repository;

import com.withdrawal.support.model.CaseInstanceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseInstanceRepository extends MongoRepository<CaseInstanceDocument, String> {
    
    /**
     * Finds case instances by document number
     * Returns a list to handle cases where multiple documents have the same identifier
     * Queries: { "identifiers.value": "documentNumber" }
     */
    @Query("{ 'identifiers.value': ?0 }")
    List<CaseInstanceDocument> findByDocumentNumber(String documentNumber);
}





