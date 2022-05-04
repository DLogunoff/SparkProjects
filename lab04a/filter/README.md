


# Сквозной проект

Начиная с этой лабораторной работы вы будете работать в рамках одного проекта. Фактически, работа проделанная в одной лабе, будет использоваться в последующих.

Вот этапы проекта:

* Лаба 4а - Сохранение логов из Kafka в Spark по расписанию, с фильтрацией датасета по признаку. Описание ниже.
* Лаба 5 - Подготовка матрицы users x items по логам из Kafka для прогнозирования пола и возраста.
* Лаба 6 - Подготовка матрицы признаков по логам, лежащим на HDFS, для модели машинного обучения
* Лаба 7 - Обучение модели, инференс модели в real-time
* Лаба 8 - Мониторинг качества работы модели машинного обучения.

# Lab04a. Сохранение логов из Kafka в Spark по расписанию

Для приложения аналитики онлайн-магазина поступают данные о визитах пользователя на странички товаров (клики) и о покупках.  
Напишите приложение для пакетной обработки событий визитов. Оно должно считывать данные из Kafka, применять фильтр и раскидывать данные в две директории, на основании фильтрации.

На верхнем уровне пайплайн будет выглядеть так: Kafka —> Spark  —> HDFS.
![Alt text](images/img4a.png?raw=true "Архитектура")

>Kafka bootstrap: spark-master-1:6667

>Топик Kafka: lab04_input_data

>Схема для сохранения на hdfs: /user/name.surname/visits/view/ и /user/name.surname/visits/buy/


## I. Задача с высоты птичьего полета

Вам нужно:

1. считать события из топика Kafka `lab04_input_data`, используя `read.format("kafka")`. Данные в этом топике уже есть. Описание данных ниже.


2. записывать события с простыми посещениями страниц в HDFS по пути `/user/name.surname/visits/view/$dateColumn=$date`, где `$date` - дата в формате `yyyyMMdd`, 
например `p_date=20200501`. Партиционировать нужно по полю, отличному от поля date. Иначе поле date не попадет в сам файл.


3. а события с покупками – в путь `/user/name.surname/visits/buy/$dateColumn=$date`. 

:warning: Внимание! События фильтруются по полю `event_type`. Также, записываемые события должны обогащаться текстовым полем `date`, которое содержит дату в формате `yyyyMMdd`. То есть, если в сообщении есть timestamp, который соответствует, например, дате 20200301, то к сообщению должно добавится поле `date`, со строковым значением 20200301. Пути $dateColumn=$date создаются на каждую дату, которая присутствует в сообщениях. Сообщения идут последовательно по дате. Формат вывода - такой же как и на входе - json в каждой строчке.

:warning: Внимание! Воспользуйтесь partitionBy для формирования директорий $dateColumn=$date

:warning: Внимание! Даты должны соответствовать временной зоне **UTC**. Для этого для spark сессии (spark) установите параметр: spark.conf.set("spark.sql.session.timeZone", "UTC")

Ознакомьтесь с краткой инструкцией по [утилитам командной строки](Kafka.md). Сервер Kafka для использования - spark-master-1:6667

## II. Описание данных

Сообщения в Kafka выглядят таким образом:

```
{
  "event_type": "buy",
  "category": "Entertainment-equipment",
  "item_id": "Entertainment-equipment-2",
  "item_price": 2529,
  "uid": "40b29579-e845-45c0-a34d-03630d296a81",
  "timestamp": 1577865600000
}
```

где:

- `event_type` – может принимать значение "view" – просмотр страницы, и "buy" - факт покупки.
- `category` – категория товара.
- `item_id` – id товара.
- `item_price` – цена товара.
- `uid` – долгосрочный уникальный идентификатор пользователя.
- `timestamp` – временная метка в миллисекундах эпохи.

### Описание выходных данных

Выходные сообщения в HDFS должны выглядеть таким образом (на каждую дату должна быть отдельная директория):

> `/user/name.surname/visits/view/p_date=20202020`

Пример строки, которую проверяет чекер:

>{"category":"Mobile-phones","event_type":"view","item_id":"Mobile-phones-5","item_price":"1029","timestamp":1588107480000,"uid":"1f295269-22ce-418a-8245-269646990dce","date":"20200428"}


## III. Оформление работы

В вашем репо в подпапке `lab04a/filter` положите sbt-project под названием filter с главным классом filter в файле filter.scala. Файл filter.scala может лежать в корне проекта или в src/main/scala. Версия проекта в build.sbt должна быть 1.0

Проект должен компилироваться и запускаться следующим образом:

```
cd lab04a/filter
sbt package
spark-submit --conf spark.filter.topic_name=lab04_input_data --conf spark.filter.offset=earliest --conf spark.filter.output_dir_prefix=/user/name.surname/visits --class filter --packages org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.5 ./target/scala-2.11/filter_2.11-1.0.jar
```

### Требования к filter.scala

Ваша программа должна получать следующие конфигурационные параметры с помощью `spark.conf`:

* `spark.filter.topic_name` – название топика для чтения
* `spark.filter.offset` – оффсет в нулевой партиции топика, с которого должно происходить чтение. Также принимаются значение "earliest".
* `spark.filter.output_dir_prefix` – путь (полный или относительный), куда будут писаться фильтрованные данные. 

:warning: Когда вы даете на вход относительный путь, он считается относительно вашей домашней директории в HDFS. То есть, spark.filter.output_dir_prefix=visits эквивалентно spark.filter.output_dir_prefix=/user/name.surname/visits

<!--
В вашей программе должен быть настроен таймаут топика 30 секунд, i.e. `option("consumer_timeout_ms", 30000)`, то есть при отсутствие данных в топике в течение этого времени чтение прекращается. Таким образом, ваша программа не будет висеть вечно.
-->

## IV. Проверка

Проверка осуществляется из [Личного кабинета](https://lk-spark-de.newprolab.com/). 

_На первом этапе_ вы должны запустить ваше приложение со следующими параметрами:

* `spark.filter.topic_name=lab04_input_data`
* `spark.filter.offset=earliest`
* `spark.filter.output_dir_prefix=visits`

Дождитесь окончания работы приложения и убедитесь, что нужные данные записаны в HDFS по пути `/user/name.surname/visits`. Затем запустите чекер.

В данном случае чекер выполняет следующие действия:

- выполнит проверку репозитория, в частности, наличие `filter.scala` 
<!-- и настройки таймаута топика в нем. Так как на втором этапе мы запускаем вашу программу сами, то программа, где не настроен таймаут чтения топика повесит чекер. -->
- считывает файлы и события в них в `hdfs:///user/name.surname/visits/view/*` и `hdfs:///user/name.surname/visits/buy/*`. Отдельно для записей типа `buy`, `view` расчитывается количество событий в них (subtotals) и "чексумма" (checksums) – сумма всех полей `item_price`.
- логи чекера доступны по пути /tmp/logs/labname/name.surname

_На втором этапе_ чекер запустит ваше приложение с другим значением оффсета и выходного пути и проверит работу. :warning: Внимание! ваше приложение будет запускаться чекером в локальном режиме (`--master local[1]`) и output_dir_prefix с протоколом file://. Проверьте работу вашего приложения в этих условиях.

### Описание полей чекера:

* `git_correct`- True/False - репозиторий проверен.
* `info_git_errors` - ошибки, связанные с проверкой репозитория.
* `info_kafka_errors` – возможные ошибки Kafka.
* `info_number_sent` – количество записей в топике `lab04_input_data`.
* `info_number_recieved` – количество записей в директории `visits` у вас в HDFS.
* `total_number_correct` – True/False: совпадает ли общее количество записей в выходных файлах с ответом (в случае False остальные поля чекера не рассчитываются).
* `info_checksums` –  "чексуммы" данных для просмотров и покупок.
* `info_subtotals` – количество записей в данных для просмотров и покупок.
* `checksums_correct` – True/False: файлы для просмотров правильные.
* `subtotals_correct` – True/False: файлы для покупок правильные.
* `offset_handling_correct` – правильно ли обрабатывается параметр офсета (второй этап).
* `info_your_data` = {}: другие данные, полученные чекером. Сюда же записываются данные второго этапа, включая ссылки на логи запускаемых чекером задач (на назначенном вам мастере).
* `info_errors` – остальные ошибки.
* `lab_result` – True/False: общий результат.

## IV. Практические советы
- Для отладки вы также можете воспользоваться этим же датасетом в HDFS по пути: `hdfs:///labs/laba04/visits-g`.
- Удаляйте выходные файлы перед каждым запуском.
- Параметры передаются в приложение Spark с помощью опции `spark-submit --conf`:
  `--conf spark.filter.topic_name=artem_trunov`
> Если вы хотите получить для тестирования последние 1000 записей из кафки, нужно сначала посчитать count в топике Кафки, 
> затем передать starting offset =(count - 1000);


