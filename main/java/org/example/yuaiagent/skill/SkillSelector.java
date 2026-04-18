package org.example.yuaiagent.skill;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SkillSelector {

    private final SkillRegistry skillRegistry;

    public SkillContext buildContextForPrompt(String userPrompt, int maxLoad) {
        List<SkillRegistry.SkillIndexEntry> index = skillRegistry.listSkills();
        if (index.isEmpty()) {
            return new SkillContext(List.of(), List.of());
        }

        // extremely small matching: keyword overlap against description + name
        String q = (userPrompt == null ? "" : userPrompt).toLowerCase(Locale.ROOT);

        record Scored(SkillRegistry.SkillIndexEntry entry, int score) {}
        List<Scored> scored = new ArrayList<>();
        for (var e : index) {
            String hay = (e.name() + " " + e.description()).toLowerCase(Locale.ROOT);
            int s = 0;
            for (String token : tokenize(q)) {
                if (token.length() < 2) continue;
                if (hay.contains(token)) {
                    s++;
                }
            }
            scored.add(new Scored(e, s));
        }
        scored.sort((a, b) -> Integer.compare(b.score, a.score));

        List<SkillContext.LoadedSkill> loaded = new ArrayList<>();
        for (var s : scored) {
            if (loaded.size() >= maxLoad) break;
            if (s.score <= 0) continue;
            skillRegistry.readSkillByPath(s.entry.path())
                    .ifPresent(content -> loaded.add(new SkillContext.LoadedSkill(s.entry.name(), content)));
        }

        return new SkillContext(index, loaded);
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return List.of(text.split("[^\\p{L}\\p{N}]+"));
    }
}
