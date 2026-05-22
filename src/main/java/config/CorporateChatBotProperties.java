import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "gifts.slave.task.manager.monitoring.chat")
public class CorporateChatBotProperties {

    private boolean enabled = false;
    private String host;
    private int port;
    private boolean secure;
    private String botName;
    private String botToken;
    private long timeoutSeconds = 10;
}
