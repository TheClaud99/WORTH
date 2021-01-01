import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;

public class Card {
    public enum cardStatus {TODO, INPROGRESS, TOBEREVISED, DONE}

    private String name;
    private String description;
    private cardStatus CurrentState;
    private ArrayList<cardStatus> cardHistory;

    public Card(String name, String description) {
        this.name = name;
        this.description = description;
        this.CurrentState = cardStatus.TODO;
        this.cardHistory = new ArrayList<>();
        this.cardHistory.add(cardStatus.TODO);
    }

    public Card() {
    }

    public String getName() {
        return this.name;
    }

    public String getState() {
        return this.CurrentState.name();
    }

    public String getDescription() {
        return description;
    }

    public ArrayList<String> getCardHistory() {
        ArrayList<String> lst = new ArrayList<>();
        for (cardStatus cardStatus : cardHistory) {
            lst.add(cardStatus.name());
        }
        return lst;
    }

    public void setCardHistory(ArrayList<cardStatus> cardHistory) {
        this.cardHistory = cardHistory;
    }

    public void setState(String CurrentState) {
        this.CurrentState = cardStatus.valueOf(CurrentState.toUpperCase());
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonIgnore
    public ArrayList<String> getInfo() {
        ArrayList<String> lst = new ArrayList<>();
        lst.add(name);
        lst.add(description);
        lst.add(CurrentState.name());
        return lst;
    }

    @JsonIgnore
    public void changeStatus(String status) {
        this.CurrentState = cardStatus.valueOf(status.toUpperCase());
        this.cardHistory.add(CurrentState);
    }
}
