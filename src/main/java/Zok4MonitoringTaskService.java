import org.springframework.stereotype.Service;

/**
 * 5) ZOK_4_MONITORING_TASK
 *
 * Проверяет количество записей в gift_task с:
 * - process_type = ZOK
 * - notification_attributes.originalSystemCode = ZOK_4
 */
@Service
public class Zok4MonitoringTaskService extends AbstractMonitoringTaskService {

    public Zok4MonitoringTaskService(
            MonitoringPeriodResolver periodResolver,
            MonitoringEventRepository monitoringEventRepository,
            MonitoringNotificationService notificationService
    ) {
        super(periodResolver, monitoringEventRepository, notificationService);
    }

    @Override
    protected MonitoringTaskDefinition getDefinition() {
        return MonitoringTaskDefinitions.ZOK_4;
    }
}
