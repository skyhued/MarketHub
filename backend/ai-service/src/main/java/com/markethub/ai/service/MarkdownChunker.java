package com.markethub.ai.service;

import java.util.ArrayList;
import java.util.List;

/** 按标题切块，相邻片段之间留重叠。 */
public final class MarkdownChunker {

    private MarkdownChunker() {}

    public record Chunk(String heading, int index, String content) {}

    public static List<Chunk> split(String markdown, int chunkSize, int overlap) {
        List<Chunk> chunks = new ArrayList<>();
        String[] headingPath = new String[7];
        StringBuilder body = new StringBuilder();
        boolean inCodeFence = false;

        for (String line : markdown.split("\n", -1)) {
            if (line.stripLeading().startsWith("```")) {
                inCodeFence = !inCodeFence;
                body.append(line).append('\n');
                continue;
            }
            int level = inCodeFence ? 0 : headingLevel(line);
            if (level > 0) {
                flush(chunks, joinPath(headingPath), body, chunkSize, overlap);
                body.setLength(0);
                headingPath[level] = line.replaceFirst("^#+\\s*", "").trim();
                for (int deeper = level + 1; deeper < headingPath.length; deeper++) {
                    headingPath[deeper] = null;
                }
            } else {
                body.append(line).append('\n');
            }
        }
        flush(chunks, joinPath(headingPath), body, chunkSize, overlap);
        return chunks;
    }

    private static int headingLevel(String line) {
        int level = 0;
        while (level < line.length() && line.charAt(level) == '#') {
            level++;
        }
        if (level == 0 || level > 6 || level >= line.length()) {
            return 0;
        }
        return line.charAt(level) == ' ' ? level : 0;
    }

    private static String joinPath(String[] headingPath) {
        StringBuilder path = new StringBuilder();
        for (int level = 1; level < headingPath.length; level++) {
            if (headingPath[level] == null || headingPath[level].isEmpty()) {
                continue;
            }
            if (path.length() > 0) {
                path.append(" > ");
            }
            path.append(headingPath[level]);
        }
        return path.toString();
    }

    private static void flush(List<Chunk> chunks, String heading, StringBuilder body, int chunkSize, int overlap) {
        String text = body.toString().trim();
        if (text.isEmpty()) {
            return;
        }
        for (String piece : pack(text, chunkSize, overlap)) {
            chunks.add(new Chunk(heading, chunks.size(), piece));
        }
    }

    private static List<String> pack(String text, int chunkSize, int overlap) {
        List<String> pieces = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        for (String rawParagraph : text.split("\n\\s*\n")) {
            String paragraph = rawParagraph.trim();
            if (paragraph.isEmpty()) {
                continue;
            }
            if (paragraph.length() > chunkSize) {
                if (buffer.length() > 0) {
                    pieces.add(buffer.toString().trim());
                    buffer.setLength(0);
                }
                pieces.addAll(hardSplit(paragraph, chunkSize, overlap));
                continue;
            }
            if (buffer.length() > 0 && buffer.length() + paragraph.length() + 2 > chunkSize) {
                String emitted = buffer.toString().trim();
                pieces.add(emitted);
                buffer.setLength(0);
                String carry = tail(emitted, overlap);
                if (!carry.isEmpty()) {
                    buffer.append(carry).append("\n\n");
                }
            }
            buffer.append(paragraph).append("\n\n");
        }

        if (buffer.length() > 0) {
            String last = buffer.toString().trim();
            if (!last.isEmpty()) {
                pieces.add(last);
            }
        }
        return pieces;
    }

    private static List<String> hardSplit(String paragraph, int chunkSize, int overlap) {
        List<String> pieces = new ArrayList<>();
        int step = Math.max(1, chunkSize - overlap);
        for (int start = 0; start < paragraph.length(); start += step) {
            pieces.add(paragraph.substring(start, Math.min(paragraph.length(), start + chunkSize)));
            if (start + chunkSize >= paragraph.length()) {
                break;
            }
        }
        return pieces;
    }

    private static String tail(String text, int length) {
        if (length <= 0) {
            return "";
        }
        return text.length() <= length ? text : text.substring(text.length() - length);
    }
}
