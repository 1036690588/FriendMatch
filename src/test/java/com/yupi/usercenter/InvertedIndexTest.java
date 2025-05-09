package com.yupi.usercenter;

import org.junit.jupiter.api.Test;

import java.util.*;

public class InvertedIndexTest {

    @Test
    public void testInvertedIndex() {
        // 模拟文档集合
        List<String> documents = Arrays.asList(
                "apple banana cherry",
                "banana date elderberry",
                "cherry fig grape"
        );

        // 创建倒排索引对象
        InvertedIndex invertedIndex = new InvertedIndex();

        // 构建倒排索引
        invertedIndex.buildIndex(documents);

        // 打印倒排索引
        invertedIndex.printIndex();

        // 查询倒排索引
        String queryTerm = "banana";
        List<Integer> result = invertedIndex.search(queryTerm);
        System.out.println("查询词 '" + queryTerm + "' 出现的文档编号: " + result);
    }
}
// 简单的分词器，这里只是简单按空格分割
class SimpleTokenizer {
    public static List<String> tokenize(String document) {
        return Arrays.asList(document.split("\\s+"));
    }
}

// 倒排索引类
class InvertedIndex {
    private Map<String, List<Integer>> index;

    public InvertedIndex() {
        this.index = new HashMap<>();
    }
    // 构建倒排索引
    public void buildIndex(List<String> documents) {
        for (int docId = 0; docId < documents.size(); docId++) {
            String document = documents.get(docId);
            List<String> tokens = SimpleTokenizer.tokenize(document);
            for (String token : tokens) {
                index.computeIfAbsent(token, k -> new ArrayList<>()).add(docId);
            }
        }
    }
    // 查询倒排索引
    public List<Integer> search(String term) {
        return index.getOrDefault(term, new ArrayList<>());
    }
    // 打印倒排索引
    public void printIndex() {
        for (Map.Entry<String, List<Integer>> entry : index.entrySet()) {
            String term = entry.getKey();
            List<Integer> docIds = entry.getValue();
            System.out.println(term + ": " + docIds);
        }
    }
}


