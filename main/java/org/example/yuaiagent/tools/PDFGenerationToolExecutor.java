package org.example.yuaiagent.tools;

import cn.hutool.json.JSONObject;

public class PDFGenerationToolExecutor implements JsonToolExecutor {

    private final PDFGenerationTool pdfGenerationTool;

    public PDFGenerationToolExecutor(PDFGenerationTool pdfGenerationTool) {
        this.pdfGenerationTool = pdfGenerationTool;
    }

    @Override
    public String toolName() {
        return "generatePDF";
    }

    @Override
    public String execute(JSONObject arguments) {
        String fileName = arguments == null ? "output.pdf" : arguments.getStr("fileName", "output.pdf");
        String content = arguments == null ? "" : arguments.getStr("content", "");
        return pdfGenerationTool.generatePDF(fileName, content);
    }
}
