import Utils.Notification;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.rmi.RemoteException;
import java.util.Arrays;

@JsonPropertyOrder({ "username", "password"})
public class User {
    private String username;
    private String password;
    private NotifyEventInterface clientInterface;
    private boolean online;

    User() {}

    User(String username, String password) {
        this.username = username;
        this.password = password;
        online = false;
    }

    @JsonIgnore
    public synchronized void notify(Notification notification) throws RemoteException {
        if(this.clientInterface == null) return;
        this.clientInterface.notifyEvent(notification);
    }

    @JsonIgnore
    public synchronized void setClient(NotifyEventInterface clientInterface) {
        this.clientInterface = clientInterface;
    }

    @JsonIgnore
    public boolean login(String password) {
        if(this.password.equals(password)) {
            online = true;
            return true;
        }

        return false;
    }

    @JsonIgnore
    public boolean isOnline() {
        return online;
    }

    @JsonIgnore
    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}
