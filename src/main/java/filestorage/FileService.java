package filestorage;

import io.minio.MinioClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    private final String URL = "http://127.0.0.1:9000";
    private final String ACCESS_KEY = "LE0BQ5JAFL1EX6995R61";
    private final String SECRET_KEY = "ZwctdyYFrpba7Pq9LHzI0+HW6Qam4x+4LhS0syR2";
    private final String BUCKET = "files";

    private MinioClient minioClient;

    public FileService() throws Exception {
        //this.minioClient = new MinioClient(URL, ACCESS_KEY, SECRET_KEY);
        //createBucket();
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
        return new UploadFileResponse("Temporary files disabled", "", "notificaction", file.getSize());
        //final String name = file.getOriginalFilename();
        //this.minioClient.putObject(BUCKET, name, file.getInputStream(), "application/octet-stream");
        //int index = file.getContentType().indexOf("/");
        //final String typeFile = file.getContentType().substring(0, index);
        //return new UploadFileResponse(name, URL + "/" + BUCKET + "/" + name, typeFile, file.getSize());
    }
}
