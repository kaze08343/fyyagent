package org.example.yuaiagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileBasedChatMemory implements ChatMemory {

    private final String BASE_DIR;
    private static final Kryo kryo = new Kryo();
    static {
        kryo.setRegistrationRequired(false);

        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }
    public FileBasedChatMemory(String dir) {
        BASE_DIR = dir;
        File baseDir=new File(dir);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }
    @Override
    public void add(String conversationId, Message message) {
        saveConversation(conversationId, List.of(message));
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> messageList = getOrCreateConversation(conversationId);
        messageList.addAll(messages);
        saveConversation(conversationId, messageList);

    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message>messageList = getOrCreateConversation(conversationId);
        if (lastN <= 0 || messageList.isEmpty()) {
            return List.of();
        }
        return messageList.stream()
                .skip(messageList.size() - lastN)
                .toList();
    }

    @Override
    public void clear(String conversationId) {

    }

    private List<Message> getOrCreateConversation(String conversationId) {
        File file = getConversationFile(conversationId);
        List<Message> messages = new ArrayList<>();
        if (file.exists()) {
            try (Input input = new Input(new FileInputStream(file))) {
                messages = kryo.readObject(input, ArrayList.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return messages;
    }
    private void saveConversation(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            kryo.writeObject(output, messages);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private File getConversationFile(String conversationId) {
        return new File(BASE_DIR, conversationId + ".kryo");
    }
}
