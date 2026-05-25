import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonitoringNotificationServiceTest {

    @Test
    void sendAlertFormatsBodyAndUsesRecipients() {
        MonitoringTestSupport.RecordingNotificationSender sender = new MonitoringTestSupport.RecordingNotificationSender();
        MonitoringNotificationService notificationService = new MonitoringNotificationService(sender);
        MonitoringTaskParameter params = new MonitoringTaskParameter();
        params.setMinEventCount(10);
        params.setRecipients(List.of("support-channel"));
        MonitoringPeriod period = new MonitoringPeriod(
                LocalDateTime.of(2026, 5, 18, 8, 0),
                LocalDateTime.of(2026, 5, 18, 9, 0)
        );

        notificationService.sendAlert(
                MonitoringTaskDefinitionRegistry.MECHANICS,
                params,
                period,
                3,
                LocalDateTime.of(2026, 5, 18, 9, 0)
        );

        assertEquals(1, sender.count());
        assertEquals(List.of("support-channel"), sender.first().recipients());
        assertTrue(sender.first().body().contains("Mechanics"));
        assertTrue(sender.first().body().contains("Ожидалось получить больше 10"));
        assertTrue(sender.first().body().contains("По факту получено: 3"));
        assertTrue(sender.first().body().contains("18.05.2026 09:00:00"));
    }

    @Test
    void sendAlertIncludesTechnicalDetails() {
        MonitoringTestSupport.RecordingNotificationSender sender = new MonitoringTestSupport.RecordingNotificationSender();
        MonitoringNotificationService notificationService = new MonitoringNotificationService(sender);
        MonitoringTaskParameter params = new MonitoringTaskParameter();
        params.setMinEventCount(10);
        params.setRecipients(List.of("support-channel"));
        MonitoringPeriod period = new MonitoringPeriod(
                LocalDateTime.of(2026, 5, 18, 8, 0),
                LocalDateTime.of(2026, 5, 18, 9, 0)
        );

        notificationService.sendAlert(
                MonitoringTaskDefinitionRegistry.CASES,
                params,
                period,
                10,
                LocalDateTime.of(2026, 5, 18, 9, 0)
        );

        assertEquals(1, sender.count());
        assertTrue(sender.first().body().contains("CASES_MONITORING_TASK"));
    }

    @Test
    void sendRecoveryUsesRecoverySubjectAndBody() {
        MonitoringTestSupport.RecordingNotificationSender sender = new MonitoringTestSupport.RecordingNotificationSender();
        MonitoringNotificationService notificationService = new MonitoringNotificationService(sender);
        MonitoringTaskParameter params = new MonitoringTaskParameter();
        params.setMinEventCount(10);
        params.setRecipients(List.of("support-channel"));
        MonitoringPeriod period = new MonitoringPeriod(
                LocalDateTime.of(2026, 5, 18, 10, 0),
                LocalDateTime.of(2026, 5, 18, 11, 0)
        );

        notificationService.sendRecovery(
                MonitoringTaskDefinitionRegistry.ZOK_4,
                params,
                period,
                11,
                LocalDateTime.of(2026, 5, 18, 11, 0)
        );

        assertEquals(1, sender.count());
        assertTrue(sender.first().body().contains("получение событий восстановлено"));
        assertTrue(sender.first().body().contains("Получено событий: 11"));
        assertTrue(sender.first().body().contains("18.05.2026 11:00:00"));
    }
}
