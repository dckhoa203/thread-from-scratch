# Phase 1 — Rebuild the Thread Mental Model

## Step 1 — Process vs Thread

### Mục tiêu

Sau step này, hãy tự giải thích được:

1. Java application đang chạy là process hay thread?
2. `main()` là thread hay code được một thread thực thi?
3. `new Thread(...)` tạo gì, và `start()` làm gì?
4. Nhiều thread có thể thuộc cùng một process không?

Step này chỉ dựng phân biệt giữa **process** và **thread**. Stack, program counter, registers và shared heap sẽ được bóc ở step tiếp theo.

## Mental model

Khi chạy một Java application, operating system tạo một process cho JVM. JVM khởi chạy `main()` trên thread tên `main`.

```text
Operating system
└── JVM process (process id)
    ├── main thread ── executes main()
    ├── worker thread ── executes its Runnable
    └── other JVM threads may also exist
```

- **Process** là một chương trình đang chạy với không gian địa chỉ và tài nguyên của process.
- **Thread** là một luồng thực thi bên trong process.
- Các thread trong cùng process chia sẻ nhiều tài nguyên của process. Mỗi thread có trạng thái thực thi riêng; phần đó sẽ học ở Step 2.
- `main()` là method. Thread `main` là execution context chạy method đó: **code != thread**.

## 1. Quan sát process và main thread

Chạy `Main`. Chương trình in process ID và tên thread hiện tại rồi thoát:

```java
public class Main {
    public static void main(String[] args) {
        System.out.printf(
                "process id=%d, current thread=%s%n",
                ProcessHandle.current().pid(),
                Thread.currentThread().getName()
        );
    }
}
```

Kết quả có dạng:

```text
process id=12345, current thread=main
```

Process ID thay đổi mỗi lần chạy. `main` là tên thread đang thực thi `main()`.

## 2. Tạo thêm một thread

Chạy `Step1`. So sánh process ID và thread name được in từ `main` và `worker`:

```java
long processId = ProcessHandle.current().pid();

System.out.printf(
        "process id=%d, current thread=%s%n",
        processId,
        Thread.currentThread().getName()
);

Thread worker = new Thread(
        () -> System.out.printf(
                "process id=%d, current thread=%s%n",
                ProcessHandle.current().pid(),
                Thread.currentThread().getName()
        ),
        "worker"
);

System.out.println("before start: worker state=" + worker.getState());
worker.start();
```

Output có dạng sau. Dòng `main` và trạng thái `NEW` được in trước khi gọi `start()`; worker chỉ có thể in sau đó:

```text
process id=12345, current thread=main
before start: worker state=NEW
process id=12345, current thread=worker
```

Cùng process ID nhưng tên thread khác cho thấy `main` và `worker` là hai thread trong cùng một JVM process. JVM cũng có thể tạo các thread nội bộ khác, nên ví dụ này chỉ khẳng định ta đã tạo thêm `worker`, không khẳng định process chỉ có hai thread.

`new Thread(...)` tạo Java `Thread` object ở trạng thái `NEW`; nó chưa chạy task. `start()` yêu cầu runtime khởi chạy execution path mới. Runtime quyết định lúc nào worker thực sự được chạy, vì vậy đừng dựa vào thứ tự output giữa `main` và `worker`.

## 3. Lab: nhiều thread trong cùng process

Chạy `ProcessVsThreadLab` để in process ID, thread name và thread ID của `main`, `worker-1`, `worker-2`:

```text
MAIN -> process id=12345, thread name=main, thread id=1
T1 -> process id=12345, thread name=worker-1, thread id=20
T2 -> process id=12345, thread name=worker-2, thread id=21
```

Process ID và thread ID trong ví dụ chỉ minh họa; giá trị thực tế phụ thuộc lần chạy và môi trường. Thứ tự hai worker cũng không cố định. Hãy quan sát rằng cả ba dòng có cùng process ID, còn thread name/ID khác nhau.

### Câu hỏi tự kiểm tra

- `main()` là gì? Thread `main` là gì?
- Sau `new Thread(...)` nhưng trước `start()`, worker đang ở state nào?
- `start()` có đảm bảo worker chạy ngay lập tức không?
- `main`, `worker-1` và `worker-2` thuộc cùng hay khác process?

### Chạy từ repository root

Các class nằm trong package `thread.fundamentals.step1`, nên cần dùng fully qualified class name khi chạy. Với JDK 21:

```bash
mkdir -p /tmp/fundamentals-step1
javac -d /tmp/fundamentals-step1 fundamentals/src/thread/fundamentals/step1/*.java

java -cp /tmp/fundamentals-step1 thread.fundamentals.step1.Main
java -cp /tmp/fundamentals-step1 thread.fundamentals.step1.Step1
java -cp /tmp/fundamentals-step1 thread.fundamentals.step1.ProcessVsThreadLab
```

## Ghi nhớ

```text
Process = chương trình đang chạy cùng không gian địa chỉ và tài nguyên
Thread  = luồng thực thi bên trong process
Code    = thứ được thread thực thi; code không phải thread
```

Step 2 sẽ trả lời câu hỏi tiếp theo: những gì thuộc riêng từng thread, và những gì được chia sẻ trong process?
