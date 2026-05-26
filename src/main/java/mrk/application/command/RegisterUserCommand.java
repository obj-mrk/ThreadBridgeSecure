package mrk.application.command;

public class RegisterUserCommand {
    private String username;

    public RegisterUserCommand() {
    }

    public RegisterUserCommand(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}