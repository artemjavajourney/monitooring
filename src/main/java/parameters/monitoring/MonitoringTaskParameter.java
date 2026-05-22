import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
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
     * Получатели уведомлений в корпоративном чате.
     */
    private List<String> recipients;

    /**
     * Необязательная тема письма.
     * Если не указана, будет использована дефолтная тема из MonitoringNotificationService.
     */
    private String subject;

    @JsonIgnore
    public boolean isRecoveryModeEnabled() {
        return Boolean.TRUE.equals(recoveryMode);
    }
}
