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
    public BotSystem corporateChatBotSystem() {
        validateProperties();

        BotSystemConfig systemConfig = BotSystemConfig.Builder.newBuilder()
                .withHost(properties.getHost())
                .withPort(properties.getPort())
                .withSecure(properties.isSecure())
                .build();

        return BotSystem.createSystem(systemConfig);
    }

    @Bean
    public Bot corporateChatBot(BotSystem corporateChatBotSystem) throws Exception {
        validateProperties();
        return corporateChatBotSystem
                .startBotWithToken(properties.getBotName(), properties.getBotToken())
                .get();
    }

    private void validateProperties() {
        if (properties.getHost() == null
                || properties.getBotName() == null
                || properties.getBotToken() == null
                || properties.getPort() <= 0) {
            throw new IllegalStateException("Invalid corporate chat bot properties");
        }
    }
}
