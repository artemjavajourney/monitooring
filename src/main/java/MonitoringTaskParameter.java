import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Общие параметры мониторинговой задачи.
 *
 * Хранятся в scheduledTask.parameters в JSON-формате.
 */
@Getter
@Setter
public class MonitoringTaskParameter {

    /**
     * Минимально допустимое количество событий.
     *
     * Важно: actualCount == minEventCount тоже считается проблемой.
     */
    private Integer minEventCount;

    /**
     * Начало бизнес-периода для CRON-режима.
     *
     * Пример: "08:00".
     * Для FIXED_DELAY не используется.
     */
    private String checkTimeFrom;

    /**
     * Конец бизнес-периода для CRON-режима.
     *
     * Пример: "09:00".
     * Для FIXED_DELAY не используется.
     */
    private String checkTimeTo;

    /**
     * Признак recovery-режима.
     *
     * true  - задача уже обнаружила проблему и должна проверять поток каждый час.
     * false - обычный режим.
     */
    private Boolean recoveryMode = false;

    /**
     * Получатели SMTP-уведомлений.
     */
    private List<String> recipients;

    /**
     * Необязательная тема письма.
     * Если не указана, будет использована дефолтная тема из MonitoringNotificationService.
     */
    private String subject;

    public boolean isRecoveryModeEnabled() {
        return Boolean.TRUE.equals(recoveryMode);
    }
}
