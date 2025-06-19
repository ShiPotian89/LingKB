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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Txt解析器
 *
 * @author shipotian
 * @date 2025/6/19
 * @since 1.0.0
 */
@Component
public class TxtParser implements DocumentParser {

    /**
     * 解析 TXT 文档，提取文本内容及基础元数据。
     *
     * <p><b>WARNING</b>: TXT 是纯文本格式，<u>无内置“作者”“页数”等结构化元数据</u>。<br>
     *
     * @param file 待解析的 TXT 文件，不可为 null
     * @return 解析结果（包含文本内容、文件名等基础元数据）
     * @throws DocumentParseException 文件读取失败或编码不支持时抛出
     * @implSpec 文本内容按 UTF-8 编码读取，异常时直接抛出 DocumentParseException。
     * @apiNote 适用于通用纯文本解析，如需复杂元数据建议使用 PDF/Word 等格式。
     */
    @Override
    public DocumentParseResult parse(File file) throws DocumentParseException {
        String fileName = file.getName();
        DocumentParseResult result = new DocumentParseResult();
        result.setSourceFileName(fileName);
        try {
            // 提取文本内容（按 UTF-8 编码读取，可根据实际情况调整）
            String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            result.setTextContent(text);
            // 提取元数据
            DocumentParseResult.DocumentMetadata metadata =
                    DocumentParseResult.DocumentMetadata.builder().creationDate(String.valueOf(file.lastModified()))
                            .build();
            result.setMetadata(metadata);

            return result;
        } catch (IOException e) {
            throw new DocumentParseException("解析TXT文件失败: " + fileName, e);
        }
    }

    @Override
    public List<String> supportedTypes() {
        return List.of("txt");
    }
}
