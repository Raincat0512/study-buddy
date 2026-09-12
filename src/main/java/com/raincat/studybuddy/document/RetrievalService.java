package com.raincat.studybuddy.document;

import com.raincat.studybuddy.config.AppProperties;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/** 相似度检索：把问题向量比到 pgvector 里，取 top-k 片段 */
@Service
public class RetrievalService {

    private final VectorStore vectorStore;
    private final AppProperties props;

    public RetrievalService(VectorStore vectorStore, AppProperties props) {
        this.vectorStore = vectorStore;
        this.props = props;
    }

    public record RetrievedChunk(String text, String title, String docId, double score) {
    }

    public List<RetrievedChunk> retrieve(String query) {
        List<Document> results = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(props.getRag().getTopK())
                .similarityThreshold(props.getRag().getSimilarityThreshold())
                .build());
        return results == null ? List.of() : results.stream()
                .map(doc -> new RetrievedChunk(
                        doc.getText(),
                        String.valueOf(doc.getMetadata().getOrDefault("title", "未知来源")),
                        String.valueOf(doc.getMetadata().getOrDefault("docId", "")),
                        doc.getScore() == null ? 0 : doc.getScore()))
                .toList();
    }

    /** 把片段编号成 [1] [2] 的上下文块，供回答时引用 */
    public String buildContext(List<RetrievedChunk> chunks) {
        if (chunks.isEmpty()) {
            return "（资料库中没有检索到相关内容）";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk c = chunks.get(i);
            sb.append("[").append(i + 1).append("] 《").append(c.title()).append("》\n")
                    .append(c.text()).append("\n\n");
        }
        return sb.toString();
    }
}
