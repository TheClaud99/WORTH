package Utils;

import java.io.Serializable;

public class Response implements Serializable {
    final public boolean success;
    final public String message;

    Response(boolean success) {
        this.success = success;
        this.message = "";
    }

    public Response(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
