import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Рассчитывает бизнес-период, за который нужно считать записи в БД.
 *
 * Важно не путать:
 * - MonitoringScheduledTask.calculateNextExecutionTime(...) решает, когда запускать задачу дальше;
 * - MonitoringPeriodResolver решает, за какой период искать записи.
 */
@Component
public class MonitoringPeriodResolver {

    public MonitoringPeriod resolve(
            ScheduledTask<GiftsScheduledTaskEnum> task,
            MonitoringTaskParameter params
    ) {
        LocalDateTime now = LocalDateTime.now();

        if (task.getTaskTimeType() == TimeTaskType.FIXED_DELAY) {
            return resolveFixedDelayPeriod(task, now);
        }

        if (task.getTaskTimeType() == TimeTaskType.CRON) {
            return resolveCronPeriod(params, now);
        }

        throw new IllegalArgumentException(
                "Unsupported task time type: " + task.getTaskTimeType()
        );
    }

    private MonitoringPeriod resolveFixedDelayPeriod(
            ScheduledTask<GiftsScheduledTaskEnum> task,
            LocalDateTime now
    ) {
        long delayInSeconds = Long.parseLong(task.getTimeParameter());

        return new MonitoringPeriod(
                now.minusSeconds(delayInSeconds),
                now
        );
    }

    private MonitoringPeriod resolveCronPeriod(
            MonitoringTaskParameter params,
            LocalDateTime now
    ) {
        if (params.isRecoveryModeEnabled()) {
            return new MonitoringPeriod(
                    now.minusHours(1),
                    now
            );
        }

        LocalDate today = LocalDate.now();
        LocalTime fromTime = LocalTime.parse(params.getCheckTimeFrom());
        LocalTime toTime = LocalTime.parse(params.getCheckTimeTo());

        LocalDateTime from = today.atTime(fromTime);
        LocalDateTime to = today.atTime(toTime);

        /*
         * Поддержка периода через полночь.
         *
         * Например:
         * checkTimeFrom = "23:00"
         * checkTimeTo   = "01:00"
         *
         * При запуске сегодня в 01:00 корректный период:
         * вчера 23:00 -> сегодня 01:00.
         */
        if (!to.isAfter(from)) {
            from = from.minusDays(1);
        }

        return new MonitoringPeriod(from, to);
    }
}
