package com.withdrawal.support.service;

import com.withdrawal.support.config.ApiConfig;
import com.withdrawal.support.dto.DataEntryCase;
import com.withdrawal.support.dto.LetterGenerationData;
import com.withdrawal.support.dto.LetterGenerationResult;
import com.withdrawal.support.model.ProcessResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LetterProcessingService {

    private final DataEntryService dataEntryService;
    private final UsBusinessDayService usBusinessDayService;
    private final ApiConfig apiConfig;
    private final WebClient.Builder webClientBuilder;

    // Column headers for Excel
    private static final String[] EXCEL_HEADERS = {
            "Correspondence Correlation ID",
            "Document Number",
            "CARRIER",
            "CONTRACT_NUMBER",
            "deliveryType",
            "XML_FILE_NAME"
    };

    /**
     * Main method to process letter waiting cases and generate Excel report
     */
    public LetterGenerationResult processLetterWaitingCases() {
        log.info("Starting letter waiting cases processing");

        List<DataEntryCase> letterWaitingCaseList = dataEntryService.getyWaitingCases(
                "letter_resolution_process", "Activity_153hvqk");

        List<LetterGenerationData> results = new ArrayList<>();
        Map<String, String> docAndCorrelationIdMapping = new HashMap<>();
        List<String> correspondenceCorrelationIds = new ArrayList<>();

        // Step 1: Extract correspondenceCorrelationIds and build mapping
        for (DataEntryCase letterWaitingCase : letterWaitingCaseList) {
            ProcessResult processResult = dataEntryService.getStartDateFromProcessInstance(
                    letterWaitingCase.getProcessInstanceId());
            
                if (usBusinessDayService.isDateDifferTwo(processResult.getLocalDate(), 2)) {
                String correspondenceCorrelationId = dataEntryService.getCamundaVariable(
                        letterWaitingCase.getProcessInstanceId(), "correspondenceCorrelationId");
                
                if (correspondenceCorrelationId != null && !correspondenceCorrelationId.isEmpty()) {
                    correspondenceCorrelationIds.add(correspondenceCorrelationId);
                    // Map correspondenceCorrelationId -> documentNumber (businessKey from letter_resolution_process)
                    docAndCorrelationIdMapping.put(correspondenceCorrelationId, processResult.getBusinessKey());
                    log.info("Added mapping: {} -> {}", correspondenceCorrelationId, processResult.getBusinessKey());
                }
            }
        }

        log.info("Found {} correspondence correlation IDs to process", correspondenceCorrelationIds.size());

        // Step 2: Process each correspondenceCorrelationId as business key for letter_generation_process
        int index = 0;
        for (String correspondenceCorrelationId : correspondenceCorrelationIds) {
            index++;
            log.info("[{}/{}] Processing correspondence correlation ID: {}", 
                    index, correspondenceCorrelationIds.size(), correspondenceCorrelationId);
            
            String documentNumber = docAndCorrelationIdMapping.get(correspondenceCorrelationId);
            
            LetterGenerationData data = processCorrespondenceCorrelationId(
                    correspondenceCorrelationId, documentNumber);
            results.add(data);
            
            // Small delay to avoid overwhelming the server
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Step 3: Build result summary
        LetterGenerationResult result = buildResult(results);
        log.info("Letter generation processing completed: {}", result.getMessage());
        
        return result;
    }

    /**
     * Process a single correspondenceCorrelationId as business key
     */
    private LetterGenerationData processCorrespondenceCorrelationId(
            String correspondenceCorrelationId, String documentNumber) {
        
        log.info("Fetching process instance for business key: {}", correspondenceCorrelationId);

        try {
            // Fetch process instances by business key for letter_generation_process
            List<Map<String, Object>> processInstances = fetchProcessInstancesByBusinessKey(
                    correspondenceCorrelationId, "letter_generation_process");

            if (processInstances == null || processInstances.isEmpty()) {
                log.warn("No process instance found for business key: {}", correspondenceCorrelationId);
                return LetterGenerationData.builder()
                        .correspondenceCorrelationId(correspondenceCorrelationId)
                        .documentNumber(documentNumber)
                        .build();
            }

            // Get the first process instance
            Map<String, Object> processInstance = processInstances.get(0);
            String processInstanceId = (String) processInstance.get("id");

            log.info("Found process instance: {}, State: {}", 
                    processInstanceId, processInstance.get("state"));

            // Fetch variables
            String carrier = fetchVariable(processInstanceId, "CARRIER");
            String contractNumber = fetchVariable(processInstanceId, "CONTRACT_NUMBER");
            String deliveryType = fetchVariable(processInstanceId, "deliveryType");
            String xmlFileName = fetchVariable(processInstanceId, "XML_FILE_NAME");

            // Build result
            LetterGenerationData data = LetterGenerationData.builder()
                    .correspondenceCorrelationId(correspondenceCorrelationId)
                    .documentNumber(documentNumber)
                    .carrier(carrier)
                    .contractNumber(contractNumber)
                    .deliveryType(deliveryType)
                    .xmlFileName(xmlFileName)
                    .build();

            log.info("Completed processing for: {}", correspondenceCorrelationId);
            return data;

        } catch (Exception e) {
            log.error("Error processing correspondence correlation ID: {}", correspondenceCorrelationId, e);
            return LetterGenerationData.builder()
                    .correspondenceCorrelationId(correspondenceCorrelationId)
                    .documentNumber(documentNumber)
                    .build();
        }
    }

    /**
     * Fetch process instances by business key and process definition key
     */
    private List<Map<String, Object>> fetchProcessInstancesByBusinessKey(
            String businessKey, String processDefinitionKey) {
        
        log.debug("Fetching process instances for business key: {} with definition: {}", 
                businessKey, processDefinitionKey);

        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(apiConfig.getFormservice().getUrl())
                    .build();

            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/history/process-instance")
                            .queryParam("processInstanceBusinessKey", businessKey)
                            .queryParam("processDefinitionKey", processDefinitionKey)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .onErrorResume(e -> {
                        log.error("Error fetching process instances for business key {}: {}", 
                                businessKey, e.getMessage());
                        return Mono.just(List.of());
                    })
                    .block();

        } catch (Exception e) {
            log.error("Failed to fetch process instances for business key: {}", businessKey, e);
            return List.of();
        }
    }

    /**
     * Fetch a specific variable for a process instance
     */
    private String fetchVariable(String processInstanceId, String variableName) {
        log.debug("Fetching variable '{}' for process instance: {}", variableName, processInstanceId);

        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(apiConfig.getFormservice().getUrl())
                    .build();

            List<Map<String, Object>> variables = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/history/variable-instance")
                            .queryParam("processInstanceId", processInstanceId)
                            .queryParam("variableName", variableName)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .onErrorResume(e -> {
                        log.warn("Error fetching variable '{}' for process instance {}: {}",
                                variableName, processInstanceId, e.getMessage());
                        return Mono.just(List.of());
                    })
                    .block();

            if (variables != null && !variables.isEmpty()) {
                Object value = variables.get(0).get("value");
                if (value != null) {
                    log.debug("Variable '{}' value: {}", variableName, value);
                    return value.toString();
                }
            }

            log.debug("Variable '{}' not found for process instance: {}", variableName, processInstanceId);
            return null;

        } catch (Exception e) {
            log.error("Failed to fetch variable '{}' for process instance: {}", 
                    variableName, processInstanceId, e);
            return null;
        }
    }

    /**
     * Build result summary from processed data
     */
    private LetterGenerationResult buildResult(List<LetterGenerationData> data) {
        int carrierFound = 0;
        int contractNumberFound = 0;
        int deliveryTypeFound = 0;
        int xmlFileNameFound = 0;

        for (LetterGenerationData item : data) {
            if (item.getCarrier() != null) carrierFound++;
            if (item.getContractNumber() != null) contractNumberFound++;
            if (item.getDeliveryType() != null) deliveryTypeFound++;
            if (item.getXmlFileName() != null) xmlFileNameFound++;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String excelFileName = "letter_generation_data_" + timestamp + ".xlsx";

        String message = String.format(
                "Letter Generation Processing: %d total. " +
                "Variables found - CARRIER: %d, CONTRACT_NUMBER: %d, deliveryType: %d, XML_FILE_NAME: %d",
                data.size(), carrierFound, contractNumberFound, deliveryTypeFound, xmlFileNameFound);

        return LetterGenerationResult.builder()
                .totalProcessed(data.size())
                .carrierFound(carrierFound)
                .contractNumberFound(contractNumberFound)
                .deliveryTypeFound(deliveryTypeFound)
                .xmlFileNameFound(xmlFileNameFound)
                .processedAt(LocalDateTime.now())
                .excelFileName(excelFileName)
                .data(data)
                .message(message)
                .build();
    }

    /**
     * Generate Excel file from the result data
     */
    public byte[] generateExcel(LetterGenerationResult result) throws IOException {
        log.info("Generating Excel file with {} records", result.getData().size());

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Letter Generation Data");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < EXCEL_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(EXCEL_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            int rowNum = 1;
            for (LetterGenerationData data : result.getData()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(nullSafe(data.getCorrespondenceCorrelationId()));
                row.createCell(1).setCellValue(nullSafe(data.getDocumentNumber()));
                row.createCell(2).setCellValue(nullSafe(data.getCarrier()));
                row.createCell(3).setCellValue(nullSafe(data.getContractNumber()));
                row.createCell(4).setCellValue(nullSafe(data.getDeliveryType()));
                row.createCell(5).setCellValue(nullSafe(data.getXmlFileName()));
            }

            // Auto-size columns
            for (int i = 0; i < EXCEL_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
                // Set max width to 50 characters (approximately 50 * 256)
                int maxWidth = 50 * 256;
                if (sheet.getColumnWidth(i) > maxWidth) {
                    sheet.setColumnWidth(i, maxWidth);
                }
            }

            // Add summary sheet
            createSummarySheet(workbook, result);

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Create summary sheet in the workbook
     */
    private void createSummarySheet(Workbook workbook, LetterGenerationResult result) {
        Sheet summarySheet = workbook.createSheet("Summary");

        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        int rowNum = 0;

        // Processing Summary
        Row titleRow = summarySheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Letter Generation Processing Summary");
        titleCell.setCellStyle(headerStyle);

        summarySheet.createRow(rowNum++); // Empty row

        addSummaryRow(summarySheet, rowNum++, "Total Processed", result.getTotalProcessed());

        summarySheet.createRow(rowNum++); // Empty row

        Row variablesTitle = summarySheet.createRow(rowNum++);
        Cell variablesTitleCell = variablesTitle.createCell(0);
        variablesTitleCell.setCellValue("Variable Statistics");
        variablesTitleCell.setCellStyle(headerStyle);

        addSummaryRow(summarySheet, rowNum++, "CARRIER found", 
                result.getCarrierFound() + "/" + result.getTotalProcessed());
        addSummaryRow(summarySheet, rowNum++, "CONTRACT_NUMBER found", 
                result.getContractNumberFound() + "/" + result.getTotalProcessed());
        addSummaryRow(summarySheet, rowNum++, "deliveryType found", 
                result.getDeliveryTypeFound() + "/" + result.getTotalProcessed());
        addSummaryRow(summarySheet, rowNum++, "XML_FILE_NAME found", 
                result.getXmlFileNameFound() + "/" + result.getTotalProcessed());

        summarySheet.createRow(rowNum++); // Empty row

        addSummaryRow(summarySheet, rowNum++, "Processed At", 
                result.getProcessedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // Auto-size columns
        summarySheet.autoSizeColumn(0);
        summarySheet.autoSizeColumn(1);
    }

    private void addSummaryRow(Sheet sheet, int rowNum, String label, Object value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value.toString());
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
