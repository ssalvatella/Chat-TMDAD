package model;

import lombok.Getter;
import websockets.Message;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
public class Censorship {

    private String adminId;
    private String word;
    private Date timestamp;
    private List<Message> censoredMessages;

    public Censorship() {

    }

    public Censorship(String adminId, String word) {
        this.adminId = adminId;
        this.word = word;
        this.censoredMessages = new ArrayList<>();
        this.timestamp = Date.from(Instant.now());
    }

}
