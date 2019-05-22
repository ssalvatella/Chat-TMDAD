package admin;

import model.CensorhipRepository;
import model.Censorship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import websockets.Message;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CensorshipService {

    @Autowired
    private CensorhipRepository censorhipRepository;

    private List<Censorship> censoredWords;

    public CensorshipService() {
    }

    @EventListener(ApplicationReadyEvent.class)
    private void loadCensoredList() {
        this.censoredWords = this.censorhipRepository.findAll();
    }

    public boolean addWordCensored(String adminId, String word) {
        Censorship exist = this.censorhipRepository.findByWord(word);
        if (exist != null) return false;
        Censorship censoredWord = new Censorship(adminId, word);
        this.censoredWords.add(censoredWord);
        this.censorhipRepository.save(censoredWord);
        return true;
    }

    public Message processMessage(Message message) {
        List<String> wordList = censoredWords.stream().map(Censorship::getWord).collect(Collectors.toList());
        String lstr = wordList.toString();
        String regex = lstr.substring(1, lstr.length() - 1).replace(", ", "|");
        String text = (String) message.getContent();
        text = text.replaceAll("\\b(" + regex + ")\\b", "***");
        message.setContent(text);
        return message;
    }

}
