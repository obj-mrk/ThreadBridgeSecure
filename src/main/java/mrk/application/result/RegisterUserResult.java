package mrk.application.result;

public class RegisterUserResult {
    private final long userId;
    private final String username;
    private final String status;

    public RegisterUserResult(long userId, String username, String status) {
        this.userId = userId;
        this.username = username;
        this.status = status;
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
}