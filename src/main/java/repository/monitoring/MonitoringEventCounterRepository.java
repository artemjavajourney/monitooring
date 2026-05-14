import java.time.LocalDateTime;

/**
 * Репозиторий подсчёта событий для мониторинговых задач.
 */
public interface MonitoringEventCounterRepository {

    long count(
            MonitoringTaskDefinition definition,
            LocalDateTime from,
            LocalDateTime to
    );
}
