package filestorage;

import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    private final String BUCKET = "files";
    @Value("${URL_MINIO:http://127.0.0.1:9000}")
    private String URL;
    @Value("${ACCESS_MINIO:LE0BQ5JAFL1EX6995R61}")
    private String ACCESS_KEY;
    @Value("${SECRET_MINIO:ZwctdyYFrpba7Pq9LHzI0+HW6Qam4x+4LhS0syR2}")
    private String SECRET_KEY;
    @Value("${USE_FILE_SERVICE:1}")
    private int useFileService;

    private MinioClient minioClient;

    public FileService() {

    }

    @EventListener(ApplicationReadyEvent.class)
    private void connectToMinio() throws Exception {
        if (useFileService != 0) {
            this.minioClient = new MinioClient(URL, ACCESS_KEY, SECRET_KEY);
            createBucket();
            logger.info("File Service started.");
        } else {
            logger.warn("File Service temporarily  disabled.");
        }
    }

    private void createBucket() throws Exception {
        boolean exist = minioClient.bucketExists(BUCKET);
        if (exist) {
            // Deleting all files...
            minioClient.listObjects(BUCKET).forEach(ob -> {
                try {
                    minioClient.removeObject(BUCKET, ob.get().objectName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            minioClient.makeBucket(BUCKET);
            minioClient.setBucketPolicy(BUCKET, "readonly");
        }
    }

    public UploadFileResponse uploadFile(MultipartFile file) throws Exception {
        if (useFileService != 0) {
            final String name = file.getOriginalFilename();
            this.minioClient.putObject(BUCKET, name, file.getInputStream(), "application/octet-stream");
            int index = file.getContentType().indexOf("/");
            final String typeFile = file.getContentType().substring(0, index);
            return new UploadFileResponse(name, URL + "/" + BUCKET + "/" + name, typeFile, file.getSize());
        } else {
            return new UploadFileResponse("File Service temporarily disabled.", "", "notificaction", file.getSize());
        }

    }
}
