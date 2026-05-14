import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * Общая бизнес-логика для всех пяти мониторинговых задач.
 *
 * Конкретные сервисы отличаются только MonitoringTaskDefinition:
 * - какой enum обслуживают;
 * - какую таблицу считают;
 * - какой business filter используют.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractMonitoringTaskService
        extends BaseTaskService<GiftsScheduledTaskEnum, MonitoringTaskParameter> {

    private final MonitoringPeriodResolver periodResolver;
    private final MonitoringEventRepository monitoringEventRepository;
    private final MonitoringNotificationService notificationService;

    protected abstract MonitoringTaskDefinition getDefinition();

    @Override
    public ExecutionResultModel<GiftsScheduledTaskEnum> run(
            ScheduledTask<GiftsScheduledTaskEnum> task
    ) throws Exception {
        MonitoringTaskDefinition definition = getDefinition();

        log.debug("Запуск мониторинговой задачи {}", definition.taskType());

        MonitoringTaskParameter params = deserialize(task.getParams());
        validateParams(params, task);

        boolean wasRecoveryMode = params.isRecoveryModeEnabled();

        MonitoringPeriod period = periodResolver.resolve(task, params);

        long actualCount = monitoringEventRepository.count(
                definition,
                period.getFrom(),
                period.getTo()
        );

        boolean hasProblem = actualCount <= params.getMinEventCount();
        LocalDateTime checkTime = LocalDateTime.now();

        if (hasProblem) {
            notificationService.sendAlert(
                    definition,
                    params,
                    period,
                    actualCount,
                    checkTime
            );

            if (task.getTaskTimeType() == TimeTaskType.CRON) {
                params.setRecoveryMode(true);
            }

            return buildSuccessResult(
                    params,
                    buildProblemResultMessage(definition, params, period, actualCount)
            );
        }

        if (task.getTaskTimeType() == TimeTaskType.CRON) {
            if (wasRecoveryMode) {
                notificationService.sendRecovery(
                        definition,
                        params,
                        period,
                        actualCount,
                        checkTime
                );
            }

            params.setRecoveryMode(false);
        }

        return buildSuccessResult(
                params,
                buildSuccessResultMessage(definition, params, period, actualCount)
        );
    }

    @Override
    public GiftsScheduledTaskEnum getEnum() {
        return getDefinition().taskType();
    }

    @Override
    protected MonitoringTaskParameter deserialize(String str) {
        try {
            return objectMapper.readValue(str, MonitoringTaskParameter.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Invalid monitoring task parameters for " + getDefinition().taskType(),
                    e
            );
        }
    }

    private void validateParams(
            MonitoringTaskParameter params,
            ScheduledTask<GiftsScheduledTaskEnum> task
    ) {
        if (params.getMinEventCount() == null || params.getMinEventCount() < 0) {
            throw new IllegalArgumentException("minEventCount must be specified and must be >= 0");
        }

        if (params.getRecipients() == null || params.getRecipients().isEmpty()) {
            throw new IllegalArgumentException("recipients must be specified");
        }

        if (task.getTaskTimeType() == TimeTaskType.CRON) {
            if (params.getCheckTimeFrom() == null || params.getCheckTimeTo() == null) {
                throw new IllegalArgumentException(
                        "checkTimeFrom and checkTimeTo must be specified for CRON task"
                );
            }
        }
    }

    private ExecutionResultModel<GiftsScheduledTaskEnum> buildSuccessResult(
            MonitoringTaskParameter params,
            String resultMessage
    ) throws JsonProcessingException {
        ExecutionResultModel<GiftsScheduledTaskEnum> result = new ExecutionResultModel<>();

        /*
         * Малое количество событий - это бизнес-проблема потока,
         * а не техническая ошибка выполнения задачи.
         */
        result.setResult(ExecutionTaskResultStatus.SUCCESS);
        result.setTaskType(getEnum());
        result.setParams(serializeParams(params));
        result.setResultMessage(resultMessage);

        return result;
    }

    private String buildProblemResultMessage(
            MonitoringTaskDefinition definition,
            MonitoringTaskParameter params,
            MonitoringPeriod period,
            long actualCount
    ) {
        return """
                Мониторинговая задача обнаружила проблему.
                taskType: %s.
                flowName: %s.
                period: %s - %s.
                minEventCount: %s.
                actualCount: %s.
                counterType: %s.
                filterValue: %s.
                """.formatted(
                definition.taskType(),
                definition.displayName(),
                period.getFrom(),
                period.getTo(),
                params.getMinEventCount(),
                actualCount,
                definition.counterType(),
                definition.filters()
        );
    }

    private String buildSuccessResultMessage(
            MonitoringTaskDefinition definition,
            MonitoringTaskParameter params,
            MonitoringPeriod period,
            long actualCount
    ) {
        return """
                Мониторинговая задача выполнена успешно.
                taskType: %s.
                flowName: %s.
                period: %s - %s.
                minEventCount: %s.
                actualCount: %s.
                counterType: %s.
                filterValue: %s.
                """.formatted(
                definition.taskType(),
                definition.displayName(),
                period.getFrom(),
                period.getTo(),
                params.getMinEventCount(),
                actualCount,
                definition.counterType(),
                definition.filters()
        );
    }
}
