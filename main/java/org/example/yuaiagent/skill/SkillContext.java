package org.example.yuaiagent.skill;

import java.util.List;

public record SkillContext(List<SkillRegistry.SkillIndexEntry> index,
                           List<LoadedSkill> loaded) {

    public record LoadedSkill(String name, String content) {}
}
