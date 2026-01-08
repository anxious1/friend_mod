package com.mom.teammod;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

public class CensorService {
    private static final CensorService INSTANCE = new CensorService();
    public static CensorService getInstance() { return INSTANCE; }

    private final Set<String> bannedWords = new HashSet<>();
    private final List<String> safeNames = new ArrayList<>();
    private final List<String> safeTags  = new ArrayList<>();
    private int tagIndex = 0;

    private CensorService() {
        loadBannedWords();
        loadJsonLists();
    }

    private void loadBannedWords() {
        try (Reader r = new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/assets/teammod/banned_words.txt")))) {
            new Scanner(r).forEachRemaining(w -> bannedWords.add(w.toLowerCase(Locale.ROOT)));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadJsonLists() {
        Gson gson = new Gson();
        try {
            // safe_names.json
            try (Reader r = new InputStreamReader(
                    Objects.requireNonNull(getClass().getResourceAsStream("/assets/teammod/safe_names.json")))) {
                safeNames.addAll(gson.fromJson(r, new TypeToken<List<String>>(){}.getType()));
            }
            // safe_tags.json
            try (Reader r = new InputStreamReader(
                    Objects.requireNonNull(getClass().getResourceAsStream("/assets/teammod/safe_tags.json")))) {
                safeTags.addAll(gson.fromJson(r, new TypeToken<List<String>>(){}.getType()));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public boolean isDirty(String s) {
        if (s == null) return false;
        String lower = s.toLowerCase(Locale.ROOT);
        return bannedWords.stream().anyMatch(lower::contains);
    }

    public String getSafeTag(String original) {
        if (!isDirty(original)) return original;
        return findFreeTag();
    }

    public String getSafeName(String original) {
        if (!isDirty(original)) return original;
        return safeNames.get(new Random().nextInt(safeNames.size()));
    }

    private String findFreeTag() {
        for (int i = 0; i < safeTags.size(); i++) {
            String tag = safeTags.get((tagIndex + i) % safeTags.size());
            if (isTagFree(tag)) {
                tagIndex = (tagIndex + i + 1) % safeTags.size();
                return tag;
            }
        }
        return safeTags.get(new Random().nextInt(safeTags.size())); // крайний случай
    }

    private boolean isTagFree(String tag) {
        return TeamManager.clientTeams.values().stream()
                .noneMatch(t -> t.getTag().equalsIgnoreCase(tag));
    }
}