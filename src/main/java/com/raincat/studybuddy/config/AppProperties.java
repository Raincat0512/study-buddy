package com.raincat.studybuddy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** 文本分块的目标长度（字符） */
    private int chunkSize = 500;

    /** 相邻分块的重叠长度（字符） */
    private int chunkOverlap = 80;

    /** 单篇资料的原文长度上限（字符），超过则截断 */
    private int maxSourceChars = 100_000;

    private final Rag rag = new Rag();

    public static class Rag {
        /** 每次问答检索的片段数 */
        private int topK = 4;

        /** 相似度阈值，低于该值的结果丢弃 */
        private double similarityThreshold = 0.30;

        /** 多轮对话携带的历史消息条数 */
        private int historyMessages = 10;

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public double getSimilarityThreshold() {
            return similarityThreshold;
        }

        public void setSimilarityThreshold(double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
        }

        public int getHistoryMessages() {
            return historyMessages;
        }

        public void setHistoryMessages(int historyMessages) {
            this.historyMessages = historyMessages;
        }
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(int chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public int getMaxSourceChars() {
        return maxSourceChars;
    }

    public void setMaxSourceChars(int maxSourceChars) {
        this.maxSourceChars = maxSourceChars;
    }

    public Rag getRag() {
        return rag;
    }
}
