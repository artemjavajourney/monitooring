import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "chat.bot.enabled", havingValue = "true")
public class ChatMonitoringNotificationSender implements MonitoringNotificationSender {

    private final ChatFacade facade;

    @Override
    public void send(List<String> recipients, String body) {
        List<String> failedRecipients = new ArrayList<>();

        for (String recipient : recipients) {
            try {
                Object peer = facade.resolvePeer(recipient);
                facade.sendMessage(peer, body);
            } catch (Exception e) {
                failedRecipients.add(recipient);
                log.error("Не удалось отправить сообщение пользователю: {}", recipient, e);
            }
        }

        if (!failedRecipients.isEmpty()) {
            throw new IllegalStateException("Не удалось отправить сообщения пользователям: " + failedRecipients);
        }
    }
}
