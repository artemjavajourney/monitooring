/**
 * Обновлённый GiftsScheduledTaskEnum.
 *
 * В существующем проекте нужно добавить 5 новых enum-констант
 * в уже существующий GiftsScheduledTaskEnum.
 */
public enum GiftsScheduledTaskEnum implements ScheduledTaskEnum<GiftsScheduledTaskEnum> {

    STATISTIC_AGGREGATE_TASK {
        @Override
        public ScheduledTask<GiftsScheduledTaskEnum> getTaskInstance(
                ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
        ) {
            return new StatisticAgregateTask(getter);
        }
    },

    ARCHIVING_GIFTS_TASK {
        @Override
        public ScheduledTask<GiftsScheduledTaskEnum> getTaskInstance(
                ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
        ) {
            return new ArchivingGiftTask(getter);
        }
    },

    CHECK_RESERVED_GIFTS_TASK {
        @Override
        public ScheduledTask<GiftsScheduledTaskEnum> getTaskInstance(
                ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
        ) {
            return new CheckReservedGiftTask(getter);
        }
    },

    CLOSE_OLD_EVENT_TASK {
        @Override
        public ScheduledTask<GiftsScheduledTaskEnum> getTaskInstance(
                ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
        ) {
            return new CloseOldEventTask(getter);
        }
    },

    SEND_NOTIFICATIONS_TASK {
        @Override
        public ScheduledTask<GiftsScheduledTaskEnum> getTaskInstance(
                ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
        ) {
            return new NotificationTask(getter);
        }
    },

    /**
     * 1) Проверяет gift_task с process_type = MECHANICS_VSP_MANAGER.
     */
    MECHANICS_VSP_MANAGER_MONITORING_TASK {
        @Override
        public ScheduledTask<GiftsScheduledTaskEnum> getTaskInstance(
                ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
        ) {
            return new MechanicsVspManagerMonitoringTask(getter);
        }
    },

    /**
     * 2) Проверяет CasesForGifts с giftProcess = MECHANICS.
     */
    MECHANICS_MONITORING_TASK {
        @Override
        public ScheduledTask<GiftsScheduledTaskEnum> getTaskInstance(
                ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
        ) {
            return new MechanicsMonitoringTask(getter);
        }
    },

    /**
     * 3) Проверяет CasesForGifts с giftProcess = CASES.
     */
    CASES_MONITORING_TASK {
        @Override
        public ScheduledTask<GiftsScheduledTaskEnum> getTaskInstance(
                ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
        ) {
            return new CasesMonitoringTask(getter);
        }
    },

    /**
     * 4) Проверяет CasesForGifts с giftProcess = NEGATIVE_CSI.
     */
    NEGATIVE_CSI_MONITORING_TASK {
        @Override
        public ScheduledTask<GiftsScheduledTaskEnum> getTaskInstance(
                ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
        ) {
            return new NegativeCsiMonitoringTask(getter);
        }
    },

    /**
     * 5) Проверяет gift_task с process_type = ZOK
     * и notification_attributes.originalSystemCode = ZOK_4.
     */
    ZOK_4_MONITORING_TASK {
        @Override
        public ScheduledTask<GiftsScheduledTaskEnum> getTaskInstance(
                ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
        ) {
            return new Zok4MonitoringTask(getter);
        }
    }
}
