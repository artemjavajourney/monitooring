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
@ConditionalOnProperty(
        name = "gifts.slave.task.manager.monitoring.chat.enabled",
        havingValue = "true"
)
public class CorporateChatMonitoringNotificationSender implements MonitoringNotificationSender {

    private final CorporateChatSdkFacade corporateChatSdkFacade;
    private final CorporateChatBotProperties properties;

    @Override
    public void send(List<String> recipients, String subject, String body) {
        if (recipients == null || recipients.isEmpty()) {
            throw new CorporateChatSendException("Не указаны получатели сообщения корпоративного чата");
        }

        String messageText = buildMessageText(subject, body);
        List<String> failedRecipients = new ArrayList<>();

        for (String recipient : recipients) {
            try {
                sendToRecipient(recipient, messageText);
            } catch (Exception e) {
                failedRecipients.add(recipient);
                log.error("Не удалось отправить monitoring-сообщение в корпоративный чат. recipient={}", recipient, e);
            }
        }

        if (!failedRecipients.isEmpty()) {
            throw new CorporateChatSendException("Не удалось отправить сообщение в корпоративный чат получателям: " + failedRecipients);
        }
    }

    private void sendToRecipient(String recipient, String messageText) throws Exception {
        if (recipient == null || recipient.isBlank()) {
            throw new CorporateChatSendException("Recipient корпоративного чата не должен быть пустым");
        }

        long timeout = properties.getTimeoutSeconds();
        Object peer = corporateChatSdkFacade.resolvePeer(recipient, timeout, TimeUnit.SECONDS);
        corporateChatSdkFacade.sendText(peer, messageText, timeout, TimeUnit.SECONDS);
    }

    private String buildMessageText(String subject, String body) {
        if (subject == null || subject.isBlank()) {
            return body;
        }

        return "%s\n\n%s".formatted(subject, body);
    }
}
