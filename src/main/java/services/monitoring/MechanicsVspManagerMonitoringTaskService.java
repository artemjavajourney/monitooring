import org.springframework.stereotype.Service;

/**
 * 1) MECHANICS_VSP_MANAGER_MONITORING_TASK
 *
 * Проверяет количество записей в gift_task
 * с process_type = MECHANICS_VSP_MANAGER.
 */
@Service
public class MechanicsVspManagerMonitoringTaskService extends AbstractMonitoringTaskService {

    public MechanicsVspManagerMonitoringTaskService(
            MonitoringPeriodResolver periodResolver,
            MonitoringEventCounterRepository monitoringEventRepository,
            MonitoringNotificationService notificationService
    ) {
        super(periodResolver, monitoringEventRepository, notificationService);
    }

    @Override
    protected MonitoringTaskDefinition getDefinition() {
        return MonitoringTaskDefinitionRegistry.MECHANICS_VSP_MANAGER;
    }
}
