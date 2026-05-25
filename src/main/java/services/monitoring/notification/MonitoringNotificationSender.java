import java.util.List;

/**
 * Канал отправки мониторинговых уведомлений (чат, email, noop).
 */
public interface MonitoringNotificationSender {

    void send(List<String> recipients, String body);
}
