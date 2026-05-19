import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonitoringPeriodResolverTest {

    private final MonitoringPeriodResolver resolver = new MonitoringPeriodResolver();

    @Test
    void fixedDelayPeriodIgnoresRecoveryModeAndUsesLastDelayWindow() throws Exception {
        String params = MonitoringTestSupport.params(
                10,
                null,
                null,
                true,
                List.of("support-channel")
        );
        ScheduledTask<GiftsScheduledTaskEnum> task = MonitoringTestSupport.task(
                GiftsScheduledTaskEnum.MECHANICS_VSP_MANAGER_MONITORING_TASK,
                TimeTaskType.FIXED_DELAY,
                "600",
                params
        );

        MonitoringPeriod period = resolver.resolve(task, MonitoringTestSupport.readParams(params));

        Duration actualWindow = Duration.between(
                MonitoringTestSupport.from(period),
                MonitoringTestSupport.to(period)
        );

        assertEquals(Duration.ofSeconds(600), actualWindow);
        assertTrue(MonitoringTestSupport.to(period).isAfter(MonitoringTestSupport.from(period)));
    }

    @Test
    void cronNormalPeriodUsesCheckTimeFromAndCheckTimeToForToday() throws Exception {
        String params = MonitoringTestSupport.params(
                10,
                "08:00",
                "09:00",
                false,
                List.of("support-channel")
        );
        ScheduledTask<GiftsScheduledTaskEnum> task = MonitoringTestSupport.task(
                GiftsScheduledTaskEnum.MECHANICS_MONITORING_TASK,
                TimeTaskType.CRON,
                "0 9 * * *",
                params
        );

        MonitoringPeriod period = resolver.resolve(task, MonitoringTestSupport.readParams(params));

        LocalDate today = LocalDate.now();
        assertEquals(today.atTime(8, 0), MonitoringTestSupport.from(period));
        assertEquals(today.atTime(9, 0), MonitoringTestSupport.to(period));
    }

    @Test
    void cronNormalPeriodSupportsWindowThroughMidnight() throws Exception {
        String params = MonitoringTestSupport.params(
                10,
                "23:00",
                "01:00",
                false,
                List.of("support-channel")
        );
        ScheduledTask<GiftsScheduledTaskEnum> task = MonitoringTestSupport.task(
                GiftsScheduledTaskEnum.CASES_MONITORING_TASK,
                TimeTaskType.CRON,
                "0 1 * * *",
                params
        );

        MonitoringPeriod period = resolver.resolve(task, MonitoringTestSupport.readParams(params));

        LocalDate today = LocalDate.now();
        assertEquals(today.minusDays(1).atTime(23, 0), MonitoringTestSupport.from(period));
        assertEquals(today.atTime(1, 0), MonitoringTestSupport.to(period));
    }

    @Test
    void cronRecoveryPeriodUsesLastHourAndIgnoresConfiguredCheckTimes() throws Exception {
        String params = MonitoringTestSupport.params(
                10,
                "08:00",
                "09:00",
                true,
                List.of("support-channel")
        );
        ScheduledTask<GiftsScheduledTaskEnum> task = MonitoringTestSupport.task(
                GiftsScheduledTaskEnum.ZOK_4_MONITORING_TASK,
                TimeTaskType.CRON,
                "0 9 * * *",
                params
        );

        LocalDateTime before = LocalDateTime.now();
        MonitoringPeriod period = resolver.resolve(task, MonitoringTestSupport.readParams(params));
        LocalDateTime after = LocalDateTime.now();

        assertTrue(!MonitoringTestSupport.from(period).isBefore(before.minusHours(1)));
        assertTrue(!MonitoringTestSupport.from(period).isAfter(after.minusHours(1)));
        assertTrue(!MonitoringTestSupport.to(period).isBefore(before));
        assertTrue(!MonitoringTestSupport.to(period).isAfter(after));
        assertEquals(Duration.ofHours(1), Duration.between(MonitoringTestSupport.from(period), MonitoringTestSupport.to(period)));
    }
}
