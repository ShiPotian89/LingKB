package com.ling.lingkb.parser;
/*
 * ------------------------------------------------------------------
 * Copyright @ 2025 Hangzhou Ling Technology Co.,Ltd. All rights reserved.
 * ------------------------------------------------------------------
 * Product: LingKB
 * Module Name: LingKB
 * Date Created: 2025/6/19
 * Description:
 * ------------------------------------------------------------------
 * Modification History
 * DATE            Name           Description
 * ------------------------------------------------------------------
 * 2025/6/19       spt
 * ------------------------------------------------------------------
 */

import com.alibaba.fastjson.JSON;
import com.ling.lingkb.common.entity.DocumentParseResult;
import com.ling.lingkb.common.exception.DocumentParseException;
import com.ling.lingkb.common.exception.UnsupportedDocumentTypeException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Excel解析器
 *
 * @author shipotian
 * @date 2025/6/19
 * @since 1.0.0
 */
public class ExcelParser implements DocumentParser {

    @Override
    public DocumentParseResult parse(File file) throws DocumentParseException {
        String fileName = file.getName();
        DocumentParseResult result = new DocumentParseResult();
        result.setSourceFileName(fileName);

        try (FileInputStream fis = new FileInputStream(file)) {
            String textContent;
            DocumentParseResult.DocumentMetadata.DocumentMetadataBuilder builder =
                    DocumentParseResult.DocumentMetadata.builder();
            if (fileName.endsWith(".xlsx")) {
                XSSFWorkbook xssfWorkbook = new XSSFWorkbook(fis);
                textContent = getTextContent(xssfWorkbook);
                builder.author(xssfWorkbook.getProperties().getCoreProperties().getCreator())
                        .creationDate(xssfWorkbook.getProperties().getCoreProperties().getCreated().toString())
                        .pageCount(String.valueOf(xssfWorkbook.getNumberOfSheets())).build();
            } else if (fileName.endsWith(".xls")) {
                HSSFWorkbook hssfWorkbook = new HSSFWorkbook(fis);
                textContent = getTextContent(hssfWorkbook);
                SummaryInformation summary = hssfWorkbook.getSummaryInformation();
                builder.author(summary.getAuthor()).creationDate(summary.getCreateDateTime().toString())

                        .pageCount(String.valueOf(summary.getPageCount())).build();
            } else {
                throw new UnsupportedDocumentTypeException("不支持的Excel文件格式: " + fileName);
            }
            result.setTextContent(textContent);
            result.setMetadata(builder.build());
            return result;
        } catch (IOException e) {
            throw new DocumentParseException("解析Excel文件失败: " + fileName, e);
        }
    }

    private String getTextContent(Workbook workbook) throws IOException {
        Map<String, List<List<String>>> allSheetData = new LinkedHashMap<>();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            String sheetName = workbook.getSheetName(i);
            Sheet sheet = workbook.getSheet(sheetName);
            List<List<String>> sheetData = extractSheetData(sheet, 1000);
            allSheetData.put(sheetName, sheetData);
        }
        return JSON.toJSONString(allSheetData);
    }

    private List<List<String>> extractSheetData(Sheet sheet, int maxRows) {
        List<List<String>> sheetData = new ArrayList<>();
        int rowCount = 0;

        for (Row row : sheet) {
            if (rowCount++ >= maxRows) {
                break; // 限制最大行数，防止OOM
            }

            List<String> rowData = new ArrayList<>();
            for (Cell cell : row) {
                rowData.add(getCellValueAsString(cell));
            }
            sheetData.add(rowData);
        }
        return sheetData;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    @Override
    public List<String> supportedTypes() {
        return Arrays.asList("xls", "xlsx");
    }
}
