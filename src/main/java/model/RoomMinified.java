package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class RoomMinified {

    private String idRoom;
    private String nameRoom;
    private String typeRoom;
    private String creatorRoom;
    private List<String> membersRoom;

    public RoomMinified() {

    }

    public RoomMinified(@JsonProperty("idRoom") String idRoom,
                        @JsonProperty("nameRoom") String nameRoom,
                        @JsonProperty("typeRoom") String typeRoom,
                        @JsonProperty("creatorRoom") String creatorRoom,
                        @JsonProperty("membersRoom") List<String> membersRoom) {
        this.idRoom = idRoom;
        this.nameRoom = nameRoom;
        this.typeRoom = typeRoom;
        this.creatorRoom = creatorRoom;
        this.membersRoom = membersRoom;
    }

    public boolean isPrivate() {
        return getTypeRoom().equals(Room.TYPE_PRIVATE);
    }

    public boolean isGroup() {
        return getTypeRoom().equals(Room.TYPE_GROUP);
    }

    public boolean isSystem() {
        return getTypeRoom().equals(Room.TYPE_SYSTEM);
    }

    public void dropMembers() {
        this.membersRoom.clear();
    }

}