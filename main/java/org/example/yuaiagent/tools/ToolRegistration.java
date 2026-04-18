package org.example.yuaiagent.tools;


import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 集中的工具注册类
 */
@Configuration
public class ToolRegistration {

    @Value("${search-api.api-key:Pehg7HBWeiFZqMtrZSMu6JKd}")
    private String searchApiKey;

    @Bean
    public WebSearchTool webSearchTool() {
        return new WebSearchTool(searchApiKey);
    }

    @Bean
    public TerminalOperationTool terminalOperationTool() {
        return new TerminalOperationTool();
    }

    @Bean
    public PDFGenerationTool pdfGenerationTool() {
        return new PDFGenerationTool();
    }

    @Bean
    public TerminateTool terminateTool() {
        return new TerminateTool();
    }

    @Bean
    public JsonToolExecutor webSearchToolExecutor(WebSearchTool webSearchTool) {
        return new WebSearchToolExecutor(webSearchTool);
    }

    @Bean
    public JsonToolExecutor terminalOperationToolExecutor(TerminalOperationTool terminalOperationTool) {
        return new TerminalOperationToolExecutor(terminalOperationTool);
    }

    @Bean
    public JsonToolExecutor pdfGenerationToolExecutor(PDFGenerationTool pdfGenerationTool) {
        return new PDFGenerationToolExecutor(pdfGenerationTool);
    }

    @Bean
    public JsonToolExecutor terminateToolExecutor(TerminateTool terminateTool) {
        return new TerminateToolExecutor(terminateTool);
    }

    @Bean
    public ToolCallback[] allTools(WebSearchTool webSearchTool,
                                   TerminalOperationTool terminalOperationTool,
                                   PDFGenerationTool pdfGenerationTool,
                                   TerminateTool terminateTool) {
        return ToolCallbacks.from(
                webSearchTool,
                terminalOperationTool,
                pdfGenerationTool,
                terminateTool
        );
    }

    @Bean
    public List<JsonToolExecutor> jsonToolExecutors(JsonToolExecutor webSearchToolExecutor,
                                                    JsonToolExecutor terminalOperationToolExecutor,
                                                    JsonToolExecutor pdfGenerationToolExecutor,
                                                    JsonToolExecutor terminateToolExecutor) {
        return List.of(
                webSearchToolExecutor,
                terminalOperationToolExecutor,
                pdfGenerationToolExecutor,
                terminateToolExecutor
        );
    }
}
