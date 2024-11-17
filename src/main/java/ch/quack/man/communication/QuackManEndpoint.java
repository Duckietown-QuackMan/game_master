package ch.quack.man.communication;


import ch.quack.man.GameMaster;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.logging.Logger;

@ServerEndpoint("/quackman")
public class QuackManEndpoint {

    private static final Logger LOGGER = Logger.getLogger(QuackManEndpoint.class.getName());

    private static BotInterface botSessions = null;

    @OnOpen
    public void onOpen(Session session) {
        LOGGER.info("QuackMan connected.");
        if (botSessions != null) {
            LOGGER.warning("Second QuackMan tried to connect. Ignoring.");
        }
        BotInterface bot = new BotInterface(session);
        GameMaster.getInstance().registerQuackMan(bot);
        botSessions = bot;
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        LOGGER.info("Received QuackMan message");
        botSessions.handleMsg(message);
    }

    @OnClose
    public void onClose(Session session) {
        LOGGER.info("QuackMan disconnected.");
        GameMaster.getInstance().deregisterQuackMan(botSessions);
        botSessions = null;
    }
}