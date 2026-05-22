import java.lang.reflect.Method;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ReflectionCorporateChatSdkFacade implements CorporateChatSdkFacade {

    private final Object bot;

    public ReflectionCorporateChatSdkFacade(CorporateChatBotProperties properties) {
        try {
            Class<?> configClass = Class.forName("BotSystemConfig");
            Class<?> builderClass = Class.forName("BotSystemConfig$Builder");
            Object builder = builderClass.getMethod("newBuilder").invoke(null);
            builderClass.getMethod("withHost", String.class).invoke(builder, properties.getHost());
            builderClass.getMethod("withPort", int.class).invoke(builder, properties.getPort());
            builderClass.getMethod("withSecure", boolean.class).invoke(builder, properties.isSecure());
            Object systemConfig = builderClass.getMethod("build").invoke(builder);

            Class<?> systemClass = Class.forName("BotSystem");
            Object system = systemClass.getMethod("createSystem", configClass).invoke(null, systemConfig);
            Future<?> botFuture = (Future<?>) systemClass
                    .getMethod("startBotWithToken", String.class, String.class)
                    .invoke(system, properties.getBotName(), properties.getBotToken());
            this.bot = botFuture.get(properties.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось инициализировать SDK корпоративного чата", e);
        }
    }

    @Override
    public Object resolvePeer(String recipient, long timeout, TimeUnit unit) throws Exception {
        Method peersApiMethod = bot.getClass().getMethod("peersApi");
        Object peersApi = peersApiMethod.invoke(bot);
        Future<?> peerFuture = (Future<?>) peersApi.getClass().getMethod("resolvePeer", String.class).invoke(peersApi, recipient);
        return peerFuture.get(timeout, unit);
    }

    @Override
    public void sendText(Object peer, String messageText, long timeout, TimeUnit unit) throws Exception {
        Object messaging = bot.getClass().getMethod("messaging").invoke(bot);
        Future<?> sendFuture = (Future<?>) messaging.getClass().getMethod("sendText", peer.getClass(), String.class)
                .invoke(messaging, peer, messageText);
        sendFuture.get(timeout, unit);
    }
}
