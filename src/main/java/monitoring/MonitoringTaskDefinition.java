import java.util.Map;
import java.util.Objects;

/**
 * Описание того, что именно считает конкретная мониторинговая задача.
 *
 * Благодаря этому классу большая часть логики остаётся общей,
 * а конкретные TaskService отличаются только definition-ом.
 */
public record MonitoringTaskDefinition(
        GiftsScheduledTaskEnum taskType,
        String displayName,
        MonitoringCounterType counterType,
        Map<MonitoringFilterKey, String> filters
) {

    public MonitoringTaskDefinition {
        Objects.requireNonNull(taskType, "taskType must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(counterType, "counterType must not be null");
        Objects.requireNonNull(filters, "filters must not be null");

        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }

        if (filters.isEmpty()) {
            throw new IllegalArgumentException("filters must not be empty");
        }

        filters = Map.copyOf(filters);
    }

    public String requiredFilter(MonitoringFilterKey key) {
        var value = filters.get(key);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Required monitoring filter is missing: " + key +
                            " for taskType: " + taskType
            );
        }

        return value;
    }
}
