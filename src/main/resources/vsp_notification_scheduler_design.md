# Техническое соглашение по реализации monitoring-шедулеров проверки потока уведомлений

## 1. Цель документа

Этот документ фиксирует актуальную договорённость по реализации monitoring-шедулеров на базе существующего механизма `TaskManager`.

Документ заменяет прежнюю модель, в которой рассматривалась одна задача проверки потока «ВСП уведомления». По итоговой договорённости нужно реализовать не одну, а пять отдельных monitoring-задач, каждая из которых проверяет свой источник данных и свой тип записей.

Документ предназначен для использования как технический источник перед разработкой и code review.

---

## 2. Итоговая бизнес-задача

Нужно реализовать 5 задач планировщика, которые регулярно проверяют, поступают ли записи в нужные таблицы за заданный период времени.

Каждая задача должна:

1. Определить период проверки.
2. Посчитать количество записей в своей таблице по своему фильтру.
3. Сравнить фактическое количество с `minEventCount`.
4. Если записей меньше или равно порогу — отправить alert-уведомление.
5. Для `CRON`-режима поддержать recovery-режим.
6. Сохранить обновлённые параметры и следующий запуск в `scheduledTask`.

Условие проблемы единое для всех пяти задач:

```java
actualCount <= minEventCount
```

Важно: равенство порогу тоже считается проблемой.

Пример:

```text
minEventCount = 10
actualCount = 10
```

Это проблема, alert нужно отправить.

---

## 3. Список новых шедулеров

Нужно добавить 5 новых задач в `GiftsScheduledTaskEnum`.

| Задача | Таблица | Фильтр | Поле даты |
|---|---|---|---|
| `MECHANICS_VSP_MANAGER_MONITORING_TASK` | `gift_task` | `process_type = MECHANICS_VSP_MANAGER` | `create_date` |
| `MECHANICS_MONITORING_TASK` | `CasesForGifts` | `giftProcess = MECHANICS` | `eventDate` |
| `CASES_MONITORING_TASK` | `CasesForGifts` | `giftProcess = CASES` | `eventDate` |
| `NEGATIVE_CSI_MONITORING_TASK` | `CasesForGifts` | `giftProcess = NEGATIVE_CSI` | `eventDate` |
| `ZOK_4_MONITORING_TASK` | `gift_task` | `process_type = ZOK` и `notification_attributes.originalSystemCode = ZOK_4` | `create_date` |

Принципиальное решение:

```text
один шедулер → один источник данных → один actualCount
```

Не нужно считать сумму `gift_task + CasesForGifts` внутри одной задачи.

---

## 4. Существующий механизм TaskManager

В проекте уже есть общий механизм планировщика задач.

Новые отдельные `@Scheduled`-методы писать не нужно.

Существующая цепочка работает так:

```text
TaskManagerAbs
    ↓
каждые N секунд ищет задачу в scheduledTask
    ↓
ScheduledTaskRepositoryImpl
    ↓
если nextExecutionTime <= now — задача готова
    ↓
задача блокируется статусом IN_PROGRESS
    ↓
создаётся объект ScheduledTask
    ↓
через TaskServiceFactory находится нужный TaskService
    ↓
выполняется бизнес-логика задачи
    ↓
рассчитывается новый nextExecutionTime
    ↓
результат сохраняется обратно в scheduledTask
```

Главный технический scheduler уже находится в `TaskManagerAbs`:

```java
@Scheduled(
    initialDelay = 10000L,
    fixedRateString = "${gifts.slave.task.manager.checkRate:30}000"
)
public void findAndExecuteTask() {
    ...
}
```

По умолчанию приложение каждые `checkRate` секунд ищет задачу, которую пора выполнить.

---

## 5. Что такое запись в scheduledTask

Одна запись в `scheduledTask` — это одна business-задача планировщика.

Так как теперь задач пять, в `scheduledTask` должно быть пять отдельных записей.

Общий вид записи:

```text
objectId = имя задачи из GiftsScheduledTaskEnum
taskType = CRON или FIXED_DELAY
timeParameter = cron-маска или delay в секундах
parameters = JSON с бизнес-параметрами
nextExecutionTime = время ближайшего запуска
isActive = true
status = SUCCESS / ERROR / IN_PROGRESS
```

Важно различать поля:

```text
objectId          — имя задачи из GiftsScheduledTaskEnum
taskType          — тип расписания: CRON или FIXED_DELAY
timeParameter     — cron-маска или задержка в секундах
parameters        — бизнес-параметры задачи
nextExecutionTime — конкретное время следующего запуска
status            — статус последнего выполнения / блокировки
```

---

## 6. Как TaskManager выбирает задачу

Задача выбирается из `scheduledTask`, если выполняются условия:

```text
isActive = true
nextExecutionTime <= now
status != IN_PROGRESS
```

Также существует механизм подхвата зависших задач:

```text
status = IN_PROGRESS
и nextExecutionTime <= now - 30 минут
```

Это нужно на случай, если задача была заблокирована, приложение упало, и статус остался `IN_PROGRESS`.

После выбора задача блокируется:

```text
status = IN_PROGRESS
```

Блокировка выполняется через `aggregateVersion`, то есть используется optimistic locking. Это защищает от двойного запуска одной задачи несколькими pod-ами.

---

## 7. Как TaskManager запускает бизнес-логику

После получения задачи из БД:

1. `ScheduledTaskRepositoryImpl` строит `TaskPropertiesModel`.
2. Через `GiftsScheduledTaskEnum.getTaskInstance(...)` создаётся конкретный `ScheduledTask`.
3. `TaskManagerAbs` через `TaskServiceFactory` находит нужный `TaskService`.
4. Вызывается:

```java
service.execute(task)
```

5. Внутри `execute(...)` вызывается бизнесовый метод:

```java
run(task)
```

6. Если в `run(...)` возникает исключение, оно превращается в `ExecutionResultModel` со статусом `ERROR`.

---

## 8. Как рассчитывается nextExecutionTime

После выполнения сервиса `TaskManagerAbs` всегда делает:

```java
executionResult.setNextExecutionTime(task.calculateNextExecutionTime(executionResult));
```

Это важный момент.

Сервис задачи не должен рассчитывать финальный `nextExecutionTime`, потому что значение всё равно будет перезаписано через `task.calculateNextExecutionTime(...)`.

Базовый `ScheduledTask` считает следующий запуск так:

```java
public LocalDateTime calculateNextExecutionTime(ExecutionResultModel<T> resultModel) {
    return this.propsModel.getTaskTimeType()
            .getNextExecutionTime(resultModel, this.propsModel.getTimeParameter());
}
```

То есть стандартно:

```text
FIXED_DELAY → FixedDelayTimeResolver
CRON        → CronTimeResolver
```

Для новых monitoring-задач требуется специальная логика только для режима:

```text
CRON + recoveryMode = true
```

Поэтому создаётся общий базовый класс:

```java
MonitoringScheduledTask extends ScheduledTask<GiftsScheduledTaskEnum>
```

Он переопределяет `calculateNextExecutionTime(...)` для всех пяти monitoring-задач.

---

## 9. Поведение FIXED_DELAY

Для `FIXED_DELAY` в `timeParameter` хранится количество секунд.

Пример:

```text
timeParameter = 600
```

Это означает 10 минут.

Период проверки считается так:

```text
from = currentTime - timeParameter seconds
to = currentTime
```

Пример:

```text
currentTime = 10:00
timeParameter = 600 секунд

период проверки:
09:50 → 10:00
```

Через 10 минут:

```text
currentTime = 10:10

период проверки:
10:00 → 10:10
```

Если:

```java
actualCount <= minEventCount
```

то отправляется alert-уведомление.

Если поток долго не восстанавливается, alert отправляется на каждой проверке:

```text
10:00 — actualCount = 0 → отправили alert
10:10 — actualCount = 0 → отправили alert
10:20 — actualCount = 0 → отправили alert
```

Recovery-уведомление для `FIXED_DELAY` не отправляется.

Если:

```java
actualCount > minEventCount
```

то для `FIXED_DELAY` ничего не отправляем.

`nextExecutionTime` для `FIXED_DELAY` считается стандартно через `FixedDelayTimeResolver`:

```text
nextExecutionTime = startTime + timeParameter + executionDuration
```

---

## 10. Поведение CRON: обычный режим

Обычный режим означает:

```text
recoveryMode = false
```

Для `CRON` важно разделять:

```text
timeParameter             — когда запускать задачу
checkTimeFrom/checkTimeTo — за какой бизнес-период считать записи
```

Пример:

```text
timeParameter = "0 9 * * *"
checkTimeFrom = "08:00"
checkTimeTo = "09:00"
```

Это означает:

```text
задача запускается каждый день в 09:00,
но записи проверяются за период 08:00 → 09:00
```

В обычном CRON-режиме период проверки берётся из параметров:

```text
from = today + checkTimeFrom
to = today + checkTimeTo
```

Если:

```java
actualCount <= minEventCount
```

то:

```text
1. отправляем alert
2. ставим recoveryMode = true
3. nextExecutionTime = startTime + 1 час
```

Если:

```java
actualCount > minEventCount
```

то:

```text
1. ничего не отправляем
2. recoveryMode = false
3. nextExecutionTime = следующий запуск по CRON
```

---

## 11. Поведение CRON: recovery-режим

Recovery-режим означает:

```text
recoveryMode = true
```

В recovery-режиме `checkTimeFrom` и `checkTimeTo` не меняются. Они остаются в `parameters` как штатный бизнес-период для обычного CRON-режима.

Период проверки в recovery-режиме считается динамически:

```text
from = currentTime - 1 час
to = currentTime
```

Пример:

```text
09:00 — обычная CRON-проверка 08:00 → 09:00
нашли проблему
recoveryMode = true
nextExecutionTime = 10:00
```

В 10:00:

```text
проверяем 09:00 → 10:00
```

В 11:00:

```text
проверяем 10:00 → 11:00
```

Если в recovery-режиме:

```java
actualCount <= minEventCount
```

то:

```text
1. отправляем alert
2. recoveryMode остаётся true
3. nextExecutionTime = startTime + 1 час
```

Если в recovery-режиме:

```java
actualCount > minEventCount
```

то:

```text
1. отправляем recovery-уведомление
2. ставим recoveryMode = false
3. nextExecutionTime возвращается к обычному CRON
```

Пример:

```text
исходный timeParameter = "0 9 * * *"
```

Если поток восстановился в 12:00, следующий запуск будет:

```text
завтра в 09:00
```

Не будет периода “с 12 до завтра 12”, потому что CRON отвечает только за расписание запуска, а бизнес-период проверки определяется отдельно.

---

## 12. Параметры monitoring-задачи

Для всех пяти задач используется общий параметр:

```java
MonitoringTaskParameter
```

Минимальный набор полей:

```java
private Integer minEventCount;
private String checkTimeFrom;
private String checkTimeTo;
private Boolean recoveryMode;
private List<String> recipients;
private String subject;
```

Смысл полей:

| Поле | Назначение |
|---|---|
| `minEventCount` | Минимально допустимое количество записей. Значение `actualCount == minEventCount` считается проблемой. |
| `checkTimeFrom` | Начало бизнес-периода для обычного CRON-режима. Для `FIXED_DELAY` не используется. |
| `checkTimeTo` | Конец бизнес-периода для обычного CRON-режима. Для `FIXED_DELAY` не используется. |
| `recoveryMode` | Признак recovery-режима для CRON. |
| `recipients` | Получатели email-уведомлений. |
| `subject` | Необязательная тема alert-письма. Если не задана, используется дефолтная. |

При ошибке десериализации параметров нельзя возвращать пустой объект. Нужно бросать исключение, чтобы задача завершилась со статусом `ERROR`.

Причина: без параметров задача не может корректно принять бизнес-решение.

---

## 13. Общая архитектура monitoring-модуля

Итоговая структура должна быть общей для всех пяти задач.

```text
GiftsScheduledTaskEnum
    ↓
создаёт конкретный MonitoringScheduledTask
    ↓
TaskManagerAbs
    ↓
через TaskServiceFactory запускает конкретный TaskService
    ↓
AbstractMonitoringTaskService
    ↓
MonitoringPeriodResolver
    ↓
MonitoringEventCounterRepository
    ↓
MonitoringNotificationService
    ↓
возврат ExecutionResultModel
    ↓
MonitoringScheduledTask.calculateNextExecutionTime(...)
    ↓
ScheduledTaskRepositoryImpl.saveTask(...)
```

---

## 14. Рекомендуемая структура пакетов

Все новые классы остаются внутри `gifts.giftsslave.taskmanager`, но раскладываются по подпакетам.

```text
gifts.giftsslave.taskmanager
│
├── enums
│   └── GiftsScheduledTaskEnum.java
│
├── monitoring
│   ├── MonitoringCounterType.java
│   ├── MonitoringFilterKey.java
│   ├── MonitoringPeriod.java
│   ├── MonitoringTaskDefinition.java
│   └── MonitoringTaskDefinitionRegistry.java
│
├── parameters
│   └── monitoring
│       └── MonitoringTaskParameter.java
│
├── repository
│   └── monitoring
│       ├── MonitoringEventCounterRepository.java
│       └── MonitoringEventCounterRepositoryImpl.java
│
├── services
│   └── monitoring
│       ├── AbstractMonitoringTaskService.java
│       ├── MonitoringPeriodResolver.java
│       ├── MechanicsVspManagerMonitoringTaskService.java
│       ├── MechanicsMonitoringTaskService.java
│       ├── CasesMonitoringTaskService.java
│       ├── NegativeCsiMonitoringTaskService.java
│       ├── Zok4MonitoringTaskService.java
│       │
│       └── notification
│           ├── MonitoringNotificationSender.java
│           ├── JavaMailMonitoringNotificationSender.java
│           └── MonitoringNotificationService.java
│
└── tasks
    └── monitoring
        ├── MonitoringScheduledTask.java
        ├── MechanicsVspManagerMonitoringTask.java
        ├── MechanicsMonitoringTask.java
        ├── CasesMonitoringTask.java
        ├── NegativeCsiMonitoringTask.java
        └── Zok4MonitoringTask.java
```

Правило раскладки:

```text
tasks.monitoring                  — классы ScheduledTask
services.monitoring               — бизнес-логика выполнения задач
services.monitoring.notification  — email alert/recovery
repository.monitoring             — Hibernate/native SQL count-запросы
parameters.monitoring             — JSON parameters из scheduledTask
monitoring                        — общие модели и definitions
enums                             — GiftsScheduledTaskEnum
```

---

## 15. GiftsScheduledTaskEnum

В `GiftsScheduledTaskEnum` нужно добавить пять новых констант.

Пример:

```java
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
```

---

## 16. MonitoringScheduledTask

`MonitoringScheduledTask` — общий базовый task-класс для пяти monitoring-задач.

Он нужен только для расчёта следующего запуска.

Он не считает записи в БД и не отправляет уведомления.

Ответственность класса:

```text
1. Для ERROR сохранить retry-поведение errorResult, если nextExecutionTime уже выставлен.
2. Для FIXED_DELAY использовать стандартный расчёт.
3. Для CRON + recoveryMode=true вернуть startTime + 1 час.
4. Для CRON + recoveryMode=false использовать стандартный CRON-расчёт.
```

Ключевая логика:

```text
if result == ERROR and result.nextExecutionTime != null:
    return result.nextExecutionTime

if taskTimeType != CRON:
    return super.calculateNextExecutionTime(result)

if recoveryMode == true:
    return result.startTime + 1 hour

return super.calculateNextExecutionTime(result)
```

Почему нужна проверка `ERROR`:

`ScheduledTask.errorResult(...)` выставляет `nextExecutionTime = now + 5 минут`, но `TaskManagerAbs` потом всё равно вызывает `calculateNextExecutionTime(...)`. Если это не защитить, CRON-задача после технической ошибки может уйти не на retry через 5 минут, а на следующий cron-запуск.

---

## 17. Конкретные MonitoringTask-классы

Пять конкретных task-классов должны быть максимально простыми.

Пример:

```java
public class MechanicsVspManagerMonitoringTask extends MonitoringScheduledTask {

    public MechanicsVspManagerMonitoringTask(
            ScheduledTaskGetter<GiftsScheduledTaskEnum> getter
    ) {
        super(getter);
    }
}
```

Аналогично создаются:

```text
MechanicsMonitoringTask
CasesMonitoringTask
NegativeCsiMonitoringTask
Zok4MonitoringTask
```

В них не нужно дублировать `calculateNextExecutionTime(...)`, потому что вся общая recovery-логика находится в `MonitoringScheduledTask`.

---

## 18. MonitoringTaskDefinition

`MonitoringTaskDefinition` — это описание одной monitoring-задачи.

Рекомендуемый тип — `record`, потому что объект неизменяемый и создаётся в коде, а не десериализуется из JSON.

Итоговая модель:

```java
public record MonitoringTaskDefinition(
        GiftsScheduledTaskEnum taskType,
        String displayName,
        MonitoringCounterType counterType,
        Map<MonitoringFilterKey, String> filters
) {
    public MonitoringTaskDefinition {
        Objects.requireNonNull(taskType, "taskType must not be null");
        requireNonBlank(displayName, "displayName");
        Objects.requireNonNull(counterType, "counterType must not be null");
        Objects.requireNonNull(filters, "filters must not be null");

        if (filters.isEmpty()) {
            throw new IllegalArgumentException("filters must not be empty");
        }

        filters = Map.copyOf(filters);
    }

    public String requiredFilter(MonitoringFilterKey key) {
        Objects.requireNonNull(key, "key must not be null");

        var value = filters.get(key);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Required monitoring filter is missing: " + key +
                            " for taskType: " + taskType
            );
        }

        return value;
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
```

Причина использования `Map<MonitoringFilterKey, String>`:

```text
1. Не нужен null для задач с одним фильтром.
2. Фильтры имеют понятные имена.
3. ZOK_4 может иметь два фильтра: PROCESS_TYPE и ORIGINAL_SYSTEM_CODE.
4. В будущем можно добавить новый фильтр без изменения структуры record.
```

---

## 19. MonitoringFilterKey

`MonitoringFilterKey` описывает имена фильтров, которые могут использовать monitoring-задачи.

```java
public enum MonitoringFilterKey {
    PROCESS_TYPE,
    GIFT_PROCESS,
    ORIGINAL_SYSTEM_CODE
}
```

---

## 20. MonitoringCounterType

`MonitoringCounterType` описывает не бизнес-задачу, а техническую стратегию подсчёта.

```java
public enum MonitoringCounterType {
    GIFT_TASK_BY_PROCESS_TYPE,
    CASES_FOR_GIFTS_BY_GIFT_PROCESS,
    GIFT_TASK_BY_PROCESS_TYPE_AND_ORIGINAL_SYSTEM_CODE
}
```

Не рекомендуется использовать название вроде:

```java
GIFT_TASK_ZOK_4
```

Потому что это зашивает конкретный бизнес-кейс в enum. Лучше, чтобы enum описывал способ подсчёта, а конкретные значения `ZOK` и `ZOK_4` жили в `MonitoringTaskDefinitionRegistry`.

---

## 21. MonitoringTaskDefinitionRegistry

`MonitoringTaskDefinitionRegistry` — это реестр всех пяти monitoring-задач.

Не рекомендуется название `MonitoringTaskDefinitions`, потому что оно слишком похоже на `MonitoringTaskDefinition` и отличается только буквой `s`.

Рекомендуемое название:

```text
MonitoringTaskDefinitionRegistry
```

Финальная модель:

```java
public final class MonitoringTaskDefinitionRegistry {

    public static final MonitoringTaskDefinition MECHANICS_VSP_MANAGER =
            new MonitoringTaskDefinition(
                    GiftsScheduledTaskEnum.MECHANICS_VSP_MANAGER_MONITORING_TASK,
                    "ВСП уведомления",
                    MonitoringCounterType.GIFT_TASK_BY_PROCESS_TYPE,
                    Map.of(
                            MonitoringFilterKey.PROCESS_TYPE,
                            "MECHANICS_VSP_MANAGER"
                    )
            );

    public static final MonitoringTaskDefinition MECHANICS =
            new MonitoringTaskDefinition(
                    GiftsScheduledTaskEnum.MECHANICS_MONITORING_TASK,
                    "Mechanics",
                    MonitoringCounterType.CASES_FOR_GIFTS_BY_GIFT_PROCESS,
                    Map.of(
                            MonitoringFilterKey.GIFT_PROCESS,
                            "MECHANICS"
                    )
            );

    public static final MonitoringTaskDefinition CASES =
            new MonitoringTaskDefinition(
                    GiftsScheduledTaskEnum.CASES_MONITORING_TASK,
                    "Cases",
                    MonitoringCounterType.CASES_FOR_GIFTS_BY_GIFT_PROCESS,
                    Map.of(
                            MonitoringFilterKey.GIFT_PROCESS,
                            "CASES"
                    )
            );

    public static final MonitoringTaskDefinition NEGATIVE_CSI =
            new MonitoringTaskDefinition(
                    GiftsScheduledTaskEnum.NEGATIVE_CSI_MONITORING_TASK,
                    "Negative CSI",
                    MonitoringCounterType.CASES_FOR_GIFTS_BY_GIFT_PROCESS,
                    Map.of(
                            MonitoringFilterKey.GIFT_PROCESS,
                            "NEGATIVE_CSI"
                    )
            );

    public static final MonitoringTaskDefinition ZOK_4 =
            new MonitoringTaskDefinition(
                    GiftsScheduledTaskEnum.ZOK_4_MONITORING_TASK,
                    "ZOK_4",
                    MonitoringCounterType.GIFT_TASK_BY_PROCESS_TYPE_AND_ORIGINAL_SYSTEM_CODE,
                    Map.of(
                            MonitoringFilterKey.PROCESS_TYPE,
                            "ZOK",
                            MonitoringFilterKey.ORIGINAL_SYSTEM_CODE,
                            "ZOK_4"
                    )
            );

    private static final Map<GiftsScheduledTaskEnum, MonitoringTaskDefinition> DEFINITIONS =
            new EnumMap<>(GiftsScheduledTaskEnum.class);

    static {
        register(MECHANICS_VSP_MANAGER);
        register(MECHANICS);
        register(CASES);
        register(NEGATIVE_CSI);
        register(ZOK_4);
    }

    private MonitoringTaskDefinitionRegistry() {
    }

    public static MonitoringTaskDefinition get(GiftsScheduledTaskEnum taskType) {
        var definition = DEFINITIONS.get(taskType);

        if (definition == null) {
            throw new IllegalArgumentException(
                    "Monitoring task definition not found for taskType: " + taskType
            );
        }

        return definition;
    }

    private static void register(MonitoringTaskDefinition definition) {
        DEFINITIONS.put(definition.taskType(), definition);
    }
}
```

---

## 22. MonitoringPeriod

`MonitoringPeriod` — это простая модель периода проверки.

Она ничего не считает и ничего не проверяет. Она только хранит две даты:

```java
public record MonitoringPeriod(
        LocalDateTime from,
        LocalDateTime to
) {
}
```

Период нужен для:

```text
1. запроса в БД;
2. текста email;
3. resultMessage;
4. логов;
5. тестов.
```

---

## 23. MonitoringPeriodResolver

`MonitoringPeriodResolver` отвечает за вопрос:

```text
за какой период искать записи в БД?
```

Он не отвечает за `nextExecutionTime`. Это делает `MonitoringScheduledTask`.

Логика:

```text
если taskTimeType = FIXED_DELAY:
    from = now - timeParameter seconds
    to = now

если taskTimeType = CRON и recoveryMode = false:
    from = today + checkTimeFrom
    to = today + checkTimeTo

если taskTimeType = CRON и recoveryMode = true:
    from = now - 1 hour
    to = now
```

Важно разделять:

```text
MonitoringScheduledTask
    решает, когда запускать задачу дальше

MonitoringPeriodResolver
    решает, за какой период считать записи
```

---

## 24. MonitoringEventCounterRepository

`MonitoringEventCounterRepository` — общий репозиторий-счётчик для всех пяти monitoring-задач.

Он отвечает только за один вопрос:

```text
сколько записей есть за период from/to для конкретной MonitoringTaskDefinition?
```

Интерфейс:

```java
public interface MonitoringEventCounterRepository {

    long count(
            MonitoringTaskDefinition definition,
            LocalDateTime from,
            LocalDateTime to
    );
}
```

Репозиторий не знает про:

```text
1. minEventCount;
2. alert;
3. recovery;
4. nextExecutionTime;
5. email recipients.
```

Он только выполняет count-запрос.

---

## 25. MonitoringEventCounterRepositoryImpl

Реализация использует Hibernate/JPA `EntityManager` и native SQL.

Причина выбора Hibernate/native SQL:

```text
1. Для monitoring-задач нужен только count(*).
2. Нет необходимости изменять бизнес-сущности.
3. Можно не использовать DataspaceCoreSearchClient.
4. Для ZOK_4 нужен JSON-фильтр по notification_attributes.originalSystemCode.
```

Рекомендуемый подход:

```java
@Repository
public class MonitoringEventCounterRepositoryImpl implements MonitoringEventCounterRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public long count(
            MonitoringTaskDefinition definition,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return switch (definition.counterType()) {
            case GIFT_TASK_BY_PROCESS_TYPE -> countGiftTaskByProcessType(
                    definition.requiredFilter(MonitoringFilterKey.PROCESS_TYPE),
                    from,
                    to
            );

            case CASES_FOR_GIFTS_BY_GIFT_PROCESS -> countCasesForGiftsByGiftProcess(
                    definition.requiredFilter(MonitoringFilterKey.GIFT_PROCESS),
                    from,
                    to
            );

            case GIFT_TASK_BY_PROCESS_TYPE_AND_ORIGINAL_SYSTEM_CODE ->
                    countGiftTaskByProcessTypeAndOriginalSystemCode(
                            definition.requiredFilter(MonitoringFilterKey.PROCESS_TYPE),
                            definition.requiredFilter(MonitoringFilterKey.ORIGINAL_SYSTEM_CODE),
                            from,
                            to
                    );
        };
    }
}
```

SQL-константы таблиц и колонок рекомендуется оставить внутри `MonitoringEventCounterRepositoryImpl`, потому что это детали реализации конкретного репозитория.

Пример:

```java
private static final String GIFT_TASK_TABLE = "gift_task";
private static final String GIFT_TASK_CREATE_DATE_COLUMN = "create_date";
private static final String GIFT_TASK_PROCESS_TYPE_COLUMN = "process_type";
private static final String GIFT_TASK_NOTIFICATION_ATTRIBUTES_COLUMN = "notification_attributes";

private static final String CASES_FOR_GIFTS_TABLE = "CasesForGifts";
private static final String CASES_FOR_GIFTS_GIFT_PROCESS_COLUMN = "giftProcess";
private static final String CASES_FOR_GIFTS_EVENT_DATE_COLUMN = "eventDate";
```

Бизнес-значения вроде `MECHANICS`, `ZOK`, `ZOK_4` не должны жить в репозитории. Они должны жить в `MonitoringTaskDefinitionRegistry`.

---

## 26. SQL-условия подсчёта

### 26.1. gift_task по process_type

Для `MECHANICS_VSP_MANAGER_MONITORING_TASK`:

```sql
select count(*)
from gift_task
where process_type = :processType
  and create_date >= :from
  and create_date < :to
```

### 26.2. CasesForGifts по giftProcess

Для:

```text
MECHANICS_MONITORING_TASK
CASES_MONITORING_TASK
NEGATIVE_CSI_MONITORING_TASK
```

SQL:

```sql
select count(*)
from CasesForGifts
where giftProcess = :giftProcess
  and eventDate >= :from
  and eventDate < :to
```

### 26.3. gift_task по process_type и originalSystemCode

Для `ZOK_4_MONITORING_TASK`:

```sql
select count(*)
from gift_task
where process_type = :processType
  and create_date >= :from
  and create_date < :to
  and notification_attributes ->> 'originalSystemCode' = :originalSystemCode
```

Этот вариант использует PostgreSQL JSON/JSONB синтаксис:

```sql
notification_attributes ->> 'originalSystemCode'
```

Если поле `notification_attributes` хранится как `text`, может потребоваться cast:

```sql
notification_attributes::jsonb ->> 'originalSystemCode'
```

Если БД не PostgreSQL, JSON-синтаксис нужно заменить на подходящий для конкретной БД.

---

## 27. Почему используется `< :to`, а не `<= :to`

Во всех count-запросах период должен задаваться так:

```sql
date_field >= :from
and date_field < :to
```

Не рекомендуется использовать:

```sql
date_field <= :to
```

Причина: соседние интервалы не должны пересекаться.

Пример:

```text
09:00 → 10:00
10:00 → 11:00
```

Запись ровно в `10:00` должна попасть только во второй период.

---

## 28. AbstractMonitoringTaskService

`AbstractMonitoringTaskService` содержит общую бизнес-логику всех пяти monitoring-задач.

Он должен:

```text
1. Десериализовать MonitoringTaskParameter.
2. Получить MonitoringTaskDefinition по getEnum().
3. Рассчитать MonitoringPeriod через MonitoringPeriodResolver.
4. Посчитать actualCount через MonitoringEventCounterRepository.
5. Проверить actualCount <= minEventCount.
6. Отправить alert или recovery при необходимости.
7. Обновить recoveryMode.
8. Вернуть ExecutionResultModel со статусом SUCCESS.
```

Общий алгоритм:

```text
params = deserialize(task.getParams())
validate(params, task)

definition = MonitoringTaskDefinitionRegistry.get(getEnum())
period = periodResolver.resolve(task, params)
actualCount = repository.count(definition, period.from, period.to)

hasProblem = actualCount <= params.minEventCount

if hasProblem:
    notificationService.sendAlert(...)

    if task.taskTimeType == CRON:
        params.recoveryMode = true

else:
    if task.taskTimeType == CRON and params.recoveryMode == true:
        notificationService.sendRecovery(...)

    if task.taskTimeType == CRON:
        params.recoveryMode = false

return SUCCESS with updated params
```

Важно:

```text
Если записей мало — это бизнес-проблема потока,
а не техническая ошибка выполнения задачи.
```

Поэтому при успешно выполненной проверке, даже если поток проблемный, результат задачи должен быть:

```java
ExecutionTaskResultStatus.SUCCESS
```

`ERROR` используется только для технических ошибок:

```text
1. невалидные parameters;
2. ошибка БД;
3. ошибка SMTP, если отправка считается критичной;
4. непредвиденная runtime-ошибка.
```

---

## 29. Конкретные TaskService-классы

Для пяти задач создаются пять маленьких сервисов.

Каждый сервис должен вернуть свой enum через `getEnum()`.

Пример:

```java
@Service
public class MechanicsVspManagerMonitoringTaskService extends AbstractMonitoringTaskService {

    public MechanicsVspManagerMonitoringTaskService(
            MonitoringPeriodResolver periodResolver,
            MonitoringEventCounterRepository repository,
            MonitoringNotificationService notificationService
    ) {
        super(periodResolver, repository, notificationService);
    }

    @Override
    public GiftsScheduledTaskEnum getEnum() {
        return GiftsScheduledTaskEnum.MECHANICS_VSP_MANAGER_MONITORING_TASK;
    }
}
```

Аналогично:

```text
MechanicsMonitoringTaskService
CasesMonitoringTaskService
NegativeCsiMonitoringTaskService
Zok4MonitoringTaskService
```

---

## 30. MonitoringNotificationService

`MonitoringNotificationService` отвечает только за подготовку и отправку уведомлений.

Он не считает записи и не принимает решение о recovery.

Методы:

```java
sendAlert(...)
sendRecovery(...)
```

Alert отправляется:

```text
для FIXED_DELAY — каждый раз, когда actualCount <= minEventCount
для CRON — каждый раз, когда actualCount <= minEventCount
```

Recovery отправляется:

```text
только для CRON,
только если задача была в recoveryMode=true,
и actualCount > minEventCount
```

Для `FIXED_DELAY` recovery не отправляется.

---

## 31. MonitoringNotificationSender

`MonitoringNotificationSender` — интерфейс отправки email.

Он нужен, чтобы monitoring-логика не зависела напрямую от конкретного SMTP-сервиса проекта.

```java
public interface MonitoringNotificationSender {

    void send(
            List<String> recipients,
            String subject,
            String body
    );
}
```

Реализация:

```text
JavaMailMonitoringNotificationSender
```

или адаптер над уже существующим mail-сервисом проекта.

Если в проекте уже есть готовый SMTP-сервис, лучше использовать его внутри `MonitoringNotificationSender`, а не писать отправку писем заново.

---

## 32. Шаблоны уведомлений

### Alert

Рекомендуемый текст:

```text
Поток "{displayName}" на момент проверки {checkTime}
за период с {from} до {to}
ожидалось получить больше {minEventCount},
по факту получено {actualCount}.
```

Важно писать именно “больше `minEventCount`”, потому что условие проблемы:

```java
actualCount <= minEventCount
```

### Recovery

Только для `CRON`:

```text
По источнику "{displayName}" получение событий восстановлено.

Время проверки: {checkTime}
Период проверки: с {from} до {to}
Получено событий: {actualCount}.
```

---

## 33. Пример поведения FIXED_DELAY

Исходные данные:

```text
taskType = FIXED_DELAY
timeParameter = 600
minEventCount = 10
```

Запуск в 10:00:

```text
period = 09:50 → 10:00
```

Если:

```text
actualCount = 8
```

то:

```text
8 <= 10 → проблема
отправляем alert
nextExecutionTime считается стандартно
```

Запуск в 10:10:

```text
period = 10:00 → 10:10
```

Если снова мало записей, снова отправляем alert.

Если записей достаточно, ничего не отправляем.

---

## 34. Пример поведения CRON

Исходные данные:

```text
taskType = CRON
timeParameter = "0 9 * * *"
checkTimeFrom = "08:00"
checkTimeTo = "09:00"
minEventCount = 10
recoveryMode = false
```

В 09:00:

```text
period = 08:00 → 09:00
```

Если:

```text
actualCount = 5
```

то:

```text
5 <= 10 → проблема
отправляем alert
recoveryMode = true
nextExecutionTime = 10:00
```

В 10:00:

```text
recoveryMode = true
period = 09:00 → 10:00
```

Если снова мало:

```text
отправляем alert
recoveryMode = true
nextExecutionTime = 11:00
```

В 11:00:

```text
period = 10:00 → 11:00
```

Если поток восстановился:

```text
actualCount > minEventCount
отправляем recovery
recoveryMode = false
nextExecutionTime = следующий CRON, например завтра 09:00
```

---

## 35. Примеры записей scheduledTask

### 35.1. MECHANICS_VSP_MANAGER_MONITORING_TASK

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
  "nextExecutionTime": "ближайший запуск по cron",
  "isActive": true,
  "status": "SUCCESS"
}
```

### 35.2. MECHANICS_MONITORING_TASK

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
  "nextExecutionTime": "ближайший запуск по cron",
  "isActive": true,
  "status": "SUCCESS"
}
```

### 35.3. CASES_MONITORING_TASK

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
  "nextExecutionTime": "ближайший запуск по cron",
  "isActive": true,
  "status": "SUCCESS"
}
```

### 35.4. NEGATIVE_CSI_MONITORING_TASK

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
  "nextExecutionTime": "ближайший запуск по cron",
  "isActive": true,
  "status": "SUCCESS"
}
```

### 35.5. ZOK_4_MONITORING_TASK

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
  "nextExecutionTime": "ближайший запуск по cron",
  "isActive": true,
  "status": "SUCCESS"
}
```

Для `FIXED_DELAY` пример параметров:

```json
{
  "objectId": "MECHANICS_VSP_MANAGER_MONITORING_TASK",
  "taskType": "FIXED_DELAY",
  "timeParameter": "600",
  "parameters": {
    "minEventCount": 10,
    "recoveryMode": false,
    "recipients": ["support@example.com"]
  },
  "nextExecutionTime": "now",
  "isActive": true,
  "status": "SUCCESS"
}
```

Для `FIXED_DELAY` поля `checkTimeFrom` и `checkTimeTo` не нужны.

---

## 36. Минимальный план разработки

### Этап 1. Добавить enum-константы

Добавить в `GiftsScheduledTaskEnum`:

```text
MECHANICS_VSP_MANAGER_MONITORING_TASK
MECHANICS_MONITORING_TASK
CASES_MONITORING_TASK
NEGATIVE_CSI_MONITORING_TASK
ZOK_4_MONITORING_TASK
```

### Этап 2. Создать task-классы

Создать:

```text
MonitoringScheduledTask
MechanicsVspManagerMonitoringTask
MechanicsMonitoringTask
CasesMonitoringTask
NegativeCsiMonitoringTask
Zok4MonitoringTask
```

### Этап 3. Создать параметры

Создать:

```text
MonitoringTaskParameter
```

### Этап 4. Создать monitoring definitions

Создать:

```text
MonitoringCounterType
MonitoringFilterKey
MonitoringTaskDefinition
MonitoringTaskDefinitionRegistry
```

### Этап 5. Создать period resolver

Создать:

```text
MonitoringPeriod
MonitoringPeriodResolver
```

### Этап 6. Создать repository

Создать:

```text
MonitoringEventCounterRepository
MonitoringEventCounterRepositoryImpl
```

Реализация через Hibernate/JPA `EntityManager` и native SQL.

### Этап 7. Создать notification service

Создать:

```text
MonitoringNotificationSender
JavaMailMonitoringNotificationSender
MonitoringNotificationService
```

Или адаптировать под существующий email-сервис проекта.

### Этап 8. Создать task services

Создать:

```text
AbstractMonitoringTaskService
MechanicsVspManagerMonitoringTaskService
MechanicsMonitoringTaskService
CasesMonitoringTaskService
NegativeCsiMonitoringTaskService
Zok4MonitoringTaskService
```

### Этап 9. Добавить записи в scheduledTask

Добавить пять записей в `scheduledTask`.

Без этих записей задачи будут зарегистрированы в коде, но не будут запускаться.

### Этап 10. Добавить тесты

Добавить unit-тесты на period resolver, task service, scheduled task recovery и repository SQL-поведение.

---

## 37. Минимальный набор тестов

### PeriodResolver

```text
1. FIXED_DELAY: period = now - timeParameter → now.
2. CRON обычный: period = today + checkTimeFrom → today + checkTimeTo.
3. CRON recovery: period = now - 1 hour → now.
```

### MonitoringScheduledTask

```text
1. FIXED_DELAY использует стандартный расчёт.
2. CRON + recoveryMode=true → nextExecutionTime = startTime + 1 hour.
3. CRON + recoveryMode=false → nextExecutionTime = next cron.
4. ERROR + result.nextExecutionTime != null → возвращается result.nextExecutionTime.
5. Невалидные params при расчёте nextExecutionTime → fallback на стандартный расчёт.
```

### AbstractMonitoringTaskService

```text
1. actualCount < minEventCount → alert.
2. actualCount == minEventCount → alert.
3. actualCount > minEventCount → alert не отправляется.
4. FIXED_DELAY + проблема → alert, recovery не отправляется.
5. CRON обычный + проблема → alert, recoveryMode=true.
6. CRON recovery + проблема → alert, recoveryMode=true.
7. CRON recovery + восстановление → recovery, recoveryMode=false.
8. CRON обычный + успешная проверка → recovery не отправляется.
9. Ошибка parameters → ERROR.
```

### Repository

```text
1. MECHANICS_VSP_MANAGER_MONITORING_TASK считает gift_task по process_type.
2. MECHANICS_MONITORING_TASK считает CasesForGifts по giftProcess=MECHANICS.
3. CASES_MONITORING_TASK считает CasesForGifts по giftProcess=CASES.
4. NEGATIVE_CSI_MONITORING_TASK считает CasesForGifts по giftProcess=NEGATIVE_CSI.
5. ZOK_4_MONITORING_TASK считает gift_task по process_type=ZOK и originalSystemCode=ZOK_4.
6. Проверить, что используется >= from и < to.
```

---

## 38. Технические риски и проверки перед merge

### 38.1. Проверить физические имена таблиц и колонок

Нужно сверить реальные имена в БД:

```text
gift_task
create_date
process_type
notification_attributes
CasesForGifts
giftProcess
eventDate
```

Возможные отличия:

```text
CasesForGifts      → cases_for_gifts
giftProcess        → gift_process
eventDate          → event_date
```

Если используется PostgreSQL и имена созданы в camelCase с кавычками, в native SQL могут потребоваться кавычки:

```sql
"CasesForGifts"
"giftProcess"
"eventDate"
```

### 38.2. Проверить JSON-синтаксис для notification_attributes

Текущий вариант предполагает PostgreSQL JSON/JSONB:

```sql
notification_attributes ->> 'originalSystemCode'
```

Если поле хранится как `text`, нужен cast:

```sql
notification_attributes::jsonb ->> 'originalSystemCode'
```

Если БД не PostgreSQL, нужен другой синтаксис.

### 38.3. Ошибка email-отправки

Нужно определить, считается ли ошибка SMTP технической ошибкой задачи.

Рекомендуемое поведение:

```text
если alert/recovery не удалось отправить, задача завершается ERROR
```

Причина: бизнес-событие мониторинга было обнаружено, но уведомление не доставлено.

### 38.4. errorResult и nextExecutionTime

`ScheduledTask.errorResult(...)` выставляет `now + 5 минут`, но `TaskManagerAbs` потом вызывает `calculateNextExecutionTime(...)`.

Поэтому `MonitoringScheduledTask` должен уважать `resultModel.getNextExecutionTime()` для `ERROR`, иначе retry-поведение может быть потеряно.

### 38.5. currentTasks не гарантирует строгий лимит параллельности

В существующем `TaskManagerAbs` задача запускается в отдельном thread, а taskType быстро удаляется из `currentTasks`.

Основная защита от дублей сейчас — это `IN_PROGRESS` и optimistic locking в `ScheduledTaskRepositoryImpl`.

---

## 39. Короткая итоговая схема

```text
Нужно реализовать 5 monitoring-задач.

Каждая задача:
    имеет отдельный objectId в scheduledTask;
    имеет отдельный enum в GiftsScheduledTaskEnum;
    имеет отдельный TaskService;
    использует общий AbstractMonitoringTaskService;
    использует общий MonitoringScheduledTask;
    использует общий MonitoringEventCounterRepository.

FIXED_DELAY:
    period = now - timeParameter → now
    if actualCount <= minEventCount:
        send alert
    recovery не отправляем
    nextExecutionTime стандартный

CRON обычный:
    period = checkTimeFrom → checkTimeTo
    if actualCount <= minEventCount:
        send alert
        recoveryMode = true
        nextExecutionTime = startTime + 1 hour
    else:
        recoveryMode = false
        nextExecutionTime = next cron

CRON recovery:
    period = now - 1 hour → now
    if actualCount <= minEventCount:
        send alert
        recoveryMode = true
        nextExecutionTime = startTime + 1 hour
    else:
        send recovery
        recoveryMode = false
        nextExecutionTime = next cron
```

---

## 40. Финальное решение по сравнению с первоначальным дизайном

Первоначальная модель рассматривала одну задачу:

```text
CHECK_VSP_NOTIFICATION_FLOW_TASK
```

Идея с суммой:

```text
actualCount = count(gift_task) + count(CasesForGifts)
```

больше не используется.

Финальная модель:

```text
MECHANICS_VSP_MANAGER_MONITORING_TASK
MECHANICS_MONITORING_TASK
CASES_MONITORING_TASK
NEGATIVE_CSI_MONITORING_TASK
ZOK_4_MONITORING_TASK
```

Каждая задача считает только свой источник:

```text
один шедулер → одна таблица/логический источник → один actualCount
```

Это решение проще для сопровождения, тестирования и дальнейшего расширения.
