package com.selsuptest.selsuptestexercise;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final String apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Semaphore requestSemaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.requestSemaphore = new Semaphore(requestLimit);
        Runnable releasePermitTask = () -> {
            try {
                requestSemaphore.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(releasePermitTask, 0, timeUnit.toSeconds(1), TimeUnit.SECONDS);
    }

    public void createDocument(Document document, String signature) {
        try {
            requestSemaphore.acquire();
            String documentJson = objectMapper.writeValueAsString(document);
            ObjectNode requestBody = objectMapper.readValue(documentJson, ObjectNode.class);
            requestBody.put("signature", signature);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                System.out.println("Документ успешно создан");
            } else {
                System.err.println("Ошибка при создании документа. HTTP Status Code: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } finally {
            requestSemaphore.release();
        }
    }


    @Getter
    @Setter
    @AllArgsConstructor
    public static class Description {
        @JsonProperty("participantInn")
        private String participantInn;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate production_date;
        private String production_type;
        private List<Product> products;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate reg_date;
        private String reg_number;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Product {
        private String certificate_document;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }
}
