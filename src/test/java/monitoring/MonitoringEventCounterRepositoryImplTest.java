import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonitoringEventCounterRepositoryImplTest {

    private MonitoringEventCounterRepositoryImpl repository;
    private EntityManager entityManager;
    private Query query;

    @BeforeEach
    void setUp() throws Exception {
        repository = new MonitoringEventCounterRepositoryImpl();
        entityManager = mock(EntityManager.class);
        query = mock(Query.class);

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(42L);

        Field field = MonitoringEventCounterRepositoryImpl.class.getDeclaredField("entityManager");
        field.setAccessible(true);
        field.set(repository, entityManager);
    }

    @Test
    void countGiftTaskByProcessTypeUsesExpectedSqlAndParameters() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 18, 8, 0);
        LocalDateTime to = LocalDateTime.of(2026, 5, 18, 9, 0);

        long count = repository.count(MonitoringTaskDefinitionRegistry.MECHANICS_VSP_MANAGER, from, to);

        assertEquals(42L, count);
        verify(entityManager).createNativeQuery(org.mockito.ArgumentMatchers.argThat(sql ->
                sql.contains("from gift_task")
                        && sql.contains("process_type = :processType")
                        && sql.contains("create_date >= :from")
                        && sql.contains("create_date < :to")
        ));
        verify(query).setParameter("processType", "MECHANICS_VSP_MANAGER");
        verify(query).setParameter("from", from);
        verify(query).setParameter("to", to);
    }

    @Test
    void countCasesForGiftsByGiftProcessUsesExpectedSqlAndParameters() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 18, 8, 0);
        LocalDateTime to = LocalDateTime.of(2026, 5, 18, 9, 0);

        long count = repository.count(MonitoringTaskDefinitionRegistry.NEGATIVE_CSI, from, to);

        assertEquals(42L, count);
        verify(entityManager).createNativeQuery(org.mockito.ArgumentMatchers.argThat(sql ->
                sql.contains("from CasesForGifts")
                        && sql.contains("giftProcess = :giftProcess")
                        && sql.contains("eventDate >= :from")
                        && sql.contains("eventDate < :to")
        ));
        verify(query).setParameter("giftProcess", "NEGATIVE_CSI");
        verify(query).setParameter("from", from);
        verify(query).setParameter("to", to);
    }

    @Test
    void countZok4UsesProcessTypeAndOriginalSystemCodeFilters() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 18, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 5, 18, 11, 0);

        long count = repository.count(MonitoringTaskDefinitionRegistry.ZOK_4, from, to);

        assertEquals(42L, count);
        verify(entityManager).createNativeQuery(org.mockito.ArgumentMatchers.argThat(sql -> {
            String normalized = sql.replaceAll("\\s+", " ");
            return normalized.contains("from gift_task")
                    && normalized.contains("process_type = :processType")
                    && normalized.contains("create_date >= :from")
                    && normalized.contains("create_date < :to")
                    && normalized.contains("notification_attributes ->> 'originalSystemCode' = :originalSystemCode");
        }));
        verify(query).setParameter("processType", "ZOK");
        verify(query).setParameter("originalSystemCode", "ZOK_4");
        verify(query).setParameter("from", from);
        verify(query).setParameter("to", to);
        assertTrue(count > 0);
    }
}
