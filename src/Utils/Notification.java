package Utils;

import java.io.Serializable;
import java.util.Map;

public class Notification implements Serializable {
    final public Map<String, Boolean> users;
    final public Map<String, String> porjectChatIps;

    public String[] list;

    public Notification(Map<String, Boolean> users, Map<String, String> porjectChatIps) {
        this.users = users;
        this.porjectChatIps = porjectChatIps;
    }

}
