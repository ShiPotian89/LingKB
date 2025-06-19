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

import com.ling.lingkb.common.entity.DocumentParseResult;
import com.ling.lingkb.common.exception.DocumentParseException;
import com.ling.lingkb.common.exception.UnsupportedDocumentTypeException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

/**
 * Word文件解析
 *
 * @author shipotian
 * @date 2025/6/19
 * @since 1.0.0
 */
@Component
public class WordParser implements DocumentParser {

    @Override
    public DocumentParseResult parse(File file) throws DocumentParseException {
        String fileName = file.getName();
        DocumentParseResult result = new DocumentParseResult();
        result.setSourceFileName(fileName);
        try {
            DocumentParseResult.DocumentMetadata.DocumentMetadataBuilder builder =
                    DocumentParseResult.DocumentMetadata.builder();
            // 根据文件扩展名判断是.doc还是.docx
            if (fileName.endsWith(".docx")) {
                XWPFDocument doc = new XWPFDocument(new FileInputStream(file.getAbsolutePath()));
                XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
                String text = extractor.getText();
                result.setTextContent(text);
                builder.author(doc.getProperties().getCoreProperties().getCreator())
                        .creationDate(doc.getProperties().getCoreProperties().getCreated().toString())
                        .pageCount(String.valueOf(doc.getProperties().getExtendedProperties().getPages()));
                result.setMetadata(builder.build());
                extractor.close();
                doc.close();
            } else if (fileName.endsWith(".doc")) {
                HWPFDocument doc = new HWPFDocument(new FileInputStream(file.getAbsolutePath()));
                WordExtractor extractor = new WordExtractor(doc);
                String text = extractor.getText();
                result.setTextContent(text);
                builder.author(doc.getSummaryInformation().getAuthor())
                        .creationDate(doc.getSummaryInformation().getCreateDateTime().toString())
                        .pageCount(String.valueOf(doc.getSummaryInformation().getPageCount()));
                result.setMetadata(builder.build());
                extractor.close();
                doc.close();
            } else {
                throw new UnsupportedDocumentTypeException("不支持的Word文件格式: " + fileName);
            }
            return result;
        } catch (IOException e) {
            throw new DocumentParseException("解析Word失败：" + fileName, e);
        }
    }

    @Override
    public List<String> supportedTypes() {
        return Arrays.asList("doc", "docx");
    }
}
