package Utils;

import java.io.Serializable;

public class Response implements Serializable {
    final public boolean success;
    final public String message;

    public String[] list;

    public Response(boolean success) {
        this.success = success;
        this.message = "";
    }

    public Response(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public Response(boolean success, String message, String[] list) {
        this.success = success;
        this.message = message;
        this.list = list;
    }
}
