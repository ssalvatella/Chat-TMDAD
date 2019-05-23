package admin;

import amqp.Application;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.sun.management.OperatingSystemMXBean;
import lombok.Getter;
import model.Room;
import model.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import websockets.Message;
import websockets.MessagingController;

import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.util.*;

@Getter
@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    private final static int MEASURE_TIME = 60000;

    private Map<String, long[]> statsMap;

    @Autowired
    private CensorshipService censorshipService;
    @Autowired
    private RoomRepository rooms;

    private Runtime runtime;
    private OperatingSystemMXBean operatingSystemMXBean;

    private int messagesLastMinute;
    private int bytesLastMinute;
    private long totalProcessTimeLastMinute;
    private long averageProcessTime;
    private int connectedUsers;

    public AdminService() {
        this.statsMap = new HashMap<>();
        this.runtime = Runtime.getRuntime();
        this.operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }

    public Message processCommand(String roomAdminId, List<String> commands) {
        Command command = null;
        try {
            command = Command.valueOf(commands.get(0));
        } catch (Exception e) {
            return getFailMessage(roomAdminId);
        }
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

            case TRENDS:
                List<Multiset.Entry<String>> trends = calculateTrends();
                StringBuilder strTrends = new StringBuilder("Trending topics: <br>");
                trends.forEach(word -> strTrends.append(word.getElement() + " -> (" + word.getCount() + ") <br>"));
                Message trendsMessage = new Message(Message.TYPE_NOTIFICATION, MessagingController.FROM_SYSTEM, roomAdminId, strTrends);
                return trendsMessage;
        }
        return null;
    }

    public void registerMessageEntry(Message message) {
        //logger.info("Registering entry message" + message);
        long entryTime = System.currentTimeMillis();
        long sizeObject = message.getContent().toString().getBytes().length;
        long[] stats = {entryTime, sizeObject, 0};
        this.statsMap.put(message.getTimestamp().toString(), stats);
        //logger.info(statsMap.toString());
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
        Command command = null;
        try {
            command = Command.valueOf(commands.get(0));
        } catch (IllegalArgumentException exception) {
            return false;
        }
        return command == Command.STATS;
    }

    public boolean isForTrends(Message message) {
        final List<String> commands = new ArrayList<>(Arrays.asList(((String) message.getContent()).split(" ")));
        Command command = null;
        try {
            command = Command.valueOf(commands.get(0));
        } catch (IllegalArgumentException exception) {
            return false;
        }
        return command == Command.TRENDS;
    }

    public Message giveStats(String roomAdmin) {
        refreshStats();
        long memory = runtime.totalMemory() - runtime.freeMemory();

        DecimalFormat df = new DecimalFormat("##.##%");

        String text = Application.nameHost + "<br>"
                + " from " + Application.ipHost + "<br><br>"
                + "Number of connected users: " + getConnectedUsers() + "<br>"
                + "Messages on last minute: " + getMessagesLastMinute() + "<br>"
                + "Processed bytes on last minute: " + format(getBytesLastMinute(), 2) + "<br>"
                + "Average process time on last minute: " + getAverageProcessTime() + " ms<br>"
                + "Used memory: " + format(memory, 2) + "<br>"
                + "Free memory: " + format(runtime.freeMemory(), 2) + "<br>"
                + "CPU: " + df.format(this.operatingSystemMXBean.getProcessCpuLoad()) + "<br>";

        return new Message(Message.TYPE_TEXT, MessagingController.FROM_SYSTEM, roomAdmin, text);
    }

    public String giveCsvStats() {
        refreshStats();
        long memory = runtime.totalMemory() - runtime.freeMemory();

        return Application.nameHost + "|"
                + Application.ipHost + "|"
                + getConnectedUsers() + "|"
                + getMessagesLastMinute() + "|"
                + format(getBytesLastMinute(), 2) + "|"
                + getAverageProcessTime() + "|"
                + format(memory, 2) + "|"
                + format(runtime.freeMemory(), 2) + "|"
                + this.operatingSystemMXBean.getProcessCpuLoad() + "\n";
    }

    private List<Multiset.Entry<String>> calculateTrends() {
        List<Room> roomsList = rooms.findAll();
        List<Message> totalMessages = new ArrayList<>();
        roomsList.forEach(room -> {
            totalMessages.addAll(room.getLastOneHourMessages());
        });
        StringBuilder globalText = new StringBuilder();
        totalMessages.forEach(message -> {
            globalText.append(message.getContent() + "\n");
        });
        Iterable<Multiset.Entry<String>> words = getMoreCommonsWords(globalText.toString().toLowerCase());
        return Lists.newArrayList(words);
    }

    private Iterable<Multiset.Entry<String>> getMoreCommonsWords(String text) {
        List<String> theWords = Arrays.asList(text.split("\\s+"));
        Multiset<String> words = HashMultiset.create(theWords);

        List<Multiset.Entry<String>> wordCounts = Lists.newArrayList(words.entrySet());
        wordCounts.sort((left, right) -> {
            // Note reversal of 'right' and 'left' to get descending order
            return Integer.compare(right.getCount(), left.getCount());
        });

        Iterable<Multiset.Entry<String>> first10 = Iterables.limit(wordCounts, 10);

        return first10;
    }


    private void refreshStats() {
        long now = System.currentTimeMillis();
        List<String> listToDelete = new ArrayList<>();
        Iterator it = statsMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            long entryTime = ((long[]) pair.getValue())[0];
            long elapsedTime = now - entryTime;
            if (elapsedTime > MEASURE_TIME) {
                messagesLastMinute--;
                bytesLastMinute -= ((long[]) pair.getValue())[1];
                totalProcessTimeLastMinute -= ((long[]) pair.getValue())[2];
                this.averageProcessTime = (messagesLastMinute != 0) ? totalProcessTimeLastMinute / messagesLastMinute : 0;
                listToDelete.add((String) pair.getKey());
            }
        }
        listToDelete.forEach(key -> this.statsMap.remove(key));
    }

    @EventListener(SessionConnectEvent.class)
    public void handleWebsocketConnectListner(SessionConnectEvent event) {
        this.connectedUsers++;
    }

    @EventListener(SessionDisconnectEvent.class)
    public void handleWebsocketDisconnectListner(SessionDisconnectEvent event) {
        this.connectedUsers--;
    }

    private Message getFailMessage(String userId) {
        return new Message(Message.TYPE_NOTIFICATION, MessagingController.FROM_SYSTEM, userId, "Sorry. I don't understand you.");
    }

    private String format(double bytes, int digits) {
        String[] dictionary = {"bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
        int index = 0;
        for (index = 0; index < dictionary.length; index++) {
            if (bytes < 1024) {
                break;
            }
            bytes = bytes / 1024;
        }
        return String.format("%." + digits + "f", bytes) + " " + dictionary[index];
    }

    private enum Command {
        CENSOR, STATS, TRENDS
    }
}
