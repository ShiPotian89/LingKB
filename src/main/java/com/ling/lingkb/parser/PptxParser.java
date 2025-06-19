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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Component;

/**
 * PPT文档解析
 *
 * @author shipotian
 * @date 2025/6/19
 * @since 1.0.0
 */
@Component
public class PptxParser implements DocumentParser {

    @Override
    public DocumentParseResult parse(File file) throws DocumentParseException {
        String fileName = file.getName();
        DocumentParseResult result = new DocumentParseResult();
        result.setSourceFileName(fileName);

        try (FileInputStream fis = new FileInputStream(file); XMLSlideShow ppt = new XMLSlideShow(fis)) {
            String textContent = extractSlideText(ppt);
            result.setTextContent(textContent);

            DocumentParseResult.DocumentMetadata metadata = DocumentParseResult.DocumentMetadata.builder()
                    .author(ppt.getProperties().getCoreProperties().getCreator())
                    .creationDate(ppt.getProperties().getCoreProperties().getCreated().toString())
                    .pageCount(String.valueOf(ppt.getSlides().size())).build();
            result.setMetadata(metadata);
        } catch (IOException e) {
            throw new DocumentParseException("解析PPTX文件失败: " + fileName, e);
        }

        return result;
    }

    /**
     * 提取每张幻灯片的文本内容（标题、正文、备注）
     */
    private String extractSlideText(XMLSlideShow ppt) {
        Map<Integer, List<String>> slideTextMap = new TreeMap<>();
        List<XSLFSlide> slides = ppt.getSlides();

        for (int i = 0; i < slides.size(); i++) {
            XSLFSlide slide = slides.get(i);
            List<String> textList = new ArrayList<>();
            // 提取标题文本（正确方式）
            for (XSLFShape shape : slide.getShapes()) {
                if (shape instanceof XSLFTextShape) {
                    XSLFTextShape textShape = (XSLFTextShape) shape;
                    if (textShape.getText() != null && !textShape.getText().trim().isEmpty()) {
                        textList.add(textShape.getText());
                    }
                }
            }

            // 提取备注文本（正确方式）
            XSLFNotes notes = slide.getNotes();
            if (notes != null) {
                for (XSLFShape noteShape : notes.getShapes()) {
                    if (noteShape instanceof XSLFTextShape) {
                        XSLFTextShape textShape = (XSLFTextShape) noteShape;
                        if (textShape.getText() != null && !textShape.getText().trim().isEmpty()) {
                            textList.add("备注: " + textShape.getText());
                        }
                    }
                }
            }
            // 幻灯片编号从 1 开始
            slideTextMap.put(i + 1, textList);
        }
        return JSON.toJSONString(slideTextMap);
    }

    @Override
    public List<String> supportedTypes() {
        return List.of("pptx");
    }
}
