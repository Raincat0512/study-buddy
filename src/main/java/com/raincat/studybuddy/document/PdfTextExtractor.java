package com.raincat.studybuddy.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 用 PDFBox 抽取 PDF 纯文本。已知限制：不处理图片、表格和复杂版式 */
@Component
public class PdfTextExtractor {

    public String extract(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new IllegalStateException("PDF 解析失败：" + e.getMessage(), e);
        }
    }
}
