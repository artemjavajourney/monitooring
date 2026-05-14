/**
 * ScheduledTask для ZOK_4_MONITORING_TASK.
 */
public class Zok4MonitoringTask extends MonitoringScheduledTask {

    public Zok4MonitoringTask(
            ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
    ) {
        super(getter);
    }
}
