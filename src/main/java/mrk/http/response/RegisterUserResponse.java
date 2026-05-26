package mrk.http.response;

public class RegisterUserResponse {
    private final String status;
    private final long userId;
    private final String username;

    public RegisterUserResponse(String status, long userId, String username) {
        this.status = status;
        this.userId = userId;
        this.username = username;
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
}