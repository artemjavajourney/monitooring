import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Безопасный fallback на случай отсутствия активного транспортного канала уведомлений.
 */
@Slf4j
@Component
@ConditionalOnMissingBean(MonitoringNotificationSender.class)
public class NoopMonitoringNotificationSender implements MonitoringNotificationSender {

    @Override
    public void send(List<String> recipients, String body) {
        log.warn("Отправка мониторинг уведомления отключена. Получатели: {}, сообщение: {}", recipients, body);
    }
}
