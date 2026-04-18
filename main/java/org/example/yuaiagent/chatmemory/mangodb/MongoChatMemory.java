//package org.example.yuaiagent.chatmemory.mangodb;
//
//import org.springframework.ai.chat.memory.ChatMemory;
//import org.springframework.ai.chat.messages.Message;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Component
//public class MongoChatMemory implements ChatMemory {
//
//    private final ChatSessionRepository repository;
//
//    public MongoChatMemory(ChatSessionRepository repository) {
//        this.repository = repository;
//    }
//
//    @Override
//    public void add(String conversationId, List<Message> messages) {
//        ChatSession session = repository.findBySessionId(conversationId)
//                .orElseGet(() -> {
//                    ChatSession s = new ChatSession();
//                    s.sessionId = conversationId;
//                    s.createdAt = java.time.LocalDateTime.now();
//                    s.updatedAt = java.time.LocalDateTime.now();
//                    return s;
//                });
//
//        for (Message msg : messages) {
//            String role = determineRole(msg);
//            String content = msg.getText();
//            session.addMessage(role, content);
//        }
//
//
//        repository.save(session);
//    }
//    private String determineRole(Message msg) {
//        if (msg instanceof org.springframework.ai.chat.messages.UserMessage) {
//            return "user";
//        } else if (msg instanceof org.springframework.ai.chat.messages.AssistantMessage) {
//            return "assistant";
//        } else if (msg instanceof org.springframework.ai.chat.messages.SystemMessage) {
//            return "system";
//        }
//        return "unknown";
//    }
//       @Override
//    public List<Message> get(String conversationId, int lastN) {
//        return repository.findBySessionId(conversationId)
//                .map(session -> session.getLastNMessages(lastN)
//                        .stream()
//                        .map(this::toSpringAiMessage)
//                        .collect(Collectors.toList()))
//                .orElse(List.of());
//    }
//
//    private Message toSpringAiMessage(ChatSession.ChatMessage chatMessage) {
//        String role = chatMessage.role();
//        String content = chatMessage.content();
//
//        return switch (role.toLowerCase()) {
//            case "user" -> new org.springframework.ai.chat.messages.UserMessage(content);
//            case "assistant" -> new org.springframework.ai.chat.messages.AssistantMessage(content);
//            case "system" -> new org.springframework.ai.chat.messages.SystemMessage(content);
//            default -> new org.springframework.ai.chat.messages.UserMessage(content);
//        };
//    }
//// ... existing code ...
//
//    @Override
//    public void clear(String conversationId) {
//        repository.findBySessionId(conversationId)
//                .ifPresent(session -> {
//                    session.clearMessages();
//                    repository.save(session);
//                });
//    }
//}

package org.example.yuaiagent.chatmemory.mangodb;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MongoChatMemory implements ChatMemory {

    private final ChatSessionRepository repository;
    private final LongShortTermMemoryService longShortTermMemoryService;

    public MongoChatMemory(ChatSessionRepository repository, LongShortTermMemoryService longShortTermMemoryService) {
        this.repository = repository;
        this.longShortTermMemoryService = longShortTermMemoryService;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        for (Message msg : messages) {
            String role = determineRole(msg);
            String content = msg.getText();
            longShortTermMemoryService.addMessage(conversationId, role, content);
        }
    }

    private String determineRole(Message msg) {
        if (msg instanceof org.springframework.ai.chat.messages.UserMessage) {
            return "user";
        } else if (msg instanceof org.springframework.ai.chat.messages.AssistantMessage) {
            return "assistant";
        } else if (msg instanceof org.springframework.ai.chat.messages.SystemMessage) {
            return "system";
        }
        return "unknown";
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        return longShortTermMemoryService.getRecentMessages(conversationId, lastN)
                .stream()
                .map(this::toSpringAiMessage)
                .collect(Collectors.toList());
    }

    private Message toSpringAiMessage(ChatSession.ChatMessage chatMessage) {
        String role = chatMessage.role();
        String content = chatMessage.content();

        return switch (role.toLowerCase()) {
            case "user" -> new org.springframework.ai.chat.messages.UserMessage(content);
            case "assistant" -> new org.springframework.ai.chat.messages.AssistantMessage(content);
            case "system" -> new org.springframework.ai.chat.messages.SystemMessage(content);
            default -> new org.springframework.ai.chat.messages.UserMessage(content);
        };
    }

    @Override
    public void clear(String conversationId) {
//        longShortTermMemoryService.clearAllMemory(conversationId);
    }
}
