import Exceptions.CardNotFoundException;
import Exceptions.IllegalMoveException;
import Utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class Project {

    private final String NAME;
    private final List<Card> cards;

    private final List<String> cards_TODO;
    private final List<String> cards_InProgess;
    private final List<String> cards_Revisited;
    private final List<String> cards_Done;

    private List<String> members;
    private final String IP_Multicast;

    public Project(String name) {
        this.NAME = name;
        this.cards = new ArrayList<>();
        this.cards_TODO = new ArrayList<>();
        this.cards_InProgess = new ArrayList<>();
        this.cards_Revisited = new ArrayList<>();
        this.cards_Done = new ArrayList<>();
        this.IP_Multicast = Utils.randomMutlticastIpv4();
        this.members = new ArrayList<>();
    }


    public String getName() {
        return this.NAME;
    }

    //Aggiunge un Nuovo membro al progetto. Se gia presente lancia una eccezione
    public void addMember(String user) {
        if (this.members.contains(user))
            throw new IllegalArgumentException("Utente già presente");
        members.add(user);
    }

    public boolean isMember(String User) {
        return members.contains(User);
    }

    public void createCard(String name, String description) {
        for (Card c : cards) {
            if (c.getName().equals(name)) throw new IllegalArgumentException();
        }

        addCard(new Card(name, description));
    }

    //Crea una nuova Card e la inserisce, se il nome non è gia usato da un'altra Card
    public void addCard(Card card) {
        for (Card c : cards) {
            if (c.getName().equals(card.getName())) throw new IllegalArgumentException("Nome card già esistente");
        }

        cards.add(card);
        switch (card.getState()) {
            case "TODO":
                cards_TODO.add(card.getName());
                break;
            case "INPROGRESS":
                cards_InProgess.add(card.getName());
                break;
            case "TOBEREVISED":
                cards_Revisited.add(card.getName());
                break;
            case "DONE":
                cards_Done.add(card.getName());
                break;
        }
    }

    public Card getCardByName(String name) throws CardNotFoundException {
        for (Card card : cards) {
            if (card.getName().equals(name))
                return card;
        }

        throw new CardNotFoundException();
    }

    //Muove gli stati di una Card
    public void moveCard(String name, String oldState, String newState) throws CardNotFoundException, IllegalArgumentException, IllegalMoveException {
        Card card = getCardByName(name);

        if (!card.getState().equals(oldState.toUpperCase()))
            throw new IllegalArgumentException("Lo stato di partenza non è quello indicato");

        switch (oldState.toUpperCase()) {
            case "TODO":
                if (!newState.equalsIgnoreCase("INPROGRESS")) {
                    throw new IllegalMoveException(oldState, newState);
                } else {
                    card.changeStatus("INPROGRESS");
                    cards_TODO.remove(name);
                    cards_InProgess.add(name);
                    break;
                }
            case "INPROGRESS":
                if (!newState.equalsIgnoreCase("TOBEREVISED") && !newState.equalsIgnoreCase("DONE")) {
                    throw new IllegalMoveException(oldState, newState);
                } else {
                    card.changeStatus(newState.toUpperCase());
                    if (newState.equalsIgnoreCase("TOBEREVISED")) {
                        cards_InProgess.remove(name);
                        cards_Revisited.add(name);
                    } else {
                        cards_InProgess.remove(name);
                        cards_Done.add(name);
                    }
                    break;
                }
            case "TOBEREVISED":
                if (!newState.equalsIgnoreCase("INPROGRESS") && !newState.equalsIgnoreCase("DONE")) {
                    throw new IllegalMoveException(oldState, newState);
                } else {
                    card.changeStatus(newState.toUpperCase());
                    if (newState.equalsIgnoreCase("INPROGRESS")) {
                        cards_Revisited.remove(name);
                        cards_InProgess.add(name);
                    } else {
                        cards_Revisited.remove(name);
                        cards_Done.add(name);
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
        for (Card card : cards) {
            lst.add(card.getName() + " " + card.getState());
        }
        return lst;
    }

    public List<Card> getCards() {
        return cards;
    }

    public boolean isDone() {
        return cards_Done.size() == cards.size();
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

    public String getIP_Multicast() {
        return IP_Multicast;
    }
}
