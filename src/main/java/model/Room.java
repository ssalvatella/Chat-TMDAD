package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;
import websockets.Message;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
@ToString
public class Room {

    final public static String TYPE_SYSTEM = "system";
    final public static String TYPE_ADMIN = "admin";
    final public static String TYPE_GROUP = "group";
    final public static String TYPE_PRIVATE = "private";

    private String id;
    private String name;
    private String type;
    private String creatorId;
    private List<User> members;
    private List<Message> messages;

    public Room() {
    }

    public Room(@JsonProperty("name") String name,
                @JsonProperty("type") String type,
                @JsonProperty("creatorId") String creatorId,
                @JsonProperty("members") List<User> members) {

        this.name = name;
        this.type = type;
        this.creatorId = creatorId;
        this.members = members;
        this.messages = new ArrayList<>();

    }

    public List<Message> getLastOneHourMessages() {
        Date now = Date.from(Instant.now());
        List<Message> messages = new ArrayList<>();
        getMessages().forEach(message -> {
            if (message.getType().equals(Message.TYPE_TEXT)) {
                long diffInMillies = Math.abs(now.getTime() - message.getTimestamp().getTime());
                long hours = TimeUnit.HOURS.convert(diffInMillies, TimeUnit.MILLISECONDS);
                if (hours <= 1) {
                    messages.add(message);
                }
            }
        });
        return messages;
    }

    public boolean addUsers(List<User> users) {
        return this.members.addAll(users);
    }

    public boolean addMessage(Message m) {
        return this.messages.add(m);
    }

    public boolean isPrivate() {
        return getType().equals(TYPE_PRIVATE);
    }

    public boolean isGroup() {
        return getType().equals(TYPE_GROUP);
    }

    public boolean isSystem() {
        return getType().equals(TYPE_SYSTEM);
    }

    public RoomMinified getMinified() {
        return new RoomMinified(getId(), getName(), getType(), getCreatorId(), getMembers().stream().map(User::getName).collect(Collectors.toList()));
    }

}
