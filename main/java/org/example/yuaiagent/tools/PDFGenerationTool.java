//package org.example.yuaiagent.tools;
//
//import cn.hutool.core.io.FileUtil;
//import com.itextpdf.kernel.font.PdfFont;
//import com.itextpdf.kernel.font.PdfFontFactory;
//import com.itextpdf.kernel.pdf.PdfDocument;
//import com.itextpdf.kernel.pdf.PdfWriter;
//import com.itextpdf.layout.Document;
//import com.itextpdf.layout.element.Paragraph;
//
//import org.example.yuaiagent.constant.FileConstant;
//import org.springframework.ai.tool.annotation.Tool;
//import org.springframework.ai.tool.annotation.ToolParam;
//
//import java.io.IOException;

/**
 * PDF 生成工具
 */
//public class PDFGenerationTool {
//
//    @Tool(description = "Generate a PDF file with given content", returnDirect = false)
//    public String generatePDF(
//            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
//            @ToolParam(description = "Content to be included in the PDF") String content) {
//        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
//        String filePath = fileDir + "/" + fileName;
//        try {
//            // 创建目录
//            FileUtil.mkdir(fileDir);
//            // 创建 PdfWriter 和 PdfDocument 对象
//            try (PdfWriter writer = new PdfWriter(filePath);
//                 PdfDocument pdf = new PdfDocument(writer);
//                 Document document = new Document(pdf)) {
//                // 自定义字体（需要人工下载字体文件到特定目录）
////                String fontPath = Paths.get("src/main/resources/static/fonts/simsun.ttf")
////                        .toAbsolutePath().toString();
////                PdfFont font = PdfFontFactory.createFont(fontPath,
////                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
//                // 使用内置中文字体
////                PdfFont font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
////                document.setFont(font);
//                // 创建段落
//                Paragraph paragraph = new Paragraph(content);
//                // 添加段落并关闭文档
//                document.add(paragraph);
//            }
//            return "PDF generated successfully to: " + filePath;
//        } catch (IOException e) {
//            return "Error generating PDF: " + e.getMessage();
//        }
//    }
//}
package org.example.yuaiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;

import org.example.yuaiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.nio.file.Paths;

public class PDFGenerationTool {

    @Tool(description = "Generate a PDF file with given content. Supports Chinese text.")
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF (e.g., report.pdf)") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {

        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String filePath = fileDir + "/" + fileName;

        try {
            FileUtil.mkdir(fileDir);

            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {

                PdfFont font = loadChineseFont();
                document.setFont(font);
                document.setFontSize(12);
                String safeContent = sanitizeUnsupportedGlyphs(content, font);

                Paragraph paragraph = new Paragraph(safeContent)
                        .setTextAlignment(TextAlignment.LEFT)
                        .setMultipliedLeading(1.5f);

                document.add(paragraph);
            }

            return "PDF generated successfully! File path: " + filePath;
        } catch (Exception e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }

    private PdfFont loadChineseFont() throws IOException {
        try {
            return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H",
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        } catch (Exception e1) {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                String fontPath;

                if (os.contains("win")) {
                    fontPath = "C:/Windows/Fonts/simsun.ttc,0";
                } else if (os.contains("mac")) {
                    fontPath = "/Library/Fonts/Songti.ttc,0";
                } else {
                    fontPath = "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc,0";
                }

                return PdfFontFactory.createFont(fontPath, "Identity-H",
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            } catch (Exception e2) {
                throw new IOException("Failed to load Chinese font. Please ensure a Chinese font is installed on the system.", e2);
            }
        }
    }

    /**
     * iText 在遇到字体不支持的字符（如 emoji）时可能抛出 glyph 为空异常。
     * 这里将不支持字符替换为占位符，避免整次 PDF 生成失败。
     */
    private String sanitizeUnsupportedGlyphs(String content, PdfFont font) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        content.codePoints().forEach(codePoint -> {
            if (font.containsGlyph(codePoint)) {
                builder.appendCodePoint(codePoint);
            } else if (codePoint == '\n' || codePoint == '\r' || codePoint == '\t') {
                builder.appendCodePoint(codePoint);
            } else {
                builder.append('□');
            }
        });
        return builder.toString();
    }
}
