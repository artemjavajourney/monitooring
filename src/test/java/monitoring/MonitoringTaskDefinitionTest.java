import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MonitoringTaskDefinitionTest {

    @Test
    void requiredFilterReturnsConfiguredValue() {
        MonitoringTaskDefinition definition = new MonitoringTaskDefinition(
                GiftsScheduledTaskEnum.MECHANICS_MONITORING_TASK,
                "Mechanics",
                MonitoringCounterType.CASES_FOR_GIFTS_BY_GIFT_PROCESS,
                Map.of(MonitoringFilterKey.GIFT_PROCESS, "MECHANICS")
        );

        assertEquals("MECHANICS", definition.requiredFilter(MonitoringFilterKey.GIFT_PROCESS));
    }

    @Test
    void requiredFilterFailsWhenFilterIsMissing() {
        MonitoringTaskDefinition definition = new MonitoringTaskDefinition(
                GiftsScheduledTaskEnum.MECHANICS_MONITORING_TASK,
                "Mechanics",
                MonitoringCounterType.CASES_FOR_GIFTS_BY_GIFT_PROCESS,
                Map.of(MonitoringFilterKey.GIFT_PROCESS, "MECHANICS")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> definition.requiredFilter(MonitoringFilterKey.PROCESS_TYPE)
        );
    }

    @Test
    void constructorRejectsBlankDisplayNameAndEmptyFilters() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MonitoringTaskDefinition(
                        GiftsScheduledTaskEnum.MECHANICS_MONITORING_TASK,
                        " ",
                        MonitoringCounterType.CASES_FOR_GIFTS_BY_GIFT_PROCESS,
                        Map.of(MonitoringFilterKey.GIFT_PROCESS, "MECHANICS")
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new MonitoringTaskDefinition(
                        GiftsScheduledTaskEnum.MECHANICS_MONITORING_TASK,
                        "Mechanics",
                        MonitoringCounterType.CASES_FOR_GIFTS_BY_GIFT_PROCESS,
                        Map.of()
                )
        );
    }

    @Test
    void registryContainsAllMonitoringDefinitions() {
        assertSame(
                MonitoringTaskDefinitionRegistry.MECHANICS_VSP_MANAGER,
                MonitoringTaskDefinitionRegistry.get(GiftsScheduledTaskEnum.MECHANICS_VSP_MANAGER_MONITORING_TASK)
        );
        assertSame(
                MonitoringTaskDefinitionRegistry.MECHANICS,
                MonitoringTaskDefinitionRegistry.get(GiftsScheduledTaskEnum.MECHANICS_MONITORING_TASK)
        );
        assertSame(
                MonitoringTaskDefinitionRegistry.CASES,
                MonitoringTaskDefinitionRegistry.get(GiftsScheduledTaskEnum.CASES_MONITORING_TASK)
        );
        assertSame(
                MonitoringTaskDefinitionRegistry.NEGATIVE_CSI,
                MonitoringTaskDefinitionRegistry.get(GiftsScheduledTaskEnum.NEGATIVE_CSI_MONITORING_TASK)
        );
        assertSame(
                MonitoringTaskDefinitionRegistry.ZOK_4,
                MonitoringTaskDefinitionRegistry.get(GiftsScheduledTaskEnum.ZOK_4_MONITORING_TASK)
        );
    }

    @Test
    void registryRejectsUnknownTaskType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MonitoringTaskDefinitionRegistry.get(GiftsScheduledTaskEnum.SEND_NOTIFICATIONS_TASK)
        );
    }

    @Test
    void zok4DefinitionContainsBothRequiredFilters() {
        MonitoringTaskDefinition definition = MonitoringTaskDefinitionRegistry.ZOK_4;

        assertEquals("ZOK", definition.requiredFilter(MonitoringFilterKey.PROCESS_TYPE));
        assertEquals("ZOK_4", definition.requiredFilter(MonitoringFilterKey.ORIGINAL_SYSTEM_CODE));
        assertEquals(
                MonitoringCounterType.GIFT_TASK_BY_PROCESS_TYPE_AND_ORIGINAL_SYSTEM_CODE,
                definition.counterType()
        );
    }
}
