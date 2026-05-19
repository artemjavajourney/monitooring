import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractMonitoringTaskServiceTest {

    private final MonitoringPeriodResolver periodResolver = new MonitoringPeriodResolver();

    @Test
    void fixedDelayProblemSendsAlertAndDoesNotEnableRecoveryMode() throws Exception {
        MonitoringTestSupport.StubMonitoringEventCounterRepository repository =
                new MonitoringTestSupport.StubMonitoringEventCounterRepository(5);
        MonitoringTestSupport.RecordingEmailSender emailSender = new MonitoringTestSupport.RecordingEmailSender();
        MonitoringNotificationService notificationService = new MonitoringNotificationService(emailSender);
        MonitoringTestSupport.TestMonitoringTaskService service = new MonitoringTestSupport.TestMonitoringTaskService(
                MonitoringTaskDefinitionRegistry.MECHANICS_VSP_MANAGER,
                periodResolver,
                repository,
                notificationService
        );
        ScheduledTask<GiftsScheduledTaskEnum> task = MonitoringTestSupport.task(
                GiftsScheduledTaskEnum.MECHANICS_VSP_MANAGER_MONITORING_TASK,
                TimeTaskType.FIXED_DELAY,
                "600",
                MonitoringTestSupport.params(10, null, null, false, List.of("support-channel"))
        );

        ExecutionResultModel<GiftsScheduledTaskEnum> result = service.run(task);
        MonitoringTaskParameter resultParams = MonitoringTestSupport.readParams(result.getParams());

        assertEquals(ExecutionTaskResultStatus.SUCCESS, result.getResult());
        assertEquals(GiftsScheduledTaskEnum.MECHANICS_VSP_MANAGER_MONITORING_TASK, result.getTaskType());
        assertFalse(resultParams.isRecoveryModeEnabled());
        assertEquals(1, emailSender.count());
        assertTrue(emailSender.first().body().contains("actualCount"));
        assertSame(MonitoringTaskDefinitionRegistry.MECHANICS_VSP_MANAGER, repository.lastDefinition());
        assertEquals(Duration.ofSeconds(600), Duration.between(repository.lastFrom(), repository.lastTo()));
    }

    @Test
    void fixedDelayHealthyFlowDoesNotSendAlertOrRecovery() throws Exception {
        MonitoringTestSupport.StubMonitoringEventCounterRepository repository =
                new MonitoringTestSupport.StubMonitoringEventCounterRepository(11);
        MonitoringTestSupport.RecordingEmailSender emailSender = new MonitoringTestSupport.RecordingEmailSender();
        MonitoringTestSupport.TestMonitoringTaskService service = new MonitoringTestSupport.TestMonitoringTaskService(
                MonitoringTaskDefinitionRegistry.NEGATIVE_CSI,
                periodResolver,
                repository,
                new MonitoringNotificationService(emailSender)
        );
        ScheduledTask<GiftsScheduledTaskEnum> task = MonitoringTestSupport.task(
                GiftsScheduledTaskEnum.NEGATIVE_CSI_MONITORING_TASK,
                TimeTaskType.FIXED_DELAY,
                "600",
                MonitoringTestSupport.params(10, null, null, true, List.of("support-channel"))
        );

        ExecutionResultModel<GiftsScheduledTaskEnum> result = service.run(task);
        MonitoringTaskParameter resultParams = MonitoringTestSupport.readParams(result.getParams());

        assertEquals(ExecutionTaskResultStatus.SUCCESS, result.getResult());
        assertTrue(resultParams.isRecoveryModeEnabled(), "FIXED_DELAY does not manage recoveryMode state");
        assertEquals(0, emailSender.count());
    }

    @Test
    void cronNormalProblemAtEqualThresholdSendsAlertAndEnablesRecoveryMode() throws Exception {
        MonitoringTestSupport.StubMonitoringEventCounterRepository repository =
                new MonitoringTestSupport.StubMonitoringEventCounterRepository(10);
        MonitoringTestSupport.RecordingEmailSender emailSender = new MonitoringTestSupport.RecordingEmailSender();
        MonitoringTestSupport.TestMonitoringTaskService service = new MonitoringTestSupport.TestMonitoringTaskService(
                MonitoringTaskDefinitionRegistry.MECHANICS,
                periodResolver,
                repository,
                new MonitoringNotificationService(emailSender)
        );
        ScheduledTask<GiftsScheduledTaskEnum> task = MonitoringTestSupport.task(
                GiftsScheduledTaskEnum.MECHANICS_MONITORING_TASK,
                TimeTaskType.CRON,
                "0 9 * * *",
                MonitoringTestSupport.params(10, "08:00", "09:00", false, List.of("support-channel"))
        );

        ExecutionResultModel<GiftsScheduledTaskEnum> result = service.run(task);
        MonitoringTaskParameter resultParams = MonitoringTestSupport.readParams(result.getParams());

        assertEquals(ExecutionTaskResultStatus.SUCCESS, result.getResult());
        assertTrue(resultParams.isRecoveryModeEnabled());
        assertEquals(1, emailSender.count());
        assertTrue(emailSender.first().subject().contains("Mechanics"));
        assertSame(MonitoringTaskDefinitionRegistry.MECHANICS, repository.lastDefinition());
    }

    @Test
    void cronRecoveryProblemKeepsRecoveryModeAndSendsAlertAgain() throws Exception {
        MonitoringTestSupport.StubMonitoringEventCounterRepository repository =
                new MonitoringTestSupport.StubMonitoringEventCounterRepository(0);
        MonitoringTestSupport.RecordingEmailSender emailSender = new MonitoringTestSupport.RecordingEmailSender();
        MonitoringTestSupport.TestMonitoringTaskService service = new MonitoringTestSupport.TestMonitoringTaskService(
                MonitoringTaskDefinitionRegistry.CASES,
                periodResolver,
                repository,
                new MonitoringNotificationService(emailSender)
        );
        ScheduledTask<GiftsScheduledTaskEnum> task = MonitoringTestSupport.task(
                GiftsScheduledTaskEnum.CASES_MONITORING_TASK,
                TimeTaskType.CRON,
                "0 9 * * *",
                MonitoringTestSupport.params(10, "08:00", "09:00", true, List.of("support-channel"))
        );

        ExecutionResultModel<GiftsScheduledTaskEnum> result = service.run(task);
        MonitoringTaskParameter resultParams = MonitoringTestSupport.readParams(result.getParams());

        assertEquals(ExecutionTaskResultStatus.SUCCESS, result.getResult());
        assertTrue(resultParams.isRecoveryModeEnabled());
        assertEquals(1, emailSender.count());
        assertTrue(emailSender.first().subject().contains("Проблема"));
        assertEquals(Duration.ofHours(1), Duration.between(repository.lastFrom(), repository.lastTo()));
    }

    @Test
    void cronRecoveryHealthyFlowSendsRecoveryAndDisablesRecoveryMode() throws Exception {
        MonitoringTestSupport.StubMonitoringEventCounterRepository repository =
                new MonitoringTestSupport.StubMonitoringEventCounterRepository(11);
        MonitoringTestSupport.RecordingEmailSender emailSender = new MonitoringTestSupport.RecordingEmailSender();
        MonitoringTestSupport.TestMonitoringTaskService service = new MonitoringTestSupport.TestMonitoringTaskService(
                MonitoringTaskDefinitionRegistry.ZOK_4,
                periodResolver,
                repository,
                new MonitoringNotificationService(emailSender)
        );
        ScheduledTask<GiftsScheduledTaskEnum> task = MonitoringTestSupport.task(
                GiftsScheduledTaskEnum.ZOK_4_MONITORING_TASK,
                TimeTaskType.CRON,
                "0 9 * * *",
                MonitoringTestSupport.params(10, "08:00", "09:00", true, List.of("support-channel"))
        );

        ExecutionResultModel<GiftsScheduledTaskEnum> result = service.run(task);
        MonitoringTaskParameter resultParams = MonitoringTestSupport.readParams(result.getParams());

        assertEquals(ExecutionTaskResultStatus.SUCCESS, result.getResult());
        assertFalse(resultParams.isRecoveryModeEnabled());
        assertEquals(1, emailSender.count());
        assertTrue(emailSender.first().subject().contains("Восстановление"));
        assertSame(MonitoringTaskDefinitionRegistry.ZOK_4, repository.lastDefinition());
    }

    @Test
    void cronNormalHealthyFlowDoesNotSendRecoveryAndKeepsRecoveryModeDisabled() throws Exception {
        MonitoringTestSupport.StubMonitoringEventCounterRepository repository =
                new MonitoringTestSupport.StubMonitoringEventCounterRepository(11);
        MonitoringTestSupport.RecordingEmailSender emailSender = new MonitoringTestSupport.RecordingEmailSender();
        MonitoringTestSupport.TestMonitoringTaskService service = new MonitoringTestSupport.TestMonitoringTaskService(
                MonitoringTaskDefinitionRegistry.ZOK_4,
                periodResolver,
                repository,
                new MonitoringNotificationService(emailSender)
        );
        ScheduledTask<GiftsScheduledTaskEnum> task = MonitoringTestSupport.task(
                GiftsScheduledTaskEnum.ZOK_4_MONITORING_TASK,
                TimeTaskType.CRON,
                "0 9 * * *",
                MonitoringTestSupport.params(10, "08:00", "09:00", false, List.of("support-channel"))
        );

        ExecutionResultModel<GiftsScheduledTaskEnum> result = service.run(task);
        MonitoringTaskParameter resultParams = MonitoringTestSupport.readParams(result.getParams());

        assertEquals(ExecutionTaskResultStatus.SUCCESS, result.getResult());
        assertFalse(resultParams.isRecoveryModeEnabled());
        assertEquals(0, emailSender.count());
    }

    @Test
    void invalidJsonParametersFailFast() {
        MonitoringTestSupport.StubMonitoringEventCounterRepository repository =
                new MonitoringTestSupport.StubMonitoringEventCounterRepository(11);
        MonitoringTestSupport.TestMonitoringTaskService service = new MonitoringTestSupport.TestMonitoringTaskService(
                MonitoringTaskDefinitionRegistry.MECHANICS,
                periodResolver,
                repository,
                new MonitoringNotificationService(new MonitoringTestSupport.RecordingEmailSender())
        );
        ScheduledTask<GiftsScheduledTaskEnum> task = MonitoringTestSupport.task(
                GiftsScheduledTaskEnum.MECHANICS_MONITORING_TASK,
                TimeTaskType.CRON,
                "0 9 * * *",
                "invalid-json"
        );

        assertThrows(IllegalArgumentException.class, () -> service.run(task));
    }

    @Test
    void cronTaskRequiresCheckTimeFromAndCheckTimeTo() throws Exception {
        MonitoringTestSupport.StubMonitoringEventCounterRepository repository =
                new MonitoringTestSupport.StubMonitoringEventCounterRepository(11);
        MonitoringTestSupport.TestMonitoringTaskService service = new MonitoringTestSupport.TestMonitoringTaskService(
                MonitoringTaskDefinitionRegistry.MECHANICS,
                periodResolver,
                repository,
                new MonitoringNotificationService(new MonitoringTestSupport.RecordingEmailSender())
        );
        ScheduledTask<GiftsScheduledTaskEnum> task = MonitoringTestSupport.task(
                GiftsScheduledTaskEnum.MECHANICS_MONITORING_TASK,
                TimeTaskType.CRON,
                "0 9 * * *",
                MonitoringTestSupport.params(10, null, "09:00", false, List.of("support-channel"))
        );

        assertThrows(IllegalArgumentException.class, () -> service.run(task));
    }

    @Test
    void recipientsAreRequired() throws Exception {
        MonitoringTestSupport.StubMonitoringEventCounterRepository repository =
                new MonitoringTestSupport.StubMonitoringEventCounterRepository(11);
        MonitoringTestSupport.TestMonitoringTaskService service = new MonitoringTestSupport.TestMonitoringTaskService(
                MonitoringTaskDefinitionRegistry.MECHANICS,
                periodResolver,
                repository,
                new MonitoringNotificationService(new MonitoringTestSupport.RecordingEmailSender())
        );
        ScheduledTask<GiftsScheduledTaskEnum> task = MonitoringTestSupport.task(
                GiftsScheduledTaskEnum.MECHANICS_MONITORING_TASK,
                TimeTaskType.FIXED_DELAY,
                "600",
                MonitoringTestSupport.params(10, null, null, false, List.of())
        );

        assertThrows(IllegalArgumentException.class, () -> service.run(task));
    }
}
