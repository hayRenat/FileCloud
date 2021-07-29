package commons;

public class AuthMessage extends Message {
    private String login;
    private String password;
    private Boolean autorized = false;

    public AuthMessage(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public AuthMessage() {
    }

    public Boolean getAutorized() {
        return autorized;
    }

    public void setAutorized(Boolean autorized) {
        this.autorized = autorized;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public AuthMessage(String login, String password, Boolean autorized) {
        this.login = login;
        this.password = password;
        this.autorized = autorized;
    }
}
