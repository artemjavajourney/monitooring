import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "gifts.slave.task.manager.monitoring.chat.enabled", havingValue = "true")
public class CorporateChatMonitoringNotificationSender implements MonitoringNotificationSender {

    private final Bot bot;
    private final CorporateChatBotProperties properties;

    @Override
    public void send(List<String> recipients, String subject, String body) {
        String messageText = (subject != null && !subject.isBlank())
                ? subject + "\n\n" + body
                : body;

        List<String> failedRecipients = new ArrayList<>();

        for (String recipient : recipients) {
            try {
                Object peer = bot.peersApi()
                        .resolvePeer(recipient)
                        .get(properties.getTimeoutSeconds(), TimeUnit.SECONDS);

                bot.messaging()
                        .sendText(peer, messageText)
                        .get(properties.getTimeoutSeconds(), TimeUnit.SECONDS);

                log.info("Monitoring message sent to recipient={}", recipient);
            } catch (Exception e) {
                failedRecipients.add(recipient);
                log.error("Failed to send monitoring message to recipient={}", recipient, e);
            }
        }

        if (!failedRecipients.isEmpty()) {
            throw new CorporateChatSendException(
                    "Failed to send monitoring message to recipients: " + failedRecipients
            );
        }
    }
}
