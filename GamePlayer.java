
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


/**
 *
 * with all forms of communication you read and write values out of an object,
 * XML will read and set this data in MessageXML. However its up to gameTable
 * thread to set this data correctly during the game. Note java has lots of ways
 * to share objects, 
 * @author Professor Tom DeDonno
 */
public class GamePlayer{
    
 
           Socket socket;
           DataInputStream in;    //getInputStream may be all we need
           DataOutputStream out;  //PrintWriter doesn't throw exception
           String name; //Player name using same variable names as Database
           String gameName;
           int playerID; //database playerID
           int gamePlayerID; //primary key for table:gamePlayer can have same player twice
           int bet;     //current players bet
           String response = null;  //response from game table's request, is readOnly on gameTable
           //can be bet (make sure you set bet), quit, hit, stand, doubleDown
           //on request bust don't send a response.
           String xml; //pointer to last xml string client sent
           int credit; //database credit amount
           int netGain;  //winnings  or losses so far in game
           String lastRound = "none"; //won loss tie or none
           int won, loss, tie; 
           //can  implement 5 or 7 or 8  card charlie, war is 3 poker 5
           int iCard; //index of last card in current hand
           int[] cards = new int[8]; 
           //face down cards are sent as zero, stored as negative values
           //You could send all cards as a 64bit long 6 bits for each card
           int handValue; //best hand Value

           
           public String toString(){
               return "GamePlayerID: " + gamePlayerID + " PlayerID: " + playerID + " bet: " + bet + " response: " + response;
           }
           
           /**
            * just set up socket, and IO RW classes, rest of object is set by XMLMessage:startGamePlayer
            * @param c client-server socket connection
            */
           public GamePlayer(Socket c){
               try{
                   this.socket = c;
                   out = new DataOutputStream(c.getOutputStream());
                   in = new DataInputStream(c.getInputStream());
               }catch(IOException ex){
                   System.err.println("GamePlayer Socket IO creation failed: " + ex);
               }
           }
           
           public GamePlayer(String name, String gameName){
               this.name = name; this.gameName = gameName; //server does maintain game for future expansion
           }
} //end GamePlayer
