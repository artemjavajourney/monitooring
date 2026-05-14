/**
 * ScheduledTask для NEGATIVE_CSI_MONITORING_TASK.
 */
public class NegativeCsiMonitoringTask extends MonitoringScheduledTask {

    public NegativeCsiMonitoringTask(
            ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
    ) {
        super(getter);
    }
}
