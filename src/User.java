import Utils.Notification;
import Utils.Utils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.rmi.RemoteException;
import java.security.SecureRandom;

@JsonPropertyOrder({ "username", "password"})
public class User {
    private String username;
    private String password;
    private String saltKey;
    private NotifyEventInterface clientInterface;
    private boolean online;

    User() {}

    User(String username, String password) {
        this.username = username;
        online = false;

        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        this.saltKey = Utils.byteToBase64(salt);
        this.password = Utils.sha512(password, this.saltKey);
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
        String hashedPassword = Utils.sha512(password, this.saltKey);
        if(this.password.equals(hashedPassword)) {
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

    public String getSaltKey() {
        return saltKey;
    }

    public void setSaltKey(String saltKey) {
        this.saltKey = saltKey;
    }
}
