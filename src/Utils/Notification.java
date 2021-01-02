package Utils;

import java.io.Serializable;
import java.util.Map;

public class Notification implements Serializable {
    final public String[] users;
    final public Map<String, String> porjectChatIps;

    public String[] list;

    public Notification(String[] users, Map<String, String> porjectChatIps) {
        this.users = users;
        this.porjectChatIps = porjectChatIps;
    }

}
