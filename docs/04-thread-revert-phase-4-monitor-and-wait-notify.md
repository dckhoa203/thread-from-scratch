# THREAD-FROM-SCRATCH — Revert Branch
## Phase 4 — Blocking, Java Object Monitor, `synchronized`, `wait/notify`

> Goal: đi từ giới hạn của spin lock sang blocking, sau đó hiểu Java Object Monitor như một cơ chế gồm ownership, contention và condition waiting.

---

# 1. Entry condition

Phase 3 kết thúc ở:

```text
critical section
→ naive lock
→ CAS
→ spin lock
→ busy waiting
→ CPU waste
```

Câu hỏi mở:

```text
Nếu thread chưa lấy được lock,
tại sao nó phải tiếp tục chạy?

Có thể dừng execution của nó
và chỉ đánh thức khi cần không?
```

Đây là nơi:

```text
blocking
monitor
synchronized
wait / notify
```

bắt đầu xuất hiện tự nhiên.

---

# 2. Target mental model

Sau Phase 4 phải tự vẽ được:

```text
                 OBJECT MONITOR

                    owner
                      │
                      ▼
              ┌─────────────┐
              │   monitor   │
              └─────────────┘
                ▲         ▲
                │         │
          contenders    wait-set
             │            │
          T2 T3 T4      T5 T6
```

Và hiểu hai loại "waiting" khác nhau:

```text
waiting to ACQUIRE monitor
vs
waiting for CONDITION
```

---

# 3. Step 1 — From spinning to blocking

Spin lock:

```java
while (!locked.compareAndSet(false, true)) {
}
```

losing thread vẫn:

```text
RUNNABLE
→ consuming CPU
```

Desired behavior:

```text
cannot progress
→ stop active execution
→ scheduler can run something else
```

Mental shift:

```text
WAITING is not failure.

Waiting can be an optimization.
```

---

# 4. Step 2 — Introduce monitor ownership

Conceptually:

```text
monitor:
    owner = one thread or none
```

A thread entering a protected region must acquire ownership.

```text
monitor free
→ T1 acquires
→ owner = T1
```

Another thread:

```text
T2 attempts acquire
→ monitor owned by T1
→ T2 cannot enter
```

Now we need a place/state for T2.

---

# 5. Step 3 — `synchronized`

Start with:

```java
synchronized (lock) {
    counter++;
}
```

Mental model:

```text
monitorenter(lock)
      ↓
critical section
      ↓
monitorexit(lock)
```

Do not treat `synchronized` as magic syntax.

Think:

```text
acquire object's monitor
→ execute
→ release monitor
```

---

# 6. Step 4 — Monitor contention

Example:

```java
synchronized (lock) {
    Thread.sleep(5000);
}
```

T1 owns monitor.

T2 reaches:

```java
synchronized (lock)
```

but cannot enter.

Observe:

```java
T2.getState()
```

Expected conceptual state:

```text
BLOCKED
```

Important:

```text
BLOCKED
=
waiting to acquire an intrinsic monitor
```

This is different from:

```text
WAITING
```

which we will reach through `wait()`.

---

# 7. Step 5 — Build the entry contention model

Mental model:

```text
                  monitor
                    │
              owner = T1
                    │
        ┌───────────┴───────────┐
        │                       │
       T2                      T3
    BLOCKED                  BLOCKED
```

When T1 releases:

```text
T2 / T3 compete
```

Do not assume FIFO unless explicitly guaranteed.

---

# 8. Step 6 — `synchronized` method

Instance method:

```java
synchronized void increment() {
    value++;
}
```

Equivalent mental model:

```java
synchronized (this) {
    value++;
}
```

Static synchronized method:

```java
static synchronized void work() {
}
```

Mental model:

```text
monitor belongs to Class object
```

roughly:

```java
synchronized (MyClass.class) {
}
```

---

# 9. Step 7 — Reentrancy

Recall Phase 3 problem:

```text
same thread acquires same lock twice
```

Intrinsic monitor is reentrant.

Example:

```java
synchronized void outer() {
    inner();
}

synchronized void inner() {
}
```

Same thread can enter again.

Mental model:

```text
monitor
├── owner = T1
└── hold count = N
```

Nested acquire:

```text
hold count++
```

Exit:

```text
hold count--
```

Monitor released when count returns to zero.

---

# 10. Step 8 — Why lock alone is not enough

Build:

```java
class Box {
    Item item;
}
```

Consumer:

```java
synchronized (lock) {
    if (item == null) {
        // what now?
    }
}
```

Problem:

```text
consumer owns monitor
but condition is false
```

If consumer just loops while holding monitor:

```java
while (item == null) {
}
```

producer cannot acquire monitor to create the item.

Result:

```text
logical deadlock / no progress
```

We need:

```text
"condition false,
I should release the monitor
and wait."
```

This is the purpose of:

```java
wait()
```

---

# 11. Step 9 — Why `wait()` belongs to `Object`

Every object can participate in intrinsic monitor synchronization.

Therefore:

```java
lock.wait();
lock.notify();
lock.notifyAll();
```

operate on the same object's monitor/wait-set semantics.

Important connection:

```text
Object
├── identity/state
└── monitor coordination semantics
```

---

# 12. Step 10 — `wait()`

Correct usage:

```java
synchronized (lock) {
    lock.wait();
}
```

Mental model:

```text
T1 owns monitor
      ↓
T1 calls wait()
      ↓
release monitor
      ↓
enter lock's wait-set
      ↓
WAITING
```

Critical property:

```text
wait() RELEASES the monitor
```

This is why producer/other threads can enter.

---

# 13. Step 11 — `IllegalMonitorStateException`

This is invalid:

```java
lock.wait();
```

without owning the monitor.

Why?

Because `wait()` conceptually performs:

```text
release monitor that I currently own
```

If current thread is not the owner:

```text
IllegalMonitorStateException
```

Same principle applies to:

```java
notify()
notifyAll()
```

---

# 14. Step 12 — Wait-set

Mental model:

```text
Object lock
│
└── monitor
    ├── owner
    ├── entry contenders
    └── wait-set
         ├── T2
         ├── T3
         └── T4
```

Important distinction:

```text
entry contenders:
waiting to ACQUIRE monitor

wait-set:
threads that OWNED monitor,
called wait(),
released it,
and now wait for condition/signal
```

---

# 15. Step 13 — `notify()`

Producer:

```java
synchronized (lock) {
    item = new Item();
    lock.notify();
}
```

Mental model:

```text
one waiting thread
is selected for wake-up eligibility
```

But:

```text
notify()
!=
immediate execution
```

Producer still owns the monitor until it exits synchronized block.

---

# 16. Step 14 — Wake-up does not mean lock acquired

Flow:

```text
T1 consumer
→ wait()
→ WAITING

T2 producer
→ acquire monitor
→ update condition
→ notify()

T1 becomes eligible
BUT T2 still owns monitor

T2 exits
→ monitor released

T1 competes to reacquire
→ succeeds
→ wait() returns
```

Mental model:

```text
wake
→ reacquire
→ continue
```

not:

```text
wake
→ immediately run
```

---

# 17. Step 15 — `notifyAll()`

If wait-set contains:

```text
T1
T2
T3
```

then:

```java
lock.notifyAll();
```

makes all waiters eligible.

But only one can own monitor at a time.

```text
wake many
→ compete
→ one acquires
```

---

# 18. Step 16 — Why `while`, not `if`

Bad:

```java
synchronized (lock) {
    if (item == null) {
        lock.wait();
    }

    consume(item);
}
```

Correct pattern:

```java
synchronized (lock) {
    while (item == null) {
        lock.wait();
    }

    consume(item);
}
```

Reasons:

```text
1. thread may wake spuriously
2. another thread may consume/change condition first
3. notification does not mean your exact condition is true
```

Rule:

```text
wake-up means:
"check again"

not:
"condition guaranteed"
```

---

# 19. Step 17 — Spurious wakeup

Java specification allows a waiting thread to wake without the expected notification condition.

Therefore condition waiting follows:

```text
while (!condition) {
    wait();
}
```

This is not defensive coding style only.

It is part of the coordination model.

---

# 20. Step 18 — Producer/Consumer from scratch

Build:

```java
class OneSlotBuffer<T> {

    private T item;

    synchronized void put(T value) throws InterruptedException {
        while (item != null) {
            wait();
        }

        item = value;
        notifyAll();
    }

    synchronized T take() throws InterruptedException {
        while (item == null) {
            wait();
        }

        T result = item;
        item = null;

        notifyAll();

        return result;
    }
}
```

Do not optimize yet.

Goal:

```text
understand monitor coordination
```

---

# 21. Step 19 — `sleep()` while holding monitor

Compare:

```java
synchronized (lock) {
    Thread.sleep(5000);
}
```

with:

```java
synchronized (lock) {
    lock.wait();
}
```

Difference:

```text
sleep()
→ thread pauses
→ monitor still held

wait()
→ thread waits
→ monitor released
```

This distinction must become automatic.

---

# 22. Step 20 — Monitor and memory visibility preview

A successful monitor handoff gives synchronization semantics.

Conceptually:

```text
T1 writes shared state
→ exits synchronized

T2 later enters synchronized on same monitor
→ sees synchronized-before writes according to JMM rules
```

Do not deep dive JMM here.

Just recognize:

```text
monitor is not only mutual exclusion

it also participates in memory visibility ordering
```

---

# 23. Required labs

Suggested structure:

```text
monitor/
└── phase-4-object-monitor/
    ├── 01-synchronized-basic/
    ├── 02-monitor-contention/
    ├── 03-blocked-state/
    ├── 04-reentrancy/
    ├── 05-wait-release-monitor/
    ├── 06-notify/
    ├── 07-notify-all/
    ├── 08-if-vs-while/
    └── 09-one-slot-buffer/
```

---

# 24. Lab — Observe BLOCKED

T1:

```java
synchronized (lock) {
    Thread.sleep(5000);
}
```

T2 attempts same monitor.

Observer prints:

```java
t2.getState()
```

Goal:

```text
see BLOCKED as observable runtime state
```

---

# 25. Lab — Prove `wait()` releases monitor

T1:

```java
synchronized (lock) {
    lock.wait();
}
```

T2:

```java
synchronized (lock) {
    System.out.println("T2 acquired");
}
```

If T2 can enter while T1 waits:

```text
wait released monitor
```

---

# 26. Lab — Prove `sleep()` does not release monitor

T1:

```java
synchronized (lock) {
    Thread.sleep(5000);
}
```

T2 tries same monitor.

Observe:

```text
T2 remains BLOCKED
```

---

# 27. Lab — notify timing

Inside notifier:

```java
synchronized (lock) {
    lock.notify();

    Thread.sleep(3000);
}
```

Observe:

```text
waiter does NOT continue immediately after notify
```

It still needs monitor reacquisition.

---

# 28. Production mapping

## Connection pool analogy

```text
pool has no available connection
```

Bad:

```text
spin forever
```

Better:

```text
request thread waits
connection returned
waiter becomes eligible
```

Real libraries may use different primitives, but the coordination shape is similar.

---

## Backend shared resource

```text
many request threads
→ one shared guarded structure
```

Monitor contention can produce:

```text
BLOCKED threads
increased latency
reduced throughput
```

This will later connect to:

```text
thread dumps
lock contention
p99 latency
```

---

# 29. Exit criteria

Before Phase 5, explain without notes:

1. What is an object monitor?
2. What is monitor ownership?
3. What does `synchronized` conceptually do?
4. What is `BLOCKED`?
5. Difference between monitor contender and wait-set waiter?
6. Why does `wait()` require monitor ownership?
7. Why does `wait()` release monitor?
8. Why does `sleep()` not solve condition coordination?
9. What does `notify()` actually guarantee?
10. Why does notified thread need to reacquire monitor?
11. Difference between `notify()` and `notifyAll()`?
12. Why must condition waiting use `while`?
13. What is reentrancy?

---

# 30. Final mental model

```text
                         Object
                           │
                           ▼
                        Monitor
             ┌─────────────┼─────────────┐
             │             │             │
           owner       contenders      wait-set
             │          BLOCKED         WAITING
             │             │             │
             └─────────────┴─────────────┘
                           │
                       coordination
```

Phase 5 asks the next lower-level question:

```text
When a thread "waits",
how does execution actually stop?

What is the relationship between:

sleep
wait
park
unpark
interrupt
scheduler?
```
