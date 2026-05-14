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

    private static final String DEFAULT_ALERT_SUBJECT = "Проблема мониторингового потока";
    private static final String DEFAULT_RECOVERY_SUBJECT = "Восстановление мониторингового потока";

    private final MonitoringEmailSender emailSender;

    public void sendAlert(
            MonitoringTaskDefinition definition,
            MonitoringTaskParameter params,
            MonitoringPeriod period,
            long actualCount,
            LocalDateTime checkTime
    ) {
        String subject = params.getSubject() != null
                ? params.getSubject()
                : DEFAULT_ALERT_SUBJECT + ": " + definition.getFlowName();

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
                definition.getFlowName(),
                checkTime,
                period.getFrom(),
                period.getTo(),
                params.getMinEventCount(),
                actualCount,
                definition.getTaskEnum(),
                definition.getCounterType(),
                definition.getFilterValue()
        );

        emailSender.send(params.getRecipients(), subject, body);
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
                definition.getFlowName(),
                checkTime,
                period.getFrom(),
                period.getTo(),
                actualCount,
                definition.getTaskEnum(),
                definition.getCounterType(),
                definition.getFilterValue()
        );

        emailSender.send(
                params.getRecipients(),
                DEFAULT_RECOVERY_SUBJECT + ": " + definition.getFlowName(),
                body
        );
    }
}
