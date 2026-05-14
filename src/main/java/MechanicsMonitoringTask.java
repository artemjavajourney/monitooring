/**
 * ScheduledTask для MECHANICS_MONITORING_TASK.
 */
public class MechanicsMonitoringTask extends MonitoringScheduledTask {

    public MechanicsMonitoringTask(
            ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
    ) {
        super(getter);
    }
}
