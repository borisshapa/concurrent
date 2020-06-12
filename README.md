## Parallelism

#### IterativeParallelism
* Класс `IterativeParallelism` обрабатывает списки в несколько потоков.
* Реализованы методы:
    * `minimum(threads, list, comparator)` — первый минимум;
    * `maximum(threads, list, comparator)` — первый максимум;
    * `all(threads, list, predicate)` — проверка, что все элементы списка удовлетворяют предикату;
    * `any(threads, list, predicate)` — проверка, что существует элемент списка, удовлетворяющий предикату;
    * `filter(threads, list, predicate)` — вернуть список, содержащий элементы удовлетворяющие предикату;
    * `map(threads, list, function)` — вернуть список, содержащий результаты применения функции;
    * `join(threads, list)` — конкатенация строковых представлений элементов списка.
* Во все функции передается параметр `threads` — сколько потоков надо использовать при вычислении.

#### ParallelMapperImpl
* Класс `ParallelMapperImpl` реализует интерфейс `ParallelMapper`.
    ```
    public interface ParallelMapper extends AutoCloseable {
        <T, R> List<R> run(
            Function<? super T, ? extends R> f,
            List<? extends T> args
        ) throws InterruptedException;
    
        @Override
        void close() throws InterruptedException;
    }
    ```
* Метод `run` параллельно вычисляет функцию `f` на каждом из указанных аргументов (`args`).
* Метод `close` останавливает все рабочие потоки.
* Конструктор `ParallelMapperImpl(int threads)` создает `threads` рабочих потоков, которые могут быть использованы для распараллеливания.
* К одному `ParallelMapperImpl` могут одновременно обращаться несколько клиентов.
* Задания на исполнение накапливаются в очереди и обрабатываться в порядке поступления.
* В реализации нет активных ожиданий.
* `IterativeParallelism` может использовать `ParallelMapper` (конструктор `IterativeParallelism(ParallelMapper)`).
* Методы класса делят работу на `threads` фрагментов и исполняют их при помощи `ParallelMapper`.
* Есть возможность одновременного запуска и работы нескольких клиентов, использующих один `ParallelMapper`.
* При наличии `ParallelMapper` сам `IterativeParallelism` новые потоки не создаёт.

#### Тестирование
* Для того, чтобы протестировать программу:
   * Скачайте
      * тесты
          * [info.kgeorgiy.java.advanced.base.jar](artifacts/info.kgeorgiy.java.advanced.base.jar)
          * [info.kgeorgiy.java.advanced.concurrent.jar](artifacts/info.kgeorgiy.java.advanced.concurrent.jar)
          * [info.kgeorgiy.java.advanced.mapper.jar](artifacts/info.kgeorgiy.java.advanced.mapper.jar)
      * и библиотеки к ним:
          * [junit-4.11.jar](lib/junit-4.11.jar)
          * [hamcrest-core-1.3.jar](lib/hamcrest-core-1.3.jar)
          * [jsoup-1.8.1.jar](lib/jsoup-1.8.1.jar)
          * [quickcheck-0.6.jar](lib/quickcheck-0.6.jar)
   * Откомпилируйте программу
   * Протестируйте программу
      * Текущая директория должна:
         * содержать все скачанные `.jar` файлы;
         * содержать скомпилированные классы;
         * __не__ содержать скомпилированные самостоятельно тесты.
      * IterativeParallelism: ```java -cp . -p . -m info.kgeorgiy.java.advanced.concurrent advanced ru.ifmo.rain.shaposhnikov.concurrent.IterativeParallelism```
      * ParallelMapper: ```java -cp . -p . -m info.kgeorgiy.java.advanced.mapper advanced ru.ifmo.rain.shaposhnikov.concurrent.IterativeParallelism```