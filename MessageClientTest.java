/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.PrintStream;
import java.util.Scanner;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author msamatar0
 */
public class MessageClientTest{
    
    public MessageClientTest(){}

    /**
     * Test of readRequest method, of class MessageClient.
     */
    @Test
    public void testReadRequest(){
        System.out.println("readRequest");
        MessageClient mc = new MessageClient();
        Scanner in;
        for(int i = 0; i < testReadResponse.length; ++i){
            in = new Scanner(testReadResponse[i][0]);
            mc.player.gamePlayerID = Integer.parseInt(in.nextLine());
            assertEquals(testReadResponse[i][1],
                    mc.readRequest(in.nextLine()));
        }
    }
    
    static String[][] testReadResponse = {
        { 
            "128\n" +  /* response and bet will be missing from the first message return from chatServer */
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><casino><gameTables>" +
            "<gameTable chairs=\"0\" decks=\"0\" gameTableID=\"2097\" name=\"chat\" newShuffle=\"false\" "+
            "request=\"\"><players><player cards=\"\" credit=\"32956\" gamePlayerID=\"128\" handValue=\"0\" " +
            "lastRound=\"none\" loss=\"0\" name=\"Minnie\" netGain=\"0\" playerID=\"1\" tie=\"0\" won=\"0\"/>" +
            "</players></gameTable></gameTables></casino>\n",
            "GamePlayerID: 128 PlayerID: 1 bet: 0 response: ",
        },{
            "129\n" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><casino><gameTables>" +
            "<gameTable chairs=\"0\" decks=\"0\" gameTableID=\"2097\" name=\"chat\" newShuffle=\"false\" " +
            "request=\"129 Mon May 15 15:41:25 PDT 2017 NewPlayerSending a Message\"><players>" +
            "<player bet=\"0\" cards=\"\" credit=\"15023\" gamePlayerID=\"129\" handValue=\"0\" " +
            "lastRound=\"Mon May 15 15:41:25 PDT 2017\" loss=\"0\" name=\"MinnieAgent\" netGain=\"0\" " +
            "playerID=\"6\" response=\"NewPlayerSending a Message, msg also in gameTable request\" tie=\"0\" won=\"0\"/><player bet=\"0\" " +
            "cards=\"\" credit=\"32956\" gamePlayerID=\"128\" handValue=\"0\" lastRound=\"none\" loss=\"0\" name=\"Minnie\" "+
            "netGain=\"0\" playerID=\"1\" response=\"\" tie=\"0\" won=\"0\"/>" + 
            "<player bet=\"0\" cards=\"\" credit=\"15023\" gamePlayerID=\"129\" handValue=\"0\" lastRound=\"Mon May 15 15:41:53 PDT 2017\" "+
            "loss=\"0\" name=\"MinnieAgent\" netGain=\"0\" playerID=\"6\" response=\"second Player sending messge\" tie=\"0\" won=\"0\"/>" +
            "</players></gameTable></gameTables></casino>\n",
            "GamePlayerID: 129 PlayerID: 6 bet: 0 response: NewPlayerSending a Message, msg also in gameTable request",
        },{
            "128\n" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><casino><gameTables>" +
            "<gameTable chairs=\"0\" decks=\"0\" gameTableID=\"2097\" name=\"chat\" newShuffle=\"false\" " +
            "request=\"129 Mon May 15 15:41:25 PDT 2017 NewPlayerSending a Message\"><players>" +
            "<player bet=\"0\" cards=\"\" credit=\"15023\" gamePlayerID=\"129\" handValue=\"0\" " +
            "lastRound=\"Mon May 15 15:41:25 PDT 2017\" loss=\"0\" name=\"MinnieAgent\" netGain=\"0\" " +
            "playerID=\"6\" response=\"NewPlayerSending a Message gamePlayer request repeats this\" tie=\"0\" won=\"0\"/><player bet=\"15\" " +
            "cards=\"\" credit=\"32956\" gamePlayerID=\"128\" handValue=\"0\" lastRound=\"none\" loss=\"0\" name=\"Minnie\" "+
            "netGain=\"0\" playerID=\"1\" response=\"First Player for chatServer not nested\" tie=\"0\" won=\"0\"/>" + 
            "<player bet=\"10\" cards=\"\" credit=\"15023\" gamePlayerID=\"129\" handValue=\"0\" lastRound=\"Mon May 15 15:41:53 PDT 2017\" "+
            "loss=\"0\" name=\"MinnieAgent\" netGain=\"0\" playerID=\"6\" response=\"second Player sending messge\" tie=\"0\" won=\"0\"/>" +
            "</players></gameTable></gameTables></casino>\n",
            "GamePlayerID: 128 PlayerID: 1 bet: 15 response: First Player for chatServer not nested",
        },
    };
}
