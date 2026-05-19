import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonitoringTaskParameterTest {

    @Test
    void serializesRecoveryModeAndDoesNotExposeDerivedRecoveryModeEnabledProperty() throws Exception {
        MonitoringTaskParameter parameter = new MonitoringTaskParameter();
        parameter.setMinEventCount(10);
        parameter.setCheckTimeFrom("08:00");
        parameter.setCheckTimeTo("09:00");
        parameter.setRecoveryMode(true);
        parameter.setRecipients(List.of("support-channel"));

        String json = TaskService.objectMapper.writeValueAsString(parameter);

        assertTrue(json.contains("\"recoveryMode\":true"));
        assertFalse(json.contains("recoveryModeEnabled"));
    }

    @Test
    void fixedDelayParametersDoNotSerializeNullCronPeriodFields() throws Exception {
        MonitoringTaskParameter parameter = new MonitoringTaskParameter();
        parameter.setMinEventCount(10);
        parameter.setRecoveryMode(false);
        parameter.setRecipients(List.of("support-channel"));

        String json = TaskService.objectMapper.writeValueAsString(parameter);

        assertFalse(json.contains("checkTimeFrom"));
        assertFalse(json.contains("checkTimeTo"));
    }

    @Test
    void deserializesUnknownRecoveryModeEnabledPropertyWithoutFailing() throws Exception {
        String json = """
                {
                  "minEventCount": 10,
                  "checkTimeFrom": "08:00",
                  "checkTimeTo": "09:00",
                  "recoveryMode": true,
                  "recoveryModeEnabled": true,
                  "recipients": ["support-channel"]
                }
                """;

        MonitoringTaskParameter parameter = TaskService.objectMapper.readValue(json, MonitoringTaskParameter.class);

        assertTrue(parameter.isRecoveryModeEnabled());
    }
}
