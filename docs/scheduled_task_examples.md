# Примеры записей scheduledTask

Ниже логические примеры. Конкретный способ создания записей зависит от вашей схемы Dataspace/БД.

## Общие поля

```text
objectId          = имя enum-константы GiftsScheduledTaskEnum
taskType          = FIXED_DELAY или CRON
timeParameter     = delay в секундах или CRON-маска
parameters        = JSON с MonitoringTaskParameter
nextExecutionTime = ближайшее время запуска
isActive          = true
status            = SUCCESS
```

## Пример parameters для CRON

```json
{
  "minEventCount": 10,
  "checkTimeFrom": "08:00",
  "checkTimeTo": "09:00",
  "recoveryMode": false,
  "recipients": ["support@example.com"]
}
```

## Пример parameters для FIXED_DELAY

```json
{
  "minEventCount": 10,
  "recoveryMode": false,
  "recipients": ["support@example.com"]
}
```

## 1. MECHANICS_VSP_MANAGER_MONITORING_TASK

```json
{
  "objectId": "MECHANICS_VSP_MANAGER_MONITORING_TASK",
  "taskType": "CRON",
  "timeParameter": "0 9 * * *",
  "parameters": {
    "minEventCount": 10,
    "checkTimeFrom": "08:00",
    "checkTimeTo": "09:00",
    "recoveryMode": false,
    "recipients": ["support@example.com"]
  },
  "nextExecutionTime": "2026-05-14T09:00:00",
  "isActive": true,
  "status": "SUCCESS"
}
```

## 2. MECHANICS_MONITORING_TASK

```json
{
  "objectId": "MECHANICS_MONITORING_TASK",
  "taskType": "CRON",
  "timeParameter": "0 9 * * *",
  "parameters": {
    "minEventCount": 10,
    "checkTimeFrom": "08:00",
    "checkTimeTo": "09:00",
    "recoveryMode": false,
    "recipients": ["support@example.com"]
  },
  "nextExecutionTime": "2026-05-14T09:00:00",
  "isActive": true,
  "status": "SUCCESS"
}
```

## 3. CASES_MONITORING_TASK

```json
{
  "objectId": "CASES_MONITORING_TASK",
  "taskType": "CRON",
  "timeParameter": "0 9 * * *",
  "parameters": {
    "minEventCount": 10,
    "checkTimeFrom": "08:00",
    "checkTimeTo": "09:00",
    "recoveryMode": false,
    "recipients": ["support@example.com"]
  },
  "nextExecutionTime": "2026-05-14T09:00:00",
  "isActive": true,
  "status": "SUCCESS"
}
```

## 4. NEGATIVE_CSI_MONITORING_TASK

```json
{
  "objectId": "NEGATIVE_CSI_MONITORING_TASK",
  "taskType": "CRON",
  "timeParameter": "0 9 * * *",
  "parameters": {
    "minEventCount": 10,
    "checkTimeFrom": "08:00",
    "checkTimeTo": "09:00",
    "recoveryMode": false,
    "recipients": ["support@example.com"]
  },
  "nextExecutionTime": "2026-05-14T09:00:00",
  "isActive": true,
  "status": "SUCCESS"
}
```

## 5. ZOK_4_MONITORING_TASK

```json
{
  "objectId": "ZOK_4_MONITORING_TASK",
  "taskType": "CRON",
  "timeParameter": "0 9 * * *",
  "parameters": {
    "minEventCount": 10,
    "checkTimeFrom": "08:00",
    "checkTimeTo": "09:00",
    "recoveryMode": false,
    "recipients": ["support@example.com"]
  },
  "nextExecutionTime": "2026-05-14T09:00:00",
  "isActive": true,
  "status": "SUCCESS"
}
```
