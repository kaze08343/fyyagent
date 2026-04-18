package org.example.yuaiagent.skill;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SkillEvolutionService {

    public static final int DEFAULT_THRESHOLD = 3;

    private final SkillTaskStatRepository statRepository;
    private final SkillRegistry skillRegistry;

    public String recordTaskAndGetTaskKey(String userPrompt) {
        // Very small heuristic key; can be improved later.
        String normalized = userPrompt == null ? "" : userPrompt;
        normalized = normalized.replaceAll("\\s+", " ").trim();
        if (normalized.length() > 120) {
            normalized = normalized.substring(0, 120);
        }
        return normalized.toLowerCase();
    }

    public long incrementUsage(String taskKey) {
        SkillTaskStat stat = statRepository.findById(taskKey).orElseGet(() -> new SkillTaskStat(taskKey));
        stat.setCount(stat.getCount() + 1);
        stat.setUpdatedAt(LocalDateTime.now());
        statRepository.save(stat);
        return stat.getCount();
    }

    public Optional<String> getBoundSkillName(String taskKey) {
        return statRepository.findById(taskKey)
                .map(SkillTaskStat::getSkillName)
                .filter(StrUtil::isNotBlank);
    }

    public void bindSkillName(String taskKey, String skillName) {
        SkillTaskStat stat = statRepository.findById(taskKey).orElseGet(() -> new SkillTaskStat(taskKey));
        stat.setSkillName(skillName);
        stat.setUpdatedAt(LocalDateTime.now());
        statRepository.save(stat);
    }

    public boolean shouldEvolve(long usageCount, boolean hasSkillAlready) {
        if (hasSkillAlready) {
            return false;
        }
        return usageCount >= DEFAULT_THRESHOLD;
    }

    /**
     * Create or update a skill file on disk.
     */
    public void upsertSkill(String skillName, String skillMdContent) {
        skillRegistry.upsertSkill(skillName, skillMdContent);
    }
}
