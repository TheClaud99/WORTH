import Utils.Response;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Users {

    private ArrayList<User> users;
    private StorageManager storage;
    private Map<SelectionKey, User> userKeys;

    Users() {
    }

    Users(StorageManager storage) throws IOException {
        this.storage = storage;
        this.users = storage.restoreUsers();
        this.userKeys = new HashMap<>();
    }

    Users(ArrayList<User> users, StorageManager storage) {
        this.storage = storage;
        this.users = users;
        this.userKeys = new HashMap<>();
    }

    public User getByUsername(String username) throws UserNotFoundException {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }

        throw new UserNotFoundException();
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

    public boolean login(String username, String password, SelectionKey userKey) throws UserNotFoundException {
        User user = getByUsername(username);
        if(user.getPassword().equals(password)) {
            userKeys.put(userKey, user);
            return true;
        }

        return false;
    }
}
