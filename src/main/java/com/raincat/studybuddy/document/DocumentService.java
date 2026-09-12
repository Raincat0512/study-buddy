package com.raincat.studybuddy.document;

import com.raincat.studybuddy.chunk.TextChunker;
import com.raincat.studybuddy.config.AppProperties;
import com.raincat.studybuddy.summarize.SummarizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 导入管道：抽取原文 -> LLM 摘要打标 -> 分块 -> 向量化入库。
 * 同步执行（单篇约 10-30 秒），换取实现透明和排错简单。
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository repository;
    private final WebPageFetcher webPageFetcher;
    private final PdfTextExtractor pdfTextExtractor;
    private final TextChunker chunker;
    private final SummarizationService summarizationService;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final AppProperties props;

    public DocumentService(DocumentRepository repository,
                           WebPageFetcher webPageFetcher,
                           PdfTextExtractor pdfTextExtractor,
                           TextChunker chunker,
                           SummarizationService summarizationService,
                           VectorStore vectorStore,
                           JdbcTemplate jdbcTemplate,
                           AppProperties props) {
        this.repository = repository;
        this.webPageFetcher = webPageFetcher;
        this.pdfTextExtractor = pdfTextExtractor;
        this.chunker = chunker;
        this.summarizationService = summarizationService;
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.props = props;
    }

    public DocumentView ingestFile(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "未命名" : file.getOriginalFilename();
        String lower = filename.toLowerCase();
        try {
            byte[] bytes = file.getBytes();
            if (lower.endsWith(".pdf")) {
                String text = pdfTextExtractor.extract(bytes);
                return ingest(filename, SourceType.FILE, filename, text);
            }
            if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown")) {
                return ingest(filename, SourceType.FILE, filename, new String(bytes, StandardCharsets.UTF_8));
            }
            throw new IllegalArgumentException("仅支持 .txt / .md / .pdf 文件（图片、PPT、docx 暂不支持）");
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("文件读取失败：" + e.getMessage(), e);
        }
    }

    public DocumentView ingestUrl(String url) {
        WebPageFetcher.WebPage page = webPageFetcher.fetch(url);
        return ingest(page.title(), SourceType.URL, url, page.text());
    }

    @Transactional
    public DocumentView ingest(String title, SourceType sourceType, String sourceRef, String rawText) {
        String text = rawText == null ? "" : rawText.strip();
        if (text.length() < 30) {
            throw new IllegalArgumentException("抽取到的正文太短（<30 字符），请检查来源是否可读");
        }
        if (text.length() > props.getMaxSourceChars()) {
            text = text.substring(0, props.getMaxSourceChars());
        }
        final String content = text;

        DocumentEntity entity = new DocumentEntity(title, sourceType, sourceRef, content);
        entity.setStatus(DocumentStatus.PROCESSING);
        entity.setCreatedAt(LocalDateTime.now());
        entity = repository.save(entity);

        try {
            var summary = summarizationService.summarize(title, content);
            entity.setSummary(summary.summary());
            entity.setTags(summary.joinedTags());

            final Long docId = entity.getId();
            final String docTitle = title;
            final String docRef = sourceRef == null ? "" : sourceRef;
            List<String> chunks = chunker.chunk(content);
            List<Document> documents = chunks.stream()
                    .map(chunk -> {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("docId", String.valueOf(docId));
                        metadata.put("title", docTitle);
                        metadata.put("sourceRef", docRef);
                        return new Document(chunk, metadata);
                    })
                    .toList();
            vectorStore.add(documents);

            entity.setStatus(DocumentStatus.READY);
            entity.setUpdatedAt(LocalDateTime.now());
            repository.save(entity);
            log.info("导入完成：{}（{} 字符，{} 个分块）", title, content.length(), chunks.size());
            return DocumentView.from(entity);
        } catch (Exception e) {
            entity.setStatus(DocumentStatus.FAILED);
            entity.setUpdatedAt(LocalDateTime.now());
            repository.save(entity);
            throw new IllegalStateException("导入失败（文档已标记为 FAILED）：" + e.getMessage(), e);
        }
    }

    public List<DocumentView> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(DocumentView::from).toList();
    }

    public DocumentView get(Long id) {
        return repository.findById(id).map(DocumentView::from)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在：" + id));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
        // 向量分块不经过 ORM，直接按 metadata 里的 docId 删除
        try {
            jdbcTemplate.update("DELETE FROM vector_store WHERE metadata->>'docId' = ?", String.valueOf(id));
        } catch (Exception e) {
            log.warn("清理向量分块失败（可能向量表尚未创建）：{}", e.getMessage());
        }
    }
}
