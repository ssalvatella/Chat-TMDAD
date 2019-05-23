package admin;

import model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import websockets.Message;
import websockets.MessagingController;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.*;

@Component
public class TestLoader implements CommandLineRunner {

    private static final int SIZE_TEXT = 600;
    private final Logger logger = LoggerFactory.getLogger(TestLoader.class);

    @Autowired
    private UserRepository users;

    @Autowired
    private RoomRepository rooms;

    @Autowired
    private CensorhipRepository censorship;

    @Autowired
    private AdminService adminService;

    @Autowired
    private MessagingController controller;

    private List<User> testUsers;
    private Room adminRoom;
    private Room normalRoom;
    private Random rand = new Random();
    private String textMessage;
    private PrintWriter printWriter;

    private boolean ready = false;
    private boolean saveStats = true;

    @Value("${TEST_USERS:1000}")
    private int numTestUsers;


    public void loadTest() {

        logger.info("Deleting previous users and rooms...");
        users.deleteAll();
        rooms.deleteAll();
        censorship.deleteAll();

        logger.info("Creating " + numTestUsers + " users for tests purposes...");
        testUsers = new ArrayList<>();

        User admin = new User("admin", User.SUPER);
        testUsers.add(admin);

        for (int i = 0; i < numTestUsers; i++) {
            User testUser = new User("" + i, User.NORMAL);
            testUsers.add(testUser);
        }
        users.saveAll(testUsers);
        logger.info(numTestUsers + " users created and saved on BD.");

        logger.info("Generating rooms...");
        adminRoom = new Room("Administration", Room.TYPE_ADMIN, MessagingController.FROM_SYSTEM, Arrays.asList(admin));
        normalRoom = new Room("Test room", Room.TYPE_GROUP, admin.getId(), testUsers);
        rooms.saveAll(Arrays.asList(adminRoom, normalRoom));
        controller.addRoomToUsers(adminRoom);
        controller.addRoomToUsers(normalRoom);
        logger.info("Generated rooms and saved on BD");


    }

    @Override
    public void run(String... args) throws Exception {
        //
        try {
            printWriter = new PrintWriter("test-results.txt", "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //loadTest();
        //textMessage = getAlphaNumericString(SIZE_TEXT);
        ready = true;
    }

    //@Scheduled(fixedRate = 20)
    private void sendRandomMessage() {
        if (!ready) return;
        User random = testUsers.get(rand.nextInt(testUsers.size()));
        Principal principal = random::getId;
        SimpMessageHeaderAccessor accesor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        Map<String, Object> session = new HashMap<>();
        session.put("sessionId", random.getId());
        accesor.setSessionAttributes(session);
        Message message = new Message(Message.TYPE_TEXT, random.getId(), normalRoom.getId(), textMessage);
        try {
            this.controller.receive(message, principal, accesor);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Scheduled(fixedDelay = 1000)
    private void saveStats() {
        if (saveStats && ready)
            printWriter.write(adminService.giveCsvStats());
    }

    @Scheduled(fixedRate = 600000)
    private void closeStats() {
        if (ready) {
            saveStats = false;
            printWriter.close();
            ready = false;
        }
    }


    private String getAlphaNumericString(int n) {

        // chose a Character random from this String
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";

        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {

            // generate a random number between
            // 0 to AlphaNumericString variable length
            int index
                    = (int) (AlphaNumericString.length()
                    * Math.random());

            // add Character one by one in end of sb
            sb.append(AlphaNumericString
                    .charAt(index));
        }

        return sb.toString();
    }
}
