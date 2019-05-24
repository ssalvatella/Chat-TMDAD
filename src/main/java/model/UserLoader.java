package model;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import websockets.MessagingController;

import java.util.Arrays;

@Component
public class UserLoader implements CommandLineRunner {

    private final Logger logger = LoggerFactory.getLogger(UserLoader.class);

    @Autowired
    private UserRepository users;

    @Autowired
    private RoomRepository rooms;

    @Autowired
    private CensorhipRepository censorship;

    @Autowired
    private MessagingController controller;


    @Override
    public void run(String... args) throws Exception {

        logger.info("Deleting previous users and rooms...");
        users.deleteAll();
        rooms.deleteAll();
        censorship.deleteAll();

        logger.info("Creating users for tests purposes...");


        User admin = new User("admin", User.SUPER);
        User laura = new User("Laura", User.NORMAL);
        User sergio = new User("Sergio", User.NORMAL);
        User daniel = new User("Daniel", User.NORMAL);

        User pedro = new User("Pedro", User.NORMAL);
        User ruben = new User("Ruben", User.NORMAL);
        User francisco = new User("Francisco", User.NORMAL);

        User samuel = new User("Samuel", User.NORMAL);
        User paco = new User("Paco", User.NORMAL);
        User sara = new User("Sara", User.NORMAL);
        User manuel = new User("Manuel", User.NORMAL);
        users.saveAll(Arrays.asList(samuel, paco, sara, admin, manuel, laura, sergio, daniel, pedro, ruben, francisco));

        logger.info("Users added.");
        logger.info("Loading rooms for tests purposes...");

        logger.info("Writing on BD...");
        final Room adminRoom = new Room("Administration", Room.TYPE_ADMIN, MessagingController.FROM_SYSTEM, Arrays.asList(admin));
        final Room systemRoom = new Room("Announcements", Room.TYPE_SYSTEM, MessagingController.FROM_SYSTEM, Arrays.asList(admin, samuel, paco, sara, manuel));
        final Room room1 = new Room("Samuel-Paco", Room.TYPE_PRIVATE, samuel.getId(), Arrays.asList(samuel, paco));
        final Room room2 = new Room("Prueba de grupo", Room.TYPE_GROUP, paco.getId(), Arrays.asList(samuel, paco, sara));
        final Room grupoClase = new Room("Clase", Room.TYPE_GROUP, samuel.getId(), Arrays.asList(samuel, sergio, laura, daniel));
        final Room grupoProfes = new Room("Profesores", Room.TYPE_GROUP, pedro.getId(), Arrays.asList(pedro, ruben, francisco));
        final Room grupoProfesAlumnos = new Room("Alumnos y profesores", Room.TYPE_GROUP, samuel.getId(), Arrays.asList(pedro, ruben, francisco, laura, daniel, sergio, samuel));

        rooms.saveAll(Arrays.asList(adminRoom, systemRoom, room1, room2, grupoClase, grupoProfes, grupoProfesAlumnos));
        controller.addRoomToUsers(adminRoom);
        controller.addRoomToUsers(systemRoom);
        controller.addRoomToUsers(room1);
        controller.addRoomToUsers(room2);

        controller.addRoomToUsers(grupoClase);
        controller.addRoomToUsers(grupoProfes);
        controller.addRoomToUsers(grupoProfesAlumnos);
        logger.info("Write finished.");
    }
}
