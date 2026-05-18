import java.util.EnumMap;
import java.util.Map;

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
                    Map.of(MonitoringFilterKey.PROCESS_TYPE, "MECHANICS_VSP_MANAGER")
            );

    public static final MonitoringTaskDefinition MECHANICS =
            new MonitoringTaskDefinition(
                    GiftsScheduledTaskEnum.MECHANICS_MONITORING_TASK,
                    "Mechanics",
                    MonitoringCounterType.CASES_FOR_GIFTS_BY_GIFT_PROCESS,
                    Map.of(MonitoringFilterKey.GIFT_PROCESS, "MECHANICS")
            );

    public static final MonitoringTaskDefinition CASES =
            new MonitoringTaskDefinition(
                    GiftsScheduledTaskEnum.CASES_MONITORING_TASK,
                    "Cases",
                    MonitoringCounterType.CASES_FOR_GIFTS_BY_GIFT_PROCESS,
                    Map.of(MonitoringFilterKey.GIFT_PROCESS, "CASES")
            );

    public static final MonitoringTaskDefinition NEGATIVE_CSI =
            new MonitoringTaskDefinition(
                    GiftsScheduledTaskEnum.NEGATIVE_CSI_MONITORING_TASK,
                    "Negative CSI",
                    MonitoringCounterType.CASES_FOR_GIFTS_BY_GIFT_PROCESS,
                    Map.of(MonitoringFilterKey.GIFT_PROCESS, "NEGATIVE_CSI")
            );

    public static final MonitoringTaskDefinition ZOK_4 =
            new MonitoringTaskDefinition(
                    GiftsScheduledTaskEnum.ZOK_4_MONITORING_TASK,
                    "ZOK_4",
                    MonitoringCounterType.GIFT_TASK_BY_PROCESS_TYPE_AND_ORIGINAL_SYSTEM_CODE,
                    Map.of(
                            MonitoringFilterKey.PROCESS_TYPE, "ZOK",
                            MonitoringFilterKey.ORIGINAL_SYSTEM_CODE, "ZOK_4"
                    )
            );

    private static final Map<GiftsScheduledTaskEnum, MonitoringTaskDefinition> DEFINITIONS =
            new EnumMap<>(GiftsScheduledTaskEnum.class);

    static {
        register(MECHANICS_VSP_MANAGER);
        register(MECHANICS);
        register(CASES);
        register(NEGATIVE_CSI);
        register(ZOK_4);
    }

    public static MonitoringTaskDefinition get(GiftsScheduledTaskEnum taskType) {
        MonitoringTaskDefinition definition = DEFINITIONS.get(taskType);
        if (definition == null) {
            throw new IllegalArgumentException(
                    "Monitoring task definition not found for taskType: " + taskType
            );
        }
        return definition;
    }

    private static void register(MonitoringTaskDefinition definition) {
        DEFINITIONS.put(definition.taskType(), definition);
    }
}
