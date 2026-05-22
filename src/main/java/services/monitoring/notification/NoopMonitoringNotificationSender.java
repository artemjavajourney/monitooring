import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "gifts.slave.task.manager.monitoring.chat.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class NoopMonitoringNotificationSender implements MonitoringNotificationSender {

    @Override
    public void send(List<String> recipients, String subject, String body) {
        log.warn("Monitoring notification sending is disabled. recipients={}", recipients);
    }
}
