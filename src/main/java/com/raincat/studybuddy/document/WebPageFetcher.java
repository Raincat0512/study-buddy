package com.raincat.studybuddy.document;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 用 jsoup 抓取网页正文（不做正文算法级过滤，只取 body 文本） */
@Component
public class WebPageFetcher {

    private static final int TIMEOUT_MS = 15_000;

    public record WebPage(String title, String text) {
    }

    public WebPage fetch(String url) {
        try {
            var connection = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; StudyBuddy/0.1)")
                    .timeout(TIMEOUT_MS)
                    .maxBodySize(5 * 1024 * 1024)
                    .followRedirects(true);
            var doc = connection.get();
            String title = doc.title() == null || doc.title().isBlank() ? url : doc.title().trim();
            String text = doc.body() == null ? "" : doc.body().text();
            return new WebPage(title, text);
        } catch (IOException e) {
            throw new IllegalStateException("网页抓取失败：" + e.getMessage(), e);
        }
    }
}
