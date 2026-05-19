import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonitoringScheduledTaskTest {

    @Test
    void errorResultKeepsRetryNextExecutionTime() throws Exception {
        ScheduledTask<GiftsScheduledTaskEnum> task = MonitoringTestSupport.task(
                GiftsScheduledTaskEnum.MECHANICS_MONITORING_TASK,
                TimeTaskType.CRON,
                "0 9 * * *",
                MonitoringTestSupport.params(10, "08:00", "09:00", false, List.of("support-channel"))
        );
        ExecutionResultModel<GiftsScheduledTaskEnum> result = new ExecutionResultModel<>();
        LocalDateTime retryTime = LocalDateTime.now().plusMinutes(5);
        result.setResult(ExecutionTaskResultStatus.ERROR);
        result.setNextExecutionTime(retryTime);

        assertEquals(retryTime, task.calculateNextExecutionTime(result));
    }

    @Test
    void fixedDelayUsesStandardFixedDelayCalculationEvenWhenRecoveryModeIsTrue() throws Exception {
        ScheduledTask<GiftsScheduledTaskEnum> task = MonitoringTestSupport.task(
                GiftsScheduledTaskEnum.MECHANICS_VSP_MANAGER_MONITORING_TASK,
                TimeTaskType.FIXED_DELAY,
                "600",
                MonitoringTestSupport.params(10, null, null, true, List.of("support-channel"))
        );
        ExecutionResultModel<GiftsScheduledTaskEnum> result = new ExecutionResultModel<>();
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 18, 10, 0);
        result.setResult(ExecutionTaskResultStatus.SUCCESS);
        result.setStartTime(startTime);
        result.setExecutionDuration(2_000L);
        result.setParams(task.getParams());

        assertEquals(startTime.plusSeconds(602), task.calculateNextExecutionTime(result));
    }

    @Test
    void cronRecoverySchedulesOneHourAfterStartTime() throws Exception {
        ScheduledTask<GiftsScheduledTaskEnum> task = MonitoringTestSupport.task(
                GiftsScheduledTaskEnum.CASES_MONITORING_TASK,
                TimeTaskType.CRON,
                "0 9 * * *",
                MonitoringTestSupport.params(10, "08:00", "09:00", false, List.of("support-channel"))
        );
        ExecutionResultModel<GiftsScheduledTaskEnum> result = new ExecutionResultModel<>();
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 18, 10, 0);
        result.setResult(ExecutionTaskResultStatus.SUCCESS);
        result.setStartTime(startTime);
        result.setExecutionDuration(500L);
        result.setParams(MonitoringTestSupport.params(10, "08:00", "09:00", true, List.of("support-channel")));

        assertEquals(startTime.plusHours(1), task.calculateNextExecutionTime(result));
    }

    @Test
    void cronRecoveryFallsBackToNowWhenStartTimeIsMissing() throws Exception {
        ScheduledTask<GiftsScheduledTaskEnum> task = MonitoringTestSupport.task(
                GiftsScheduledTaskEnum.CASES_MONITORING_TASK,
                TimeTaskType.CRON,
                "0 9 * * *",
                MonitoringTestSupport.params(10, "08:00", "09:00", false, List.of("support-channel"))
        );
        ExecutionResultModel<GiftsScheduledTaskEnum> result = new ExecutionResultModel<>();
        result.setResult(ExecutionTaskResultStatus.SUCCESS);
        result.setParams(MonitoringTestSupport.params(10, "08:00", "09:00", true, List.of("support-channel")));

        LocalDateTime before = LocalDateTime.now().plusHours(1);
        LocalDateTime nextExecutionTime = task.calculateNextExecutionTime(result);
        LocalDateTime after = LocalDateTime.now().plusHours(1);

        assertTrue(!nextExecutionTime.isBefore(before));
        assertTrue(!nextExecutionTime.isAfter(after));
    }

    @Test
    void cronNormalModeUsesStandardCronSchedule() throws Exception {
        ScheduledTask<GiftsScheduledTaskEnum> task = MonitoringTestSupport.task(
                GiftsScheduledTaskEnum.ZOK_4_MONITORING_TASK,
                TimeTaskType.CRON,
                "0 9 * * *",
                MonitoringTestSupport.params(10, "08:00", "09:00", false, List.of("support-channel"))
        );
        ExecutionResultModel<GiftsScheduledTaskEnum> result = new ExecutionResultModel<>();
        result.setResult(ExecutionTaskResultStatus.SUCCESS);
        result.setStartTime(LocalDateTime.now());
        result.setExecutionDuration(0L);
        result.setParams(MonitoringTestSupport.params(10, "08:00", "09:00", false, List.of("support-channel")));

        LocalDateTime before = LocalDateTime.now();
        LocalDateTime nextExecutionTime = task.calculateNextExecutionTime(result);

        assertTrue(nextExecutionTime.isAfter(before));
        assertTrue(Duration.between(before, nextExecutionTime).toDays() <= 366);
    }

    @Test
    void invalidResultParamsFallBackToStandardCronSchedule() throws Exception {
        ScheduledTask<GiftsScheduledTaskEnum> task = MonitoringTestSupport.task(
                GiftsScheduledTaskEnum.ZOK_4_MONITORING_TASK,
                TimeTaskType.CRON,
                "0 9 * * *",
                MonitoringTestSupport.params(10, "08:00", "09:00", false, List.of("support-channel"))
        );
        ExecutionResultModel<GiftsScheduledTaskEnum> result = new ExecutionResultModel<>();
        result.setResult(ExecutionTaskResultStatus.SUCCESS);
        result.setStartTime(LocalDateTime.of(2026, 5, 18, 10, 0));
        result.setExecutionDuration(0L);
        result.setParams("not-json");

        LocalDateTime nextExecutionTime = task.calculateNextExecutionTime(result);

        assertTrue(nextExecutionTime.isAfter(LocalDateTime.now()));
    }
}
