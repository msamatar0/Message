import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import org.w3c.dom.Document;
import org.xml.sax.*;

/**
 *
 * @author msamatar0
 */
public class MessageClient{
    GamePlayer player = new GamePlayer("Minnie", "chat");
    GameTable table = new GameTable();
    
    public MessageClient(){}
    
    public MessageClient(String name, String game){
        player = new GamePlayer(name, game);
    }
    
    public GamePlayer getGamePlayer(){
        return player;
    }
    
    public String readRequest(String XML){
        try{
            Document docx = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new ByteArrayInputStream(XML.getBytes("utf-8"))));
            String path = "//player[@gamePlayerID=" + player.gamePlayerID + "]";
            XPath xp = XPathFactory.newInstance()
                    .newXPath();
            player.bet = ((Double)xp.compile(path + "/@bet")
                    .evaluate(docx, XPathConstants.NUMBER)).intValue();
            player.playerID = ((Double)xp.compile(path + "/@playerID")
                    .evaluate(docx, XPathConstants.NUMBER)).intValue();
            player.response = (String)xp.compile(path + "/@response")
                    .evaluate(docx, XPathConstants.STRING);
            return player.toString();
        }catch(SAXException | UnsupportedEncodingException ex){
            Logger.getLogger(MessageClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch(IOException | ParserConfigurationException | XPathExpressionException ex){
            Logger.getLogger(MessageClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return player.toString();
    }
    
    public String sendResponse(String response){
        return "";
    }
    
    public String processIO(Scanner in, PrintStream out){
        StringBuilder sb = new StringBuilder();
        //while(in.hasNext())
            output(readRequest(in.nextLine()), sb, out);
        return sb.toString();
    }
    
    public void output(String s, StringBuilder sb, PrintStream out){
        sb.append(s);
        out.println(s);
    }
    
    public String processIO(Scanner in){
        return processIO(in, null);
    }
    
    public static void main(String[] args){
        Scanner in = new Scanner(sample);
        MessageClient mc = new MessageClient();
        mc.player.playerID = 128;
        System.out.println(mc.readRequest(sample));
    }
    
    static String sample = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><casino><gameTables>" +
        "<gameTable chairs=\"0\" decks=\"0\" gameTableID=\"2097\" name=\"chat\" newShuffle=\"false\" "+
        "request=\"\"><players><player cards=\"\" credit=\"32956\" gamePlayerID=\"128\" handValue=\"0\" " +
        "lastRound=\"none\" loss=\"0\" name=\"Minnie\" netGain=\"0\" playerID=\"1\" tie=\"0\" won=\"0\" bet=\"1\"/>" +
        "</players></gameTable></gameTables></casino>";
}
