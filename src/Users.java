import Utils.Response;

import java.io.IOException;
import java.util.ArrayList;

public class Users {

    private ArrayList<User> users;
    private StorageManager storage;

    Users() {
    }

    Users(StorageManager storage) throws IOException {
        this.storage = storage;
        this.users = storage.restoreUsers();
    }

    Users(ArrayList<User> users, StorageManager storage) {
        this.storage = storage;
        this.users = users;
    }

    public boolean register(String username, String password) throws IOException {
        for (User user : users) {
            System.out.println(user.getUsername());
            if (user.getUsername().equals(username)) {
                return false;
            }
        }

        users.add(new User(username, password));
        storage.updateUsers(users);
        return true;
    }
}
