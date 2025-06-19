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
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * PDF文件解析
 *
 * @author shipotian
 * @date 2025/6/19
 * @since 1.0.0
 */
public class PdfParser implements DocumentParser {

    @Override
    public DocumentParseResult parse(File file) throws DocumentParseException {
        DocumentParseResult result = new DocumentParseResult();
        String fileName = file.getName();
        result.setSourceFileName(fileName);
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            //按位置排序文本（适用于复杂布局）
            stripper.setSortByPosition(true);
            // 从第1页开始
            stripper.setStartPage(1);
            String text = stripper.getText(document);
            result.setTextContent(text);
            // 提取元数据
            PDDocumentInformation info = document.getDocumentInformation();
            DocumentParseResult.DocumentMetadata documentMetadata =
                    DocumentParseResult.DocumentMetadata.builder().author(info.getAuthor())
                            .creationDate(info.getCreationDate().toString())
                            .pageCount(String.valueOf(document.getNumberOfPages())).build();
            result.setMetadata(documentMetadata);

            return result;
        } catch (IOException e) {
            throw new DocumentParseException("解析PDF失败：" + fileName, e);
        }
    }

    @Override
    public List<String> supportedTypes() {
        return Arrays.asList("pdf", "PDF");
    }
}
