import ch.quack.man.communication.BotInterface;

import java.util.function.Consumer;

public class MockBot extends BotInterface {

    private final Consumer<String> messageConsumer;

    public MockBot(Consumer<String> messageConsumer) {
        super(null);
        this.messageConsumer = messageConsumer;
    }

    @Override
    public void send(String message) {
        messageConsumer.accept(message);
    }
}
