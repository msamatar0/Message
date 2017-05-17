import java.io.IOException;
import java.util.TreeMap;

/**
 * Note you need to implement a correct send, and receive
 * this is geared for fake data.
 *
 * @author Professor Tom DeDonno
 */
public abstract class AbstractMessage{
    
    boolean debugMode = true;
    /**
     * <table>
     * <caption>Various Testing Modes for AbstractMessage/ MessageXML.java </caption>
     * <tr><td>fakeDatabase</td><td>fakeClient</td><td>comments</td></tr>
     * <tr><td>true</td><td>true</td>
     * <td>both client and database are both fake, will be using mutliMap data for both, 
     *  but you need to manually set the server/gameThread gameTable fields, working with Minnie</td></tr>
     * <tr><td>true</td><td>false</td>
     * <td>using fake Database data, but the client is live, client needs to set its response and read server requests, working with name Minnie</td></tr>
     * <tr><td>false</td><td>true</td><td>Not tested database is real, player is fake, need to set name to Minnie</td></tr> 
     * <tr><td>false</td><td>false</td><td>works okay but you need a working client, clients name must be in database, consult 
     * <a href="http://cim.saddleback.edu:8080/jstudent0/casino.jsp">casino interface</a> in general database has first name of every student</td></tr>
     * 
     * </table>
    */
    protected boolean fakeDatabase =  false;  
    protected boolean fakeClient = false;
    /**
     * set fake name whenever you have a fake client, not Minnie is only one supported in all current modes except live mode,
     * live mode is when both client and database are live */
    protected String fakeName = "Minnie"; //(String)null; //need to set fakeName with fakeClient for startGameTable.
    /**
     * send doesn't need to be a thread, but you should be able to catch
     * IOException if socket has been closed.
     * @param msg to send to  player 
     * @param p player to receive message
     * @throws IOException on errors
     * @return  true
     * 
     */
    public boolean send( String msg, GamePlayer p ) throws IOException {
        
        if( debugMode ) {
            System.out.println("Sending message to " + p );
            System.out.println( msg );
        }
        if( ! fakeClient ) { p.out.writeUTF( msg ); p.out.flush( ); }
        return true;
    }
    
    /**
     * you have lots of options on writing this, it should be a separate thread,
     * it should be a threadpool, you can synchronize the xml message and implement,
     * notify and wait. YOu can consider a conditional wait, await. You can consider
     * non blocking IO.
     * send and receive make sense so we can easily move to UDP datagram socket
     * or a WebSocket.
     * 
     * @param p player to read message from
     * @throws IOException on Socket readUTF error
     * @return the XML string read from client
     */
    protected String receive( GamePlayer p ) throws IOException {
       return (fakeClient ? "hello" : p.in.readUTF());

    }
    
    /**
     * need to register gameTable with casino database bank,
     * set GameTable to correct IP, port, decks and gameName 
     * Casino database will respond with correct gameTableID, and gameID.
     * StartGameTable will set gameTableID and gameID
     * @param t gameTable 
     * @return true on success
     * @throws IOException on errors
     */
    public abstract boolean startGameTable( GameTable t ) throws IOException;  
    public abstract boolean stopGameTable( GameTable t ) throws IOException;
       
    /**
     * Start new game player, must set player name,
     * casino database returns with credit, playerID and gamePlayerID
     * @param t current gameTable
     * @param p player being added to game Table t
     * @throws IOException on socket error
     * @return true on okay
     */
    public abstract boolean startGamePlayer( GameTable t, GamePlayer p ) throws IOException; 
    public abstract boolean stopGamePlayer( GamePlayer p ) throws IOException;
    
    /**
     * Once gameThread has finalized outcome of previous game, 
     * gameThread needs to call newRoundMessage, to create a new XML document with 
     * the current table state relative last game, new players should have lastRound set to firstTime
     * @param t gameTable description
     * @param table entire table of all players, note we need to construct an initial message of state of entire table
     * @throws IOException on socket I/O error
     * @return xml file contents
     */
    public abstract String newRoundMessages( GameTable t, TreeMap<Integer,GamePlayer> table  ) throws IOException;
    
    /**
     * Send newRound message to gamePlayer p, method will set p.response to bet or quit,
     * it will also return bet or quit.
     * @param t gameTable
     * @param p player to send message to
     * @return player response bet or quit
     * @throws IOException for bad client response - may also throw nullpointerexception when response is missing attributes.
     */
    public abstract String newRound( GameTable t, GamePlayer p ) throws IOException;
    /** 
     * once dealer has dealt two cards to each player, we need to update the XML document, with the 
     * new cards, this routine will do that, the first dealers cards is set to 0 in the XML document
     * dealers's face down card is made visible in newRound. we don't send this message to the players. 
     * We wait until  nextCard to send this message one at time to each player.
     * @param t gametable
     * @param table table of players
     * @return return updated xml message
     * @throws IOException on errors
     */
    public abstract String dealCardMessages( GameTable t, TreeMap<Integer,GamePlayer> table ) throws IOException;
    
    /**
     * each player gets the option to hit, stand or doubleDown split; split
     * and downDown options are up to game table. But remember we use gamePlayerIDs to identify
     * players to adding a 2nd player is not that hard.
     * @param t gameTable
     * @param p player that will response to nextCard request.
     * @throws IOException on socket write error
     * @return the player response string p.response
     */
    public abstract String nextCard( GameTable t, GamePlayer p  ) throws IOException;
    
    /**
     * string return is for testing, no response from player when bust is sent.
     * @param t active game table
     * @param p game player to sent bust to
     * @return not important just for testing
     * @throws IOException on errors
     */
    protected abstract String bust( GameTable t, GamePlayer p ) throws IOException;
}
