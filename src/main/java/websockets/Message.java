package websockets;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
public class Message implements Serializable {

    final public static String TYPE_FILE = "file";
    final public static String TYPE_LOGIN = "login";
    final public static String TYPE_LOGIN_ACK = "login_ack";
    final public static String TYPE_TEXT = "text";
    final public static String TYPE_USERS = "users";
    final public static String TYPE_CREATE_ROOM = "create_room";
    final public static String TYPE_DROP_ROOM = "drop_room";
    final public static String TYPE_REQUEST_MESSAGES = "request_messages";
    final public static String TYPE_SYSTEM_MESSAGE = "system_message";
    final public static String TYPE_INVITE_USER = "invite_user";
    final public static String TYPE_NOTIFICATION = "notification";
    final public static String TYPE_ADMIN = "admin";

    private String type;
    private String from;
    private String to;
    private Object content;
    private Date timestamp;

    public Message(@JsonProperty("type") String type,
                   @JsonProperty("from") String from,
                   @JsonProperty("to") String to,
                   @JsonProperty("content") Object content) {

        this.type = type;
        this.from = from;
        this.to = to;
        this.content = content;

    }

    @Override
    public String toString() {
        return "Message{" +
                "type='" + type + '\'' +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", content='" + content + '\'' +
                '}';
    }


}