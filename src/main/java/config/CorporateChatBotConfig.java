import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(CorporateChatBotProperties.class)
@ConditionalOnProperty(
        name = "gifts.slave.task.manager.monitoring.chat.enabled",
        havingValue = "true"
)
public class CorporateChatBotConfig {

    private final CorporateChatBotProperties properties;

    @Bean
    public CorporateChatSdkFacade corporateChatSdkFacade() {
        validateProperties();
        return new ReflectionCorporateChatSdkFacade(properties);
    }

    private void validateProperties() {
        if (isBlank(properties.getHost())) {
            throw new IllegalStateException("Chat bot host must not be blank");
        }
        if (properties.getPort() <= 0) {
            throw new IllegalStateException("Chat bot port must be positive");
        }
        if (isBlank(properties.getBotName())) {
            throw new IllegalStateException("Chat bot name must not be blank");
        }
        if (isBlank(properties.getBotToken())) {
            throw new IllegalStateException("Chat bot token must not be blank");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
