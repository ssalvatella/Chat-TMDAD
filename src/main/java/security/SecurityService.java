package security;

import model.Room;
import model.RoomRepository;
import model.User;
import model.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import websockets.Message;

@Service
public class SecurityService {


    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    public boolean checkRoomCreator(String idUser, String idRoom) {
        User user = this.userRepository.findById(idUser).get();
        Room room = this.roomRepository.findById(idRoom).get();
        return room.getCreatorId().equals(user.getId());
    }

    public boolean checkAdminMessage(String idUser, Message message) {
        User adminUser = this.userRepository.findById(idUser).get();
        Room roomDst = this.roomRepository.findById(message.getTo()).get();
        if (adminUser.isSuper() && roomDst.getType().equals(Room.TYPE_ADMIN)) return true;
        return false;
    }

}
