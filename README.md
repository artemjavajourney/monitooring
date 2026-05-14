# Monitoring scheduler code package

Этот ZIP содержит финализированный вариант новых классов для пяти мониторинговых задач планировщика:

1. `MECHANICS_VSP_MANAGER_MONITORING_TASK`  
   Проверяет количество записей в `gift_task` с `process_type = MECHANICS_VSP_MANAGER`.

2. `MECHANICS_MONITORING_TASK`  
   Проверяет количество записей в `CasesForGifts` с `giftProcess = MECHANICS`.

3. `CASES_MONITORING_TASK`  
   Проверяет количество записей в `CasesForGifts` с `giftProcess = CASES`.

4. `NEGATIVE_CSI_MONITORING_TASK`  
   Проверяет количество записей в `CasesForGifts` с `giftProcess = NEGATIVE_CSI`.

5. `ZOK_4_MONITORING_TASK`  
   Проверяет количество записей в `gift_task` с:
   - `process_type = ZOK`
   - `notification_attributes.originalSystemCode = ZOK_4`

## Как встроить

В загруженной кодовой базе package/imports не были видны, поэтому классы в ZIP оставлены без `package`.
Практический вариант интеграции:

1. Поместить классы в тот же package, где находятся существующие классы планировщика:
   - `ScheduledTask`
   - `BaseTaskService`
   - `GiftsScheduledTaskEnum`
   - `ExecutionResultModel`
   - `ExecutionTaskResultStatus`
   - `TimeTaskType`

2. Если классы будут лежать в другом package — добавить package/imports согласно структуре проекта.

3. В существующий `GiftsScheduledTaskEnum` добавить 5 новых enum-констант из файла `GiftsScheduledTaskEnum.java`.

4. Проверить SQL в `MonitoringEventRepositoryImpl`:
   - имена таблиц;
   - имена колонок;
   - JSON-синтаксис для `notification_attributes.originalSystemCode`.

По умолчанию для `ZOK_4` используется PostgreSQL JSON/JSONB синтаксис:

```sql
notification_attributes ->> 'originalSystemCode' = :originalSystemCode
```

Если БД не PostgreSQL, нужно заменить этот фрагмент на синтаксис вашей БД.

## Поведение

### FIXED_DELAY

Период проверки:

```text
now - timeParameter seconds -> now
```

Если:

```text
actualCount <= minEventCount
```

отправляется alert.

Recovery-письмо для `FIXED_DELAY` не отправляется.

### CRON обычный режим

Период проверки берётся из параметров:

```json
{
  "checkTimeFrom": "08:00",
  "checkTimeTo": "09:00"
}
```

Если `actualCount <= minEventCount`:

```text
alert
recoveryMode = true
nextExecutionTime = startTime + 1 hour
```

### CRON recovery-режим

Период проверки:

```text
now - 1 hour -> now
```

Если поток восстановился:

```text
actualCount > minEventCount
```

отправляется recovery-письмо, `recoveryMode = false`, следующий запуск возвращается к обычному CRON.

## Пример parameters для scheduledTask

```json
{
  "minEventCount": 10,
  "checkTimeFrom": "08:00",
  "checkTimeTo": "09:00",
  "recoveryMode": false,
  "recipients": ["support@example.com"]
}
```

Для `FIXED_DELAY` поля `checkTimeFrom` и `checkTimeTo` не обязательны.
