import Exceptions.MultipleLoginsException;
import Exceptions.UserAlreadyLoggedException;
import Exceptions.UserNotFoundException;
import Utils.Notification;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Users {

    private final ArrayList<User> users;
    private final StorageManager storage;
    private final Map<SelectionKey, User> userKeys;

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

    public synchronized User getByUsername(String username) throws UserNotFoundException {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }

        throw new UserNotFoundException("Utente '" + username + "' non trovato");
    }

    public synchronized boolean register(String username, String password) throws IOException {
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

    public synchronized void notifyAll(Projects projects) {
        for (User user : users) {
            try {
                user.notify(new Notification(getUsersList(), projects.getChatList(user.getUsername())));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean login(String username, String password, SelectionKey userKey) throws UserNotFoundException, UserAlreadyLoggedException, MultipleLoginsException {
        User user = getByUsername(username);
        if(userKeys.containsKey(userKey)) throw new UserAlreadyLoggedException();
        if(user.isOnline()) throw new MultipleLoginsException();
        if (user.login(password)) {
            userKeys.put(userKey, user);
            return true;
        }

        return false;
    }

    public void setClient(String username, NotifyEventInterface clientInterface) throws UserNotFoundException {
        User user = getByUsername(username);
        user.setClient(clientInterface);
    }

    public boolean isLogged(SelectionKey userKey) {
        return userKeys.containsKey(userKey);
    }

    public Map<String, Boolean> getUsersList() {
        Map<String, Boolean> usersList = new HashMap<>();
        for (User user : users) {
            usersList.put(user.getUsername(), user.isOnline());
        }

        return usersList;
    }

    public String[] getOnlineUsersList() {
        String[] usersList = new String[userKeys.size()];
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            if(user.isOnline())
                usersList[i] = user.getUsername();
        }

        return usersList;
    }

    public void logout(SelectionKey key) {
        if(!userKeys.containsKey(key))
            return;

        User user = userKeys.get(key);
        user.setOnline(false);
        user.setClient(null);
        userKeys.remove(key);
    }

    public String getUsernameByKey(SelectionKey key) {
        return userKeys.get(key).getUsername();
    }

    public ArrayList<User> getUsers() {
        return users;
    }
}
