package admin;

import amqp.Application;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import websockets.Message;
import websockets.MessagingController;

import java.util.*;

@Getter
@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    private final static int MEASURE_TIME = 60000;

    private Map<String, long[]> statsMap;

    @Autowired
    private CensorshipService censorshipService;

    private int messagesLastMinute;
    private int bytesLastMinute;
    private long totalProcessTimeLastMinute;
    private long averageProcessTime;

    public AdminService() {
        this.statsMap = new HashMap<>();
    }

    public Message processCommand(String roomAdminId, List<String> commands) {
        Command command = Command.valueOf(commands.get(0));
        if (command == null) return getFailMessage(roomAdminId);
        switch (command) {
            case CENSOR:
                String firstParam = commands.get(1);
                if (firstParam == null) return getFailMessage(roomAdminId);
                if (this.censorshipService.addWordCensored(roomAdminId, firstParam)) {
                    return new Message(Message.TYPE_NOTIFICATION, MessagingController.FROM_SYSTEM, roomAdminId, "Word '" + firstParam + "' censored.");
                } else {
                    return new Message(Message.TYPE_NOTIFICATION, MessagingController.FROM_SYSTEM, roomAdminId, "Word already censored.");
                }
            case STATS:
                Message statsMessage = new Message(Message.TYPE_NOTIFICATION, MessagingController.FROM_SYSTEM, roomAdminId, "Asking for statistics...");
                return statsMessage;
        }
        return null;
    }

    public void registerMessageEntry(Message message) {
        logger.info("Registering entry message" + message);
        long entryTime = System.currentTimeMillis();
        long sizeObject = message.getContent().toString().getBytes().length;
        long[] stats = {entryTime, sizeObject, 0};
        this.statsMap.put(message.getTimestamp().toString(), stats);
        logger.info(statsMap.toString());
    }

    public void registerMessageExit(Message message) {
        long exitTime = System.currentTimeMillis();
        long[] stats = this.statsMap.get(message.getTimestamp().toString());
        if (stats == null) return;
        long entryTime = stats[0];
        long elapsedTime = exitTime - entryTime;
        stats[2] = elapsedTime;
        this.statsMap.put(message.getTimestamp().toString(), stats);
        messagesLastMinute++;
        bytesLastMinute += stats[1];
        this.totalProcessTimeLastMinute += elapsedTime;
        this.averageProcessTime = (messagesLastMinute != 0) ? totalProcessTimeLastMinute / messagesLastMinute : 0;
    }

    public boolean isForStats(Message message) {
        final List<String> commands = new ArrayList<>(Arrays.asList(((String) message.getContent()).split(" ")));
        Command command = Command.valueOf(commands.get(0));
        return command == Command.STATS;
    }

    public Message giveStats(String roomAdmin) {
        refreshStats();
        String text = Application.nameHost + "<br>"
                + " from " + Application.ipHost + "<br><br>"
                + "Messages on last minute: " + getMessagesLastMinute() + "<br>"
                + "Processed bytes on last minute: " + getBytesLastMinute() + " Bytes<br>"
                + "Average process time on last minute: " + getAverageProcessTime() + " ms<br>";

        return new Message(Message.TYPE_TEXT, MessagingController.FROM_SYSTEM, roomAdmin, text);
    }

    private void refreshStats() {
        long now = System.currentTimeMillis();
        List<String> listToDelete = new ArrayList<>();
        Iterator it = statsMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            long entryTime = ((long[]) pair.getValue())[0];
            long elapsedTime = now - entryTime;
            logger.info("Elpased time for message " + elapsedTime);
            if (elapsedTime > MEASURE_TIME) {
                messagesLastMinute--;
                bytesLastMinute -= ((long[]) pair.getValue())[1];
                totalProcessTimeLastMinute -= ((long[]) pair.getValue())[2];
                this.averageProcessTime = (messagesLastMinute != 0) ? totalProcessTimeLastMinute / messagesLastMinute : 0;
                listToDelete.add((String) pair.getKey());
            }
        }

        listToDelete.forEach(key -> this.statsMap.remove(key));

        logger.info(this.statsMap.toString());
    }

    private Message getFailMessage(String userId) {
        return new Message(Message.TYPE_NOTIFICATION, MessagingController.FROM_SYSTEM, userId, "Sorry. I don't understand you.");
    }

    private enum Command {
        CENSOR, STATS
    }
}
