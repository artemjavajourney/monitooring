import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Адаптер отправки уведомлений через корпоративный Bot SDK.
 *
 * Реализация специально построена через reflection:
 * - код компилируется даже если SDK-зависимость подключается только в целевом приложении;
 * - при наличии SDK в classpath использует стандартный сценарий:
 *   BotSystemConfig -> BotSystem -> Bot -> resolvePeer -> sendText.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CorporateChatMonitoringSender implements MonitoringEmailSender {

    @Value("${monitoring.chat.host:}")
    private String host;

    @Value("${monitoring.chat.port:0}")
    private int port;

    @Value("${monitoring.chat.secure:true}")
    private boolean secure;

    @Value("${monitoring.chat.bot-name:}")
    private String botName;

    @Value("${monitoring.chat.bot-token:}")
    private String botToken;

    @Override
    public void send(List<String> recipients, String subject, String body) {
        if (recipients == null || recipients.isEmpty()) {
            throw new IllegalArgumentException("Chat recipients must not be empty");
        }

        String text = "[" + subject + "]\n\n" + body;
        for (String recipient : recipients) {
            sendToRecipient(recipient, text);
        }
    }

    private void sendToRecipient(String recipient, String text) {
        try {
            Object bot = startBot();

            Method peersApiMethod = bot.getClass().getMethod("peersApi");
            Object peersApi = peersApiMethod.invoke(bot);

            Method resolvePeerMethod = peersApi.getClass().getMethod("resolvePeer", String.class);
            CompletableFuture<?> peerFuture = (CompletableFuture<?>) resolvePeerMethod.invoke(peersApi, recipient);
            Object peer = peerFuture.get();

            Method messagingMethod = bot.getClass().getMethod("messaging");
            Object messaging = messagingMethod.invoke(bot);

            Method sendTextMethod = messaging.getClass().getMethod("sendText", peer.getClass(), String.class);
            sendTextMethod.invoke(messaging, peer, text);

            log.info("Monitoring notification sent to corporate chat recipient={}", recipient);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to send monitoring notification to corporate chat recipient=" + recipient,
                    e
            );
        }
    }

    private Object startBot() throws Exception {
        validateConfig();

        Class<?> configBuilderClass = Class.forName("ru.sberbank.pprb.sbbol.partners.integrations.chat.bot.sdk.BotSystemConfig$Builder");
        Object builder = configBuilderClass.getMethod("newBuilder").invoke(null);
        builder = configBuilderClass.getMethod("withHost", String.class).invoke(builder, host);
        builder = configBuilderClass.getMethod("withPort", int.class).invoke(builder, port);
        builder = configBuilderClass.getMethod("withSecure", boolean.class).invoke(builder, secure);
        Object config = configBuilderClass.getMethod("build").invoke(builder);

        Class<?> botSystemClass = Class.forName("ru.sberbank.pprb.sbbol.partners.integrations.chat.bot.sdk.BotSystem");
        Object system = botSystemClass.getMethod("createSystem", config.getClass()).invoke(null, config);

        CompletableFuture<?> botFuture = (CompletableFuture<?>) botSystemClass
                .getMethod("startBotWithToken", String.class, String.class)
                .invoke(system, botName, botToken);

        return botFuture.get();
    }

    private void validateConfig() {
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("monitoring.chat.host must be configured");
        }
        if (port <= 0) {
            throw new IllegalStateException("monitoring.chat.port must be configured and > 0");
        }
        if (botName == null || botName.isBlank()) {
            throw new IllegalStateException("monitoring.chat.bot-name must be configured");
        }
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalStateException("monitoring.chat.bot-token must be configured");
        }
    }
}
