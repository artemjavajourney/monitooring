import org.springframework.stereotype.Service;

/**
 * 4) NEGATIVE_CSI_MONITORING_TASK
 *
 * Проверяет количество записей в CasesForGifts
 * с giftProcess = NEGATIVE_CSI.
 */
@Service
public class NegativeCsiMonitoringTaskService extends AbstractMonitoringTaskService {

    public NegativeCsiMonitoringTaskService(
            MonitoringPeriodResolver periodResolver,
            MonitoringEventRepository monitoringEventRepository,
            MonitoringNotificationService notificationService
    ) {
        super(periodResolver, monitoringEventRepository, notificationService);
    }

    @Override
    protected MonitoringTaskDefinition getDefinition() {
        return MonitoringTaskDefinitions.NEGATIVE_CSI;
    }
}
