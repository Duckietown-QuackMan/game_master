import ch.quack.man.GameMaster;

import java.util.function.Consumer;

public class Utils {


    public static MockBot connectQuackMan(Consumer<String> messageConsumer) {
        MockBot quackMan = new MockBot(messageConsumer);
        GameMaster.getInstance().registerQuackMan(quackMan);
        return quackMan;
    }

    public static MockBot connectGhostBot(Consumer<String> messageConsumer) {
        MockBot ghostBot = new MockBot(messageConsumer);
        GameMaster.getInstance().registerGhostBot(ghostBot);
        return ghostBot;
    }

}
