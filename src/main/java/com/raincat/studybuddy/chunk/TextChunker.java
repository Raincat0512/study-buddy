package com.raincat.studybuddy.chunk;

import com.raincat.studybuddy.config.AppProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 固定长度 + 重叠的滑动窗口分块。按字符切分而不是按 token：
 * 对以中文为主的资料，字符数与 token 数接近，且实现透明、面试可讲清。
 */
@Component
public class TextChunker {

    private final AppProperties props;

    public TextChunker(AppProperties props) {
        this.props = props;
    }

    public List<String> chunk(String text) {
        String cleaned = text.replace("\r\n", "\n").replace('\r', '\n').strip();
        List<String> chunks = new ArrayList<>();
        if (cleaned.isEmpty()) {
            return chunks;
        }
        int size = Math.max(1, props.getChunkSize());
        int overlap = Math.max(0, Math.min(props.getChunkOverlap(), size - 1));
        int step = size - overlap;

        int start = 0;
        while (start < cleaned.length()) {
            int end = Math.min(start + size, cleaned.length());
            String piece = cleaned.substring(start, end);
            if (!piece.isBlank()) {
                chunks.add(piece);
            }
            if (end == cleaned.length()) {
                break;
            }
            start += step;
        }
        return chunks;
    }
}
