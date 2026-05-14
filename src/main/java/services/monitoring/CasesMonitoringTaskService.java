import org.springframework.stereotype.Service;

/**
 * 3) CASES_MONITORING_TASK
 *
 * Проверяет количество записей в CasesForGifts
 * с giftProcess = CASES.
 */
@Service
public class CasesMonitoringTaskService extends AbstractMonitoringTaskService {

    public CasesMonitoringTaskService(
            MonitoringPeriodResolver periodResolver,
            MonitoringEventCounterRepository monitoringEventRepository,
            MonitoringNotificationService notificationService
    ) {
        super(periodResolver, monitoringEventRepository, notificationService);
    }

    @Override
    protected MonitoringTaskDefinition getDefinition() {
        return MonitoringTaskDefinitionRegistry.CASES;
    }
}
