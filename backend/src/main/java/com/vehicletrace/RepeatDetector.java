package com.vehicletrace;

import java.util.HashSet;
import java.util.Set;

// FR8: decides whether a new problem is the same as a previous one
public class RepeatDetector {

    // Common words that don't describe the actual fault
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "was", "were", "are", "for", "with", "very", "again",
            "when", "while", "has", "have", "had", "not", "car", "vehicle",
            "problem", "issue", "from", "but", "too", "also", "some", "after",
            "before", "its", "this", "that", "there", "still", "keeps", "keep"
    );

    // How similar two problems must be to count as a repeat (60%)
    private static final double THRESHOLD = 0.6;

    // Returns true if the new problem matches the old one
    public static boolean isRepeat(String newProblem, String oldProblem) {
        return similarity(newProblem, oldProblem) >= THRESHOLD;
    }

    // Returns a score from 0.0 (nothing in common) to 1.0 (same key words)
    public static double similarity(String a, String b) {
        Set<String> wordsA = keywords(a);
        Set<String> wordsB = keywords(b);

        // If there are no key words, only an exact match counts
        if (wordsA.isEmpty() || wordsB.isEmpty()) {
            boolean same = a != null && b != null
                    && a.trim().equalsIgnoreCase(b.trim());
            return same ? 1.0 : 0.0;
        }

        Set<String> common = new HashSet<>(wordsA);
        common.retainAll(wordsB);

        return (double) common.size() / Math.min(wordsA.size(), wordsB.size());
    }

    // "Front brakes squeaking loudly" -> [front, brake, squeak, loud]
    public static Set<String> keywords(String text) {
        Set<String> result = new HashSet<>();
        if (text == null) return result;

        for (String word : text.toLowerCase().split("[^a-z0-9]+")) {
            if (word.length() < 3 || STOP_WORDS.contains(word)) continue;
            result.add(simplify(word));
        }
        return result;
    }

    // Reduces words to a basic form: "squeaking" -> "squeak", "brakes" -> "brake"
    private static String simplify(String w) {
        if (w.length() > 5 && w.endsWith("ing")) return w.substring(0, w.length() - 3);
        if (w.length() > 5 && w.endsWith("ly")) return w.substring(0, w.length() - 2);
        if (w.length() > 4 && w.endsWith("ed")) return w.substring(0, w.length() - 2);
        if (w.length() > 3 && w.endsWith("s") && !w.endsWith("ss")) return w.substring(0, w.length() - 1);
        return w;
    }
}