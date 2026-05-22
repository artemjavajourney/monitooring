import java.util.List;

public interface MonitoringNotificationSender {

    void send(List<String> recipients, String subject, String body);
}
