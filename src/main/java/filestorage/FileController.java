package filestorage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import websockets.Message;
import websockets.MessagingController;

@RestController
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileService fileService;

    @Autowired
    private MessagingController messagingController;

    @PostMapping("/uploadFile")
    public void uploadFile(@RequestParam("from") String from,
                           @RequestParam("to") String to,
                           @RequestParam("file") MultipartFile file) {
        // Gestión del archivo
        try {
            final UploadFileResponse response = this.fileService.uploadFile(file);
            Message message = new Message(Message.TYPE_FILE, from, to, response);
            this.messagingController.sendMessageFile(message);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

    }
}
