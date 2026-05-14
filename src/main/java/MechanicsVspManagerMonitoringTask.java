/**
 * ScheduledTask для MECHANICS_VSP_MANAGER_MONITORING_TASK.
 */
public class MechanicsVspManagerMonitoringTask extends MonitoringScheduledTask {

    public MechanicsVspManagerMonitoringTask(
            ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
    ) {
        super(getter);
    }
}
