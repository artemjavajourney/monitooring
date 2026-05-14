/**
 * ScheduledTask для CASES_MONITORING_TASK.
 */
public class CasesMonitoringTask extends MonitoringScheduledTask {

    public CasesMonitoringTask(
            ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
    ) {
        super(getter);
    }
}
