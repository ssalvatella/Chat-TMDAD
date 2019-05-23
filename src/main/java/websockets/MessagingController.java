package websockets;

import admin.AdminService;
import admin.CensorshipService;
import amqp.Application;
import model.Room;
import model.RoomRepository;
import model.User;
import model.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import security.SecurityService;

import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class MessagingController {

    public static final String FROM_SYSTEM = "BigBrotherSystem";
    private static final Logger logger = LoggerFactory.getLogger(MessagingController.class);

    @Autowired
    SimpMessagingTemplate template;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private UserRepository usersRepository;

    @Autowired
    private RoomRepository roomsRepository;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private CensorshipService censorshipService;

    @Autowired
    private AdminService adminService;


    @Async
    public void addMessageToRoom(Message message) {
        Room roomDst = roomsRepository.findById(message.getTo()).get();
        roomDst.addMessage(message);
        roomsRepository.save(roomDst);
    }

    private boolean manageLogin(Message message, String session, Principal principal) {
        User user = usersRepository.findByName((String) message.getContent());
        if (user == null) return false;

        user.dropSystemMembersRoom(); // User didn't need to see all system members of room
        Message mg = new Message(Message.TYPE_LOGIN_ACK, FROM_SYSTEM, user.getId(), user);
        this.template.convertAndSendToUser(principal.getName(), "/queue/reply", mg);

        Message mgUsers = new Message(Message.TYPE_USERS, FROM_SYSTEM, user.getId(), usersRepository.findAll());
        this.template.convertAndSendToUser(principal.getName(), "/queue/reply", mgUsers);
        return true;
    }

    /**
     * Messages from users websockets
     * Session domain
     *
     * @param message
     * @param principal
     * @param headerAccessor
     * @return
     * @throws Exception
     */
    @MessageMapping("/send")
    public Message receive(Message message, Principal principal, SimpMessageHeaderAccessor headerAccessor) throws Exception {

        logger.info("Received from WebSocket: " + message.toString());
        String sessionId = headerAccessor.getSessionAttributes().get("sessionId").toString();
        message.setTimestamp(Date.from(Instant.now())); // Setting timestamp to message

        switch (message.getType()) {
            case Message.TYPE_LOGIN:
                manageLogin(message, sessionId, principal);
                break;

            case Message.TYPE_REQUEST_MESSAGES:
                String idRoom = (String) message.getContent();
                Room room = this.roomsRepository.findById(idRoom).get();
                List<Message> censoredMessages = room.getMessages().stream().map(mg -> this.censorshipService.processMessage(mg)).collect(Collectors.toList());
                Message mg = new Message(Message.TYPE_REQUEST_MESSAGES, "big brother", principal.getName(), censoredMessages);
                this.template.convertAndSendToUser(principal.getName(), "/queue/reply", mg);
                break;

            case Message.TYPE_NOTIFICATION:
            case Message.TYPE_FILE:
            case Message.TYPE_TEXT:
                addMessageToRoom(message); // -> Async
                sendToFanoutBroker(message);
                adminService.registerMessageEntry(message); // -> Stats
                break;
            case Message.TYPE_ADMIN:
                if (adminService.isForStats(message)) break;
            default: // By default, messages are send to consistency domain
                sendToFanoutBroker(message);
                break;
        }
        return null;
    }

    /**
     * Messages from RabbitMQ
     * Consistency domain
     *
     * @param message
     * @return
     */
    @SendTo("/chat")
    public Message send(Message message) {

        logger.info("Received from RabbitMQ: " + message.toString());

        try {

            switch (message.getType()) {
                case Message.TYPE_NOTIFICATION:
                case Message.TYPE_FILE:
                case Message.TYPE_TEXT:
                    final Room room = roomsRepository.findById(message.getTo()).get();
                    if (room.getType().equals(Room.TYPE_SYSTEM)) {
                        User fromUser = usersRepository.findById(message.getFrom()).get();
                        if (!fromUser.getType().equals(User.SUPER)) break;
                    }
                    Message censored = this.censorshipService.processMessage(message);
                    room.getMembers()
                            .forEach(user -> this.template.convertAndSendToUser(user.getId(), "/queue/reply", censored));
                    adminService.registerMessageExit(message); // -> Stats
                    break;

                case Message.TYPE_CREATE_ROOM:
                    final LinkedHashMap map = (LinkedHashMap) message.getContent();
                    final String name = map.get("nameRoom").toString();
                    List<String> membersId = (List<String>) map.get("membersRoom");
                    membersId.add(message.getFrom());
                    final List<User> members = this.usersRepository.findByIdIn(membersId);
                    if (members.size() <= 1 || name.isEmpty()) break;
                    final String typeRoom = (members.size() > 2) ? Room.TYPE_GROUP : Room.TYPE_PRIVATE;
                    final Room newRoom = new Room(name, typeRoom, message.getFrom(), members);
                    this.roomsRepository.save(newRoom);
                    addRoomToUsers(newRoom);
                    notifyNewRoom(newRoom);
                    break;

                case Message.TYPE_DROP_ROOM:
                    String roomToDrop = message.getTo();
                    String userId = message.getFrom();
                    if (this.securityService.checkRoomCreator(userId, roomToDrop)) {
                        Room roomDropped = this.roomsRepository.findById(roomToDrop).get();
                        dropRoomOnUsers(roomDropped);
                        notifyDropRoom(roomDropped);
                    }
                    break;

                case Message.TYPE_INVITE_USER:
                    final Room roomToInvite = roomsRepository.findById(message.getTo()).get();
                    final LinkedHashMap mapContent = (LinkedHashMap) message.getContent();
                    final List<User> invitedUsers = this.usersRepository.findByIdIn((List<String>) mapContent.get("membersRoom"));
                    roomToInvite.addUsers(invitedUsers);
                    this.roomsRepository.save(roomToInvite);
                    addRoomToUsers(roomToInvite, invitedUsers);
                    notifyInvitedRoom(roomToInvite, invitedUsers);
                    break;

                case Message.TYPE_ADMIN:
                    if (this.securityService.checkAdminMessage(message.getFrom(), message)) {
                        final List<String> commands = new ArrayList<>(Arrays.asList(((String) message.getContent()).split(" ")));
                        Message messageProcess = this.adminService.processCommand(message.getTo(), commands);
                        sendToWebSocket(message.getFrom(), messageProcess);
                        if (adminService.isForStats(message)) {
                            sendToFanoutBroker(new Message(Message.TYPE_STATS, message.getFrom(), message.getTo(), "Please, give me stats"));
                        }
                    }
                    break;

                case Message.TYPE_STATS:
                    sendToFanoutBroker(adminService.giveStats(message.getTo()));
                    break;

            }
        } catch (Exception exception) {
            logger.error("Exception processing message.");
            logger.error(exception.getMessage());
            exception.printStackTrace();
        }

        return message;
    }

    public void sendMessageFile(Message message) {
        message.setTimestamp(Date.from(Instant.now()));
        sendToFanoutBroker(message);
        addMessageToRoom(message); // -> Async
    }

    public void sendToFanoutBroker(Message message) {
        message.setTimestamp(Date.from(Instant.now()));
        rabbitTemplate.convertAndSend(Application.fanoutExchangeName, "Don't care", message);
    }

    private void sendToWebSocket(String userId, Message message) {
        this.template.convertAndSendToUser(userId, "/queue/reply", message);
    }

    private void addRoomToUsers(Room r, List<User> users) {
        users.forEach(user -> user.addRoom(r));
        this.usersRepository.saveAll(r.getMembers());
    }

    private void dropRoomOnUsers(Room r) {
        List<String> userId = r.getMembers().stream().map(User::getId).collect(Collectors.toList());
        List<User> users = this.usersRepository.findByIdIn(userId);
        users.forEach(user -> user.removeRoom(r));
        this.usersRepository.saveAll(users);
    }

    public void addRoomToUsers(Room r) {
        r.getMembers().forEach(user -> user.addRoom(r));
        this.usersRepository.saveAll(r.getMembers());
    }

    private void notifyNewRoom(Room r) {
        r.getMembers().forEach(member -> {
            Message message = new Message(Message.TYPE_CREATE_ROOM, FROM_SYSTEM, member.getId(), r.getMinified());
            message.setTimestamp(Date.from(Instant.now()));
            this.template.convertAndSendToUser(member.getId(), "/queue/reply", message);
        });
    }

    private void notifyDropRoom(Room r) {
        r.getMembers().forEach(member -> {
            Message message = new Message(Message.TYPE_DROP_ROOM, FROM_SYSTEM, member.getId(), r.getId());
            message.setTimestamp(Date.from(Instant.now()));
            this.template.convertAndSendToUser(member.getId(), "/queue/reply", message);
        });
    }

    private void notifyInvitedRoom(Room room, List<User> invitedUsers) {
        invitedUsers.forEach(invitedUser -> {
            Message message = new Message(Message.TYPE_NOTIFICATION, FROM_SYSTEM, room.getId(), invitedUser.getName() + " has joined the room.");
            message.setTimestamp(Date.from(Instant.now()));
            sendToFanoutBroker(message);
        });
    }

}
