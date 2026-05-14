# Важные замечания по SQL в MonitoringEventRepositoryImpl

Репозиторий использует Hibernate/JPA `EntityManager` и native SQL.

## gift_task

Используется SQL:

```sql
select count(*)
from gift_task
where process_type = :processType
  and create_date >= :from
  and create_date < :to
```

## CasesForGifts

Используется SQL:

```sql
select count(*)
from CasesForGifts
where giftProcess = :giftProcess
  and eventDate >= :from
  and eventDate < :to
```

Если физическая схема БД использует snake_case, нужно поменять константы в `MonitoringEventRepositoryImpl`:

```java
CASES_FOR_GIFTS_TABLE = "cases_for_gifts";
CASES_FOR_GIFTS_GIFT_PROCESS_COLUMN = "gift_process";
CASES_FOR_GIFTS_EVENT_DATE_COLUMN = "event_date";
```

## ZOK_4

По умолчанию используется PostgreSQL JSON/JSONB синтаксис:

```sql
notification_attributes ->> 'originalSystemCode' = :originalSystemCode
```

Для Oracle может понадобиться примерно такой фрагмент:

```sql
JSON_VALUE(notification_attributes, '$.originalSystemCode') = :originalSystemCode
```

Для MySQL может понадобиться:

```sql
JSON_UNQUOTE(JSON_EXTRACT(notification_attributes, '$.originalSystemCode')) = :originalSystemCode
```
