package com.raincat.studybuddy.summarize;

import java.util.List;

/** 导入资料时让 LLM 产出的结构化结果 */
public record SummaryAndTags(String summary, List<String> tags) {

    public String joinedTags() {
        return tags == null ? "" : String.join(",", tags);
    }
}
