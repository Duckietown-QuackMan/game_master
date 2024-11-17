package ch.quack.man.communication;

import ch.quack.man.GameMaster;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.*;
import java.util.logging.Logger;

@ServerEndpoint("/ghostbot")
public class GhostBotEndpoint {

    private static final Logger LOGGER = Logger.getLogger(GhostBotEndpoint.class.getName());

    private static final Map<Session, BotInterface> botSessions = Collections.synchronizedMap(new HashMap<>());

    @OnOpen
    public void onOpen(Session session) {
        LOGGER.info("GhostBot connected.");
        BotInterface bot = new BotInterface(session);
        GameMaster.getInstance().registerGhostBot(bot);
        botSessions.put(session, bot);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        LOGGER.info("Received GhostBot message");
        botSessions.get(session).handleMsg(message);
    }

    @OnClose
    public void onClose(Session session) {
        LOGGER.info("GhostBot disconnected.");
        GameMaster.getInstance().deregisterGhostBot(botSessions.get(session));
        botSessions.remove(session);
    }
}