# thread.overall

Package này là phần mở đầu: nhìn lại hành trình từ `Thread` truyền thống, `ThreadPool`, cho tới `VirtualThread`.

Mục tiêu không phải là viết scheduler, mà là thấy rõ vì sao Java cần virtual thread.

## Bước 1: Một request thường làm gì?

Trong `FakeRequestSimulation`, mỗi request giả lập:

```text
dbCall()       -> sleep 50ms
partnerCall() -> sleep 200ms
```

Tổng thời gian chờ là khoảng 250ms/request.

Vấn đề nằm ở chỗ: khi dùng platform thread truyền thống, trong lúc request đang `sleep`, thread đó vẫn bị chiếm. Nó không làm CPU work, nhưng vẫn giữ tài nguyên OS thread.

## Bước 2: Tạo quá nhiều OS thread sẽ nổ

`OsThreadExplosion` tạo thread liên tục:

```java
Thread t = new Thread(...);
t.start();
```

Mỗi platform thread là một OS thread thật. OS thread có stack memory, kernel resource, scheduling cost.

Nếu tạo quá nhiều:

```text
1 request = 1 OS thread
100_000 request = 100_000 OS thread
```

hệ thống sẽ quá tải.

Ý chính:

```text
Platform thread đắt.
Không thể scale bằng cách tạo vô hạn OS thread.
```

## Bước 3: Thread pool giảm số thread, nhưng có starvation

`ThreadPoolStarvation` dùng:

```java
Executors.newFixedThreadPool(4)
```

Có 100 task, nhưng chỉ có 4 worker thread.

Luồng chạy:

```text
task 0..3   -> chạy ngay
task 4..99  -> nằm chờ trong queue
```

Nếu 4 task đầu đều `sleep(10_000)`, thì 96 task còn lại phải chờ, dù CPU có thể đang rảnh.

Ý chính:

```text
Thread pool tiết kiệm OS thread.
Nhưng blocking task có thể giữ worker quá lâu.
Worker bị chiếm thì task khác bị đói.
```

## Bước 4: Virtual thread giải quyết bài toán blocking nhiều

`VirtualThreadDemo` dùng:

```java
Executors.newVirtualThreadPerTaskExecutor()
```

Ý tưởng:

```text
1 task/request = 1 virtual thread
nhiều virtual thread chạy trên ít platform thread
```

Khi virtual thread bị blocking ở các API hỗ trợ virtual thread, JVM có thể unmount nó khỏi carrier thread. Carrier thread được dùng để chạy virtual thread khác.

Nói nôm na:

```text
Virtual thread rẻ hơn OS thread rất nhiều.
Blocking không nhất thiết giữ carrier thread mãi.
```

## Bước 5: Virtual thread không phải phép màu cho CPU-bound

`ContextSwitchHell` tạo nhiều virtual thread chạy vòng lặp CPU:

```java
while (true) {
    Math.sin(System.nanoTime());
}
```

Đây là CPU-bound workload. Nó không chờ I/O, không sleep, không nhường CPU.

Virtual thread giúp tốt nhất với:

```text
blocking I/O
sleep
request chờ database/network
```

Virtual thread không biến 8 core CPU thành 20_000 core CPU.

## Bước Nhảy Quan Trọng

Package này cho thấy lịch sử vấn đề:

```text
OS thread:
  dễ hiểu nhưng đắt

Thread pool:
  giới hạn OS thread nhưng dễ starvation khi task blocking

Virtual thread:
  tạo task/thread rẻ hơn, phù hợp nhiều blocking task

CPU-bound:
  vẫn bị giới hạn bởi số core thật
```

Từ đây chuyển sang `virtualthread.simple`: ta thử tự viết một scheduler nhỏ để hiểu ý tưởng "nhiều task nhẹ chạy trên ít worker thread".
