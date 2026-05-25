public interface ChatFacade {

    Object resolvePeer(String recipient) throws Exception;

    void sendMessage(Object peer, String message) throws Exception;
}
