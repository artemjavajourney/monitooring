import java.time.LocalDateTime;

/**
 * Репозиторий подсчёта событий для мониторинговых задач.
 */
public interface MonitoringEventRepository {

    long count(
            MonitoringTaskDefinition definition,
            LocalDateTime from,
            LocalDateTime to
    );
}
