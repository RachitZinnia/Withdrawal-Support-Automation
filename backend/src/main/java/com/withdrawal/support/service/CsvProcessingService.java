package com.withdrawal.support.service;

import com.withdrawal.support.dto.DailyReportRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvProcessingService {

    /**
     * Parses CSV file and extracts daily report rows
     */
    public List<DailyReportRow> parseCsvFile(MultipartFile file) {
        log.info("Parsing CSV file: {}", file.getOriginalFilename());
        
        List<DailyReportRow> rows = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new RuntimeException("CSV file is empty");
            }
            
            log.debug("CSV headers: {}", headerLine);
            
            String line;
            int rowNumber = 1;
            
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                try {
                    DailyReportRow row = parseCsvLine(line);
                    rows.add(row);
                } catch (Exception e) {
                    log.warn("Error parsing row {}: {} - Skipping", rowNumber, e.getMessage());
                }
            }
            
            log.info("Successfully parsed {} rows from CSV", rows.size());
            
        } catch (Exception e) {
            log.error("Error parsing CSV file", e);
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage());
        }
        
        return rows;
    }

    /**
     * Parses a single CSV line into DailyReportRow
     * Expected columns:
     * Camunda Business Key, Camunda Client Code, Camunda Contract Number, Camunda Start Time,
     * OnBase Business Key, OnBase ClientCode, OnBase CaseQueue, OnBase ContractNumber,
     * Match, Document Processing Date, Aging (In Days), OnBase PendingCallOut, OnBase Notes
     */
    private DailyReportRow parseCsvLine(String line) {
        String[] columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1); // Split by comma, respecting quotes
        
        // Clean up quoted values
        for (int i = 0; i < columns.length; i++) {
            columns[i] = columns[i].trim().replaceAll("^\"|\"$", "");
        }
        
        return DailyReportRow.builder()
                .camundaBusinessKey(getColumn(columns, 0))
                .camundaClientCode(getColumn(columns, 1))
                .camundaContractNumber(getColumn(columns, 2))
                .camundaStartTime(getColumn(columns, 3))
                .onBaseBusinessKey(getColumn(columns, 4))
                .onBaseClientCode(getColumn(columns, 5))
                .onBaseCaseQueue(getColumn(columns, 6))
                .onBaseContractNumber(getColumn(columns, 7))
                .match(getColumn(columns, 8))
                .documentProcessingDate(getColumn(columns, 9))
                .agingInDays(getColumn(columns, 10))
                .onBasePendingCallOut(getColumn(columns, 11))
                .onBaseNotes(getColumn(columns, 12))
                .build();
    }

    /**
     * Safely gets a column value or returns empty string
     */
    private String getColumn(String[] columns, int index) {
        if (index < columns.length) {
            return columns[index];
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




