package cn.edu.sdua.jiankang.controller;

import cn.edu.sdua.jiankang.dto.ApiResponse;
import cn.edu.sdua.jiankang.dto.SearchDto;
import cn.edu.sdua.jiankang.service.RetrievalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 向量相似度搜索接口(返回高亮结果列表)
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final RetrievalService retrievalService;

    public SearchController(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @PostMapping
    public ApiResponse<SearchDto.SearchResponse> search(@Valid @RequestBody SearchDto.SearchRequest req) {
        return ApiResponse.ok(retrievalService.search(req.getQuery(), req.getCategory(), req.getTopK()));
    }
}
