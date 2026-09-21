package com.ram.ai.stockagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private final SimpleVectorStore vectorStore;

    public RagService(SimpleVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void addDocument(String content) {

        Document document = new Document(content);

        vectorStore.add(List.of(document));
    }

    public List<Document> search(String query) {

        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(3)
                        .build()
        );
    }
}