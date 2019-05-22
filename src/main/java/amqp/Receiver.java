package amqp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import websockets.Message;
import websockets.MessagingController;

@Component
public class Receiver {

    @Autowired
    private MessagingController wbController;

    public void receiveMessage(Message message) throws Exception {
        wbController.send(message);
    }

}