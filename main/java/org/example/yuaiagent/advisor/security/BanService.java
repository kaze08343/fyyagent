package org.example.yuaiagent.advisor.security;

import java.time.*;
import java.util.concurrent.*;

public class BanService {

    private final ConcurrentHashMap<String, Integer> violations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> bannedUsers = new ConcurrentHashMap<>();

    private static final int BAN_THRESHOLD = 10;
    private static final Duration BAN_DURATION = Duration.ofHours(1);

    public void record(String userId, int count) {
        int total = violations.merge(userId, count, Integer::sum);

        if (total >= BAN_THRESHOLD) {
            bannedUsers.put(userId, LocalDateTime.now().plus(BAN_DURATION));
        }
    }

    public boolean isBanned(String userId) {
        LocalDateTime expire = bannedUsers.get(userId);
        if (expire == null) return false;

        if (LocalDateTime.now().isAfter(expire)) {
            bannedUsers.remove(userId);
            return false;
        }
        return true;
    }
}