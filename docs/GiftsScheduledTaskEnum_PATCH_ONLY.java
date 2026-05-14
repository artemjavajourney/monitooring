/*
 * Если не хотите заменять весь GiftsScheduledTaskEnum,
 * добавьте в существующий enum только этот фрагмент.
 */

MECHANICS_VSP_MANAGER_MONITORING_TASK {
    @Override
    public ScheduledTask<GiftsScheduledTaskEnum> getTaskInstance(
            ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
    ) {
        return new MechanicsVspManagerMonitoringTask(getter);
    }
},

MECHANICS_MONITORING_TASK {
    @Override
    public ScheduledTask<GiftsScheduledTaskEnum> getTaskInstance(
            ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
    ) {
        return new MechanicsMonitoringTask(getter);
    }
},

CASES_MONITORING_TASK {
    @Override
    public ScheduledTask<GiftsScheduledTaskEnum> getTaskInstance(
            ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
    ) {
        return new CasesMonitoringTask(getter);
    }
},

NEGATIVE_CSI_MONITORING_TASK {
    @Override
    public ScheduledTask<GiftsScheduledTaskEnum> getTaskInstance(
            ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
    ) {
        return new NegativeCsiMonitoringTask(getter);
    }
},

ZOK_4_MONITORING_TASK {
    @Override
    public ScheduledTask<GiftsScheduledTaskEnum> getTaskInstance(
            ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
    ) {
        return new Zok4MonitoringTask(getter);
    }
}
