/**
 * Field names match the field names of the table casino2:gameTable,
 * 
 * this just adds three fields decks, newShuffle and request,
 * decks is the number of decks in the shoe
 * newShuffle is set true when shoe is shuffled, 
 * request is the current dealer request
 *
 * @author Tom DeDonno
 */
public class GameTable{
    
    int gameTableID; //set by API call startGameTable
    int gameID; //set by API call startGameTable
    String IP; //match upper case of database
    int port;
    int chairs;
    String name = "blackjack21"; //name of game at tables  match database
    int decks; //number of decks in shoe
    boolean newShuffle; //only set on newRound bet quit
    String request; //game table request 
    //of the form   bet quit, hit stand, hit stand doubleDown, hit stand doubleDown split, bust 
    
    public String toString(){
        return this.getClass().getCanonicalName() + " gameTableID: " + gameTableID 
                + "gameID: " + gameID + " IP: " + IP + " name: " + name + " newShuffle: " + newShuffle
                + "request: " + request;
    }
}
