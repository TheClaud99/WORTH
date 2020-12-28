import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class StorageManager {

    private final ObjectMapper mapper;
    private final String storageDir;
    private final String usersFilePath;
    private final String cardFile;

    StorageManager(String storageDir, String usersFilePath, String cardFile) {
        this.storageDir = storageDir;
        this.usersFilePath = usersFilePath;
        this.cardFile = cardFile;
        this.mapper = new ObjectMapper();

        File storage = new File(storageDir);
        if(!storage.exists()) {
            storage.mkdir();
        }
    }

    public ArrayList<User> restoreUsers() throws IOException {
        File file = new File(usersFilePath);
        if(file.createNewFile()) {
            mapper.writeValue(file, User[].class);
        }

        return new ArrayList<>(Arrays.asList(mapper.readValue(file, User[].class)));
    }

    public void updateUsers(ArrayList<User> users) throws IOException {
        File file = new File(usersFilePath);
        file.createNewFile();
        mapper.writeValue(file, users);
    }
}
