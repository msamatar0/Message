import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * <strong>Two Updates</strong>
 * <ol>
 * <li> xmlFile is now built into player, so no longer passing xmlFile as
 * argument</li>
 * <li>once game player sends name we send an xml message back to just that game
 * player with just one player node, the player node will have the gamePlayerID
 * for that client.</li>
 * <li>added gameName to gamePlayer, for possibility of one server starting up
 * multiple games</li>
 * </ol>
 *
 * XML Communication Protocol between server (gameTable) and client (gamePlayer)
 * and also to the database casino web service. XML schema follows the casino
 * database schema. All gameThread server message is written. However, blackjack
 * client will need to parse the incoming XML message set response and return
 * the XML code. See sample XPATH in parseClientResonse.
 * <h2>Basic communication order...</h2>
 * <ol>
 * <li>GameTable sends startGameTable to casino to register game, it gets gameID
 * and gameTableID, it has not players at first, but central location will keep
 * track of all available gameTables. You can set fakeDatabase=true, in
 * AbstractMessaage if you do use the player name Minnie,</li>
 * <li>Server socket, waits for client connection, the server socket and
 * gameThread is a standard producer consumer problem, using a thread safe data
 * structure server write new socket connection, gameThread reads them.
 * <li>Client sends name to GameTable thread</li>
 * <li>GameTable responds back to just that with XML file with player
 * gamePlayerID</li>
 * <li>GameTable sends dealerTeamName, all dealers start with keyword dealer, to
 * casino database.</li>
 * <li>GameTable sends startGamePlayer with new player to get playerID,
 * gamePlayerID and credit</li>
 * <li>GameTable card rounds with players
 * <ol>
 * <li>GameTable calls newRoundMessages to set up new round message, need to
 * fill message with results from previous round</li>
 * <li>GameTable sends newRound game state to each player, players xml response
 * attribute bet or quit, with bet set bet</li>
 * <li>GameTable calls dealCardMessages to set new card values in the XML
 * Message. Note newRoundMessages uses previous cards, this is the new hand</li>
 * <li>GameTable goes thru dealCard hit-stand-bust-split-doubleDown with each
 * player one at a time, its up to gameTable to support split, doubleDown</li>
 * <li>GameTable sends bust if player has busted.
 * <li>GameTable updates handValues, lastRound results, and goes back to
 * newRound</li>
 * </ol></li>
 * <li>When player quits GameTable sends stopGamePlayer to casino database with
 * netGain, won, loss and tie</li>
 * <li>When tables closes up GameTable sends stopGameTable to casino
 * database</li>
 * </ol>
 * <p>
 * Refer to <strong>main method code for sample run.</strong> Its just a
 * standard format</p>
 * <pre>
 * <strong>xml messages</strong>
 * Client first sends &lt; casino  &gt;
 *                    &lt; players  &gt;
 *                    &lt; player name="playersname" gameName="blackjack21" / &gt;
 *                    &lt;/ players &gt;
 *                    &lt;/ casino  &gt;
 *
 * GameTable contacts casino database startGamePlayer, and gets credit, playerID and gamePlayerID
 * player must set attributes gamePlayerID, bet and response in all responses to server, after initial response.
 * Note casino schema supports both casino players player and also
 * casino gameTables gameTable players player, player <strong>so client can use either long or short schema</strong>.
 *
 * For consistency, we will be nesting all players and  gameTables with
 * players and gameTables respectively.
 *
 * For simplicity, we will be using attributes rather than nested nodes.
 * Note we will be using attributes here to avoid deep trees,
 * Nota Bene XML Attributes is actually very similar to JSON,
 * but most JSON users are not aware you can use XML attributes
 * we could also send log file changes, and decode cards as just a long
 * but instead we will fully describe game state on each message.
 *
 * game play consists of the loop, newRound, hit-stand-doubleDown-bust, newRound
 * Server responds with newRound Game State...
 *                      &lt; casino  &gt;
 *                      &lt; gameTables  &gt;
 *                      &lt; gameTable request="bet or quit"
 *                        gameTableID="n" gameName="blackjack21" chairs="x" newShuffle="true|false only on opening round, false otherwise" decks="n"  &gt;
 *                      &lt; players   &gt; or &lt; players / &gt; for no players in game yet, but you should send dealer right away
 *                      .... all players in games
 *                      &lt; player name="playername" playerID="playerID" bet="?" lastRound="won|loss|tie|firstTime is set to firstTime when player doesn't have previous round"
 *                               gameTablePlayerID="unique value, can support splits or same player in table twice"
 *                              credit="n" won="x" loss="x" tie="x" netGain="x"
 *                              cards="2 3 5" handValue="10 of last round"
 *                              response="bet or quit, hit, stand, doubleDown, split no response to bust request, only send what you support" / &gt;
 *
 *                      &lt; player name="dealerTeamName" ... cards="3 5 note dealers first card is face down 0, will be visible at newRound" / &gt;
 *                      &lt;/ players   &gt;
 *                      &lt;/ gameTable  &gt;
 *                      &lt;/gameTables  &gt;
 *                      &lt;/ casino  &gt;
 *
 *
 * Client may respond with same packet or a simiplired  casino players player with just one player himselft
 * remember client needs to fill in bet on new round, and always response
 *
 * server sends hit-stand-doubleDown-bust one at a time til player response with stand, doubleDown or if bust occurs
 *
 * deal all cards face up... (first round) same packets as before just request, newShuffle and cards are updated
 *                      &lt; casino  &gt;
 *                      &lt; gameTables  &gt;
 *                      &lt; gameTable request="hit stand or doubleDown doubleDown should only appear first round, rest of message same as above, except newShuffle is always false"
 *                       gameTableID="n" gameName="blackjack21" chairs="x" newShuffle="false" decks="n" / &gt;
 *
 *                      &lt; players  &gt;
 *                      &lt; player name="playername" id="playerID" bet="n" lastRound="won|loss|tie"
 *                              credit="n" won="x" loss="x" tie="x" netGain="x" response="hit stand or doubleDown or if supported split"
 *                              cards"4 13" handValue="has value of previous round" / &gt;
 *
 *                      .... all players in games
 *                      &lt; player name="dealer" ... cards="0 5 not dealers first card is face down, will be visible at newRound" / &gt;
 *                      &lt;/ players  &gt;
 *                      &lt;/ gameTables  &gt;
 *                      &lt;/ casino  &gt;
 *
 * If player wants a third card, only update request="hit stand", cards="2 3 4"
 *
 * If Player busts send  request="bust" cards="7 7 13" client doesn't send response on bust just wait for newRound
 *
 * when player quits, GameTable sends stopGamePlayer to casino database with netGain, won, loss and tie results
 *
 * when table closes up it sends stopGameTable to casino database.
 * </pre>
 *
 * @author Professor Tom DeDonno
 * @version Copyright 5/5/2017
 */
public class MessageXML extends AbstractMessage{

    /**
     * can re-use these for all calls to create doc
     */
    private final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private DocumentBuilder documentBuilder;

    XPathFactory xPathfactory = XPathFactory.newInstance();
    XPath xpath = xPathfactory.newXPath();

    //newRoundMessage recreates doc, all others update the doc message
    private Document doc;   //active doc
    private String xmlFile; //xml String created by doc

    /* Will create a multiMap class instead of TreeMap<k,Collection<v>>
    need a mutlimap of fake messages database and client are not operational
     */
    MultiMap fakeData = new MultiMap();

    /**
     * setup message once you have dealt the cards,
     *
     * @param t gameTable
     * @param table table of players, dealerTeamName is a player
     * @return return xmlString but this.doc is set by this method
     * @throws IOException on error
     */
    @Override
    public String dealCardMessages(GameTable t, TreeMap<Integer, GamePlayer> table) throws IOException{

        NamedNodeMap request = doc.getElementsByTagName("gameTable").item(0).getAttributes();
        request.getNamedItem("request").setNodeValue(t.request);
        request.getNamedItem("newShuffle").setNodeValue(Boolean.toString(t.newShuffle));

        //just rewriting the entire message each time is looking good 
        // NamedNodeMap nodes  = // doc.getElementsByTagName("player"); //.item(0).getAttributes();
        NodeList nodeList = doc.getElementsByTagName("player");
        XPathFactory xpFactory = XPathFactory.newInstance();
        XPath xp = xpFactory.newXPath();
        try{
            for(Map.Entry<Integer, GamePlayer> entry : table.entrySet()){
                //Integer k = entry.getKey();
                GamePlayer p = entry.getValue();
                XPathExpression expr = xp.compile("//player[@gamePlayerID='" + p.gamePlayerID + "']");
                //"/casino/gameTables/gameTable/players/player[@gamePlayerID='"+p.gamePlayerID+"']");
                NodeList node = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                NamedNodeMap n = node.item(0).getAttributes(); //only one here
                StringBuilder str = new StringBuilder();

                int i;
                //all dealers are dealerTeamName, set first card to 0 face down for dealer
                if(n.getNamedItem("name").getNodeValue().indexOf("dealer") == 0){
                    i = 1;
                    str.append("0 ");
                } else{
                    i = 0;
                }

                for(; i < p.iCard; ++i){
                    str.append(p.cards[i]).append(" ");
                }

                n.getNamedItem("cards").setNodeValue(str.toString());
                n.getNamedItem("bet").setNodeValue(p.bet + "");
            } //end for
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            StringWriter out = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(out));
            return xmlFile = out.toString();
        }catch(XPathExpressionException | TransformerException ex){
            Logger.getLogger(MessageXML.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "error";
    }

    /**
     * used for keeping track of fake data, just a standard multimap, Google
     * Guava and apache common collections has them but this is simple to write
     */
    private class MultiMap{
        TreeMap< String, ArrayList<String>> map = new TreeMap<>();
        
        public String put(String key, String value){
            ArrayList<String> a;
            if(map.containsKey(key)){
                a = map.get(key);
                a.add(value);
            }else{
                a = new ArrayList<>();
                a.add(value);
                map.put(key, a);
            }
            return value;
        } //end put

        public ArrayList<String> get(String key){
            return map.get(key);
        }

        public String get(String key, String contains){
            ArrayList<String> a = map.get(key);
            for(int i = 0; i < a.size(); ++i)
                if(a.get(i).contains(contains))
                    return a.get(i);
            return null;
        }
    } //end MultiMap

    public MessageXML(){
        try{
            documentBuilder = factory.newDocumentBuilder();
        }catch(ParserConfigurationException ex){
            Logger.getLogger(MessageXML.class.getName()).log(Level.SEVERE, null, ex);
        }

        /* nota bene client can send either /casino/gameTables or /casino/players /casino/players not test yet */
        fakeData.put("newRound", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><casino><gameTables><gameTable chairs=\"2\" decks=\"6\" gameTableID=\"2082\" name=\"blackjack21\" newShuffle=\"true\" request=\"bet quit\"><players><player bet=\"15\" cards=\"\" credit=\"10000\" gamePlayerID=\"21\" lastRound=\"firstTime\" loss=\"0\" name=\"guestJoe\" netGain=\"0\" playerID=\"3\" response=\"bet\" tie=\"0\" won=\"0\"/><player bet=\"0\" cards=\"\" credit=\"20023\" gamePlayerID=\"20\" lastRound=\"firstTime\" loss=\"0\" name=\"dealerJoe\" netGain=\"0\" playerID=\"2\" response=\"\" tie=\"0\" won=\"0\"/></players></gameTable></gameTables></casino>\n");
        /* using cards to identify nextCard client response cards 1 13 */
        fakeData.put("nextCard", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><casino><gameTables><gameTable chairs=\"2\" decks=\"6\" gameTableID=\"2082\" name=\"blackjack21\" newShuffle=\"false\" request=\"hit stand quit\"><players><player bet=\"xx\" cards=\"1 13 \" credit=\"10000\" gamePlayerID=\"21\" lastRound=\"firstTime\" loss=\"0\" name=\"guestJoe\" netGain=\"0\" playerID=\"3\" response=\"hit\" tie=\"0\" won=\"0\"/><player bet=\"0\" cards=\"0 10 \" credit=\"20023\" gamePlayerID=\"20\" lastRound=\"firstTime\" loss=\"0\" name=\"dealerJoe\" netGain=\"0\" playerID=\"2\" response=\"\" tie=\"0\" won=\"0\"/></players></gameTable></gameTables></casino>\n");
        /* cards 1 13 5 */
        fakeData.put("nextCard", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><casino><gameTables><gameTable chairs=\"2\" decks=\"6\" gameTableID=\"2082\" name=\"blackjack21\" newShuffle=\"false\" request=\"hit stand\"><players><player bet=\"xx\" cards=\"1 13 5 \" credit=\"10000\" gamePlayerID=\"21\" lastRound=\"firstTime\" loss=\"0\" name=\"guestJoe\" netGain=\"0\" playerID=\"3\" response=\"stand\" tie=\"0\" won=\"0\"/><player bet=\"0\" cards=\"0 10 \" credit=\"20023\" gamePlayerID=\"20\" lastRound=\"firstTime\" loss=\"0\" name=\"dealerJoe\" netGain=\"0\" playerID=\"2\" response=\"\" tie=\"0\" won=\"0\"/></players></gameTable></gameTables></casino>\n");

        /**
         * need to mimic the players first message, only four player names are
         * supported,
         */
        fakeData.put("firstMessage", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><casino><players><player name=\"guestJoe\" /></players></casino>");
        fakeData.put("firstMessage", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><casino><players><player name=\"Minnie\" /></players></casino>");
        fakeData.put("firstMessage", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><casino><players><player name=\"dealerJoe\" /></players></casino>");
        fakeData.put("firstMessage", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><casino><gameTables><gameTable><players><player name=\"BJSWAgent\"/></players></gameTable></gameTables></casino>");

        //stopGameTable will summarize
        fakeData.put("startGameTable", "<casino><gameTables><gameTable IP=\"localhost\" casinoID=\"2\" chairs=\"3\" endTime=\"null\" gameID=\"1\" gameTableID=\"2082\" port=\"2\" startTime=\"2017-05-06 12:18:18.0\"><players><player clientIP=\"127.0.0.1\" endTime=\"null\" gamePlayerTableID=\"34\" gameTableID=\"2082\" loss=\"null\" netGain=\"null\" playerID=\"3\" startTime=\"2017-05-06 19:14:35.0\" tie=\"null\" won=\"null\"/></players></gameTable></gameTables></casino>");
        //stopGameTable will summarize entire state of table, all players should have endTimes, casino database doesn't handle dangling players, its up to the gameTable to close all players first
        fakeData.put("stopGameTable", "<casino><gameTables><gameTable IP=\"localhost\" casinoID=\"2\" chairs=\"3\" endTime=\"2017-05-06 19:42:22.0\" gameID=\"1\" gameTableID=\"2082\" port=\"2\" startTime=\"2017-05-06 12:18:18.0\"><players><player clientIP=\"127.0.0.1\" endTime=\"2017-05-06 19:41:17.0\" gamePlayerID=\"34\" gameTableID=\"2082\" loss=\"2\" netGain=\"43.0\" playerID=\"3\" startTime=\"2017-05-06 19:14:35.0\" tie=\"3\" won=\"2\"/><player clientIP=\"127.0.0.1\" endTime=\"2017-05-06 19:38:12.0\" gamePlayerID=\"35\" gameTableID=\"2082\" loss=\"2\" netGain=\"43.0\" playerID=\"2\" startTime=\"2017-05-06 19:35:37.0\" tie=\"3\" won=\"2\"/></players></gameTable></gameTables></casino>");
        fakeData.put("stopGamePlayer", "<casino><gamePlayers><gamePlayer clientIP=\"127.0.0.1\" endTime=\"2017-05-06 19:41:17.0\" gamePlayerID=\"34\" gameTableID=\"2082\" loss=\"2\" netGain=\"43.0\" playerID=\"3\" startTime=\"2017-05-06 19:14:35.0\" tie=\"3\" won=\"2\"/></gamePlayers></casino>");
        fakeData.put("stopGamePlayer", "<casino><noTables><noTable Error=\"2082gameTableID does not have playerID:60\"/></noTables></casino>");

        /**
         * using playerName to identifier player all players must have a player
         * name in this list the gamePlayerID changes each time you access
         * server but its set in these file.
         */
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"32956\" email=\"mmouse@disney.com\" name=\"Minnie\" password=\"48714ab0eaee88086285c5a5e977171c\" playerID=\"1\" gamePlayerID=\"19\" /></players></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"20023\" email=\"jstudent0@cim.saddleback.edu\" gamePlayerID=\"20\" name=\"dealerJoe\" password=\"3cbe3eb3e0164476ae4a120975960606\" playerID=\"2\"/></players></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"10000\" email=\"unknown\" gamePlayerID=\"21\" name=\"guestJoe\" password=\"860c0e9bf88237a91725d9b10fea45e1\" playerID=\"3\"/></players></casino>\n");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"9960\" email=\"agent@sb.edu\" gamePlayerID=\"22\" name=\"BJSWAgent\" password=\"0636550122b85be3a7cd676bfaef7166\" playerID=\"5\"/></players></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"-500\" email=\"jloser@saddleback.edu\" gamePlayerID=\"23\" name=\"Joe Loser\" password=\"3fdd873f61dce6c7a4c9f509fc0ab828\" playerID=\"7\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"dumlang0@socccd.edu\" gamePlayerID=\"24\" name=\"Drew\" password=\"8b66041e89518c5ec27f4760673e4aba\" playerID=\"60\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"ptang2@socccd.edu\" gamePlayerID=\"25\" name=\"Peter\" password=\"237c13a2c98598b01973d48d0ce17565\" playerID=\"59\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"ksemenov0@socccd.edu\" gamePlayerID=\"26\" name=\"Kevin\" password=\"c02d3a417e298db8b534c9ac1fdd33e7\" playerID=\"58\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"msamatar0@socccd.edu\" gamePlayerID=\"27\" name=\"Mohamed\" password=\"b2a061540c16ae3c957955fb4dd5563d\" playerID=\"57\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"cross21@socccd.edu\" gamePlayerID=\"28\" name=\"Chad\" password=\"8e3f8f2ea9dba407cff9bab1573b3814\" playerID=\"56\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"jrauch2@socccd.edu\" gamePlayerID=\"29\" name=\"Jared\" password=\"bcc30fbcdc378cdaac33d3c7350e83fc\" playerID=\"55\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"99990\" email=\"jpollard5@socccd.edu\" gamePlayerID=\"30\" name=\"Jacob\" password=\"922837e9ba3b662f39440a5ebee5c2b7\" playerID=\"54\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"cpeace0@socccd.edu\" gamePlayerID=\"31\" name=\"Connor\" password=\"5eee6e3e5912b1c2f56f9b408eeaaeae\" playerID=\"53\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"bmontes5@socccd.edu\" gamePlayerID=\"32\" name=\"Brandon\" password=\"0f5c6ce63303bf2a28687291f89dff89\" playerID=\"52\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"jmciver1@socccd.edu\" gamePlayerID=\"33\" name=\"Jacob\" password=\"b788ae7ec5c4f7e6578d8d770bd66d98\" playerID=\"51\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"jlyle2@socccd.edu\" gamePlayerID=\"34\" name=\"Jonathan\" password=\"8e07a9c21a077d2bf94ce87cd5c0b666\" playerID=\"50\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"jkahn3@socccd.edu\" gamePlayerID=\"35\" name=\"Jason\" password=\"1a1fb6705d9b1a6782f6f9e8430683d4\" playerID=\"49\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"mjohnson144@socccd.edu\" gamePlayerID=\"36\" name=\"Matthew\" password=\"c1036c8891b77ffac5e87100e34d5768\" playerID=\"48\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"sheydarimolaei0@socccd.edu\" gamePlayerID=\"37\" name=\"Sina\" password=\"eb5a405124dd3dc68451f7c1a6de9d5c\" playerID=\"47\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"whendley0@socccd.edu\" gamePlayerID=\"38\" name=\"Wyatt\" password=\"066796a4381b0baa89b00fa3b7a4bdff\" playerID=\"46\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"mharris51@socccd.edu\" gamePlayerID=\"39\" name=\"Mariah\" password=\"a4fcd5e48e9a0cecd4e94701917ce5e4\" playerID=\"45\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"rgrover1@socccd.edu\" gamePlayerID=\"40\" name=\"Randy\" password=\"5613561f2810484015dcddb7f1bcf2e0\" playerID=\"44\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"ddesiatco0@socccd.edu\" gamePlayerID=\"41\" name=\"Daniel\" password=\"db6871d06896f48aea53130611d4cee9\" playerID=\"43\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"cdelprato0@socccd.edu\" gamePlayerID=\"42\" name=\"Prato\" password=\"95f3aba0634648c9bb250a5fa8c7d987\" playerID=\"42\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"nconnors0@socccd.edu\" gamePlayerID=\"43\" name=\"Noah\" password=\"bd4cc689638fb5c82bbd14d8f3cf3336\" playerID=\"41\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"fchahla0@socccd.edu\" gamePlayerID=\"44\" name=\"Farid\" password=\"1c45403fabb0c2d5f74ea3d13e9d5fe7\" playerID=\"40\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"jcamargo1@socccd.edu\" gamePlayerID=\"45\" name=\"Jose\" password=\"d23750deed4cd9d4607dedd077cf62e3\" playerID=\"39\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"nagudelo1@socccd.edu\" gamePlayerID=\"46\" name=\"Noah\" password=\"1f194025e0144e731b61e51ddca648c8\" playerID=\"38\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"acabrejos0@socccd.edu\" gamePlayerID=\"47\" name=\"Augusto\" password=\"e3068e8a380abd41c6786b7bf0b0157f\" playerID=\"37\"/></player></casino>");
        fakeData.put("startGamePlayer", "<casino><players><player credit=\"100000\" email=\"jstudent0@socccd.edu\" gamePlayerID=\"48\" name=\"Student\" password=\"e705f55efbd3ad1561abfc7b546643f3\" playerID=\"61\"/></player></casino>");
    }

    /**
     * for fakeClient we need to append the player info for firstMessage,
     * nextCard, newRound Nota Bene needs to be upgraded, so that any player
     * name in key startGamePlayer is supported, just need to modify some of the
     * exiting mutliMap values.
     *
     * @param url
     * @return
     */
    private String fakeClient(String url){

        //System.out.println("======\nInside FakeData:" + url );
        TreeMap<String, String> args = new TreeMap<>();
        try{
            String[] variables = URLDecoder.decode(url, "UTF-8").split("\\?")[1].split("&");

            for(int i = 0; i < variables.length; ++i){
                //System.out.println( "variables: " + variables[i] );
                String[] a = variables[i].split("=");
                args.put(a[0], a[1]);
            }
        }catch(UnsupportedEncodingException ex){
            Logger.getLogger(MessageXML.class.getName()).log(Level.SEVERE, null, ex);
        }

        ArrayList<String> a = fakeData.get(args.get("method"));
        String XML = a.get(0);

        String player
                = args.get("playerName") != null ? args.get("playerName") : args.get("playerID");
        if(player != null){
            int i;
            for(i = 0; i < a.size() && !a.get(i).contains(player); ++i){}
            if(i < a.size()){
                XML = a.get(i);
            }
        }

        //System.out.println("XML file Before: " + xmlFile );
        for(Map.Entry<String, String> entry : args.entrySet()){
            String k = entry.getKey();
            String v = entry.getValue();
            int j = XML.indexOf(k + "=\"");
            if(j > 0){
                j += k.length() + 2;
                //System.out.print("j=" + j );
                String temp = XML.substring(0, j) + v;
                int j1 = XML.indexOf("\"", j);
                //System.out.println("j1=" + j1 + " " + temp );
                if(j1 > 0){
                    XML = temp + "\" " + XML.substring(j1 + 1);
                }

            }
            // xmlFile = xmlFile.replaceFirst( "^(" + k +"=\".*\" )??", k+"=\""+v+"\" " );    
            //System.out.println("Changed k,v: " + k + ", " + v );
        }
        //System.out.println("XML file after: " +  xmlFile );

        return XML;
    }

    private Document readCasinoDatabase(String urlEncode){

        InputStream is = null;
        String XML = null;
        String dbURL
                = "http://localhost:8084/casino2/casinoAPI.jsp?"; //local testing URL
        //= "http://cim.saddleback.edu:8080/jstudent0/casinoAPI.jsp?"; 
        //joe student wrote a cim URL for us

        URL url;
        Document docx = null;

        try{
            if(fakeDatabase){
                XML = fakeClient(dbURL + urlEncode);
            } else{
                url = new URL(dbURL + urlEncode);
                is = url.openStream();
                Scanner in = new Scanner(new InputStreamReader(is));
                in.useDelimiter("</casino>");
                XML = in.next() + "</casino>";
            }

        }catch(MalformedURLException ex){
            Logger.getLogger(MessageXML.class.getName()).log(Level.SEVERE, null, ex);
        }catch(IOException ex){
            Logger.getLogger(MessageXML.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            try{
                if(is != null){
                    is.close();
                }
            }catch(IOException ex){
                Logger.getLogger(MessageXML.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try{
            DocumentBuilder b = dbFactory.newDocumentBuilder();
            docx = b.parse(new InputSource(new ByteArrayInputStream(XML.getBytes("utf-8"))));
            docx.getDocumentElement().normalize();
            //system should now all other data
        }catch(ParserConfigurationException | SAXException | IOException ex){
            Logger.getLogger(MessageXML.class.getName()).log(Level.SEVERE, null, ex);
        }

        if(debugMode){
            System.out.println("casinoDatabaseOutput:" + XML);
        }
        return docx;
    }

    @Override
    public boolean startGameTable(GameTable t){

        String response = null;
        //System.out.println("Inside startGameTable getting XML" );
        Document docDB = readCasinoDatabase("method=startGameTable&IP=local&gameName=" + t.name);

        /* <casino><gameTables><gameTable  gameTableID=> assume just one */
        Element e = (Element) docDB.getFirstChild().getFirstChild().getFirstChild();

        // System.out.println( e + "," + e.getAttribute("gameTableID") );
        t.gameTableID = Integer.parseInt(e.getAttribute("gameTableID"));
        t.gameID = Integer.parseInt(e.getAttribute("gameID"));
        t.IP = e.getAttribute("IP");
        //nota bene setting  chairs from database 
        //System.out.println("tableID = "  + t.gameTableID  );
        //System.out.println("gameID = "  + t.gameID );
        //System.out.println("IP = "  + t.IP  );
        //system should now all other data

        return true;

    } //end startGameTable

    /**
     * it is the game server's responsibility to stop all game players before
     * calling stopGameTable
     *
     * @param t gameTable to start up, must have correct gameName
     * @return true
     * @throws IOException on read error
     */
    @Override
    public boolean stopGameTable(GameTable t) throws IOException{
        Document docDB = readCasinoDatabase("method=stopGameTable&gameTableID=" + t.gameTableID);

        return true;
    }

    /**
     * need to update netGain, won, loss and ties consult
     * <a href="http://cim.saddleback.edu:8080/jstudent0/casino.jsp">Casino.jsp</a>
     * on what arguments to set.
     *
     * @param p game player to stop, must have gamePlayerID set
     * @return true
     * @throws IOException on read error
     */
    @Override
    public boolean stopGamePlayer(GamePlayer p) throws IOException{

        Document docDB = readCasinoDatabase("method=stopGamePlayer"
                + "&playerID=" + p.playerID + "&gamePlayerID=" + p.gamePlayerID
                + "&won=" + p.won + "&loss=" + p.loss + "&tie=" + p.tie
                + "&netGain=" + p.netGain);

        return true;
    }

    /**
     * start game player p on the given game table, this routine does two things
     * it will first read the player initial response aka casino players player
     * to get name and then calls the database API to get rest of data for
     * gamePlayer
     *
     * @param t table gamePlayer is on
     * @param p gamePlayer to start game with name must be set correctly
     * @return true if okay, false if something went wrong
     * @throws java.io.IOException
     */
    @Override
    public boolean startGamePlayer(GameTable t, GamePlayer p) throws IOException{

        if(fakeClient && fakeName == null){
            throw new IOException("need fakeName when fakeClient is set ");
        }

        p.xml
                = (fakeClient
                        ? fakeData.get("firstMessage", fakeName)
                        : receive(p));

        try{

            // System.out.println( p.xml );
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbFactory.newDocumentBuilder();
            Document docLocal
                    = builder.parse(new InputSource(new StringReader(p.xml)));

            p.name = (String) xpath.compile("//player/@name").evaluate(docLocal, XPathConstants.STRING);

            docLocal = readCasinoDatabase("method=startGamePlayer&gameTableID=" + t.gameTableID
                    + "&playerName=" + p.name + "&clientIP=localMaybe");

            /* <casino><players><player> assume first one */
            Element e = (Element) docLocal.getFirstChild().getFirstChild().getFirstChild();
            p.playerID = Integer.parseInt(e.getAttribute("playerID"));
            p.credit = Integer.parseInt(e.getAttribute("credit"));
            p.gamePlayerID = Integer.parseInt(e.getAttribute("gamePlayerID"));
            System.out.println("GamePlayer: " + p);
            //p.bet comes from player

            //need to send startGame back to Player with only one player in it.
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            StringWriter out = new StringWriter(); //note out could be System.out
            transformer.transform(new DOMSource(docLocal), new StreamResult(out));

            send(out.toString(), p);

        }catch(ParserConfigurationException | SAXException ex){
            Logger.getLogger(MessageXML.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }catch(TransformerConfigurationException ex){
            Logger.getLogger(MessageXML.class.getName()).log(Level.SEVERE, null, ex);
        }catch(TransformerException | XPathExpressionException ex){
            Logger.getLogger(MessageXML.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    /**
     * for
     *
     * @param doc
     * @param nodeName
     * @param value for integer just use i+""
     * @return
     */
    @Deprecated
    private Element setNode(Document doc, String nodeName, String value){
        if(value == null || value.isEmpty()){
            throw new NullPointerException("need a value for nodeName, value:" + nodeName + ", " + value);
        }

        Element n = doc.createElement(nodeName);
        n.appendChild(doc.createTextNode(value));
        return n;
    }

    /**
     * send player message, must contain entire state of the current or previous
     * table
     * <casino><gameTables><gameTable>....</gameTable></gameTables><players><player>...
     * one player is dealer</players></casino>
     *
        not players should be inside of gameTable, but I want a standard DTD,
     * plus we are recording gameTableID
     */
    @Deprecated
    private String playerMessageNodesOnly(GameTable t, TreeMap<Integer, GamePlayer> table){

        try{
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder b = dbFactory.newDocumentBuilder();
            doc = b.newDocument();

            Element root = doc.createElement("casino");

            Element firstChild = doc.createElement("gameTables");
            Element child = doc.createElement("gameTable");
            child.appendChild(setNode(doc, "newShuffle", Boolean.toString(t.newShuffle)));
            child.appendChild(setNode(doc, "name", t.name));
            child.appendChild(setNode(doc, "gameTableID", t.gameTableID + ""));
            //do not need to set the other data only set at newRound
            firstChild.appendChild(child);
            root.appendChild(firstChild);

            System.out.println("====root : " + root + "," + root.getFirstChild());

            //for computer generated code you normally don't add extra white spaces, for testing we need readability
            //root.appendChild( doc.createTextNode("\n" ) );
            //Create XML file and use right Click Format
            /* see blackboard casino discussion on how to iterate a TreeMap with threads */
            firstChild = doc.createElement("players");
            for(Map.Entry<Integer, GamePlayer> entry : table.entrySet()){
                //Integer k = entry.getKey();
                GamePlayer p = entry.getValue();
                child = doc.createElement("player");

                child.appendChild(setNode(doc, "playerID", p.playerID + ""));
                child.appendChild(setNode(doc, "name", p.name));
                child.appendChild(setNode(doc, "credit", p.credit + ""));
                child.appendChild(setNode(doc, "bet", p.bet + ""));
                child.appendChild(setNode(doc, "netGain", p.netGain + ""));
                child.appendChild(setNode(doc, "won", p.won + ""));
                child.appendChild(setNode(doc, "loss", p.loss + ""));
                child.appendChild(setNode(doc, "tie", p.tie + ""));

                StringBuilder str = new StringBuilder();
                for(int i = 0; i < p.iCard; ++i){
                    str.append(p.cards[i]).append(" ");
                }

                child.appendChild(setNode(doc, "cards", str.toString()));

                child.appendChild(setNode(doc, "lastRound", p.lastRound + ""));
                child.appendChild(setNode(doc, "bet", p.bet + ""));

                firstChild.appendChild(child);
            }

            root.appendChild(firstChild);
            doc.appendChild(root);

            System.out.println("=====document: " + doc);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            StringWriter out = new StringWriter(); //note out could be System.out
            transformer.transform(new DOMSource(doc), new StreamResult(out));
            return out.toString();
        }catch(ParserConfigurationException | TransformerException ex){
            Logger.getLogger(MessageXML.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "error"; //but should be throwing an exception
    }

    /**
     * used by nextCard, to update cards, and request for a specific player
     * only, file updated is xmlFile.
     *
     * @param t active gameTable
     * @param p player dealer is communicating with
     * @return updated xmlFile string
     */
    public String updatePlayerMessage(GameTable t, GamePlayer p){

        try{
            /* update gameTableMessage */
            NamedNodeMap request = doc.getElementsByTagName("gameTable").item(0).getAttributes();
            request.getNamedItem("request").setNodeValue(t.request);
            request.getNamedItem("newShuffle").setNodeValue(Boolean.toString(t.newShuffle));

            /* update players cards  */
            XPathFactory xpFactory = XPathFactory.newInstance();
            XPath xp = xpFactory.newXPath();

            XPathExpression expr = xp.compile("//player[@gamePlayerID='" + p.gamePlayerID + "']");
            // "/casino/gameTables/gameTable/players/player[@gamePlayerID='"+p.gamePlayerID+"']");
            NodeList node = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

            //we are using uniqe gamePlayerID so its only one node
            //System.out.println( node.item(0) );
            NamedNodeMap n = node.item(0).getAttributes();

            StringBuilder str = new StringBuilder();
            for(int i = 0; i < p.iCard; ++i){
                str.append(p.cards[i]).append(" ");
            }

            n.getNamedItem("cards").setNodeValue(str.toString());
            //next two added for chatServer
            n.getNamedItem("response").setNodeValue(p.response);
            n.getNamedItem("lastRound").setNodeValue(p.lastRound);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            StringWriter out = new StringWriter(); //note out could be System.out
            transformer.transform(new DOMSource(doc), new StreamResult(out));
            return xmlFile = out.toString();
        }catch(TransformerException | XPathExpressionException ex){
            Logger.getLogger(MessageXML.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "error"; //but should be throwing an exception

    }

    /**
     * just a basic strcmp with extra output. note Google Guava and Apache
     * Commons has lots of strcmp methods but I will write my own
     *
     * @param s1 string s1
     * @param s2 string s2
     * @return true if both are equal
     */
    public boolean strcmp(final String s1, final String s2){

        if(s1.equals(s2)){
            return true;
        }

        int i;
        for(i = 0; i < s1.length(); ++i){
            if(i > s2.length()){
                System.out.println("s1,s2: " + s1.length() + "," + s2.length()
                        + " s2 is shorter at->" + s1.substring(i) + "<-");
                return false;
            }
            if(s1.charAt(i) != s2.charAt(i)){
                System.out.println("Difference at " + s1.substring(i));
                System.out.printf("Character %3d %s\n", i, s2.substring(i));
                return false;
            }
        }

        if(i < s2.length()){
            System.out.println("s1,s2: " + s1.length() + "," + s2.length()
                    + " s1 is shorter at->" + s2.substring(i) + "<-");
            return false;
        }

        return true;
    }

    /**
     * Does a full system test using fakeClient and fakeDatabase. A full set of
     * jUnit tests should still be created. This system test should be moved to
     * a jUnit testing class. In general its always best to do too many unit
     * test, rather than not enough.
     *
     * @param args standard runtime not used
     */
    public static void main(String[] args){

        try{
            MessageXML msg = new MessageXML();
            if(msg.fakeClient == false || msg.fakeDatabase == false){

                System.out.println("You need to set both fakeClient and fakeDatabase to true in Abstract Message to use main system test");
                System.out.println("Manually setting both to true and name to Minnie");
                msg.fakeClient = msg.fakeDatabase = true;
                msg.fakeName = "Minnie";
            }

            GameTable t = new GameTable();
            t.decks = 6;
            t.chairs = 6;
            //make sure you have the correct name, http://cim.saddleback.edu:8080/jstudent0/casino.jsp
            t.name = "blackjack21";
            msg.startGameTable(t);
            TreeMap<Integer, GamePlayer> table = new TreeMap<>();
            GamePlayer p = new GamePlayer("guestJoe", t.name);
            table.put(0, p);
            //note on a socket connection, startGame will read from the socket 
            msg.fakeName = "guestJoe";
            msg.startGamePlayer(t, p);

            //all dealers begin with the keyword name dealer, its generally dealerTeamName
            GamePlayer dealer = new GamePlayer("dealerJoe", t.name);
            table.put(5, dealer);
            msg.fakeName = "dealerJoe";
            msg.startGamePlayer(t, dealer);

            //create message based on previous round, this time no round
            p.lastRound = "firstTime";
            dealer.lastRound = "firstTime";
            p.handValue = -1;
            dealer.handValue = -2;
            //just putting in dummy values for hand value making sure newRoundMesasges adds them
            //may want to set them to default 0

            t.newShuffle = true;
            t.request = "bet quit";
            //but dealers last round will vary if more than one player
            t.chairs = 2; //setting it to number of players at table needed for fake data
            String str = msg.newRoundMessages(t, table);

            if(msg.strcmp(str, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><casino><gameTables><gameTable chairs=\"2\" decks=\"6\" gameTableID=\"2082\" name=\"blackjack21\" newShuffle=\"true\" request=\"bet quit\"><players><player bet=\"0\" cards=\"\" credit=\"10000\" gamePlayerID=\"21\" handValue=\"-1\" lastRound=\"firstTime\" loss=\"0\" name=\"guestJoe\" netGain=\"0\" playerID=\"3\" response=\"\" tie=\"0\" won=\"0\"/><player bet=\"0\" cards=\"\" credit=\"20023\" gamePlayerID=\"20\" handValue=\"-2\" lastRound=\"firstTime\" loss=\"0\" name=\"dealerJoe\" netGain=\"0\" playerID=\"2\" response=\"\" tie=\"0\" won=\"0\"/></players></gameTable></gameTables></casino>")){
                System.out.println("Okay newRoundMessage");
            } else{
                System.out.println("NOT OKAY newRoundMessage========\n" + msg.xmlFile);
            }

            //ask player if they want to bet or quit
            //you need to write the client side parser, will need XPATH send and receive not implemented
            t.request = "bet quit";
            //player will set the next two nodes, 
            p.response = "xxx";
            p.bet = 2;

            String str1 = msg.newRound(t, p);
            if(str1.equals("bet")){
                System.out.println("Okay parseClientResponse newRound working with fakedata chairs=2 response is bet");
            } else{
                System.out.println("Not Okay expected bet but player response: " + str1);
            }

            if(p.bet == 15) //15 is set by xml fakeData for chairs=2
            {
                System.out.println("Okay for bet its 15 now");
            } else{
                System.out.println("Not Okay  bet for player guestJoe: " + p.bet);
            }

            //deal cards
            p.iCard = 2;
            p.cards[0] = 1;
            p.cards[1] = 13;
            dealer.iCard = 2;
            dealer.cards[0] = 20;
            dealer.cards[1] = 10;

            t.newShuffle = false;
            t.request = "hit stand doubleDown";

            str = msg.dealCardMessages(t, table);
            if(msg.strcmp(str,
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><casino><gameTables><gameTable chairs=\"2\" decks=\"6\" gameTableID=\"2082\" "
                    + "name=\"blackjack21\" newShuffle=\"false\" request=\"hit stand doubleDown\"><players><player bet=\"15\" cards=\"1 13 \" credit=\"10000\" "
                    + "gamePlayerID=\"21\" handValue=\"-1\" lastRound=\"firstTime\" loss=\"0\" name=\"guestJoe\" netGain=\"0\" playerID=\"3\" response=\"bet\" tie=\"0\" won=\"0\"/><player bet=\"0\" cards=\"0 10 \" credit=\"20023\" gamePlayerID=\"20\" handValue=\"-2\" lastRound=\"firstTime\" loss=\"0\" name=\"dealerJoe\" netGain=\"0\" playerID=\"2\" response=\"\" tie=\"0\" won=\"0\"/></players></gameTable></gameTables></casino>")){
                System.out.println("okay for dealCardMessage");
            } else{
                System.out.println("not okay dealCardMessage XML file:" + str);
            }

            str = msg.nextCard(t, p);
            if(str.equals("hit")){
                System.out.println("Okay nextCard");
            } else{
                System.out.println("Note Okay, player response: " + str);
            }

            p.iCard = 3;
            p.cards[2] = 5; //king and ace he took a hit must be IVC
            t.request = "hit stand";  //you cannot double down after first round

            str = msg.nextCard(t, p);
            if(str.equals("stand")){
                System.out.println("Okay nextCard");
            } else{
                System.out.println("Note Okay, player response: " + str);
            }

            t.request = "bust"; //kinda hard to test bust but we send message to client don't get response
            //for testing return xmlFile string of what is sent to client
            str = msg.bust(t, p);

            if(str.indexOf("request=\"bust\"") > 0){
                System.out.println("okay bust");
            } else{
                System.out.println("Not Okay bust");
            }

            /* cards at http://cim.saddleback.edu/casino/c/small/default/ */
            p.handValue = 16; //1 13 5
            dealer.handValue = 17;   // 10 20 

            /* gameThread needs to update first round result */
            p.loss++;
            p.lastRound = "loss";
            p.netGain -= p.bet;
            dealer.won++;
            dealer.netGain += p.bet;
            t.request = "bet quit"; //this will vary by game

            //add new player to game
            msg.fakeName = "BJSWAgent";
            p = new GamePlayer(msg.fakeName, t.name);
            table.put(1, p);  //key for first player was 0
            msg.startGamePlayer(t, p);
            p.lastRound = "firstTime";
            t.chairs = 3;
            t.newShuffle = false;

            str = msg.newRoundMessages(t, table);
            if(msg.strcmp(str,
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><casino><gameTables><gameTable chairs=\"3\" decks=\"6\" gameTableID=\"2082\" "
                    + "name=\"blackjack21\" newShuffle=\"false\" request=\"bet quit\"><players><player bet=\"15\" cards=\"1 13 5 \" credit=\"10000\" gamePlayerID=\"21\" "
                    + "handValue=\"16\" lastRound=\"loss\" loss=\"1\" name=\"guestJoe\" netGain=\"-15\" playerID=\"3\" response=\"stand\" tie=\"0\" won=\"0\"/>"
                    + "<player bet=\"0\" cards=\"\" credit=\"9960\" gamePlayerID=\"22\" handValue=\"0\" lastRound=\"firstTime\" loss=\"0\" name=\"BJSWAgent\" netGain=\"0\" "
                    + "playerID=\"5\" response=\"\" tie=\"0\" won=\"0\"/><player bet=\"0\" cards=\"20 10 \" credit=\"20023\" gamePlayerID=\"20\" handValue=\"17\" "
                    + "lastRound=\"firstTime\" loss=\"0\" name=\"dealerJoe\" netGain=\"15\" playerID=\"2\" response=\"\" tie=\"0\" won=\"1\"/></players></gameTable>"
                    + "</gameTables></casino>")){
                System.out.println("Okay added a third player dealer cards face up results previous round in message");
            } else{
                System.out.println("Not okay new round");
            }

            //need to send new round message to each player, but skipping this in testing
            /* deal two cards to each player at table including dealer
                   dealCardMessage will place first dealer card face down i.e., 0 */
            int i = 1;
            for(Map.Entry<Integer, GamePlayer> entry : table.entrySet()){
                //Integer k = entry.getKey();
                p = entry.getValue();
                p.iCard = 2;
                p.cards[0] = i++;
                p.cards[1] = i++;
                p.bet = i++;
                //p.handValue=-1; or previous round value
            }
            t.request = "hit stand doubleDown";
            str = msg.dealCardMessages(t, table);

            if(msg.strcmp(str,
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><casino><gameTables><gameTable chairs=\"3\" decks=\"6\" gameTableID=\"2082\" name=\"blackjack21\" newShuffle=\"false\" request=\"hit stand doubleDown\"><players><player bet=\"3\" cards=\"1 2 \" credit=\"10000\" gamePlayerID=\"21\" handValue=\"16\" lastRound=\"loss\" loss=\"1\" name=\"guestJoe\" netGain=\"-15\" playerID=\"3\" response=\"stand\" tie=\"0\" won=\"0\"/><player bet=\"6\" cards=\"4 5 \" credit=\"9960\" gamePlayerID=\"22\" handValue=\"0\" lastRound=\"firstTime\" loss=\"0\" name=\"BJSWAgent\" netGain=\"0\" playerID=\"5\" response=\"\" tie=\"0\" won=\"0\"/><player bet=\"9\" cards=\"0 8 \" credit=\"20023\" gamePlayerID=\"20\" handValue=\"17\" "
                    + "lastRound=\"firstTime\" loss=\"0\" name=\"dealerJoe\" netGain=\"15\" playerID=\"2\" response=\"\" tie=\"0\" won=\"1\"/></players></gameTable></gameTables></casino>")){
                System.out.println("Okay w 3 players 2 cards dealt to everyone");
            } else{
                System.out.println("Not Okay deal card  message 3  players");
            }

//player IDs are  0,1 dealer was 5
            p = table.get(0);
            msg.stopGamePlayer(p);
            /* can paste strings into netbeans xml window and format */
// "<casino><gamePlayers><gamePlayer clientIP=\"127.0.0.1\" endTime=\"2017-05-06 19:41:17.0\" gamePlayerID=\"21\"  gameTableID=\"2082\" loss=\"1\"  netGain=\"-15\"  playerID=\"3\"  startTime=\"2017-05-06 19:14:35.0\" tie=\"0\"  won=\"0\" /></gamePlayers></casino>\";"
            p = table.get(1);
            p.netGain = 1500;
            p.won = 10;
            p.loss = 4;
            p.tie = 3;
            msg.stopGamePlayer(p);
// "casinoDatabaseOutput:<casino><gamePlayers><gamePlayer clientIP=\"127.0.0.1\" endTime=\"2017-05-06 19:41:17.0\" gamePlayerID=\"22\"  gameTableID=\"2082\" loss=\"4\"  netGain=\"1500\"  playerID=\"5\"  startTime=\"2017-05-06 19:14:35.0\" tie=\"3\"  won=\"10\" /></gamePlayers></casino>\n" +

            msg.stopGamePlayer(table.get(5)); //stop dealer
//"<casino><gamePlayers><gamePlayer clientIP=\"127.0.0.1\" endTime=\"2017-05-06 19:41:17.0\" gamePlayerID=\"20\"  gameTableID=\"2082\" loss=\"0\"  netGain=\"15\"  playerID=\"2\"  startTime=\"2017-05-06 19:14:35.0\" tie=\"0\"  won=\"1\" /></gamePlayers></casino>\n" +

            /* using fakeData so its not correct xml relative to state */
            msg.stopGameTable(t);
// "<casino><gameTables><gameTable IP=\"localhost\" casinoID=\"2\" chairs=\"3\" endTime=\"2017-05-06 19:42:22.0\" gameID=\"1\" gameTableID=\"2082\"  port=\"2\" startTime=\"2017-05-06 12:18:18.0\"><players><player clientIP=\"127.0.0.1\" endTime=\"2017-05-06 19:41:17.0\" gamePlayerID=\"34\" gameTableID=\"2082\" loss=\"2\" netGain=\"43.0\" playerID=\"3\" startTime=\"2017-05-06 19:14:35.0\" tie=\"3\" won=\"2\"/><player clientIP=\"127.0.0.1\" endTime=\"2017-05-06 19:38:12.0\" gamePlayerID=\"35\" gameTableID=\"2082\" loss=\"2\" netGain=\"43.0\" playerID=\"2\" startTime=\"2017-05-06 19:35:37.0\" tie=\"3\" won=\"2\"/></players></gameTable></gameTables></casino>\n" +

        }catch(IOException ex){
            Logger.getLogger(MessageXML.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * reads the players response can use either supported schema,
     * /casino/gameTables or /casino/players updates the server side DOM with
     * response; bet is only read on server request with bet, note this is using
     * chaining, if you have a nulpointer in the chain it will not work.
     * Deprecated used to pass the xmlFile string from player it is now built
     * into the GamePlayer class
     *
     * @param t gameTame
     * @param p player
     * @return p.response sets and returns p.response
     * @throws IOException when player is missing response or we cannot find
     * gamePlayerID in xml response
     */
    public String parseClientResponse(GameTable t, GamePlayer p) throws IOException{

        try{
            //for 21 its just bet quit hit stand doubleDown or bet check bet

            String path = "//player[@gamePlayerID='" + p.gamePlayerID + "']";
            // XPathExpression expr = xpath.compile( path  );

            // used to update response and bet inside of server doc;
            NodeList serverNode = (NodeList) xpath.compile(path).evaluate(doc, XPathConstants.NODESET);

            if(!p.xml.contains(p.gamePlayerID + "")){
                if(debugMode){
                    System.out.println("The xmlFile doesn't have the gamePlayerID\n" + p.gamePlayerID + "\n" + xmlFile);
                    System.out.println("Not Found Fake client: " + fakeClient + ", fakeDatabase: " + fakeDatabase);
                }
                throw new IOException("Cannot find the gamePlayerID: " + p.gamePlayerID);
            }

            /* cannot reuse InputSource once it is handed to parse, parse closes it
                   you can however resuse a Document doc..
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                //only need to declare DocumentBuilderFactor, DocumentBuilder once
                Document responseDoc = db.parse(xmlFile);
                 InputSource input = new InputSource( new StringReader( xmlFile ) );
             */
            Document responseDoc = documentBuilder.parse(new InputSource(new ByteArrayInputStream(p.xml.getBytes("utf-8"))));
            try{

                p.response = (String) xpath.compile(path + "/@response").evaluate(responseDoc, XPathConstants.STRING);

            }catch(XPathExpressionException ex){
                System.out.println("Not Found Fake client: " + fakeClient + ", useDatabase: " + fakeDatabase + "\n"
                        + "\nxmlFile: " + "\npath: " + path);

                if(debugMode){
                    System.out.print("For  gamePlayerID: " + p.gamePlayerID);
                    System.out.println(" we need a response: " + p.xml);
                }
                throw new IOException("cannot find: " + path + "/@response");
            }

            serverNode.item(0).getAttributes().getNamedItem("response").setNodeValue(p.response);

            //dig up table request only set bet value on bet quit, can change this for other games
            if(t.request != null && t.request.matches(".*bet.*")){
                //automatically set bet to positive could throw new IOException( "negative bet not allowed" );
                p.bet = (int) Math.abs((Double) xpath.compile(path + "/@bet").evaluate(responseDoc, XPathConstants.NUMBER));
                serverNode.item(0).getAttributes().getNamedItem("bet").setNodeValue(p.bet + "");
            }

        }catch(XPathExpressionException | SAXException ex){
            Logger.getLogger(MessageXML.class.getName()).log(Level.SEVERE, null, ex);
        }
        return p.response;
    }

    /**
     * gameThread server needs to set the attributes prior the call, different
     * games set different attributes.
     *
     * @param t gameTable with attributes set
     * @param table table of all players
     * @return xml string
     * @throws IOException on errors creating XML document
     */
    @Override
    public String newRoundMessages(GameTable t, TreeMap<Integer, GamePlayer> table) throws IOException{

        try{

            /* made these private class data, no reason to recreate them 
          private final DocumentBuilderFactory  factory = DocumentBuilderFactory.newInstance( );
          private DocumentBuilder documentBuilder = factory.newDocumentBuilder();
             */
            doc = documentBuilder.newDocument();

            Element casino = doc.createElement("casino");

            //casino gameTables gameTable players player /gameTable .../gameTables /casino
            Element gameTables = doc.createElement("gameTables");
            Element gameTable = doc.createElement("gameTable");
            gameTable.setAttribute("newShuffle", Boolean.toString(t.newShuffle));
            gameTable.setAttribute("name", t.name);
            gameTable.setAttribute("gameTableID", t.gameTableID + "");
            gameTable.setAttribute("request", t.request);
            gameTable.setAttribute("decks", t.decks + "");
            gameTable.setAttribute("chairs", t.chairs + "");
            //client already knows IP, port

            //for computer generated code you normally don't add extra white spaces, for testing we need readability
            //root.appendChild( doc.createTextNode("\n" ) );
            //Create XML file and use right Click Format
            /* see blackboard casino discussion on how to iterate a TreeMap with threads */
            Element players = doc.createElement("players");
            for(Map.Entry<Integer, GamePlayer> entry : table.entrySet()){
                //Integer k = entry.getKey();
                GamePlayer p = entry.getValue();

                Element player = doc.createElement("player");

                player.setAttribute("playerID", p.playerID + "");
                player.setAttribute("name", p.name);
                player.setAttribute("credit", p.credit + "");
                player.setAttribute("bet", p.bet + "");
                player.setAttribute("netGain", p.netGain + "");
                player.setAttribute("won", p.won + "");
                player.setAttribute("loss", p.loss + "");
                player.setAttribute("tie", p.tie + "");
                player.setAttribute("gamePlayerID", p.gamePlayerID + "");
                player.setAttribute("handValue", p.handValue + "");

                StringBuilder str = new StringBuilder();
                for(int i = 0; i < p.iCard; ++i){
                    str.append(p.cards[i]).append(" ");
                }

                player.setAttribute("cards", str.toString());

                player.setAttribute("lastRound", p.lastRound + "");
                player.setAttribute("bet", p.bet + "");
                player.setAttribute("response", p.response);

                players.appendChild(player);
            }

            gameTable.appendChild(players);
            gameTables.appendChild(gameTable);
            casino.appendChild(gameTables);
            doc.appendChild(casino);

            // System.out.println( "====root : " + casino + "," + casino.getFirstChild() );            
            // System.out.println( "=====document: " + doc );
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            StringWriter out = new StringWriter(); //note out could be System.out
            transformer.transform(new DOMSource(doc), new StreamResult(out));
            return xmlFile = out.toString();
        }catch(TransformerException ex){
            Logger.getLogger(MessageXML.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "<casino><noTables><noTable Error=\"Unable to create new round message\" /></noTables></casino>";
        //but should be throwing an exception
    }

    @Override
    public String newRound(GameTable t, GamePlayer p) throws IOException{

        send(xmlFile, p);
        p.xml = fakeClient
                ? fakeData.get("newRound", "chairs=\"2")
                : receive(p);
        //will update xml doc on dealCards with new bet
        return parseClientResponse(t, p);
    }

    @Override
    public String nextCard(GameTable t, GamePlayer p) throws IOException{

        send(updatePlayerMessage(t, p), p);

        if(fakeClient){
            //use cards to find fake result
            StringBuilder str = new StringBuilder();
            for(int i = 0; i < p.iCard; ++i){
                str.append(p.cards[i]).append(" ");
            }
            p.xml = fakeData.get("nextCard", str.toString());
        } else{
            p.xml = receive(p);
        }

        // System.out.println("clientResponse: " + clientResponse );
        return parseClientResponse(t, p);
    }

    @Override
    public String bust(GameTable t, GamePlayer p) throws IOException{
        // should be set by gameThread 
        t.request = "bust";
        String XML = updatePlayerMessage(t, p);
        send(XML, p);
        return XML;
    }

}
