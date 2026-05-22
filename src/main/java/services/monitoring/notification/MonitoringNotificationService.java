import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Формирует и отправляет alert/recovery уведомления.
 *
 * Здесь нет бизнес-решения "проблема или не проблема".
 * Этот сервис только отправляет письмо по команде TaskService.
 */
@Service
@RequiredArgsConstructor
public class MonitoringNotificationService {

    private static final String DEFAULT_ALERT_SUBJECT = "Проблема monitoring-потока";
    private static final String DEFAULT_RECOVERY_SUBJECT = "Восстановление monitoring-потока";

    private final MonitoringNotificationSender notificationSender;

    public void sendAlert(
            MonitoringTaskDefinition definition,
            MonitoringTaskParameter params,
            MonitoringPeriod period,
            long actualCount,
            LocalDateTime checkTime
    ) {
        String subject = params.getSubject() != null
                ? params.getSubject()
                : DEFAULT_ALERT_SUBJECT + ": " + definition.displayName();

        String body = """
                Поток "%s" на момент проверки %s работает некорректно.

                Период проверки: с %s до %s.
                Ожидалось получить больше %s событий.
                Фактически получено: %s.

                taskType: %s
                counterType: %s
                filters: %s
                """.formatted(
                definition.displayName(),
                checkTime,
                period.from(),
                period.to(),
                params.getMinEventCount(),
                actualCount,
                definition.taskType(),
                definition.counterType(),
                definition.filters()
        );

        notificationSender.send(params.getRecipients(), subject, body);
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

                taskType: %s
                counterType: %s
                filters: %s
                """.formatted(
                definition.displayName(),
                checkTime,
                period.from(),
                period.to(),
                actualCount,
                definition.taskType(),
                definition.counterType(),
                definition.filters()
        );

        notificationSender.send(
                params.getRecipients(),
                DEFAULT_RECOVERY_SUBJECT,
                body
        );
    }
}
