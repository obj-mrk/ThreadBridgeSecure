package mrk.http.response;

public class RegisterUserResponse {
    private final String status;
    private final long userId;
    private final String username;
    private final String apiToken;

    public RegisterUserResponse(
            String status,
            long userId,
            String username,
            String apiToken
    ) {
        this.status = status;
        this.userId = userId;
        this.username = username;
        this.apiToken = apiToken;
    }

    public String getStatus() {
        return status;
    }

    public long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getApiToken() {
        return apiToken;
    }
}