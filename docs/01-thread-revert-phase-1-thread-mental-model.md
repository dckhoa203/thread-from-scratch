# THREAD-FROM-SCRATCH — Revert Branch
## Phase 1 — Rebuild the Thread Mental Model

> Goal: quay lại phần đầu tiên của thread để xây mental model chắc chắn trước khi tiếp tục monitor, wait/notify, parking và Virtual Thread.

---

## 1. Why this phase exists

Ở nhánh Virtual Thread, ta đã đi khá xa:

```text
OS Thread
→ Thread Pool
→ Virtual Thread
→ Continuation
→ Carrier Thread
→ Scheduler
→ Work Stealing
```

Nhưng nếu chưa thật sự chắc:

```text
Thread là gì?
Thread sở hữu gì?
Thread chia sẻ gì?
Thread state thay đổi như thế nào?
Scheduler thực sự schedule cái gì?
```

thì phần Virtual Thread phía sau dễ trở thành kiến thức "nhớ cơ chế" thay vì hiểu từ nền tảng.

Phase này là một **revert branch**.

Ta chưa học lock.
Ta chưa học `synchronized`.
Ta chưa học `wait/notify`.

Ta chỉ dựng lại object quan trọng nhất của concurrency:

```text
THREAD
```

---

# 2. Target mental model

Sau Phase 1 phải tự vẽ được:

```text
PROCESS
│
├── Heap
│   └── shared by threads
│
├── OS resources
│   ├── file descriptors
│   ├── sockets
│   └── process address space
│
├── Thread A
│   ├── Stack
│   ├── Program Counter
│   └── Registers
│
└── Thread B
    ├── Stack
    ├── Program Counter
    └── Registers
```

Và hiểu:

```text
Thread != code
Thread != task
Thread != CPU core

Thread = execution context
```

---

# 3. Learning rule

Mỗi step giữ format:

```text
Problem
→ naive assumption
→ experiment
→ observe
→ WHY
→ mental model
→ map to Java
→ map to production
```

Không nhảy vào API trước khi hiểu problem.

---

# 4. Step 1 — Process vs Thread

## Problem

Một Java application đang chạy có:

```java
public static void main(String[] args) {
    while (true) {
    }
}
```

Question:

```text
Application này là process hay thread?
```

Answer:

```text
Java application process
└── main thread
```

Sau đó tạo thêm:

```java
Thread t = new Thread(() -> {
    while (true) {
    }
});

t.start();
```

Mental model:

```text
JVM Process
│
├── main thread
└── t thread
```

---

## Experiment

In ra:

```java
System.out.println(Thread.currentThread().getName());
```

ở:

- `main`
- thread mới

Expected:

```text
main
Thread-0
```

---

## Questions

Phải trả lời được:

1. Process là gì?
2. Thread là gì?
3. Tại sao một process có thể có nhiều thread?
4. Thread có address space riêng hay dùng chung process?
5. Nếu một thread crash thì chuyện gì có thể xảy ra với process?

---

# 5. Step 2 — What belongs to a Thread?

Ta tách thành hai nhóm.

## Private execution state

Một thread cần tối thiểu:

```text
Stack
Program Counter
CPU register state
```

Mental model:

```text
Thread A
│
├── stack
├── current instruction
└── execution state
```

---

## Shared process state

Các thread trong cùng process có thể truy cập:

```text
Heap
Static fields
Objects
File descriptors
Sockets
Process resources
```

Mental model:

```text
Thread A ─┐
          │
          ▼
         HEAP
          ▲
          │
Thread B ─┘
```

Đây chính là nguồn gốc của concurrency problem ở Phase 2.

---

# 6. Step 3 — Stack is per-thread

## Experiment

```java
static void work(String threadName) {
    int local = 0;

    for (int i = 0; i < 5; i++) {
        local++;
        System.out.println(threadName + " local=" + local);
    }
}
```

Run bằng hai thread.

Question:

```text
local của Thread A và Thread B có dùng chung không?
```

Answer:

```text
No.
```

Mental model:

```text
Thread A Stack          Thread B Stack

local = 5               local = 5
i                       i
frames                  frames
```

---

## Important distinction

```java
int local;
```

không tự động có nghĩa "thread-safe".

Nếu local reference trỏ tới shared object:

```java
List<String> list = sharedList;
```

thì reference nằm trên stack nhưng object vẫn ở heap.

Mental model:

```text
Thread A stack
    │
    │ reference
    ▼
Shared Object on Heap
    ▲
    │ reference
Thread B stack
```

---

# 7. Step 4 — Heap is shared

## Build the first shared object

```java
class Counter {
    int value = 0;
}
```

```java
Counter counter = new Counter();
```

Pass cùng object cho hai thread:

```java
Thread t1 = new Thread(() -> {
    System.out.println(counter.value);
});

Thread t2 = new Thread(() -> {
    System.out.println(counter.value);
});
```

Mental model:

```text
T1 stack ──┐
           ▼
        Counter
        value = 0
           ▲
T2 stack ──┘
```

Ở Phase này **chưa modify concurrently**.

Chỉ cần hiểu:

```text
same object
same heap
multiple execution contexts
```

---

# 8. Step 5 — Program Counter

Một thread cần biết:

```text
"Tôi đang chạy instruction nào?"
```

Ví dụ:

```java
foo();
bar();
baz();
```

Thread có thể đang dừng giữa:

```text
bar()
```

Scheduler chuyển CPU sang thread khác.

Khi quay lại, thread phải biết tiếp tục từ đâu.

Mental model:

```text
Thread
├── stack
├── registers
└── program counter
       │
       ▼
 current instruction
```

Đây là foundation để sau này hiểu:

```text
continuation
yield
park
mount
unmount
```

---

# 9. Step 6 — Context Switch

## Scenario

Một CPU core:

```text
Core 0
```

Hai runnable threads:

```text
T1
T2
```

Scheduler có thể:

```text
T1 runs
↓
save T1 context

T2 runs
↓
save T2 context

T1 resumes
```

Conceptually:

```text
SAVE:
- registers
- instruction position
- execution state

RESTORE:
- registers
- instruction position
- execution state
```

---

## Critical insight

Concurrency không cần multiple cores.

Ngay cả:

```text
1 CPU core
2 threads
```

vẫn có concurrency vì execution bị interleave.

```text
T1
T1
T2
T2
T1
T2
```

Parallelism là chuyện khác:

```text
Core 0 → T1
Core 1 → T2
```

---

# 10. Step 7 — Concurrency vs Parallelism

## Concurrency

```text
multiple tasks make progress over overlapping time
```

Có thể chỉ có một CPU core.

Example:

```text
T1 ──run──pause────run────
T2 ───────run──run────pause
```

---

## Parallelism

```text
multiple executions happen physically at the same time
```

Example:

```text
Core 0 → T1
Core 1 → T2
```

Important:

```text
Concurrency != Parallelism
```

---

# 11. Step 8 — Java Thread Lifecycle

Java exposes:

```java
Thread.State
```

States:

```text
NEW
RUNNABLE
BLOCKED
WAITING
TIMED_WAITING
TERMINATED
```

Ở Phase 1 chỉ cần overview.

Chưa đào sâu:

```text
BLOCKED
WAITING
TIMED_WAITING
```

vì chúng sẽ quay lại ở phase monitor / coordination.

---

## Basic lifecycle lab

```java
Thread t = new Thread(() -> {
    System.out.println("running");
});
```

Before:

```java
t.start();
```

observe:

```text
NEW
```

After start:

```text
RUNNABLE
```

After completion:

```text
TERMINATED
```

---

# 12. Step 9 — start() vs run()

Build:

```java
Thread t = new Thread(() -> {
    System.out.println(Thread.currentThread().getName());
});
```

Case A:

```java
t.run();
```

Case B:

```java
t.start();
```

Observe difference.

Mental model:

```text
run()
→ normal method call
→ current thread executes it

start()
→ create/start execution path
→ new thread eventually calls run()
```

This distinction must be crystal clear.

---

# 13. Step 10 — Thread is execution, not work

Bad mental model:

```text
Thread = task
```

Better model:

```text
Task = work description
Thread = execution resource/context
```

Example:

```java
Runnable task = () -> doWork();
```

Same task can be executed by:

```text
Thread A
Thread B
Thread Pool Worker
Virtual Thread
```

This distinction is essential before Thread Pool and Virtual Thread.

---

# 14. Lab structure

Suggested package:

```text
fundamentals/
└── phase-1-thread-mental-model/
    ├── 01-process-vs-thread/
    ├── 02-thread-local-stack/
    ├── 03-shared-heap/
    ├── 04-context-switch/
    ├── 05-concurrency-vs-parallelism/
    ├── 06-thread-state/
    └── 07-start-vs-run/
```

---

# 15. Required experiments

## Experiment A — Thread identity

Print:

```java
Thread.currentThread().getName()
Thread.currentThread().threadId()
```

from multiple threads.

---

## Experiment B — Stack isolation

Each thread owns its own:

```java
int localCounter
```

Observe no sharing.

---

## Experiment C — Shared heap

Both threads reference:

```java
Counter counter
```

Do not mutate concurrently yet.

Just prove shared reference.

---

## Experiment D — start vs run

Compare:

```java
t.run();
```

and:

```java
t.start();
```

using thread names.

---

## Experiment E — state observer

One thread observes another:

```java
t.getState()
```

at different lifecycle points.

---

# 16. Production mapping

Map this phase to backend code.

Example request processing:

```text
HTTP Request A
      │
      ▼
Thread A stack
      │
      ├── controller frame
      ├── service frame
      ├── repository frame
      │
      ▼
Shared Heap
      │
      ├── singleton beans
      ├── connection pool
      ├── caches
      └── shared objects
```

Another request:

```text
HTTP Request B
      │
      ▼
Thread B stack
      │
      └──────────────┐
                     ▼
                 Shared Heap
```

This is the first bridge to:

```text
race condition
shared mutable state
thread safety
```

---

# 17. Exit criteria

Do not move to Phase 2 until you can explain without notes:

1. What is a thread?
2. What is a process?
3. What is private to a thread?
4. What is shared between threads?
5. Why does every thread need a stack?
6. Why is Program Counter necessary?
7. What is a context switch?
8. Can concurrency exist on one CPU core?
9. Difference between concurrency and parallelism?
10. Difference between `start()` and `run()`?
11. Why is `Runnable` not a Thread?
12. Why does shared heap create concurrency problems?

---

# 18. Final mental model

```text
                 JVM PROCESS

       shared address space / heap
                  │
      ┌───────────┼───────────┐
      │           │           │
   Object A    Object B    Object C
      ▲                       ▲
      │                       │
      │                       │
 Thread 1                 Thread 2
 ├── stack                ├── stack
 ├── PC                   ├── PC
 └── registers            └── registers
```

The next problem naturally appears:

```text
What happens when Thread 1 and Thread 2
modify the SAME object at the SAME time?
```

That is Phase 2.
