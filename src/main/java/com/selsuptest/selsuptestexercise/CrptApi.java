package com.selsuptest.selsuptestexercise;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Component
public class CrptApi {
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private long lastTimeReset;
    @Autowired
    private RestTemplate restTemplate;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.lastTimeReset = System.currentTimeMillis();
    }

    @Bean
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
    @Bean
    public int getRequestLimit() {
        return requestLimit;
    }
    @Bean
    public RestTemplate getRestTemplate() {
        return restTemplate;
    }


    @Data
    private static class Document {
        private String participantInn;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        private List<Product> products;
        private String regDate;
        private String regNumber;
    }

    @Data
    private static class Product {
        private String certificateDocument;
        private String certificateDocumentDate;
        private String certificateDocumentNumber;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;
    }

    public synchronized void createDocument(Document document, String sign) {
        refreshRequestCount();
        if (requestCount.get() < requestLimit) {
            String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";
            String requestBody = constructRequestBody(document, sign);
            String response = restTemplate.postForObject(url, requestBody, String.class);
            System.out.println("Document created and signed successfully.");
            requestCount.incrementAndGet();
        } else {
            System.out.println("Request limit exceeded. Please try again later.");
        }
    }
    private String constructRequestBody(Document document, String sign) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("participantInn", document.getParticipantInn());
        requestBody.put("docId", document.getDocId());
        requestBody.put("docStatus", document.getDocStatus());
        requestBody.put("docType", document.getDocType());
        requestBody.put("importRequest", document.isImportRequest());
        requestBody.put("ownerInn", document.getOwnerInn());
        requestBody.put("producerInn", document.getProducerInn());
        requestBody.put("productionDate", document.getProductionDate());
        requestBody.put("productionType", document.getProductionType());
        requestBody.put("regDate", document.getRegDate());
        requestBody.put("regNumber", document.getRegNumber());

        ArrayNode productsArray = objectMapper.createArrayNode();
        for (Product product : document.getProducts()) {
            ObjectNode productNode = objectMapper.createObjectNode();
            productNode.put("certificateDocument", product.getCertificateDocument());
            productNode.put("certificateDocumentDate", product.getCertificateDocumentDate());
            productNode.put("certificateDocumentNumber", product.getCertificateDocumentNumber());
            productNode.put("ownerInn", product.getOwnerInn());
            productNode.put("producerInn", product.getProducerInn());
            productNode.put("productionDate", product.getProductionDate());
            productNode.put("tnvedCode", product.getTnvedCode());
            productNode.put("uitCode", product.getUitCode());
            productNode.put("uituCode", product.getUituCode());
            productsArray.add(productNode);
        }
        requestBody.set("products", productsArray);

        requestBody.put("sign", sign);

        return requestBody.toString();
    }

    private void refreshRequestCount() {
        long currentTime = System.currentTimeMillis();
        long passedTime = currentTime - lastTimeReset;
        if (timeUnit.toMillis(1) <= passedTime) {
            requestCount.set(0);
            lastTimeReset = currentTime;
        }
    }
}
