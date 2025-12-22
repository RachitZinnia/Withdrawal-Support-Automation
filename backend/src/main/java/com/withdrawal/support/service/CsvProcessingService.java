package com.withdrawal.support.service;

import com.withdrawal.support.dto.DailyReportRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvProcessingService {

    // Column indices for the CSV file
    private static final int COL_CAMUNDA_BUSINESS_KEY = 0;
    private static final int COL_CAMUNDA_CLIENT_CODE = 1;
    private static final int COL_CAMUNDA_CONTRACT_NUMBER = 2;
    private static final int COL_CAMUNDA_START_TIME = 3;
    private static final int COL_ONBASE_BUSINESS_KEY = 4;
    private static final int COL_ONBASE_CLIENT_CODE = 5;
    private static final int COL_ONBASE_CASE_QUEUE = 6;
    private static final int COL_ONBASE_CONTRACT_NUMBER = 7;
    private static final int COL_MATCH = 8;
    private static final int COL_DOCUMENT_PROCESSING_DATE = 9;
    private static final int COL_AGING_IN_DAYS = 10;
    private static final int COL_ONBASE_PENDING_CALLOUT = 11;
    private static final int COL_ONBASE_NOTES = 12;

    /**
     * Parses CSV file and extracts daily report rows using Apache Commons CSV
     * This provides robust parsing with proper handling of:
     * - Quoted fields with commas
     * - Escaped quotes
     * - Multi-line fields
     * - Special characters
     */
    public List<DailyReportRow> parseCsvFile(MultipartFile file) {
        log.info("Parsing CSV file: {} using array-based indexing", file.getOriginalFilename());
        
        List<DailyReportRow> rows = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            // Configure CSV format with header auto-detection
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader()  // Use first row as headers
                    .setSkipHeaderRecord(true)  // Skip header row in records
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .setAllowMissingColumnNames(true)
                    .build();
            
            CSVParser csvParser = csvFormat.parse(reader);
            
            // Get headers for logging
            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            log.info("CSV Headers detected: {}", headerMap.keySet());
            log.info("Total columns: {}", headerMap.size());
            
            int rowNumber = 0;
            
            // Process each record (row) in the CSV
            for (CSVRecord record : csvParser) {
                rowNumber++;
                try {
                    DailyReportRow row = parseRecordByIndex(record, rowNumber);
                    rows.add(row);
                    
                    if (rowNumber % 100 == 0) {
                        log.debug("Processed {} rows...", rowNumber);
                    }
                    
                } catch (Exception e) {
                    log.warn("Error parsing row {}: {} - Skipping", rowNumber, e.getMessage());
                    log.debug("Problematic record: {}", record);
                }
            }
            
            log.info("Successfully parsed {} rows from CSV (out of {} total rows)", rows.size(), rowNumber);
            
        } catch (Exception e) {
            log.error("Error parsing CSV file", e);
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage(), e);
        }
        
        return rows;
    }

    /**
     * Parses a single CSV record into DailyReportRow using column indices
     * This method accesses data by row and column index for accuracy
     * 
     * Expected column order:
     * [0]  Camunda Business Key
     * [1]  Camunda Client Code
     * [2]  Camunda Contract Number
     * [3]  Camunda Start Time
     * [4]  OnBase Business Key
     * [5]  OnBase ClientCode
     * [6]  OnBase CaseQueue
     * [7]  OnBase ContractNumber
     * [8]  Match
     * [9]  Document Processing Date
     * [10] Aging (In Days)
     * [11] OnBase PendingCallOut
     * [12] OnBase Notes
     */
    private DailyReportRow parseRecordByIndex(CSVRecord record, int rowNumber) {
        
        // Log detailed parsing for first few rows to help with debugging
        if (rowNumber <= 3) {
            log.debug("Parsing row {}: Total columns in record = {}", rowNumber, record.size());
            for (int i = 0; i < record.size(); i++) {
                log.debug("  Column [{}]: '{}'", i, record.get(i));
            }
        }
        
        return DailyReportRow.builder()
                .camundaBusinessKey(getColumnByIndex(record, COL_CAMUNDA_BUSINESS_KEY))
                .camundaClientCode(getColumnByIndex(record, COL_CAMUNDA_CLIENT_CODE))
                .camundaContractNumber(getColumnByIndex(record, COL_CAMUNDA_CONTRACT_NUMBER))
                .camundaStartTime(getColumnByIndex(record, COL_CAMUNDA_START_TIME))
                .onBaseBusinessKey(getColumnByIndex(record, COL_ONBASE_BUSINESS_KEY))
                .onBaseClientCode(getColumnByIndex(record, COL_ONBASE_CLIENT_CODE))
                .onBaseCaseQueue(getColumnByIndex(record, COL_ONBASE_CASE_QUEUE))
                .onBaseContractNumber(getColumnByIndex(record, COL_ONBASE_CONTRACT_NUMBER))
                .match(getColumnByIndex(record, COL_MATCH))
                .documentProcessingDate(getColumnByIndex(record, COL_DOCUMENT_PROCESSING_DATE))
                .agingInDays(getColumnByIndex(record, COL_AGING_IN_DAYS))
                .onBasePendingCallOut(getColumnByIndex(record, COL_ONBASE_PENDING_CALLOUT))
                .onBaseNotes(getColumnByIndex(record, COL_ONBASE_NOTES))
                .build();
    }

    /**
     * Safely gets a column value by index or returns empty string
     * Handles missing columns gracefully
     */
    private String getColumnByIndex(CSVRecord record, int columnIndex) {
        try {
            if (columnIndex < record.size()) {
                String value = record.get(columnIndex);
                // Return empty string for null values, otherwise trim the value
                return value != null ? value.trim() : "";
            }
        } catch (Exception e) {
            log.warn("Error accessing column index {}: {}", columnIndex, e.getMessage());
        }
        return "";
    }

    /**
     * Filters rows to only include "Not Matching" entries
     */
    public List<DailyReportRow> filterNotMatchingRows(List<DailyReportRow> rows) {
        List<DailyReportRow> notMatchingRows = rows.stream()
                .filter(DailyReportRow::shouldProcess)
                .toList();
        
        log.info("Filtered {} 'Not Matching' rows from {} total rows", notMatchingRows.size(), rows.size());
        return notMatchingRows;
    }

    /**
     * Extracts business keys from filtered rows
     * Uses OnBase Business Key if available, otherwise Camunda Business Key
     */
    public List<String> extractBusinessKeys(List<DailyReportRow> rows) {
        List<String> businessKeys = rows.stream()
                .map(DailyReportRow::getBusinessKeyToUse)
                .filter(key -> key != null && !key.trim().isEmpty())
                .distinct()
                .toList();
        
        log.info("Extracted {} unique business keys", businessKeys.size());
        return businessKeys;
    }
}




