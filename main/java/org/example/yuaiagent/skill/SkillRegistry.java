package org.example.yuaiagent.skill;

import org.example.yuaiagent.constant.FileConstant;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Component
public class SkillRegistry {

    public Path skillRoot() {
        return Paths.get(FileConstant.FILE_SAVE_DIR, "skill");
    }

    public List<SkillIndexEntry> listSkills() {
        Path root = skillRoot();
        if (!Files.exists(root)) {
            return List.of();
        }

        List<SkillIndexEntry> result = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            stream.filter(p -> p.getFileName().toString().equalsIgnoreCase("SKILL.md"))
                    .forEach(p -> {
                        try {
                            String content = Files.readString(p, StandardCharsets.UTF_8);
                            Optional<String> name = extractFrontmatterField(content, "name");
                            Optional<String> description = extractFrontmatterField(content, "description");
                            result.add(new SkillIndexEntry(
                                    name.orElse(p.getParent().getFileName().toString()),
                                    description.orElse(""),
                                    p
                            ));
                        } catch (Exception ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }

        result.sort(Comparator.comparing(SkillIndexEntry::name));
        return result;
    }

    public Optional<String> readSkillByName(String skillName) {
        for (SkillIndexEntry e : listSkills()) {
            if (e.name().equals(skillName)) {
                return readSkillByPath(e.path());
            }
        }
        return Optional.empty();
    }

    public Optional<String> readSkillByPath(Path skillMdPath) {
        try {
            return Optional.of(Files.readString(skillMdPath, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public Path upsertSkill(String skillName, String skillMdContent) {
        // skill/<skillName>/SKILL.md
        Path dir = skillRoot().resolve(skillName);
        Path file = dir.resolve("SKILL.md");
        try {
            Files.createDirectories(dir);
            Files.writeString(file, skillMdContent, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write skill: " + file, e);
        }
    }

    private Optional<String> extractFrontmatterField(String content, String field) {
        // minimal frontmatter parse: --- ... ---
        int start = content.indexOf("---");
        if (start != 0) {
            return Optional.empty();
        }
        int end = content.indexOf("---", 3);
        if (end < 0) {
            return Optional.empty();
        }
        String fm = content.substring(3, end);
        for (String line : fm.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(field + ":")) {
                String value = trimmed.substring((field + ":").length()).trim();
                // strip quotes if present
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    public record SkillIndexEntry(String name, String description, Path path) {}
}
