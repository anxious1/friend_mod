package com.mom.teammod;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
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
        bannedWords.clear();

        // 3 файла, которые ты добавил
        loadWordFileToSet("/assets/teammod/banned_words_ru.txt", bannedWords);
        loadWordFileToSet("/assets/teammod/banned_words_en.txt", bannedWords);
        loadWordFileToSet("/assets/teammod/banned_words_translit.txt", bannedWords);
    }

    private static void loadWordFileToSet(String resourcePath, Set<String> out) {
        try (InputStream is = CensorService.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.out.println("[TeamMod:Censor] Missing resource: " + resourcePath);
                return;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (line.startsWith("#")) continue;

                    out.add(line.toLowerCase(Locale.ROOT));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        String norm = normalizeForCensor(s);
        if (norm.isEmpty()) return false;

        // токены: только буквы/цифры (и кириллица, и латиница)
        String[] tokens = norm.split("\\s+");

        for (String bw : bannedWords) {
            if (bw == null) continue;
            String w = bw.trim().toLowerCase(Locale.ROOT);
            if (w.isEmpty()) continue;

            // Супер-важно: короткие бан-слова не ищем как подстроку
            if (w.length() <= 3) {
                // 1) если вся строка == бан-слово (важно для тегов типа "abc")
                if (norm.equals(w)) return true;
                // 2) если какой-то токен == бан-слово
                for (String t : tokens) {
                    if (t.equals(w)) return true;
                }
                continue;
            }

            // Длинные: ищем в токенах, а не в сырой строке (меньше фп)
            for (String t : tokens) {
                if (t.contains(w)) return true;
            }
        }

        return false;
    }

    private static String normalizeForCensor(String s) {
        String lower = s.toLowerCase(Locale.ROOT)
                .replace('ё', 'е');

        // заменяем всё, что не буква/цифра, на пробел
        lower = lower.replaceAll("[^\\p{L}\\p{N}]+", " ");

        // схлопываем пробелы
        lower = lower.trim().replaceAll("\\s{2,}", " ");
        return lower;
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