import java.util.List;

/**
 * Порт отправки email.
 *
 * Если в проекте уже есть собственный SMTP/mail service,
 * можно реализовать этот интерфейс через него и удалить JavaMail-реализацию.
 */
public interface MonitoringNotificationSender {

    void send(
            List<String> recipients,
            String subject,
            String body
    );
}
