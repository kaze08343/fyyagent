package org.example.yuaiagent.advisor.dfa;

import java.util.*;

public class SensitiveWordFilter {

    private final Map<Character, Map> root = new HashMap<>();
    private static final char END = '#';

    public void addWord(String word) {
        Map<Character, Map> current = root;

        for (char c : word.toCharArray()) {
            current.putIfAbsent(c, new HashMap<>());
            current = current.get(c);
        }
        current.put(END, null);
    }

    public void loadWords(Collection<String> words) {
        for (String w : words) {
            addWord(w);
        }
    }

    public List<String> match(String text) {
        List<String> result = new ArrayList<>();

        for (int i = 0; i < text.length(); i++) {
            Map<Character, Map> current = root;
            int j = i;
            StringBuilder sb = new StringBuilder();

            while (j < text.length() && current.containsKey(text.charAt(j))) {
                char c = text.charAt(j);
                sb.append(c);
                current = current.get(c);

                if (current.containsKey(END)) {
                    result.add(sb.toString());
                }
                j++;
            }
        }
        return result;
    }

    public String replace(String text) {
        List<String> words = match(text);
        for (String w : words) {
            text = text.replace(w, "*".repeat(w.length()));
        }
        return text;
    }
}