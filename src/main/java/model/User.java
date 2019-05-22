package model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class User {

    public static final String SUPER = "superUser";
    public static final String NORMAL = "normalUser";

    private String id;
    private String name;
    private String type;
    private List<RoomMinified> rooms;

    public User() {
    }

    public User(String name, String type) {
        this.name = name;
        this.type = type;
        this.rooms = new ArrayList<>();
    }

    public boolean isSuper() {
        return this.type.equals(SUPER);
    }

    public boolean addRoom(Room r) {
        return this.rooms.add(r.getMinified());
    }

    public boolean removeRoom(Room roomToDrop) {
        for (int i = 0; i < getRooms().size(); i++) {
            RoomMinified r = getRooms().get(i);
            if (r.getIdRoom().equals(roomToDrop.getId())) {
                this.rooms.remove(i);
                return true;
            }
        }
        return false;
    }

    public void dropSystemMembersRoom() {
        getRooms().forEach(r -> {
            if (r.isSystem()) {
                r.dropMembers();
            }
        });

    }
}
