package org.example.yuaiagent.skill;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "skill_task_stats")
public class SkillTaskStat {

    @Id
    private String taskKey;

    private long count;

    private String skillName;

    private LocalDateTime updatedAt;

    public SkillTaskStat() {
    }

    public SkillTaskStat(String taskKey) {
        this.taskKey = taskKey;
        this.count = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public String getTaskKey() {
        return taskKey;
    }

    public void setTaskKey(String taskKey) {
        this.taskKey = taskKey;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
