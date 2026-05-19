import com.fasterxml.jackson.core.JsonProcessingException;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

final class MonitoringTestSupport {

    private MonitoringTestSupport() {
    }

    static ScheduledTask<GiftsScheduledTaskEnum> task(
            GiftsScheduledTaskEnum taskType,
            TimeTaskType taskTimeType,
            String timeParameter,
            String params
    ) {
        return new MonitoringScheduledTask(
                new TestScheduledTaskGetter(taskType, taskTimeType, timeParameter, params)
        );
    }

    static String params(
            Integer minEventCount,
            String checkTimeFrom,
            String checkTimeTo,
            Boolean recoveryMode,
            List<String> recipients
    ) throws JsonProcessingException {
        MonitoringTaskParameter parameter = new MonitoringTaskParameter();
        parameter.setMinEventCount(minEventCount);
        parameter.setCheckTimeFrom(checkTimeFrom);
        parameter.setCheckTimeTo(checkTimeTo);
        parameter.setRecoveryMode(recoveryMode);
        parameter.setRecipients(recipients);
        return TaskService.objectMapper.writeValueAsString(parameter);
    }

    static MonitoringTaskParameter readParams(String json) throws JsonProcessingException {
        return TaskService.objectMapper.readValue(json, MonitoringTaskParameter.class);
    }

    static LocalDateTime from(MonitoringPeriod period) {
        return readPeriodDate(period, "from", "getFrom");
    }

    static LocalDateTime to(MonitoringPeriod period) {
        return readPeriodDate(period, "to", "getTo");
    }

    private static LocalDateTime readPeriodDate(MonitoringPeriod period, String recordAccessor, String getter) {
        try {
            Method method = period.getClass().getMethod(recordAccessor);
            return (LocalDateTime) method.invoke(period);
        } catch (ReflectiveOperationException ignored) {
            try {
                Method method = period.getClass().getMethod(getter);
                return (LocalDateTime) method.invoke(period);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("MonitoringPeriod must expose either " + recordAccessor + "() or " + getter + "()", e);
            }
        }
    }

    static final class TestScheduledTaskGetter implements ScheduledTaskGetter<GiftsScheduledTaskEnum> {
        private final GiftsScheduledTaskEnum taskType;
        private final TimeTaskType taskTimeType;
        private final String timeParameter;
        private final String params;

        TestScheduledTaskGetter(
                GiftsScheduledTaskEnum taskType,
                TimeTaskType taskTimeType,
                String timeParameter,
                String params
        ) {
            this.taskType = taskType;
            this.taskTimeType = taskTimeType;
            this.timeParameter = timeParameter;
            this.params = params;
        }

        @Override
        public TimeTaskType getTaskTimeType() {
            return taskTimeType;
        }

        @Override
        public GiftsScheduledTaskEnum getTaskType() {
            return taskType;
        }

        @Override
        public String getTimeParameter() {
            return timeParameter;
        }

        @Override
        public String getParams() {
            return params;
        }
    }

    static final class RecordingEmailSender implements MonitoringEmailSender {
        private final List<SentMessage> messages = new ArrayList<>();

        @Override
        public void send(List<String> recipients, String subject, String body) {
            messages.add(new SentMessage(recipients, subject, body));
        }

        int count() {
            return messages.size();
        }

        SentMessage first() {
            return messages.get(0);
        }

        SentMessage last() {
            return messages.get(messages.size() - 1);
        }
    }

    record SentMessage(List<String> recipients, String subject, String body) {
    }

    static final class StubMonitoringEventCounterRepository implements MonitoringEventCounterRepository {
        private long count;
        private MonitoringTaskDefinition lastDefinition;
        private LocalDateTime lastFrom;
        private LocalDateTime lastTo;

        StubMonitoringEventCounterRepository(long count) {
            this.count = count;
        }

        @Override
        public long count(MonitoringTaskDefinition definition, LocalDateTime from, LocalDateTime to) {
            this.lastDefinition = definition;
            this.lastFrom = from;
            this.lastTo = to;
            return count;
        }

        void setCount(long count) {
            this.count = count;
        }

        MonitoringTaskDefinition lastDefinition() {
            return lastDefinition;
        }

        LocalDateTime lastFrom() {
            return lastFrom;
        }

        LocalDateTime lastTo() {
            return lastTo;
        }
    }

    static final class TestMonitoringTaskService extends AbstractMonitoringTaskService {
        private final MonitoringTaskDefinition definition;

        TestMonitoringTaskService(
                MonitoringTaskDefinition definition,
                MonitoringPeriodResolver periodResolver,
                MonitoringEventCounterRepository monitoringEventRepository,
                MonitoringNotificationService notificationService
        ) {
            super(periodResolver, monitoringEventRepository, notificationService);
            this.definition = definition;
        }

        @Override
        protected MonitoringTaskDefinition getDefinition() {
            return definition;
        }
    }
}
