package ru.pariy.cryptapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.time.temporal.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private final String CREATE_DOCUMENT_ENDPOINT = "/api/v3/lk/documents/create";
    private final static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    private final String ismpCreateDocumentURL;
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private volatile int lastTime;

    public CrptApi(String ismpBaseHost, TimeUnit timeUnit, int requestLimit) {
        this.ismpCreateDocumentURL = ismpBaseHost + CREATE_DOCUMENT_ENDPOINT;
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.lastTime = LocalDateTime.now().get(toTemporalField(timeUnit));
    }

    /**
     * Метод создания документа для ввода в оборот товара, произведенного в РФ
     *
     * @param document  - Содержимое документа
     * @param signature - Открепленная подпись
     * @return - Идентификатор документа в ИС МП
     * @throws IOException
     * @throws URISyntaxException
     * @throws InterruptedException
     */
    public CreateDocumentResponse createDocumentIntroduceGoods(Document document, String signature)
            throws IOException, URISyntaxException, InterruptedException {
        requestCondition();
        String documentJson = objectMapper
                .writeValueAsString(document);

        CreateDocumentRequest createDocumentRequest = new CreateDocumentRequest();
        createDocumentRequest.setProductDocument(documentJson);
        createDocumentRequest.setSignature(signature);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(this.ismpCreateDocumentURL))
                .timeout(Duration.of(60, ChronoUnit.SECONDS))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(createDocumentRequest)))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        handleNegativeResponse(response);

        return objectMapper.readValue(response.body(), CreateDocumentResponse.class);
    }

    private void handleNegativeResponse(HttpResponse<String> response) {
        if (response.statusCode() / 100 != 2) {
            try {
                ErrorResponse errorResponse = objectMapper.readValue(response.body(), ErrorResponse.class);
                throw new ApiInteractionException(errorResponse.getErrorMessage(),
                        errorResponse.getCode(), errorResponse.getDescription());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void requestCondition() {
        if (lastTime != LocalDateTime.now().get(toTemporalField(timeUnit))) {
            synchronized (this) {
                if (lastTime != LocalDateTime.now().get(toTemporalField(timeUnit))) {
                    requestCount.set(0);
                    lastTime = LocalDateTime.now().get(toTemporalField(timeUnit));
                }
            }
        }
        if (requestCount.incrementAndGet() > requestLimit) {
            throw new RequestExcitedException("Excited the number of request");
        }
    }

    private static TemporalField toTemporalField(TimeUnit timeUnit) {
        switch (timeUnit) {
            case NANOSECONDS:
                return ChronoField.NANO_OF_SECOND;
            case MICROSECONDS:
                return ChronoField.MICRO_OF_SECOND;
            case MILLISECONDS:
                return ChronoField.MILLI_OF_SECOND;
            case SECONDS:
                return ChronoField.SECOND_OF_MINUTE;
            case MINUTES:
                return ChronoField.MINUTE_OF_HOUR;
            case HOURS:
                return ChronoField.HOUR_OF_DAY;
            case DAYS:
                return ChronoField.DAY_OF_MONTH;
            default:
                throw new IllegalArgumentException("Unsupported TimeUnit: " + timeUnit);
        }
    }


    private static class Document {
        private Description description;
        private String docId;
        private String docStatus;
        private String docType;
        private String importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        private List<Product> products;
        private String regDate;
        private String regNumber;

        public Description getDescription() {
            return description;
        }

        public void setDescription(Description description) {
            this.description = description;
        }

        public String getDocId() {
            return docId;
        }

        public void setDocId(String docId) {
            this.docId = docId;
        }

        public String getDocStatus() {
            return docStatus;
        }

        public void setDocStatus(String docStatus) {
            this.docStatus = docStatus;
        }

        public String getDocType() {
            return docType;
        }

        public void setDocType(String docType) {
            this.docType = docType;
        }

        public String getImportRequest() {
            return importRequest;
        }

        public void setImportRequest(String importRequest) {
            this.importRequest = importRequest;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public void setOwnerInn(String ownerInn) {
            this.ownerInn = ownerInn;
        }

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }

        public String getProducerInn() {
            return producerInn;
        }

        public void setProducerInn(String producerInn) {
            this.producerInn = producerInn;
        }

        public String getProductionDate() {
            return productionDate;
        }

        public void setProductionDate(String productionDate) {
            this.productionDate = productionDate;
        }

        public String getProductionType() {
            return productionType;
        }

        public void setProductionType(String productionType) {
            this.productionType = productionType;
        }

        public List<Product> getProducts() {
            return products;
        }

        public void setProducts(Product product) {
            products = new ArrayList<>();
            products.add(product);
        }

        public String getRegDate() {
            return regDate;
        }

        public void setRegDate(String regDate) {
            this.regDate = regDate;
        }

        public String getRegNumber() {
            return regNumber;
        }

        public void setRegNumber(String regNumber) {
            this.regNumber = regNumber;
        }
    }

    private static class Description {
        private String participantInn;

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }
    }

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

        public String getCertificateDocument() {
            return certificateDocument;
        }

        public void setCertificateDocument(String certificateDocument) {
            this.certificateDocument = certificateDocument;
        }

        public String getCertificateDocumentDate() {
            return certificateDocumentDate;
        }

        public void setCertificateDocumentDate(String certificateDocumentDate) {
            this.certificateDocumentDate = certificateDocumentDate;
        }

        public String getCertificateDocumentNumber() {
            return certificateDocumentNumber;
        }

        public void setCertificateDocumentNumber(String certificateDocumentNumber) {
            this.certificateDocumentNumber = certificateDocumentNumber;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public void setOwnerInn(String ownerInn) {
            this.ownerInn = ownerInn;
        }

        public String getProducerInn() {
            return producerInn;
        }

        public void setProducerInn(String producerInn) {
            this.producerInn = producerInn;
        }

        public String getProductionDate() {
            return productionDate;
        }

        public void setProductionDate(String productionDate) {
            this.productionDate = productionDate;
        }


        public String getTnvedCode() {
            return tnvedCode;
        }

        public void setTnvedCode(String tnvedCode) {
            this.tnvedCode = tnvedCode;
        }

        public String getUitCode() {
            return uitCode;
        }

        public void setUitCode(String uitCode) {
            this.uitCode = uitCode;
        }

        public String getUituCode() {
            return uituCode;
        }

        public void setUituCode(String uituCode) {
            this.uituCode = uituCode;
        }
    }

    private static class CreateDocumentRequest {
        private String productDocument;
        private String signature;

        public String getProductDocument() {
            return productDocument;
        }

        public void setProductDocument(String productDocument) {
            this.productDocument = productDocument;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

    }

    public static class CreateDocumentResponse {
        private String value;

        public String getValue() {
            return value;
        }
    }

    public static class RequestExcitedException extends RuntimeException {
        public RequestExcitedException(String message) {
            super(message);
        }
    }

    public static class ApiInteractionException extends RuntimeException {
        private final String code;
        private final String description;

        public ApiInteractionException(String message, String code, String description) {
            super(message);
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    private static class ErrorResponse {
        private String errorMessage;
        private String code;
        private String description;

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
