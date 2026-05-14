import lombok.Value;

import java.time.LocalDateTime;

/**
 * Период, за который мониторинговая задача должна посчитать события.
 *
 * Этот класс ничего не проверяет сам по себе.
 * Это простая модель-значение: from -> to.
 */
@Value
public class MonitoringPeriod {
    LocalDateTime from;
    LocalDateTime to;
}
