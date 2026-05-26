package mrk.http.request;

public class AcceptSecureMessageRequest {
    private String requestId;
    private Long recipientId;
    private String text;
    private Integer ttlSeconds;
    private Boolean oneTime;

    public AcceptSecureMessageRequest() {
    }

    public String getRequestId() {
        return requestId;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public String getText() {
        return text;
    }

    public Integer getTtlSeconds() {
        return ttlSeconds;
    }

    public Boolean getOneTime() {
        return oneTime;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTtlSeconds(Integer ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public void setOneTime(Boolean oneTime) {
        this.oneTime = oneTime;
    }
}