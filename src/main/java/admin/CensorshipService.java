package admin;

import model.CensorhipRepository;
import model.Censorship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import websockets.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CensorshipService {

    @Autowired
    private CensorhipRepository censorhipRepository;

    private Map<String, Censorship> censoredWords;

    public CensorshipService() {
        this.censoredWords = new HashMap<>();
    }

    @EventListener(ApplicationReadyEvent.class)
    private void loadCensoredList() {
        this.censorhipRepository.findAll().forEach(word -> this.censoredWords.put(word.getWord(), word));
    }

    public boolean addWordCensored(String adminId, String word) {
        Censorship exist = this.censorhipRepository.findByWord(word);
        if (exist != null) {
            this.censoredWords.putIfAbsent(exist.getWord(), exist);
            return false;
        }
        Censorship censoredWord = new Censorship(adminId, word);
        this.censoredWords.put(censoredWord.getWord(), censoredWord);
        this.censorhipRepository.save(censoredWord);
        return true;
    }

    public Message processMessage(Message message) {
        if (message.getType().equals(Message.TYPE_TEXT)) {
            message.setContent(censorText((String) message.getContent()));
        }
        return message;
    }

    public String censorText(String text) {
        if (censoredWords.isEmpty()) return text;
        List<String> wordList = censoredWords.values().stream().map(Censorship::getWord).collect(Collectors.toList());
        if (wordList.isEmpty()) return text;
        String lstr = wordList.toString();
        String regex = lstr.substring(1, lstr.length() - 1).replace(", ", "|");
        text = text.replaceAll("\\b(" + regex + ")\\b", "***");
        return text;
    }


}
