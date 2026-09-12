package com.raincat.studybuddy.document;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@Validated
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    public record UrlRequest(@NotBlank String url) {
    }

    /** 上传文件导入：.txt / .md / .pdf */
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentView ingestFile(@RequestPart("file") MultipartFile file) {
        return documentService.ingestFile(file);
    }

    /** 抓取网页导入 */
    @PostMapping("/url")
    public DocumentView ingestUrl(@RequestBody UrlRequest request) {
        return documentService.ingestUrl(request.url());
    }

    @GetMapping
    public List<DocumentView> list() {
        return documentService.list();
    }

    @GetMapping("/{id}")
    public DocumentView detail(@PathVariable Long id) {
        return documentService.get(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        documentService.delete(id);
    }
}
