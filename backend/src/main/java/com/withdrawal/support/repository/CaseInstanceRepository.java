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

    /**
     * Finds case instances where document number is in the given list
     * and has a task with the specified task name
     */
    @Query("{ 'identifiers.value': { $in: ?0 }, 'tasks.taskName': ?1 }")
    List<CaseInstanceDocument> findByDocumentNumbersAndTaskName(List<String> documentNumbers, String taskName);

    /**
     * Finds case instances that have a task with the specified task name
     */
    @Query("{ 'tasks.taskName': ?0 }")
    List<CaseInstanceDocument> findByTaskName(String taskName);

    /**
     * Finds case instances where document number is in the given list
     */
    @Query("{ 'identifiers.value': { $in: ?0 } }")
    List<CaseInstanceDocument> findByDocumentNumbers(List<String> documentNumbers);
}





