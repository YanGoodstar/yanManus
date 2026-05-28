package com.zixiang.yanmanus.tools;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 网页搜索工具 - 通过 SerpApi 调用百度搜索引擎
 */
public class WebSearchTool {

    private static final String SEARCH_URL = "https://serpapi.com/search";

    private final String apiKey;

    public WebSearchTool(String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "Search the web using Baidu search engine. Returns search results including titles, snippets, and URLs.")
    public String search(
            @ToolParam(description = "the search query") String query) {
        if (StrUtil.isBlank(apiKey)) {
            return "Error: SERPAPI_API_KEY is not configured";
        }
        try {
            String result = HttpUtil.get(SEARCH_URL,
                    MapUtil.builder(new java.util.HashMap<String, Object>())
                            .put("engine", "baidu")
                            .put("q", query)
                            .put("api_key", apiKey)
                            .build());

            JSONObject json = JSONUtil.parseObj(result);
            StringBuilder sb = new StringBuilder();

            if (json.containsKey("organic_results")) {
                JSONArray organicResults = json.getJSONArray("organic_results");
                sb.append("=== Search Results ===\n\n");
                int limit = Math.min(organicResults.size(), 10);
                for (int i = 0; i < limit; i++) {
                    JSONObject item = organicResults.getJSONObject(i);
                    sb.append(String.format("%d. %s\n", i + 1, item.getStr("title", "")));
                    sb.append(String.format("   URL: %s\n", item.getStr("link", "")));
                    sb.append(String.format("   %s\n\n", item.getStr("snippet", "")));
                }
            } else {
                sb.append("No results found.");
            }

            if (json.containsKey("knowledge_graph")) {
                JSONObject kg = json.getJSONObject("knowledge_graph");
                sb.append("=== Knowledge Graph ===\n");
                sb.append(kg.toStringPretty());
            }

            return sb.toString();
        } catch (Exception e) {
            return "Search error: " + e.getMessage();
        }
    }
}
