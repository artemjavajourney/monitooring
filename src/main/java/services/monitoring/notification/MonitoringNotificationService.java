import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Формирует и отправляет alert/recovery уведомления в корпоративный чат.
 *
 * Здесь нет бизнес-решения "проблема или не проблема".
 * Этот сервис только отправляет письмо по команде TaskService.
 */
@Service
@RequiredArgsConstructor
public class MonitoringNotificationService {

    private static final DateTimeFormatter MESSAGE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final MonitoringNotificationSender notificationSender;

    @Value("${task.manager.monitoring.notification.recipients:}")
    private List<String> configuredRecipients;

    public void sendAlert(
            MonitoringTaskDefinition definition,
            MonitoringTaskParameter params,
            MonitoringPeriod period,
            long actualCount,
            LocalDateTime checkTime
    ) {
        String body = """
                Поток "%s" на момент проверки %s работает некорректно.

                Период проверки: с %s до %s.
                Ожидалось получить больше %s событий.
                По факту получено: %s.

                Техническая информация:
                taskType: %s
                counterType: %s
                filterValue: %s
                """.formatted(
                definition.displayName(),
                formatDateTime(checkTime),
                formatDateTime(period.getFrom()),
                formatDateTime(period.getTo()),
                params.getMinEventCount(),
                actualCount,
                definition.taskType(),
                definition.counterType(),
                definition.filters()
        );

        notificationSender.send(resolveRecipients(params), body);
    }

    public void sendRecovery(
            MonitoringTaskDefinition definition,
            MonitoringTaskParameter params,
            MonitoringPeriod period,
            long actualCount,
            LocalDateTime checkTime
    ) {
        String body = """
                По потоку "%s" получение событий восстановлено.

                Время проверки: %s.
                Период проверки: с %s до %s.
                Получено событий: %s.

                Техническая информация:
                taskType: %s
                counterType: %s
                filterValue: %s
                """.formatted(
                definition.displayName(),
                formatDateTime(checkTime),
                formatDateTime(period.getFrom()),
                formatDateTime(period.getTo()),
                actualCount,
                definition.taskType(),
                definition.counterType(),
                definition.filters()
        );

        notificationSender.send(resolveRecipients(params), body);
    }

    private List<String> resolveRecipients(MonitoringTaskParameter params) {
        if (configuredRecipients != null && !configuredRecipients.isEmpty()) {
            return configuredRecipients;
        }

        return params.getRecipients();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(MESSAGE_DATE_TIME_FORMATTER);
    }
}
