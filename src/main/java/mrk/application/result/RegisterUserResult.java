package mrk.application.result;

public class RegisterUserResult {
    private final long userId;
    private final String username;
    private final String status;
    private final String apiToken;

    public RegisterUserResult(long userId, String username, String status, String apiToken) {
        this.userId = userId;
        this.username = username;
        this.status = status;
        this.apiToken = apiToken;
    }

    public long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getStatus() {
        return status;
    }

    public String getApiToken() {
        return apiToken;
    }
}