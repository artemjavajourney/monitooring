import java.util.concurrent.TimeUnit;

public interface CorporateChatSdkFacade {

    Object resolvePeer(String recipient, long timeout, TimeUnit unit) throws Exception;

    void sendText(Object peer, String messageText, long timeout, TimeUnit unit) throws Exception;
}
