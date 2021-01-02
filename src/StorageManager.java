import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import Utils.Utils;

public class StorageManager {

    private final ObjectMapper mapper;
    private final String storageDir;
    private final String usersFilePath;
    private final String projecstDir;
    private final String membersFileName = "members.json";

    StorageManager(String storageDir, String usersFilePath, String projectDir) {
        this.storageDir = storageDir;
        this.usersFilePath = usersFilePath;
        this.projecstDir = projectDir;
        this.mapper = new ObjectMapper();

        File storage = new File(storageDir);
        if (!storage.exists()) {
            storage.mkdir();
        }
    }

    public ArrayList<User> restoreUsers() throws IOException {
        File file = new File(usersFilePath);
        if (file.createNewFile()) {
            mapper.writeValue(file, User[].class);
        }

        return new ArrayList<>(Arrays.asList(mapper.readValue(file, User[].class)));
    }

    public ArrayList<Project> restoreProjects() throws IOException {
        ArrayList<Project> projects = new ArrayList<>();
        File dir = new File(projecstDir);

        if (!dir.exists())
            dir.mkdir();

        for (File projectDir : dir.listFiles()) {
            if (projectDir.isDirectory()) {
                Project project = new Project(projectDir.getName());
                for (File cardFile : projectDir.listFiles()) {
                    if (cardFile.getName().equals(membersFileName)) {
                        project.setMembers(new ArrayList<>(Arrays.asList(mapper.readValue(cardFile, String[].class))));
                        continue;
                    }
                    project.addCard(mapper.readValue(cardFile, Card.class));
                }
                projects.add(project);
            }
        }

        return projects;
    }

    public void updateProjects(ArrayList<Project> projects) throws IOException {

        // Elimina la cartella e la ricrea vuota
        File projectsDirectory = new File(projecstDir);
        Utils.deleteDirectory(projectsDirectory);
        projectsDirectory.mkdir();

        // Salva i progetti
        for (Project project : projects) {
            String projectPath = projecstDir + "/" + project.getName();
            File projectDir = new File(projectPath);
            if (!projectDir.exists()) {
                projectDir.mkdir();
            }
            for (Card card : project.getCards()) {
                File cardFile = new File(projectPath + "/card_" + card.getName() + ".json");
                cardFile.createNewFile();
                mapper.writeValue(cardFile, card);
            }

            File membersFile = new File(projectPath + "/" + membersFileName);
            membersFile.createNewFile();
            mapper.writeValue(membersFile, new ArrayList<>(project.getMembers()));
        }
    }

    public void updateUsers(ArrayList<User> users) throws IOException {
        File file = new File(usersFilePath);
        file.createNewFile();
        mapper.writeValue(file, users);
    }
}
