import Exceptions.CardNotFoundException;
import Exceptions.IllegalMoveException;
import Exceptions.ProjectNotFoundException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Projects {

    private final ArrayList<Project> projects;
    private final StorageManager storage;

    Projects(StorageManager storage) throws IOException {
        this.storage = storage;
        this.projects = storage.restoreProjects();
    }

    public Project getByName(String name) throws ProjectNotFoundException {
        for (Project project : projects) {
            if(project.getName().equals(name)) {
                return project;
            }
        }

        throw new ProjectNotFoundException();
    }

    public void addProject(String name) throws IOException, IllegalArgumentException {
        for (Project project : projects) {
            if(project.getName().equals(name)) {
                throw new IllegalArgumentException("Esiste già un progetto con quel nome");
            }
        }
        projects.add(new Project(name));
        storage.updateProjects(projects);
    }

    public void deleteProject(String name) throws IOException {
        Project project = getByName(name);
        projects.remove(project);
        storage.updateProjects(projects);
    }

    public void deleteProject(Project project) throws IOException {
        projects.remove(project);
        storage.updateProjects(projects);
    }

    public void addMember(String name, String user) throws IOException {
        Project project = getByName(name);
        project.addMember(user);
        storage.updateProjects(projects);
    }

    public boolean isMember(String name, String User) {
        return getByName(name).isMember(User);
    }

    public void createCard(String name, String cardName, String description) throws IOException {
        getByName(name).createCard(cardName, description);
        storage.updateProjects(projects);
    }

    //Muove gli stati di una Card
    public void moveCard(String name, String cardName, String oldState, String newState) throws CardNotFoundException, IllegalArgumentException, IllegalMoveException, IOException {
        getByName(name).moveCard(cardName, oldState, newState);
        storage.updateProjects(projects);
    }

    //restituisce una lista con i nomi di tutte le Card
    public List<String> getCardsList(String name) {
        return getByName(name).getCardsList();
    }

    public List<String> listProjects(String user) {
        List<String> projectList = new ArrayList<>();
        for (Project project : projects) {
            if(project.isMember(user)) {
                projectList.add(project.getName());
            }
        }

        return projectList;
    }

    public List<Card> getCards(String name) {
        return getByName(name).getCards();
    }

    public boolean isDone(String name) {
        return getByName(name).isDone();
    }

    public List<String> getCardHistory(String name, String cardName) {
        return getByName(name).getCardHistory(cardName);
    }

    public List<String> getCardInfo(String name, String cardName) {
        return getByName(name).getCardInfo(cardName);
    }

    public void updateProjects() throws IOException {
        storage.updateProjects(projects);
    }

    public Map<String, String> getChatList(String user) {
        Map<String, String> chatList = new HashMap<>();
        for(Project project : projects) {
            if(project.isMember(user)) {
                chatList.put(project.getName(), project.getIP_Multicast());
            }
        }
        return chatList;
    }
}
