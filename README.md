# wise-task-plugin

Микросервис плагинов. Хранит библиотеку плагинов над графами и выполняет их: внутренние — как Spring-бины в своей JVM, внешние (загруженные пользователями jar) — в изолированных Docker-контейнерах.

## Технологии

- Java 25, Spring Boot 4.1.x, gRPC-сервер
- Spring Data JPA + PostgreSQL (база `wise_task_plugin`, таблица `plugin`)
- Docker Java SDK — запуск внешних плагинов в контейнерах

## Типы плагинов

Типы по прото (`PluginType`): `GRAPH_CHARACTERISTIC` (число), `GRAPH_PROPERTY` (да/нет), `GRAPH_NEW_GRAPH` (преобразование графа), `GRAPH_STRING`.

- **Внутренние** (`is_internal = true`) — ~80 встроенных алгоритмов теории графов (паросочетания, хроматическое число, связность, планарность и т.д.). Ссылаются на Spring-бин по `bean_name`.
- **Внешние** — jar, загруженный пользователем. Реализуют интерфейсы из библиотеки `wise-task-library` (`GraphProperty`, `GraphCharacteristic`, ...), класс указывается в `jar_name`, сам jar хранится в БД (`jar_file`, bytea).

## Выполнение внешнего плагина (JarExecutor)

1. На диск временно пишутся: jar плагина, `runner.jar` (зашит в ресурсы, `classpath:runner.jar`) и `graph.json`.
2. Создаётся контейнер образа `eclipse-temurin:25-jre-alpine` (образ тянется автоматически, если его нет) с ограничениями: сеть `none`, лимит памяти (`memory-usage-mb`), CPU-quota, таймаут (`container-work-timeout`).
3. Файлы копируются в контейнер через Docker API (`copyArchiveToContainerCmd`) в `/tmp` — bind-mount не используется, т.к. путь внутри контейнера сервиса невиден демону на хосте.
4. Запускается `ru.leti.wise.task.Runner` — он грузит jar плагина через `URLClassLoader` и вызывает `run(graph)`.
5. Логи контейнера собираются, результат = последняя строка stdout. Если в логах есть исключение — возвращается ошибка с логами плагина.
6. Контейнер и временные файлы удаляются.

При `CreatePlugin` плагин **валидируется**: прогоняется на случайном графе, сгенерированном graph-сервисом. Невалидный jar не сохраняется.

## gRPC API (порт 6566)

Proto: `src/main/resources/proto/plugin_grpc.proto`, пакет `ru.leti.wise.task.plugin`.

| Метод | Назначение |
|---|---|
| `GetAllPlugins` / `GetPlugin` / `GetPluginsByIds` | Каталог плагинов (фильтры + пагинация) |
| `CreatePlugin` | Загрузка внешнего jar (с валидацией) |
| `UpdatePlugin` / `DeletePlugin` | Управление |
| `CheckPluginSolution` | Выполнить плагин на графе из решения |
| `CheckPluginImplementation` | Проверить задачу IMPLEMENTATION (компиляция и прогон кода) |
| `ValidatePlugin` | Повторная валидация загруженного плагина |

Фильтры `GetAllPlugins`: `name`, `description`, `category` (like), `graphType`, `pluginType`, `isValid`, `isInternal`, `authorId`.

## Особенности развёртывания

Сервису нужен доступ к Docker-демону хоста, чтобы запускать контейнеры плагинов. В `docker-compose.yaml`:

```yaml
volumes:
  - /var/run/docker.sock:/var/run/docker.sock
group_add:
  - "${DOCKER_GID}"   # gid группы docker на хосте: stat -c %g /var/run/docker.sock
```

Контейнер работает от non-root пользователя, поэтому без `group_add` доступ к сокету будет запрещён.

## Конфигурация

| Переменная | Назначение |
|---|---|
| `PLUGIN_GRPC_PORT` | Порт gRPC-сервера (6566) |
| `PLUGIN_DB`, `PLUGIN_DB_USER`, `PLUGIN_DB_PASSWORD` | Подключение к PostgreSQL |
| `PLUGIN_TEMP_FOLDER` | Временная папка для jar/json (по умолчанию `/tmp/wise-task-plugin`) |
| `PLUGIN_RUNNER_IMAGE` | Образ для запуска внешних плагинов |
| `spring.docker.*` | Хост Docker, таймауты, лимиты памяти/CPU/времени контейнера |

## Запуск

Через общий docker-стенд gateway (сервис стартует только с примонтированным сокетом, см. выше):

```bash
cd ../wise-task-gateway && docker compose up -d --build wise-task-plugin
```

Локально: `./gradlew bootRun` (нужны PostgreSQL `wise_task_plugin`, graph-сервис для валидации и доступный `DOCKER_HOST`).
