import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Hibernate/JPA-реализация подсчёта событий.
 *
 * Используем EntityManager и native SQL, чтобы не зависеть от DataspaceCoreSearchClient.
 *
 * ВАЖНО:
 * имена таблиц/колонок нужно сверить с реальной схемой БД.
 */
@Slf4j
@Repository
public class MonitoringEventRepositoryImpl implements MonitoringEventRepository {

    /**
     * Таблица gift_task.
     */
    private static final String GIFT_TASK_TABLE = "gift_task";

    /**
     * Поле времени создания в gift_task.
     */
    private static final String GIFT_TASK_CREATE_DATE_COLUMN = "create_date";

    /**
     * Поле process_type в gift_task.
     */
    private static final String GIFT_TASK_PROCESS_TYPE_COLUMN = "process_type";

    /**
     * JSON/JSONB поле notification_attributes в gift_task.
     */
    private static final String GIFT_TASK_NOTIFICATION_ATTRIBUTES_COLUMN = "notification_attributes";

    /**
     * Таблица CasesForGifts.
     *
     * Если физическая таблица в БД называется иначе, например cases_for_gifts,
     * изменить значение этой константы.
     */
    private static final String CASES_FOR_GIFTS_TABLE = "CasesForGifts";

    /**
     * Поле giftProcess в CasesForGifts.
     */
    private static final String CASES_FOR_GIFTS_GIFT_PROCESS_COLUMN = "giftProcess";

    /**
     * Поле eventDate в CasesForGifts.
     *
     * По аналитике считать нужно именно по eventDate.
     */
    private static final String CASES_FOR_GIFTS_EVENT_DATE_COLUMN = "eventDate";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public long count(
            MonitoringTaskDefinition definition,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return switch (definition.counterType()) {
            case GIFT_TASK_BY_PROCESS_TYPE -> countGiftTaskByProcessType(
                    definition.requiredFilter(MonitoringFilterKey.PROCESS_TYPE),
                    from,
                    to
            );
            case CASES_FOR_GIFTS_BY_GIFT_PROCESS -> countCasesForGiftsByGiftProcess(
                    definition.requiredFilter(MonitoringFilterKey.GIFT_PROCESS),
                    from,
                    to
            );
            case GIFT_TASK_BY_PROCESS_TYPE_AND_ORIGINAL_SYSTEM_CODE -> countGiftTaskByProcessTypeAndOriginalSystemCode(
                    definition.requiredFilter(MonitoringFilterKey.PROCESS_TYPE),
                    definition.requiredFilter(MonitoringFilterKey.ORIGINAL_SYSTEM_CODE),
                    from,
                    to
            );
        };
    }

    private long countGiftTaskByProcessType(
            String processType,
            LocalDateTime from,
            LocalDateTime to
    ) {
        String sql = """
                select count(*)
                from %s
                where %s = :processType
                  and %s >= :from
                  and %s < :to
                """.formatted(
                GIFT_TASK_TABLE,
                GIFT_TASK_PROCESS_TYPE_COLUMN,
                GIFT_TASK_CREATE_DATE_COLUMN,
                GIFT_TASK_CREATE_DATE_COLUMN
        );

        Number result = (Number) entityManager.createNativeQuery(sql)
                .setParameter("processType", processType)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        return result.longValue();
    }

    private long countCasesForGiftsByGiftProcess(
            String giftProcess,
            LocalDateTime from,
            LocalDateTime to
    ) {
        String sql = """
                select count(*)
                from %s
                where %s = :giftProcess
                  and %s >= :from
                  and %s < :to
                """.formatted(
                CASES_FOR_GIFTS_TABLE,
                CASES_FOR_GIFTS_GIFT_PROCESS_COLUMN,
                CASES_FOR_GIFTS_EVENT_DATE_COLUMN,
                CASES_FOR_GIFTS_EVENT_DATE_COLUMN
        );

        Number result = (Number) entityManager.createNativeQuery(sql)
                .setParameter("giftProcess", giftProcess)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        return result.longValue();
    }

    private long countGiftTaskByProcessTypeAndOriginalSystemCode(
            String processType,
            String originalSystemCode,
            LocalDateTime from,
            LocalDateTime to
    ) {
        /*
         * Используется PostgreSQL JSON/JSONB синтаксис:
         * notification_attributes ->> 'originalSystemCode'
         *
         * Если БД не PostgreSQL, этот фрагмент нужно заменить.
         * Например, для Oracle может потребоваться JSON_VALUE(...).
         */
        String sql = """
                select count(*)
                from %s
                where %s = :processType
                  and %s >= :from
                  and %s < :to
                  and %s ->> 'originalSystemCode' = :originalSystemCode
                """.formatted(
                GIFT_TASK_TABLE,
                GIFT_TASK_PROCESS_TYPE_COLUMN,
                GIFT_TASK_CREATE_DATE_COLUMN,
                GIFT_TASK_CREATE_DATE_COLUMN,
                GIFT_TASK_NOTIFICATION_ATTRIBUTES_COLUMN
        );

        Number result = (Number) entityManager.createNativeQuery(sql)
                .setParameter("processType", processType)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("originalSystemCode", originalSystemCode)
                .getSingleResult();

        return result.longValue();
    }
}
