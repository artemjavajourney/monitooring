import org.springframework.stereotype.Service;

/**
 * 2) MECHANICS_MONITORING_TASK
 *
 * Проверяет количество записей в CasesForGifts
 * с giftProcess = MECHANICS.
 */
@Service
public class MechanicsMonitoringTaskService extends AbstractMonitoringTaskService {

    public MechanicsMonitoringTaskService(
            MonitoringPeriodResolver periodResolver,
            MonitoringEventRepository monitoringEventRepository,
            MonitoringNotificationService notificationService
    ) {
        super(periodResolver, monitoringEventRepository, notificationService);
    }

    @Override
    protected MonitoringTaskDefinition getDefinition() {
        return MonitoringTaskDefinitions.MECHANICS;
    }
}
