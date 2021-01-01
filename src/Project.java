import Exceptions.CardNotFoundException;
import Exceptions.IllegalMoveException;

import java.util.ArrayList;
import java.util.List;

public class Project {

    private String NAME;
    private List<Card> Cards;

    private List<String> Cards_TODO;
    private List<String> Cards_InProgess;
    private List<String> Cards_Revisited;
    private List<String> Cards_Done;

    private List<String> members;

    private String IP_Multicast;
    private int PORT = 1998;

    public Project(String name) {
        this.NAME = name;
        this.Cards = new ArrayList<>();
        this.Cards_TODO = new ArrayList<>();
        this.Cards_InProgess = new ArrayList<>();
        this.Cards_Revisited = new ArrayList<>();
        this.Cards_Done = new ArrayList<>();

        this.members = new ArrayList<>();
    }


    public String getName() {
        return this.NAME;
    }

    //Aggiunge un Nuovo membro al progetto. Se gia presente lancia una eccezione
    public void addMember(String user) {
        if (this.members.contains(user))
            throw new IllegalArgumentException();
        members.add(user);
    }

    public boolean isMember(String User) {
        return members.contains(User);
    }

    public void createCard(String name, String description) {
        for (Card c : Cards) {
            if (c.getName().equals(name)) throw new IllegalArgumentException();
        }

        addCard(new Card(name, description));
    }

    //Crea una nuova Card e la inserisce, se il nome non è gia usato da un'altra Card
    public void addCard(Card card) {
        for (Card c : Cards) {
            if (c.getName().equals(card.getName())) throw new IllegalArgumentException();
        }

        Cards.add(card);
        switch (card.getState()) {
            case "TODO":
                Cards_TODO.add(card.getName());
                break;
            case "INPROGRESS":
                Cards_InProgess.add(card.getName());
                break;
            case "TOBEREVISED":
                Cards_Revisited.add(card.getName());
                break;
            case "DONE":
                Cards_Done.add(card.getName());
                break;
        }
    }

    public Card getCardByName(String name) throws CardNotFoundException {
        for (Card card : Cards) {
            if (card.getName().equals(name))
                return card;
        }

        throw new CardNotFoundException();
    }

    //Muove gli stati di una Card
    public void moveCard(String name, String oldState, String newState) throws CardNotFoundException, IllegalArgumentException, IllegalMoveException {
        Card card = getCardByName(name);

        if (!card.getState().equals(oldState.toUpperCase()))
            throw new IllegalArgumentException("Lo stato vecchio non combacia");

        switch (oldState.toUpperCase()) {
            case "TODO":
                if (!newState.equalsIgnoreCase("INPROGRESS")) {
                    throw new IllegalMoveException(oldState, newState);
                } else {
                    card.changeStatus("INPROGRESS");
                    Cards_TODO.remove(name);
                    Cards_InProgess.add(name);
                    break;
                }
            case "INPROGRESS":
                if (!newState.equalsIgnoreCase("TOBEREVISED") && !newState.equalsIgnoreCase("DONE")) {
                    throw new IllegalMoveException(oldState, newState);
                } else {
                    card.changeStatus(newState.toUpperCase());
                    if (newState.equalsIgnoreCase("TOBEREVISED")) {
                        Cards_InProgess.remove(name);
                        Cards_Revisited.add(name);
                    } else {
                        Cards_InProgess.remove(name);
                        Cards_Done.add(name);
                    }
                    break;
                }
            case "TOBEREVISED":
                if (!newState.equalsIgnoreCase("INPROGRESS") && !newState.equalsIgnoreCase("DONE")) {
                    throw new IllegalMoveException(oldState, newState);
                } else {
                    card.changeStatus(newState.toUpperCase());
                    if (newState.equalsIgnoreCase("INPROGRESS")) {
                        Cards_Revisited.remove(name);
                        Cards_InProgess.add(name);
                    } else {
                        Cards_Revisited.remove(name);
                        Cards_Done.add(name);
                    }
                    break;
                }
            default:
                throw new IllegalArgumentException("Movimento non riconosciuto");
        }
    }

    //restituisce una lista con i nomi di tutte le Card
    public List<String> getCardsList() {
        List<String> lst = new ArrayList<>();
        lst.addAll(Cards_TODO);
        lst.addAll(Cards_InProgess);
        lst.addAll(Cards_Revisited);
        lst.addAll(Cards_Done);
        return lst;
    }

    public List<Card> getCards() {
        return Cards;
    }

    public boolean isDone() {
        return Cards_Done.size() == Cards.size();
    }

    public List<String> getCardHistory(String name) {
        return getCardByName(name).getCardHistory();
    }

    public List<String> getCardInfo(String name) {
        return getCardByName(name).getInfo();
    }

    public List<String> getMembers() {
        return this.members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }
}
