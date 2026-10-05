# THREAD-FROM-SCRATCH — Revert Branch
## Phase 3 — Build a Lock From Scratch

> Goal: không học `synchronized` trước. Tự build một lock sai, phá nó, sửa dần bằng atomic primitive, rồi chạm tới giới hạn của spin waiting.

---

# 1. Entry condition

Phase 2 đã cho ta problem:

```text
multiple threads
+
shared mutable state
+
uncontrolled scheduling
=
race condition
```

Ví dụ:

```java
class Counter {
    int value;

    void increment() {
        value++;
    }
}
```

Ta đã thấy:

```text
READ
MODIFY
WRITE
```

có thể bị interleave.

Question bây giờ:

```text
How do we make a critical section exclusive?
```

---

# 2. Target mental model

Sau Phase 3 phải hiểu flow:

```text
critical section
      ↓
need mutual exclusion
      ↓
naive boolean lock
      ↓
lock itself races
      ↓
need atomic state transition
      ↓
CAS
      ↓
spin lock
      ↓
correctness improves
      ↓
but CPU burns while waiting
      ↓
need blocking / parking
```

Đây là bridge sang Phase 4.

---

# 3. Learning rule

Không dùng ngay:

```text
synchronized
ReentrantLock
Semaphore
```

Ta phải tự gặp problem trước.

Sequence:

```text
build
→ break
→ observe
→ identify missing guarantee
→ add primitive
→ observe new limitation
```

---

# 4. Step 1 — Define the lock contract

Trước khi code, phải biết lock cần guarantee gì.

## Minimal contract

```text
lock()
→ current thread acquires exclusive access

unlock()
→ current thread releases access
```

Critical section:

```java
lock.lock();

try {
    counter++;
} finally {
    lock.unlock();
}
```

Desired guarantee:

```text
At most one thread is inside the protected section.
```

Keyword:

```text
MUTUAL EXCLUSION
```

---

# 5. Step 2 — Build the naive boolean lock

```java
class NaiveLock {

    private boolean locked = false;

    void lock() {
        while (locked) {
        }

        locked = true;
    }

    void unlock() {
        locked = false;
    }
}
```

At first glance:

```text
if unlocked
→ enter
→ mark locked
```

Looks reasonable.

It is broken.

---

# 6. Step 3 — Break the lock itself

Initial:

```text
locked = false
```

Possible interleaving:

```text
T1 reads locked = false
T2 reads locked = false

T1 exits while
T2 exits while

T1 writes locked = true
T2 writes locked = true
```

Now:

```text
T1 enters critical section
T2 enters critical section
```

The lock violated its own contract.

Key insight:

```text
A lock implementation can itself have a race condition.
```

---

# 7. Step 4 — The real operation we need

Naive lock does:

```text
CHECK:
is locked == false?

THEN:
set locked = true
```

That is another:

```text
check-then-act
```

We need this whole transition to be atomic:

```text
false → true
```

as one indivisible operation.

Question:

```text
Can hardware / JVM provide an atomic state transition?
```

This leads to:

```text
Compare-And-Set
```

---

# 8. Step 5 — CAS mental model

Conceptual API:

```text
CAS(memory, expected, newValue)
```

Meaning:

```text
if currentValue == expected
    set currentValue = newValue
    return true
else
    return false
```

Crucially:

```text
compare + write
```

happen atomically.

Example:

```text
current = false

T1 CAS(false, true) → success
T2 CAS(false, true) → fail
```

Now only one thread wins.

---

# 9. Step 6 — Java representation

Use:

```java
AtomicBoolean
```

Example:

```java
class SpinLock {

    private final AtomicBoolean locked = new AtomicBoolean(false);

    void lock() {
        while (!locked.compareAndSet(false, true)) {
        }
    }

    void unlock() {
        locked.set(false);
    }
}
```

Mental model:

```text
CAS success
→ lock acquired

CAS fail
→ someone else owns it
→ retry
```

---

# 10. Step 7 — Why this is now safer

With:

```java
compareAndSet(false, true)
```

two threads cannot both transition:

```text
false → true
```

successfully.

Possible sequence:

```text
T1 CAS(false,true) → success

T2 CAS(false,true) → fail
T2 CAS(false,true) → fail
T2 CAS(false,true) → fail
```

Mutual exclusion is now much stronger than with the naive boolean lock.

---

# 11. Step 8 — Build the shared counter again

Use:

```java
class Counter {
    int value;
}
```

Protect:

```java
lock.lock();

try {
    value++;
} finally {
    lock.unlock();
}
```

Run:

```text
T1 → 100_000 increments
T2 → 100_000 increments
```

Expected:

```text
200_000
```

Compare:

```text
without lock
vs
naive lock
vs
CAS spin lock
```

---

# 12. Step 9 — What did the lock actually do?

Without lock:

```text
T1 READ
T2 READ
T1 WRITE
T2 WRITE
```

With working lock:

```text
T1 acquire
T1 READ
T1 WRITE
T1 release

T2 acquire
T2 READ
T2 WRITE
T2 release
```

The lock did not make:

```java
value++;
```

magically atomic.

Instead it guaranteed:

```text
only one thread executes that compound operation at a time
```

Important distinction:

```text
atomic primitive
≠
critical section protected by lock
```

CAS is used to build the mechanism.

The lock protects arbitrary code.

---

# 13. Step 10 — Spin waiting

Current implementation:

```java
while (!locked.compareAndSet(false, true)) {
}
```

What is the losing thread doing?

Answer:

```text
RUNNING
RUNNING
RUNNING
RUNNING
```

It repeatedly consumes CPU.

This is:

```text
BUSY WAITING
```

or:

```text
SPINNING
```

---

# 14. Step 11 — Observe CPU waste

Create:

```text
T1 acquires lock
T1 sleeps for 5 seconds while holding lock

T2 tries to acquire lock
```

Pseudo:

```java
lock.lock();

try {
    Thread.sleep(5000);
} finally {
    lock.unlock();
}
```

T2:

```java
lock.lock();
```

With spin lock:

```text
T2 keeps executing CAS attempts for ~5 seconds
```

Question:

```text
Is this a good use of CPU?
```

Usually no for long waits.

---

# 15. Step 12 — Spin lock trade-off

Spinning is not always useless.

It can make sense when:

```text
expected wait is extremely short
context switch cost may be higher than waiting
contention is low
CPU budget is acceptable
```

Bad when:

```text
lock may be held for milliseconds/seconds
many threads contend
CPU is limited
critical section contains blocking I/O
```

---

# 16. Step 13 — Add Thread.yield()

Try:

```java
while (!locked.compareAndSet(false, true)) {
    Thread.yield();
}
```

Question:

```text
Did we solve waiting?
```

No.

`yield()` is only a scheduling hint.

It does not give us:

```text
sleep until lock becomes available
```

Important:

```text
yield != blocking primitive
```

---

# 17. Step 14 — Add sleep polling

Try:

```java
while (!locked.compareAndSet(false, true)) {
    Thread.sleep(1);
}
```

Now CPU burn decreases.

But we introduced:

```text
polling
```

Problems:

```text
latency
arbitrary sleep interval
wasted wakeups
unnecessary scheduling
```

If sleep too short:

```text
still wasteful
```

If sleep too long:

```text
lock becomes free
but waiter reacts late
```

---

# 18. Step 15 — The missing capability

What we actually want:

```text
If lock unavailable:
    stop executing

When lock becomes available:
    wake me up
```

That is conceptually:

```text
BLOCK
+
SIGNAL
```

This question should emerge naturally:

```text
How can a thread stop consuming CPU
while waiting for a condition?
```

That becomes Phase 4.

---

# 19. Step 16 — Ownership problem

Our current `SpinLock` has another issue.

```java
void unlock() {
    locked.set(false);
}
```

Any thread can call `unlock()`.

Example:

```text
T1 acquires
T2 calls unlock
```

Our implementation allows it.

A real lock usually needs ownership semantics.

Mental model:

```text
Lock
├── locked?
└── owner = Thread X
```

Do not fully solve yet.

Just identify:

```text
mutual exclusion is not the whole lock design
```

---

# 20. Step 17 — Reentrancy problem preview

Scenario:

```java
void outer() {
    lock.lock();

    try {
        inner();
    } finally {
        lock.unlock();
    }
}

void inner() {
    lock.lock();
    ...
}
```

Same thread tries to acquire same lock twice.

With our simple spin lock:

```text
T1 already owns lock
T1 CAS(false,true) forever fails
```

Result:

```text
self-deadlock / infinite spin
```

This introduces:

```text
REENTRANCY
```

Do not solve in Phase 3.

Save it for monitor / `ReentrantLock`.

---

# 21. Step 18 — Fairness problem preview

Suppose:

```text
T1 releases
```

waiting:

```text
T2
T3
T4
```

Who acquires next?

Our spin lock says:

```text
whoever wins CAS
```

No fairness guarantee.

Possible:

```text
T2 waits a long time
while T3/T4 repeatedly win
```

This introduces:

```text
STARVATION
FAIRNESS
```

Again, only identify.

---

# 22. Step 19 — Memory visibility preview

Question:

```text
If T1 modifies shared state inside lock,
will T2 see those writes after acquiring?
```

A real synchronization primitive needs both:

```text
mutual exclusion
+
memory visibility semantics
```

With Java atomic classes, there are memory semantics involved.

But do not turn Phase 3 into full JMM study.

Save the formal model for later.

---

# 23. Required labs

Suggested structure:

```text
locking/
└── phase-3-build-a-lock/
    ├── 01-naive-boolean-lock/
    ├── 02-break-naive-lock/
    ├── 03-cas-introduction/
    ├── 04-spin-lock/
    ├── 05-spin-cpu-cost/
    ├── 06-yield-spin/
    ├── 07-sleep-polling/
    ├── 08-owner-problem/
    └── 09-reentrancy-preview/
```

---

# 24. Lab 1 — Naive Lock

Implement:

```java
class NaiveLock
```

Protect shared counter.

Run multiple times.

Goal:

```text
prove a lock can be incorrectly implemented
```

---

# 25. Lab 2 — Force the naive lock race

Expand:

```java
if (!locked) {
    locked = true;
}
```

with controlled pauses / barriers.

Goal:

```text
make two threads both believe they acquired the lock
```

Use logging like:

```text
T1 sees false
T2 sees false
T1 sets true
T2 sets true
```

---

# 26. Lab 3 — CAS Spin Lock

Implement:

```java
class SpinLock
```

using:

```java
AtomicBoolean.compareAndSet
```

Test mutual exclusion.

---

# 27. Lab 4 — Long critical section

Thread 1:

```text
acquire
sleep 5s
release
```

Thread 2:

```text
attempt acquire
```

Observe:

```text
CPU behavior
number of CAS retries
```

Optional counter:

```java
long spins;
```

Increment per failed CAS.

Print final spin count.

---

# 28. Lab 5 — Yield vs Pure Spin

Compare:

```text
pure spin
```

with:

```text
spin + Thread.yield()
```

Observe:

```text
correctness unchanged
scheduling behavior may differ
no real blocking guarantee
```

---

# 29. Lab 6 — Sleep Polling

Compare:

```java
Thread.sleep(1)
Thread.sleep(10)
Thread.sleep(100)
```

Observe trade-off:

```text
CPU usage
vs
wake-up latency
```

---

# 30. Lab 7 — Wrong-owner unlock

Demonstrate:

```text
T1 acquires
T2 unlocks
```

Show that our custom lock permits invalid ownership.

Record this as a missing feature.

---

# 31. Lab 8 — Reentrancy failure

Same thread:

```text
acquire
→ call nested method
→ acquire again
```

Observe:

```text
infinite spin
```

Question:

```text
How would a reentrant lock know
that the current owner is acquiring again?
```

Expected future model:

```text
owner
+
hold count
```

---

# 32. Important vocabulary

By the end of this phase:

```text
mutual exclusion
critical section
lock ownership
CAS
compare-and-set
spin lock
busy waiting
polling
contention
fairness
starvation
reentrancy
blocking
```

---

# 33. Production mapping

## Case 1 — In-memory shared state

```text
Spring singleton
+
mutable field
+
many request threads
```

Need coordination.

But avoid immediately assuming:

```text
just synchronize everything
```

Trade-offs matter.

---

## Case 2 — Connection Pool

Imagine 10 DB connections:

```text
available = 10
```

11th request arrives.

Bad design:

```text
while (noConnection) {
    keep checking;
}
```

That resembles spinning / polling.

Better high-level behavior:

```text
no connection
→ waiter sleeps / parks
→ connection returned
→ wake waiter
```

This is the exact bridge to your Oracle / Hikari experience.

---

## Case 3 — TCP / gateway

If many request threads contend on one shared structure:

```text
spin
```

can waste CPU while useful work is blocked elsewhere.

This becomes more important in I/O-heavy backend systems.

---

# 34. Phase 3 comparison table

| Approach | Correct mutual exclusion? | CPU while waiting | Wake latency | Main problem |
|---|---:|---:|---:|---|
| no lock | No | N/A | N/A | race |
| naive boolean lock | No | High | Low | lock itself races |
| CAS spin lock | Yes, basic case | High | Very low | burns CPU |
| CAS + yield | Yes, basic case | Variable | Variable | still not real blocking |
| CAS + sleep polling | Yes, basic case | Lower | Worse | polling latency |

---

# 35. What NOT to conclude yet

Do not conclude:

```text
CAS is always better than synchronized
```

Wrong.

Do not conclude:

```text
spin lock is production-ready
```

Wrong.

Do not conclude:

```text
AtomicBoolean == full lock implementation
```

Wrong.

Phase 3 only teaches:

```text
how atomic state transition can establish ownership
```

and:

```text
why spinning is not enough
```

---

# 36. Exit criteria

Do not move to Phase 4 until you can explain:

1. Why the naive boolean lock is broken.
2. Why check-then-set must be atomic.
3. What CAS does.
4. Why CAS can be used to build a lock.
5. What a spin lock is.
6. Why a spin lock can waste CPU.
7. Why `yield()` is not blocking.
8. Why sleep-based waiting is polling.
9. Why lock ownership matters.
10. What reentrancy means.
11. What fairness/starvation mean.
12. Why a useful lock needs more than a boolean flag.

---

# 37. Final mental model

```text
Shared State
    │
    ▼
Critical Section
    │
    ▼
Need Mutual Exclusion
    │
    ▼
Naive Boolean Lock
    │
    ├── check
    └── set
       not atomic
    │
    ▼
CAS
    │
    ▼
Spin Lock
    │
    ├── correct ownership competition
    └── busy waiting
    │
    ▼
CPU Waste
    │
    ▼
Need:
BLOCK + SIGNAL
```

---

# 38. Bridge to Phase 4

Next phase starts with one question:

```text
A thread cannot acquire the lock.

Instead of spinning...

Can we remove it from active execution
and wake it only when progress becomes possible?
```

That leads to:

```text
blocking
thread state
monitor
synchronized
entry set
wait set
wait / notify
```

Phase 4 will be the first place where `Object.wait()` starts to make structural sense instead of looking like an isolated API.
