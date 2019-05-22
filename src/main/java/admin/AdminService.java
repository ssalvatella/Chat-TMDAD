package admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import websockets.Message;
import websockets.MessagingController;

import java.util.List;

@Service
public class AdminService {

    @Autowired
    private CensorshipService censorshipService;

    public Message processCommand(String roomAdminId, List<String> commands) {
        Command command = Command.valueOf(commands.get(0));
        if (command == null) return getFailMessage(roomAdminId);
        switch (command) {
            case CENSOR:
                String firstParam = commands.get(1);
                if (firstParam == null) return getFailMessage(roomAdminId);
                if (this.censorshipService.addWordCensored(roomAdminId, firstParam)) {
                    return new Message(Message.TYPE_NOTIFICATION, MessagingController.FROM_SYSTEM, roomAdminId, "Word censored.");
                } else {
                    return new Message(Message.TYPE_NOTIFICATION, MessagingController.FROM_SYSTEM, roomAdminId, "Word already censored.");
                }
        }
        return null;
    }

    private Message getFailMessage(String userId) {
        return new Message(Message.TYPE_NOTIFICATION, MessagingController.FROM_SYSTEM, userId, "Sorry. I don't understand you.");
    }

    private enum Command {
        CENSOR
    }
}
