# THREAD-FROM-SCRATCH
## Branch A — Revert to Fundamentals

> Purpose: quay lại nền tảng của Thread để hiểu chắc shared state, locking, monitor, waiting và parking trước khi merge với Branch B — Virtual Thread Scheduler.

---

# Why this branch exists

Branch B đã đi từ phía scheduler:

```text
OS Thread
→ Thread Pool
→ Virtual Thread
→ Continuation
→ Carrier Thread
→ Scheduler
→ Work Stealing
```

Nhưng để hiểu sâu:

```text
blocking
parking
mount/unmount
pinning
```

cần một mental model từ phía Thread fundamentals.

Branch A đi theo chiều ngược lại:

```text
Thread
→ Shared State
→ Race
→ Lock
→ Monitor
→ Wait
→ Park
→ Blocking
```

Hai nhánh gặp nhau tại:

```text
WHAT HAPPENS WHEN EXECUTION BLOCKS?
```

---

# Branch map

```text
                     THREAD-FROM-SCRATCH

         BRANCH A                           BRANCH B
   REVERT TO FUNDAMENTALS            VIRTUAL THREAD SCHEDULER

        Thread                              OS Thread
          │                                    │
    Shared State                          Thread Pool
          │                                    │
         Race                           Virtual Thread
          │                                    │
         Lock                           Continuation
          │                                    │
       Monitor                         Carrier Thread
          │                                    │
    wait / notify                         Scheduler
          │                                    │
    park / unpark                      Work Stealing
          │                                    │
       blocking                                │
          └──────────────┐      ┌──────────────┘
                         ▼      ▼
                          MERGE
               What happens when execution
                       blocks / waits?
```

---

# Phase 1 — Rebuild the Thread Mental Model

File:

```text
01-thread-revert-phase-1-thread-mental-model.md
```

Core flow:

```text
Process vs Thread
→ Thread-private state
→ shared heap
→ stack
→ Program Counter
→ context switch
→ concurrency vs parallelism
→ lifecycle
→ start() vs run()
```

Primary question:

```text
What exactly is a Thread?
```

Exit mental model:

```text
multiple execution contexts
can access the same heap objects
```

---

# Phase 2 — Break Shared State Intentionally

File:

```text
02-thread-revert-phase-2-shared-state.md
```

Core flow:

```text
shared mutable state
→ value++
→ interleaving
→ lost update
→ race condition
→ critical section
→ atomicity
→ visibility
→ ordering
→ volatile preview
→ AtomicInteger preview
```

Primary question:

```text
What goes wrong when multiple threads
modify the same state?
```

Exit mental model:

```text
correctness can depend on uncontrollable scheduling
```

---

# Phase 3 — Build a Lock From Scratch

File:

```text
03-thread-revert-phase-3-build-a-lock.md
```

Core flow:

```text
need mutual exclusion
→ naive boolean lock
→ break the lock
→ check-then-set race
→ CAS
→ spin lock
→ busy waiting
→ yield
→ sleep polling
→ ownership
→ reentrancy preview
→ fairness preview
```

Primary question:

```text
How can we stop two threads
from entering the same critical section?
```

Exit problem:

```text
spin lock may be correct enough for exclusion,
but waiting threads burn CPU
```

---

# Phase 4 — Blocking and Java Object Monitor

File:

```text
04-thread-revert-phase-4-monitor-and-wait-notify.md
```

Core flow:

```text
spin is wasteful
→ blocking
→ monitor ownership
→ synchronized
→ BLOCKED contenders
→ reentrancy
→ lock alone is insufficient
→ wait-set
→ wait()
→ notify()
→ notifyAll()
→ while vs if
→ spurious wakeup
→ producer/consumer
```

Primary question:

```text
How does a thread wait for a condition
without keeping the monitor or burning CPU?
```

Key mental model:

```text
Object Monitor
├── owner
├── contenders
└── wait-set
```

---

# Phase 5 — Parking and Blocking States

File:

```text
05-thread-revert-phase-5-parking-and-blocking.md
```

Core flow:

```text
BLOCKED
vs
WAITING
vs
TIMED_WAITING
→ sleep
→ wait
→ park
→ unpark
→ interrupt
→ waiter queue
→ blocking synchronizer
→ scheduler
```

Primary question:

```text
What does "thread waits" mean
at execution/scheduler level?
```

Final bridge:

```text
logical thread waits
→ scheduler must stop running it
→ later make it runnable again
```

---

# Branch A learning rules

Every phase follows:

```text
Problem
→ naive implementation
→ intentionally break it
→ observe
→ WHY
→ build mechanism
→ Java abstraction
→ trade-off
→ production mapping
```

Avoid:

```text
API memorization first
```

Prefer:

```text
problem creates the need for the API
```

---

# Repository suggestion

```text
thread-from-scratch/
│
├── README.md
│
├── docs/
│   ├── 00-thread-revert-branch-a-root.md
│   ├── 01-thread-revert-phase-1-thread-mental-model.md
│   ├── 02-thread-revert-phase-2-shared-state.md
│   ├── 03-thread-revert-phase-3-build-a-lock.md
│   ├── 04-thread-revert-phase-4-monitor-and-wait-notify.md
│   └── 05-thread-revert-phase-5-parking-and-blocking.md
│
├── fundamentals/
│   ├── phase-1-thread-mental-model/
│   └── phase-2-shared-state/
│
├── locking/
│   └── phase-3-build-a-lock/
│
├── monitor/
│   └── phase-4-object-monitor/
│
├── parking/
│   └── phase-5-parking-and-blocking/
│
├── virtual-thread/
│   └── ...
│
└── merge/
    └── ...
```

---

# Full knowledge dependency

```text
PHASE 1
What is a Thread?
     │
     ▼
PHASE 2
Why shared state breaks
     │
     ▼
PHASE 3
How mutual exclusion can be built
     │
     ▼
PHASE 4
How monitor-based blocking/coordination works
     │
     ▼
PHASE 5
How waiting maps to parking and scheduling
     │
     ▼
========================
      MERGE POINT
========================
     │
     ▼
Platform Thread blocking
vs
Virtual Thread blocking
     │
     ▼
mount / unmount
     │
     ▼
pinning
     │
     ▼
scheduler under blocking
     │
     ▼
production mapping
Oracle / Hikari / Gateway
```

---

# What Branch A intentionally does NOT cover before merge

These topics are useful but are not mandatory before merging:

```text
ReentrantLock internals
Condition internals
Semaphore
CountDownLatch
AQS deep dive
StampedLock
ReadWriteLock
full Java Memory Model
memory barriers
VarHandle internals
```

Reason:

```text
They are abstractions or deeper implementations
built on concepts Branch A already establishes.
```

They can be studied after the merge.

---

# Merge condition

Do not merge until these feel obvious:

```text
Thread owns execution-local state
but shares heap
```

```text
shared mutable state + uncontrolled scheduling
can break correctness
```

```text
lock gives mutual exclusion
```

```text
spin waits actively
```

```text
monitor allows ownership + condition coordination
```

```text
wait releases monitor
```

```text
park stops active execution
until progress becomes possible
```

```text
a waiting thread eventually has to become runnable
and be scheduled again
```

When all of those are clear, Branch A has completed its job.

---

# The merge question

The first merged lesson should begin with exactly this question:

```text
When a Java thread blocks...

what physical execution resource
is actually being occupied?
```

Then compare:

```text
Platform Thread
Java Thread ≈ OS Thread
```

with:

```text
Virtual Thread
Virtual Thread → mounted on Carrier Thread
```

From there:

```text
blocking
→ park
→ continuation
→ unmount
→ carrier reuse
→ remount
```

becomes one continuous mental model instead of two separate topics.

---

# Production destination

The final goal is not only JVM theory.

The merged track should return to real backend situations:

```text
HTTP request
→ Virtual Thread
→ Hikari connection acquisition
→ Oracle query / stored procedure
→ socket wait
→ connection pool exhaustion
→ timeout
→ carrier behavior
→ pinning risk
→ throughput / latency
```

and:

```text
TCP Gateway
→ many concurrent requests
→ waiting I/O
→ platform thread model
vs
virtual thread model
```

Branch A supplies:

```text
what waiting/locking/blocking means
```

Branch B supplies:

```text
how Virtual Thread scheduler handles that execution
```

The merge supplies:

```text
the complete runtime picture.
```
