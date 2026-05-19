import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonitoringNotificationServiceTest {

    @Test
    void sendAlertUsesCustomSubjectWhenSpecified() {
        MonitoringTestSupport.RecordingEmailSender emailSender = new MonitoringTestSupport.RecordingEmailSender();
        MonitoringNotificationService notificationService = new MonitoringNotificationService(emailSender);
        MonitoringTaskParameter params = new MonitoringTaskParameter();
        params.setMinEventCount(10);
        params.setRecipients(List.of("support-channel"));
        params.setSubject("Custom alert subject");
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

        assertEquals(1, emailSender.count());
        assertEquals("Custom alert subject", emailSender.first().subject());
        assertEquals(List.of("support-channel"), emailSender.first().recipients());
        assertTrue(emailSender.first().body().contains("Mechanics"));
        assertTrue(emailSender.first().body().contains("Ожидалось получить больше 10"));
        assertTrue(emailSender.first().body().contains("По факту получено: 3"));
    }

    @Test
    void sendAlertUsesDefaultSubjectWithDisplayNameWhenCustomSubjectIsMissing() {
        MonitoringTestSupport.RecordingEmailSender emailSender = new MonitoringTestSupport.RecordingEmailSender();
        MonitoringNotificationService notificationService = new MonitoringNotificationService(emailSender);
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

        assertEquals(1, emailSender.count());
        assertTrue(emailSender.first().subject().contains("Проблема мониторингового потока"));
        assertTrue(emailSender.first().subject().contains("Cases"));
        assertTrue(emailSender.first().body().contains("CASES_MONITORING_TASK"));
    }

    @Test
    void sendRecoveryUsesRecoverySubjectAndBody() {
        MonitoringTestSupport.RecordingEmailSender emailSender = new MonitoringTestSupport.RecordingEmailSender();
        MonitoringNotificationService notificationService = new MonitoringNotificationService(emailSender);
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

        assertEquals(1, emailSender.count());
        assertTrue(emailSender.first().subject().contains("Восстановление мониторингового потока"));
        assertTrue(emailSender.first().subject().contains("ZOK_4"));
        assertTrue(emailSender.first().body().contains("получение событий восстановлено"));
        assertTrue(emailSender.first().body().contains("Получено событий: 11"));
    }
}
