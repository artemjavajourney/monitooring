import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * Общий ScheduledTask для всех новых мониторинговых задач.
 *
 * Нужен только для специального расчёта nextExecutionTime в CRON recovery-режиме.
 */
@Slf4j
public class MonitoringScheduledTask extends ScheduledTask<GiftsScheduledTaskEnum> {

    private static final ObjectMapper OBJECT_MAPPER = TaskService.objectMapper;

    public MonitoringScheduledTask(
            ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
    ) {
        super(getter);
    }

    @Override
    public LocalDateTime calculateNextExecutionTime(
            ExecutionResultModel<GiftsScheduledTaskEnum> resultModel
    ) {
        /*
         * Если задача упала технически, базовый ScheduledTask.errorResult(...)
         * уже выставил retry через 5 минут.
         *
         * TaskManagerAbs после execute(...) всё равно вызывает calculateNextExecutionTime(...),
         * поэтому здесь сохраняем retry-время, а не перетираем его обычным CRON.
         */
        if (ExecutionTaskResultStatus.ERROR.equals(resultModel.getResult())
                && resultModel.getNextExecutionTime() != null) {
            return resultModel.getNextExecutionTime();
        }

        /*
         * Для FIXED_DELAY используем стандартный расчёт:
         * startTime + delay + executionDuration.
         */
        if (getTaskTimeType() != TimeTaskType.CRON) {
            return super.calculateNextExecutionTime(resultModel);
        }

        /*
         * Для CRON смотрим обновлённые params из результата.
         * Именно сервис задачи меняет recoveryMode и кладёт новые params в ExecutionResultModel.
         */
        if (isRecoveryMode(resultModel.getParams())) {
            LocalDateTime baseTime = resultModel.getStartTime() != null
                    ? resultModel.getStartTime()
                    : LocalDateTime.now();

            return baseTime.plusHours(1);
        }

        return super.calculateNextExecutionTime(resultModel);
    }

    private boolean isRecoveryMode(String params) {
        try {
            MonitoringTaskParameter parameter = OBJECT_MAPPER.readValue(
                    params,
                    MonitoringTaskParameter.class
            );

            return parameter.isRecoveryModeEnabled();
        } catch (Exception e) {
            log.warn(
                    "Could not read recoveryMode for task {}. Standard schedule will be used",
                    getTaskType(),
                    e
            );
            return false;
        }
    }
}
