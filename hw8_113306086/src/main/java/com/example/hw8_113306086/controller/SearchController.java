package com.example.hw8_113306086.controller;
//限制多次執行
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ExecutionException;


import com.example.hw8_113306086.crawler.Crawler;
import com.example.hw8_113306086.dto.SearchResultDTO;
import com.example.hw8_113306086.service.GoogleSearchService;
import com.example.hw8_113306086.webTree.Keyword;
import com.example.hw8_113306086.webTree.TreeBuilder;
import com.example.hw8_113306086.webTree.WebTree;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.io.IOException;

@Controller
public class SearchController {

    @Autowired
    private GoogleSearchService googleSearchService;

    // 處理根目錄 "/" 和 "/search" 請求
    @GetMapping("/")
    public String home() {
        // 只顯示 index.html (只有搜尋框)
        return "index";
    }

    @GetMapping("/search")
    public String search(
            @RequestParam(value = "query", required = false) String query,
            Model model
    ) {
        if (query != null && !query.isEmpty()) {
            // 1. 呼叫 Service 取得搜尋結果
            //先試一下幾個keyword
            ArrayList<Keyword> keywords = new ArrayList<Keyword>();
            keywords.add(new Keyword(query,7));
            keywords.add(new Keyword("判決",5.0));
            keywords.add(new Keyword("殺人",10.0));
            keywords.add(new Keyword("綁架",10.0));
            keywords.add(new Keyword("爭議",10.0));
            keywords.add(new Keyword("被告",5.0));
            keywords.add(new Keyword("裁定",5.0));
            keywords.add(new Keyword("起訴",5.0));
            keywords.add(new Keyword("通緝",5.0));
            keywords.add(new Keyword("警",4.0));
            keywords.add(new Keyword("涉嫌",4.0));
            keywords.add(new Keyword("嫌",4.0));
            keywords.add(new Keyword("涉",4.0));
            keywords.add(new Keyword("嗆",10.0));
            keywords.add(new Keyword("逮補",4.0));
            keywords.add(new Keyword("遭逮",4.0));
            keywords.add(new Keyword("落網",4.0));
            keywords.add(new Keyword("羈押",4.0));
            keywords.add(new Keyword("移送",4.0));
            keywords.add(new Keyword("調查",4.0));
            keywords.add(new Keyword("徒刑",4.0));
            keywords.add(new Keyword("勒",4.0));
            keywords.add(new Keyword("恐嚇",4.0));
            keywords.add(new Keyword("騷擾",4.0));
           // keywords.add(new Keyword("地檢署",4.0));
            keywords.add(new Keyword("爭議",4.0));
            keywords.add(new Keyword("不法",4.0));
            keywords.add(new Keyword("行兇",4.0));
            keywords.add(new Keyword("歹徒",4.0));
            keywords.add(new Keyword("指控",4.0));
            keywords.add(new Keyword("犯",4.0));
            keywords.add(new Keyword("案",4.0));
            keywords.add(new Keyword("賠",2.0));
           // keywords.add(new Keyword("法院",2.0));
           // keywords.add(new Keyword("法",2.0));
            keywords.add(new Keyword("死",2.0));
            keywords.add(new Keyword("判",2.0));
            keywords.add(new Keyword("貪",2.0));
            keywords.add(new Keyword("告",2.0));
            //keywords.add(new Keyword("檢",2.0));
            keywords.add(new Keyword("警方",2.0));
           // keywords.add(new Keyword("社會",1.0));
           // keywords.add(new Keyword("權",1.0));
            //keywords.add(new Keyword("訟",1.0));
            keywords.add(new Keyword("恐",1.0));
            keywords.add(new Keyword("黑",1.0));
            String query1 = query+" 醜聞";
            Map<String, String> searchResults = googleSearchService.search(query1);
            Crawler crawler1 = new Crawler(searchResults);
            //建立網站樹
            List<com.example.hw8_113306086.webTree.WebTree> trees = TreeBuilder.buildTreesFromLines(crawler1.resultsAndItsKids());
            // 算分喔
            /*int i=0;
            for( com.example.hw8_113306086.webTree.WebTree tree: trees){
                System.out.println("第" + i + "網站樹：");
                try {
                    tree.root.setNodeScore(keywords);
                     System.out.println("網站分數：" + tree.root.nodeScore);
                } catch (IOException e) {
                    // 記錄錯誤並繼續處理其他樹
                    e.printStackTrace();
                }
                i++;
            }*/
           ForkJoinPool customThreadPool = new ForkJoinPool(4);
            int i=0;
            /*trees.parallelStream().forEach(tree -> {
                 try {
                    System.out.println("第" + i + "網站樹：");
                    tree.root.setNodeScore(keywords);
                    System.out.println("網站分數：" + tree.root.nodeScore);
                 } catch (IOException e) {
                         e.printStackTrace();
                }
        });*/
        try {
            // 2. 將 parallelStream 包在 submit 裡面執行
            // 這樣 Stream 就會使用我們自定義的 Pool，而不是系統預設的
            customThreadPool.submit(() -> 
                trees.parallelStream().forEach(tree -> {
                    try {
                        System.out.println("正在處理網站樹 (Thread: " + Thread.currentThread().getName() + ")");
                        tree.root.setNodeScore(keywords);
                        System.out.println("網站分數：" + tree.root.nodeScore);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                })
            ).get(); // .get() 會等待所有任務完成才繼續往下走

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            // 3. 關閉 Pool，釋放資源
            customThreadPool.shutdown();
        }


            // 依網站總分排序（descending）
            trees.sort(Comparator.comparingDouble(WebTree::getScore).reversed());
            List<SearchResultDTO> dtoList = new ArrayList<>();
            for (WebTree tree : trees) {
                SearchResultDTO dto = new SearchResultDTO();
                dto.setTitle(tree.root.webPage.name);
                dto.setUrl(tree.root.webPage.url);
                dto.setScore(tree.getScore());
                dtoList.add(dto);
            }
            model.addAttribute("results", dtoList);

            /*//測試有沒有抓到小孩網址
                int j =0;
            for(String i: crawler1.resultsAndItsKids()){
                
                System.out.println("第"+j+"筆結果和小孩網址:");
                j++;
                System.out.println(i);
            }*/
            
            // 2. 將結果放進 Model，這樣 HTML 頁面才能讀取
            //model.addAttribute("results", searchResults);
            model.addAttribute("query", query); // 把查詢詞也傳回去，可以顯示在搜尋框
        }

        // 3. 返回 index.html 頁面 (這次會包含結果)
        return "index";
    }
}