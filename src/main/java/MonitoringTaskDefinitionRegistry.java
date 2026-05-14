/**
 * Единое место, где зафиксировано соответствие:
 * task enum -> таблица -> бизнес-фильтр.
 */
public final class MonitoringTaskDefinitionRegistry {

    private MonitoringTaskDefinitionRegistry() {
    }

    public static final MonitoringTaskDefinition MECHANICS_VSP_MANAGER =
            new MonitoringTaskDefinition(
                    GiftsScheduledTaskEnum.MECHANICS_VSP_MANAGER_MONITORING_TASK,
                    "ВСП уведомления",
                    MonitoringCounterType.GIFT_TASK_BY_PROCESS_TYPE,
                    "MECHANICS_VSP_MANAGER"
            );

    public static final MonitoringTaskDefinition MECHANICS =
            new MonitoringTaskDefinition(
                    GiftsScheduledTaskEnum.MECHANICS_MONITORING_TASK,
                    "Mechanics",
                    MonitoringCounterType.CASES_FOR_GIFTS_BY_GIFT_PROCESS,
                    "MECHANICS"
            );

    public static final MonitoringTaskDefinition CASES =
            new MonitoringTaskDefinition(
                    GiftsScheduledTaskEnum.CASES_MONITORING_TASK,
                    "Cases",
                    MonitoringCounterType.CASES_FOR_GIFTS_BY_GIFT_PROCESS,
                    "CASES"
            );

    public static final MonitoringTaskDefinition NEGATIVE_CSI =
            new MonitoringTaskDefinition(
                    GiftsScheduledTaskEnum.NEGATIVE_CSI_MONITORING_TASK,
                    "Negative CSI",
                    MonitoringCounterType.CASES_FOR_GIFTS_BY_GIFT_PROCESS,
                    "NEGATIVE_CSI"
            );

    public static final MonitoringTaskDefinition ZOK_4 =
            new MonitoringTaskDefinition(
                    GiftsScheduledTaskEnum.ZOK_4_MONITORING_TASK,
                    "ZOK_4",
                    MonitoringCounterType.GIFT_TASK_ZOK_4,
                    null
            );
}
