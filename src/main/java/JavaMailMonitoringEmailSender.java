import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SMTP-реализация отправки email через Spring JavaMailSender.
 *
 * Требует spring-boot-starter-mail или аналогичную зависимость.
 * Если в проекте уже есть собственный сервис отправки писем,
 * лучше заменить эту реализацию адаптером к существующему сервису.
 */
@Component
@RequiredArgsConstructor
public class JavaMailMonitoringEmailSender implements MonitoringEmailSender {

    private final JavaMailSender javaMailSender;

    @Override
    public void send(
            List<String> recipients,
            String subject,
            String body
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipients.toArray(new String[0]));
        message.setSubject(subject);
        message.setText(body);

        javaMailSender.send(message);
    }
}
