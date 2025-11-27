package com.withdrawal.support.repository;

import com.withdrawal.support.model.CaseInstanceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaseInstanceRepository extends MongoRepository<CaseInstanceDocument, String> {
    
    /**
     * Finds a case instance by document number
     * Queries: { "identifiers.value": "documentNumber" }
     */
    @Query("{ 'identifiers.value': ?0 }")
    Optional<CaseInstanceDocument> findByDocumentNumber(String documentNumber);
}





