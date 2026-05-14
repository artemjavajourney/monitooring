/**
 * Тип источника данных, который нужно считать для конкретной мониторинговой задачи.
 */
public enum MonitoringCounterType {

    /**
     * Таблица gift_task, фильтр по process_type.
     */
    GIFT_TASK_BY_PROCESS_TYPE,

    /**
     * Таблица CasesForGifts, фильтр по giftProcess.
     */
    CASES_FOR_GIFTS_BY_GIFT_PROCESS,

    /**
     * Таблица gift_task, фильтр:
     * process_type = ZOK
     * notification_attributes.originalSystemCode = ZOK_4
     */
    GIFT_TASK_ZOK_4
}
