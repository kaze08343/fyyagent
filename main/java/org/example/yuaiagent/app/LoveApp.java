package org.example.yuaiagent.app;

import dev.langchain4j.agent.tool.P;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.yuaiagent.advisor.MyLoggerAdvisor;
import org.example.yuaiagent.advisor.SensitiveWordAdvisor;
import org.example.yuaiagent.chatmemory.FileBasedChatMemory;
import org.example.yuaiagent.chatmemory.mangodb.ChatSessionRepository;
import org.example.yuaiagent.chatmemory.mangodb.LongShortTermMemoryService;
import org.example.yuaiagent.chatmemory.mangodb.MongoChatMemory;
import org.example.yuaiagent.rag.LoveAppRagCustomAdvisorFactory;
import org.example.yuaiagent.rag.QueryRewriter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {


    private final ChatClient chatClient;
    private final ToolCallback[] allTools;

//    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
//            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
//            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
//            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";
    private static final String SYSTEM_PROMPT ="\"\"\"\n" +
        "            你是一个智能助手，可以访问多种工具来帮助用户完成任务。\n" ;

    public LoveApp(ChatModel dashscopeChatModel , ChatSessionRepository chatSessionRepository, LongShortTermMemoryService l,  ToolCallback[] allTools) {
        this.allTools = allTools;
//        String fileDir = System.getProperty("user.dir") + "/chat-memory";
//        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        ChatMemory chatMemory = new MongoChatMemory(chatSessionRepository,l);
//       ChatMemory chatMemory = new InMemoryChatMemory();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new SensitiveWordAdvisor(),
                        new MessageChatMemoryAdvisor(chatMemory),
                        new MyLoggerAdvisor()
                )
//                .defaultTools(allTools)
                .build();
    }
    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
    record LoveReport(String title, List<String> suggestions) {
    }
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }
    @Resource
    private VectorStore loveAppVectorStore;
    @Resource
    private QueryRewriter queryRewriter;
    public String doChatWithRag(String message, String chatId) {

//        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
       int n= allTools.length;
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))

                .advisors(new MyLoggerAdvisor())
                .tools(allTools)
//                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                .advisors(LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(loveAppVectorStore, "已婚"))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}

