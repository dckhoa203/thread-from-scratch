# Phase 1 — Rebuild the Thread Mental Model

## Step 2 — What Belongs to a Thread?

### Mục tiêu

Step 1 phân biệt process với thread. Bây giờ ta hỏi: một thread cần giữ execution state nào để có thể tiếp tục công việc sau khi CPU chuyển sang thread khác, và tài nguyên nào được các thread trong process chia sẻ?

```text
Thread-private execution state
├── stack / call frames
├── current execution position (program counter, conceptually)
└── CPU execution state (registers, when saved across a context switch)

Process-shared state and resources
├── heap objects
├── class/static state
├── loaded code and runtime metadata
└── process resources such as files and sockets
```

Đây là mental model để học concurrency, không phải cam kết về cách mọi JVM lưu từng biến trong bộ nhớ vật lý. JVM có thể tối ưu biến local; CPU/runtime cũng lưu và khôi phục execution state theo cách phụ thuộc implementation.

## 1. Stack và method frame

Khi một thread gọi method, execution cần giữ thông tin cho lời gọi đó: tham số, biến local, điểm quay lại sau khi gọi method khác và trạng thái của call chain.

```java
static void work() {
    int x = 10;
    foo(x);
}
```

Nếu hai thread cùng gọi `work()`, mỗi lời gọi thuộc execution context riêng:

```text
Thread A                         Thread B
└── work() frame                 └── work() frame
    ├── x = 10                       ├── x = 10
    └── foo() frame                  └── foo() frame
```

Hai biến local `x` thuộc hai lời gọi khác nhau. Nếu chúng chỉ là giá trị local và không đưa một shared object vào cuộc chơi, một thread không đọc hay sửa trực tiếp biến local của thread kia.

### Local reference không đồng nghĩa object riêng

```java
List<String> list = sharedList;
```

Biến tham chiếu `list` thuộc lời gọi hiện tại, nhưng nó có thể trỏ tới object được nhiều thread cùng truy cập:

```text
Thread A execution                 Thread B execution
list ───────────────┐               list ───────────────┐
                    └──────┐  ┌─────┘
                           ▼  ▼
                       Shared List object
```

Vì vậy:

```text
local reference != local object
```

Đây là cầu nối tới shared state ở Phase 2. Có local reference không tự làm object mà nó trỏ tới trở thành thread-safe.

## 2. Program counter và execution position

Với code:

```java
foo();
bar();
baz();
```

thread cần tiếp tục đúng execution path sau khi bị tạm dừng. Program counter là cách gọi khái niệm cho vị trí instruction hiện tại của execution.

```text
Thread A
├── stack / call chain
└── execution position ──► current instruction
```

Đây là mental model để hình dung resume. Không cần tưởng tượng mỗi Java `Thread` object chứa một field `PC` mà ta có thể đọc trực tiếp. Chi tiết instruction pointer phụ thuộc JVM, interpreter/JIT và CPU.

## 3. Registers và context switch

Khi thread đang chạy, CPU dùng registers để giữ giá trị và trạng thái execution tạm thời. Nếu scheduler chuyển CPU sang thread khác, hệ thống cần bảo toàn đủ context của thread cũ để tiếp tục nó về sau.

```text
T1 đang chạy
  ↓ save execution context khi chuyển thread
T2 chạy
  ↓ save context của T2
T1 được khôi phục và tiếp tục
```

Conceptually, context có thể bao gồm register state, execution position, stack pointer và các trạng thái liên quan. Cách lưu cụ thể do runtime/OS/CPU quyết định; mục tiêu của step này là hiểu rằng mỗi thread cần resume đúng execution của chính nó.

## 4. Heap và process-shared state

Các thread trong cùng process thường có thể truy cập cùng heap objects và class/static state. Ví dụ:

```java
class App {
    static final Counter counter = new Counter();
}
```

Hai thread có thể cùng lấy `App.counter` và trỏ tới cùng object:

```text
Thread A ──┐
           ├────► Counter object on heap
Thread B ──┘
```

Shared access chưa tự động là lỗi. Nhưng nếu nhiều thread cùng đọc/ghi mutable state, correctness có thể phụ thuộc vào cách các thao tác xen kẽ. Phase 2 sẽ chủ động tạo và phân tích race condition; ở Step này ta chỉ nhận diện object được chia sẻ.

Các thread cũng có thể dùng chung process resources như file descriptors và sockets. Việc cùng truy cập một resource không đồng nghĩa mọi thao tác trên resource đó đều an toàn khi chạy đồng thời.

## 5. Lab — local variable và cùng một shared object

Mở [ThreadOwnershipLab.java](ThreadOwnershipLab.java). `main()` tạo một `Counter` rồi truyền cùng reference đó cho hai worker. Mỗi worker gọi `work()` với một biến local riêng:

```java
Counter sharedCounter = new Counter();
Thread t1 = new Thread(() -> work(sharedCounter), "worker-1");
Thread t2 = new Thread(() -> work(sharedCounter), "worker-2");

static void work(Counter sharedCounter) {
    int localValue = 0;
    localValue++;

    System.out.printf(
            "%s -> localValue=%d, sharedCounterIdentityHash=%x, sharedCounter.value=%d%n",
            Thread.currentThread().getName(),
            localValue,
            System.identityHashCode(sharedCounter),
            sharedCounter.value
    );
}
```

Output có dạng:

```text
worker-1 -> localValue=1, sharedCounterIdentityHash=1a2b3c, sharedCounter.value=0
worker-2 -> localValue=1, sharedCounterIdentityHash=1a2b3c, sharedCounter.value=0
```

Thứ tự hai dòng có thể thay đổi. Mỗi lần gọi `work()` bắt đầu với `localValue` riêng bằng `0` rồi in `1`. Cả hai worker nhận cùng `Counter`; `sharedCounter.value` chỉ được đọc và vẫn là `0`.

`System.identityHashCode` trả về identity hash code, không phải địa chỉ bộ nhớ. Cùng object được thể hiện trực tiếp trong code vì `main()` truyền đúng một biến `sharedCounter` cho cả hai worker; hash chỉ là dấu hiệu dễ quan sát, không phải phép đo địa chỉ tổng quát. Lab không sửa `sharedCounter.value` và không kiểm tra thread safety.

Điều lab minh họa:

- Mỗi lời gọi `work()` có local state riêng.
- Hai lời gọi nhận cùng object qua tham số `sharedCounter`.
- Nó không cho biết chính xác JVM đặt từng local ở đâu trong máy vật lý.

## 6. Concurrency và parallelism

Hai thread có thể tiến triển xen kẽ ngay cả trên một CPU core. Đó là concurrency; không bắt buộc hai lệnh chạy đúng cùng thời điểm.

```text
Một core:  T1 chạy → T2 chạy → T1 chạy tiếp
```

Parallelism là nhiều execution thực sự diễn ra cùng lúc, chẳng hạn trên nhiều core:

```text
Core 0: T1 chạy
Core 1: T2 chạy cùng lúc
```

```text
Concurrency != Parallelism
```

## 7. Java Thread lifecycle — overview

Java biểu diễn state qua `Thread.State`:

```text
NEW --start()--> RUNNABLE --run() ends--> TERMINATED
                   ↕
       BLOCKED / WAITING / TIMED_WAITING
```

Ở Phase 1 chỉ cần nhận diện tên các state. `RUNNABLE` bao gồm thread đang chạy hoặc sẵn sàng chạy theo cách Java biểu diễn; các state chờ có thể quay lại `RUNNABLE` khi điều kiện chờ kết thúc. Chi tiết chuyển state phụ thuộc nguyên nhân chờ và sẽ được đào sâu ở phần monitor, waiting và parking. Việc đọc `getState()` chỉ là snapshot tại một thời điểm, không điều khiển scheduler.

## Mental model tổng hợp

```text
JVM PROCESS
├── shared heap / class state / process resources
├── Thread A
│   ├── stack / call chain
│   ├── execution position
│   └── execution state
└── Thread B
    ├── stack / call chain
    ├── execution position
    └── execution state
```

```text
Threads share access to process memory and resources,
but each thread has its own execution context.
```

### Backend mapping

Trong một Spring Boot process, request A và request B có thể được xử lý trên các thread khác nhau. Mỗi request đi qua call stack riêng, trong khi các thread có thể cùng truy cập service singleton, datasource, cache hoặc client dùng chung.

```text
Request A → Thread A → request call stack ─┐
                                           ├─► shared application objects
Request B → Thread B → request call stack ─┘
```

Đây là lý do “biến local” và “object local” không thể dùng thay cho nhau khi nói về thread safety.

## Checkpoint

**Riêng theo execution context:** call chain/stack và trạng thái cần để tiếp tục execution.

**Có thể được chia sẻ trong process:** heap objects, class/static state và process resources.

Câu hỏi để mở sang Phase 2:

> Nếu hai thread cùng truy cập và sửa một mutable object, kết quả có luôn đúng không?

Phase 1 Step 3 sẽ cô lập experiment về stack-per-thread; sau đó Phase 1 tiếp tục với shared heap. Phase 2 mới chủ động tạo race condition và lost update.

## Chạy lab từ repository root

Với JDK 21:

```bash
mkdir -p /tmp/fundamentals-step2
javac -d /tmp/fundamentals-step2 fundamentals/src/thread/fundamentals/step2/*.java
java -cp /tmp/fundamentals-step2 thread.fundamentals.step2.ThreadOwnershipLab
```
