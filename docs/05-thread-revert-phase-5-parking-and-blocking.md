# THREAD-FROM-SCRATCH — Revert Branch
## Phase 5 — Parking, Blocking States, Interrupts, and the Merge Point

> Goal: đi xuống dưới monitor-level API để hiểu "thread waits" nghĩa là gì ở execution/scheduler level, rồi gặp lại Branch B — Virtual Thread Scheduler.

---

# 1. Entry condition

Phase 4 established:

```text
synchronized
→ monitor ownership
→ BLOCKED contenders

wait()
→ release monitor
→ wait-set
→ WAITING
```

Now ask:

```text
What does it mean for execution to stop?
```

And:

```text
Who wakes it?
```

This phase compares:

```text
sleep
wait
park
unpark
interrupt
```

and prepares the merge with Virtual Thread.

---

# 2. Target mental model

After Phase 5:

```text
logical thread
    │
    ├── RUNNABLE
    ├── BLOCKED
    ├── WAITING
    └── TIMED_WAITING
```

and:

```text
waiting ≠ consuming CPU continuously
```

Most importantly:

```text
Platform Thread waiting
vs
Virtual Thread waiting
```

becomes the merge question.

---

# 3. Step 1 — Revisit Java Thread States

States:

```text
NEW
RUNNABLE
BLOCKED
WAITING
TIMED_WAITING
TERMINATED
```

Focus now on:

```text
BLOCKED
WAITING
TIMED_WAITING
```

---

# 4. Step 2 — `BLOCKED`

Typical cause:

```java
synchronized (lock) {
}
```

but another thread owns monitor.

Mental model:

```text
I want to enter critical section
but monitor is unavailable.
```

Important:

```text
BLOCKED is specifically tied to intrinsic monitor acquisition
in Java Thread.State terminology.
```

---

# 5. Step 3 — `WAITING`

Examples:

```java
Object.wait()
Thread.join()
LockSupport.park()
```

Conceptual meaning:

```text
I cannot make progress until some event/signal occurs.
```

No timeout required.

---

# 6. Step 4 — `TIMED_WAITING`

Examples:

```java
Thread.sleep(...)
Object.wait(timeout)
Thread.join(timeout)
LockSupport.parkNanos(...)
LockSupport.parkUntil(...)
```

Conceptual meaning:

```text
wait for event
OR
deadline
```

---

# 7. Step 5 — `Thread.sleep()`

```java
Thread.sleep(1000);
```

Mental model:

```text
current thread asks not to run
until timeout expires
```

Important:

```text
sleep has no monitor-release semantics
```

If thread holds a monitor:

```text
it keeps holding it
```

---

# 8. Step 6 — `Object.wait()`

```java
synchronized (lock) {
    lock.wait();
}
```

Adds semantics beyond generic waiting:

```text
release this object's monitor
+
wait in this monitor's condition wait-set
+
reacquire monitor before return
```

So:

```text
wait()
```

is a coordination operation tied to monitor semantics.

---

# 9. Step 7 — Why introduce `LockSupport.park()`?

We want a lower-level conceptual primitive:

```text
stop this thread's execution
until permitted to continue
```

Java exposes:

```java
LockSupport.park();
```

and:

```java
LockSupport.unpark(thread);
```

Unlike `Object.wait()`:

```text
park does not require owning an Object monitor
```

This is a major simplification for building higher-level synchronizers.

---

# 10. Step 8 — Park / Unpark mental model

Conceptually, a thread has a permit-like state.

```text
permit available?
```

If yes:

```text
park consumes permit and returns
```

If no:

```text
park waits
```

`unpark(thread)`:

```text
makes a permit available / allows future progress
```

This avoids a simple "signal lost because thread was not waiting yet" mental model.

---

# 11. Step 9 — `unpark()` before `park()`

Important experiment:

```java
LockSupport.unpark(t);
```

before thread later executes:

```java
LockSupport.park();
```

The subsequent park may return immediately because permit is already available.

Mental model:

```text
unpark is not simply:
"wake right now"

it grants permission to pass one park
```

---

# 12. Step 10 — Park is not a condition variable by itself

Bad mental model:

```text
park/unpark solves all synchronization
```

No.

You still need:

```text
state
ownership
queueing
conditions
memory semantics
```

High-level locks use park/unpark as part of a larger protocol.

---

# 13. Step 11 — Interrupt

A thread may be waiting/blocking and another thread may request interruption:

```java
thread.interrupt();
```

Important mental model:

```text
interrupt is cooperative cancellation / signaling
```

Not:

```text
force-kill thread
```

Behavior depends on API.

---

# 14. Step 12 — Interrupt and `sleep()`

If sleeping thread is interrupted:

```java
Thread.sleep(...)
```

throws:

```java
InterruptedException
```

and interrupt status handling follows Java API semantics.

Lab this explicitly.

---

# 15. Step 13 — Interrupt and `wait()`

Waiting thread:

```java
lock.wait();
```

can wake because:

```text
notify
notifyAll
interrupt
timeout (for timed wait)
spurious wakeup
```

Important:

```text
wait can end for multiple reasons
```

Another reason to recheck condition.

---

# 16. Step 14 — Interrupt and `park()`

`park()` can return when interrupted.

But unlike `sleep()` / `wait()`, usage pattern and interrupt-status behavior differs.

Goal here is not memorizing edge cases.

Goal:

```text
park is a low-level building block,
not a business-level waiting API.
```

---

# 17. Step 15 — Build a tiny parking lock

Conceptual exercise:

```java
class ParkingLock {
    AtomicBoolean locked;
    Queue<Thread> waiters;
}
```

Pseudo:

```text
lock():
    if CAS succeeds:
        return

    enqueue current thread

    while cannot acquire:
        park()

unlock():
    release ownership
    unpark one waiter
```

Do not try to make production-correct lock.

Goal:

```text
replace spin waiting
with queued + parked waiting
```

---

# 18. Step 16 — Compare Spin vs Park

## Spin

```text
cannot acquire
→ keep executing
→ keep retrying
```

## Park

```text
cannot acquire
→ stop active execution
→ scheduler can run other work
→ signal/unpark
→ retry
```

This is the key scheduler bridge.

---

# 19. Step 17 — Queue of waiters

A blocking synchronizer usually needs some representation of:

```text
who is waiting?
```

Conceptual:

```text
Lock
├── owner
└── wait queue
    ├── T2
    ├── T3
    └── T4
```

Unlock:

```text
release state
→ select waiter
→ unpark
```

This sets up future study of:

```text
AbstractQueuedSynchronizer
ReentrantLock
Semaphore
CountDownLatch
```

but those are outside the mandatory pre-merge path.

---

# 20. Step 18 — Scheduler connection

A parked thread is not useful work on CPU.

Conceptually:

```text
RUNNING
   ↓ park
WAITING
   ↓ unpark
RUNNABLE
   ↓ scheduled
RUNNING
```

Now Branch A has finally reached:

```text
scheduler-visible execution state
```

This is where Branch B starts becoming directly relevant.

---

# 21. Step 19 — Platform Thread waiting

Mental model:

```text
Java Platform Thread
        │
        ▼
     OS Thread
```

If operation blocks at OS/thread level:

```text
platform thread unavailable for other Java work
during that block
```

This is the old cost model behind:

```text
large thread pools
connection/request thread sizing
```

---

# 22. Step 20 — Virtual Thread question appears

Branch B has already introduced:

```text
Virtual Thread
→ Continuation
→ Carrier Thread
→ Scheduler
```

Now Branch A asks:

```text
If a Virtual Thread parks or blocks...

does the Carrier Thread also have to remain blocked?
```

That is the merge point.

---

# 23. MERGE POINT

```text
BRANCH A — REVERT TO FUNDAMENTALS

Thread
→ shared state
→ lock
→ monitor
→ wait
→ park
→ blocking
               \
                \
                 ▼
          WHAT HAPPENS
       WHEN EXECUTION BLOCKS?
                 ▲
                /
               /
BRANCH B — VIRTUAL THREAD SCHEDULER

OS Thread
→ Thread Pool
→ Virtual Thread
→ Continuation
→ Carrier Thread
→ Scheduler
```

The two branches now talk about the same event from two sides.

---

# 24. The first merged comparison

## Platform Thread

```text
Java thread
    │
    ▼
OS thread
    │
blocking
    ▼
execution resource remains tied up
```

## Virtual Thread

Conceptually for supported blocking points:

```text
Virtual Thread
     │ mounted
     ▼
Carrier Thread
     │
     ▼
park / blocking operation
     │
Virtual Thread can unmount
     │
Carrier can run another Virtual Thread
```

This is the mechanism that should now "click" because Branch A supplied the meaning of waiting.

---

# 25. Pinning preview

Important caveat:

```text
Virtual Thread blocking
does not always imply
carrier is immediately reusable in every situation.
```

This opens the next merged topic:

```text
pinning
```

Do not deep dive in Branch A.

Pinning belongs after merge.

---

# 26. Required labs

Suggested structure:

```text
parking/
└── phase-5-parking-and-blocking/
    ├── 01-blocked-vs-waiting/
    ├── 02-sleep/
    ├── 03-wait/
    ├── 04-park-unpark/
    ├── 05-unpark-before-park/
    ├── 06-interrupt-sleep/
    ├── 07-interrupt-wait/
    ├── 08-interrupt-park/
    └── 09-mini-parking-lock/
```

---

# 27. Lab — State Matrix

Create deterministic examples for:

```text
BLOCKED
WAITING
TIMED_WAITING
```

Observe with:

```java
thread.getState()
```

Document:

```text
what API caused the state
what event releases it
whether monitor is held/released
```

---

# 28. Lab — park/unpark

T1:

```java
LockSupport.park();
```

T2:

```java
LockSupport.unpark(t1);
```

Print timestamps around park.

Goal:

```text
see execution stop and resume
```

---

# 29. Lab — unpark before park

Issue permit first.

Then call park.

Observe immediate/near-immediate return.

Goal:

```text
understand permit semantics
```

---

# 30. Lab — interrupt comparison

Compare:

```text
sleep
wait
park
```

under interrupt.

Record:

```text
exception?
interrupt flag?
return behavior?
monitor requirements?
```

---

# 31. Production mapping

## Hikari / DB pool

When no connection is available:

```text
request thread
→ cannot progress
→ wait
```

The interesting production question is no longer only:

```text
"how many connections?"
```

but:

```text
what happens to each waiting execution context?
```

This matters greatly when comparing:

```text
platform threads
vs
virtual threads
```

---

## Oracle blocking I/O

Your backend often waits on:

```text
DB socket
stored procedure
network
connection pool
```

Branch A now gives vocabulary:

```text
waiting
parking
blocking
wake-up
interrupt
```

Branch B gives:

```text
carrier
scheduler
mount/unmount
```

Now the two can be analyzed together.

---

# 32. Exit criteria

Branch A is complete when you can explain:

1. `BLOCKED` vs `WAITING` vs `TIMED_WAITING`.
2. Why `sleep()` is different from `wait()`.
3. Why `wait()` is tied to an object monitor.
4. Why `park()` is a lower-level primitive.
5. What `unpark()` conceptually does.
6. Why unpark-before-park matters.
7. What interrupt is conceptually.
8. Why blocking can be better than spinning.
9. Why blocking requires wake-up/queueing semantics.
10. What scheduler does after a waiting thread becomes runnable.
11. Why platform-thread blocking and virtual-thread blocking have different resource costs.
12. Why this is the exact point to merge with Virtual Thread scheduler knowledge.

---

# 33. Final mental model

```text
CONDITION / RESOURCE unavailable
            │
            ▼
       cannot progress
            │
      ┌─────┴─────┐
      │           │
    SPIN         PARK
      │           │
 burn CPU     stop active
              execution
                  │
               signal
                  │
               runnable
                  │
               scheduler
```

Branch A ends here.

The next document should no longer be "Revert Branch".

It should be a merged track:

```text
BLOCKING ON PLATFORM THREAD
vs
BLOCKING ON VIRTUAL THREAD
```

followed by:

```text
mount/unmount
pinning
scheduler behavior
Oracle/Hikari production mapping
```
