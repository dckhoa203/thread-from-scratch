# THREAD-FROM-SCRATCH — Revert Branch
## Phase 2 — Break Shared State Intentionally

> Goal: tự tạo race condition, lost update và visibility problem để hiểu tại sao concurrency primitives tồn tại.

---

# 1. Entry condition

Phase 1 established:

```text
Each thread has:
- its own stack
- execution state
- program counter

Threads share:
- heap
- objects
- process resources
```

Now the dangerous part:

```text
multiple execution contexts
        +
shared mutable state
        =
concurrency bugs
```

Phase 2 intentionally creates those bugs.

---

# 2. Learning rule

Do not start with:

```text
synchronized
volatile
AtomicInteger
Lock
```

Start with broken code.

Sequence:

```text
shared state
→ concurrent mutation
→ broken result
→ inspect interleaving
→ classify the problem
→ only then introduce terminology
```

---

# 3. Step 1 — Build a shared Counter

```java
class Counter {
    int value = 0;

    void increment() {
        value++;
    }
}
```

Run:

```java
Counter counter = new Counter();
```

with:

```text
Thread A
Thread B
```

Each performs:

```text
100_000 increments
```

Expected:

```text
200_000
```

Observed:

```text
maybe 183_921
maybe 196_442
maybe 200_000
```

Important:

```text
The bug may disappear sometimes.
```

That is normal for concurrency bugs.

---

# 4. Step 2 — Decompose `value++`

Source:

```java
value++;
```

Looks like one operation.

Conceptually:

```text
READ value
ADD 1
WRITE value
```

Example:

```text
initial value = 10
```

Possible interleaving:

```text
T1 READ 10
T2 READ 10

T1 ADD 1 → 11
T2 ADD 1 → 11

T1 WRITE 11
T2 WRITE 11
```

Final:

```text
11
```

Expected:

```text
12
```

This is:

```text
LOST UPDATE
```

---

# 5. Step 3 — Build an interleaving visualizer

Write an intentionally expanded increment:

```java
void incrementBroken() {
    int current = value;

    Thread.yield();

    int next = current + 1;

    Thread.yield();

    value = next;
}
```

`Thread.yield()` does not guarantee a context switch.

It is only used here to increase the chance of interleaving.

Goal:

```text
make race easier to observe
```

---

# 6. Step 4 — Understand Race Condition

Definition for this track:

```text
A race condition happens when correctness depends
on uncontrollable execution ordering between threads.
```

Example:

```text
Correct if:

T1 read/write completes
before
T2 read/write

Broken if:

operations overlap
```

The key problem is:

```text
schedule-dependent correctness
```

---

# 7. Step 5 — Critical Section

In:

```java
void increment() {
    value++;
}
```

the part touching shared mutable state is:

```java
value++;
```

Call this:

```text
critical section
```

Mental model:

```text
Thread A ──┐
           │
           ▼
    CRITICAL SECTION
           ▲
           │
Thread B ──┘
```

Question for the next phase will become:

```text
How do we guarantee only one thread enters this section?
```

But do not solve it yet.

---

# 8. Step 6 — Atomicity

Atomicity means:

```text
an operation appears indivisible
```

Other threads cannot observe it halfway through.

Bad assumption:

```text
one Java statement = one atomic operation
```

Example:

```java
value++;
```

is one source-code statement but not necessarily one atomic read-modify-write operation.

---

# 9. Step 7 — Atomic vs Compound Operation

Examples conceptually simple:

```java
int x = value;
```

versus compound:

```java
value++;
```

and:

```java
if (balance >= amount) {
    balance -= amount;
}
```

The latter is:

```text
CHECK
then
ACT
```

which is vulnerable to race.

---

# 10. Step 8 — Check-Then-Act bug

Build:

```java
class Account {
    int balance = 100;

    boolean withdraw(int amount) {
        if (balance >= amount) {
            balance -= amount;
            return true;
        }

        return false;
    }
}
```

Two threads:

```text
balance = 100

T1 withdraw(80)
T2 withdraw(80)
```

Possible flow:

```text
T1 checks 100 >= 80 → true
T2 checks 100 >= 80 → true

T1 writes 20
T2 writes 20
```

Both transactions may report success.

Business meaning:

```text
160 withdrawn
from 100 balance
```

This maps much better to backend/banking than a simple counter.

---

# 11. Step 9 — Read-Modify-Write pattern

Recognize this pattern:

```text
READ
MODIFY
WRITE
```

Examples:

```java
counter++;
balance -= amount;
map.put(key, map.get(key) + 1);
```

Any shared mutable read-modify-write deserves suspicion.

---

# 12. Step 10 — Visibility Problem

Now use a different bug.

```java
class Worker {
    boolean running = true;

    void run() {
        while (running) {
        }
    }

    void stop() {
        running = false;
    }
}
```

Run:

```text
Thread A → run()
Thread B → stop()
```

Naive expectation:

```text
Thread B writes false
→ Thread A immediately sees false
→ loop exits
```

But Java concurrency requires us to think about visibility guarantees.

Do not dive into full Java Memory Model yet.

Only establish:

```text
A write by one thread is not automatically
something you should reason about as
"instantly visible everywhere".
```

---

# 13. Step 11 — Atomicity vs Visibility

This distinction must become explicit.

## Atomicity

Question:

```text
Can another thread interleave with this operation?
```

Example:

```java
counter++;
```

Problem:

```text
lost update
```

---

## Visibility

Question:

```text
When one thread writes a value,
when is another thread guaranteed to see it?
```

Example:

```java
running = false;
```

Problem:

```text
reader may not have the required visibility guarantee
```

---

# 14. Step 12 — Ordering

Third concept:

```text
Ordering
```

Compiler/JIT/CPU may reorder operations when allowed, as long as single-thread semantics are preserved.

For this phase, do not go deep into barriers or happens-before.

Only establish three independent questions:

```text
1. Atomicity
2. Visibility
3. Ordering
```

They are not the same problem.

---

# 15. Step 13 — First glimpse of Java Memory Model

Do not teach JMM fully.

Use this mental model only:

```text
In single-threaded code:
program order is easy to reason about.

In multi-threaded code:
we need explicit synchronization rules
to define what writes become visible
and in what order.
```

Save for later:

```text
happens-before
volatile
monitor enter/exit
final field semantics
memory barriers
```

---

# 16. Step 14 — Why `volatile` does NOT fix everything

Show:

```java
volatile int value = 0;
```

Then:

```java
value++;
```

Still broken for atomicity.

Why:

```text
volatile can help visibility/order semantics
but value++ is still READ + ADD + WRITE
```

Mental model:

```text
visibility solved
≠
compound operation becomes atomic
```

This distinction is critical.

---

# 17. Step 15 — Why `AtomicInteger` exists

Only now introduce:

```java
AtomicInteger
```

Not as the main solution yet.

Just observe:

```java
AtomicInteger counter = new AtomicInteger();

counter.incrementAndGet();
```

Question:

```text
How can this update happen atomically
without using synchronized?
```

This creates the bridge to Phase 3:

```text
Compare-And-Set
```

Do not deep dive CAS in Phase 2.

---

# 18. Required labs

Suggested structure:

```text
fundamentals/
└── phase-2-shared-state/
    ├── 01-shared-counter/
    ├── 02-lost-update/
    ├── 03-check-then-act/
    ├── 04-visibility/
    ├── 05-volatile-counter/
    └── 06-atomic-counter-preview/
```

---

## Lab 1 — Shared Counter

Target:

```text
expected = 200_000
actual < expected
```

Repeat many runs.

Record results.

---

## Lab 2 — Forced Interleaving

Expand:

```java
value++;
```

into:

```java
read
pause/yield
modify
pause/yield
write
```

Goal:

```text
make lost update observable
```

---

## Lab 3 — Bank Account

```java
withdraw()
```

Run two concurrent withdrawals.

Goal:

```text
show business invariant violation
```

Invariant:

```text
balance must never be withdrawn beyond available amount
```

---

## Lab 4 — Visibility Flag

Create:

```java
boolean running
```

One writer thread.

One reader loop.

Then compare later with:

```java
volatile boolean running
```

Do not focus on reproducing identical behavior on every machine.

Focus on memory semantics.

---

## Lab 5 — Volatile Counter

```java
volatile int counter;
counter++;
```

Show:

```text
volatile does not guarantee atomic compound operations
```

---

## Lab 6 — AtomicInteger Preview

Replace:

```java
int counter
```

with:

```java
AtomicInteger
```

Observe correct result.

Do not inspect implementation yet.

Leave question:

```text
How does AtomicInteger achieve this?
```

---

# 19. Production mapping

## Singleton Spring Bean

Typical Spring service:

```java
@Service
class PaymentService {
}
```

Bean instance is shared across request threads.

Mental model:

```text
Request Thread A ─┐
                  │
                  ▼
            PaymentService
                  ▲
                  │
Request Thread B ─┘
```

Therefore mutable instance fields can be dangerous.

---

## Connection Pool

Multiple threads share:

```text
HikariPool
```

Pool itself must coordinate access safely.

Later we can connect this to:

```text
locks
conditions
parking
waiting threads
pool exhaustion
```

---

## Banking mapping

Example invariant:

```text
account balance >= 0
```

Without correct concurrency control:

```text
check balance
↓
context switch
↓
another transaction modifies balance
↓
first transaction continues with stale assumption
```

This maps to:

```text
race condition
lost update
check-then-act
transaction isolation
```

Later this will bridge nicely to DB-FROM-SCRATCH.

---

# 20. Questions to answer before leaving Phase 2

1. Why is shared mutable state dangerous?
2. Why is `value++` not automatically atomic?
3. What is an interleaving?
4. What is a race condition?
5. What is a lost update?
6. What is a critical section?
7. What is a check-then-act race?
8. Difference between atomicity and visibility?
9. What is ordering?
10. Why does `volatile int counter; counter++` still break?
11. What kind of problem does `volatile` address?
12. Why does `AtomicInteger` suggest there is a lower-level atomic primitive underneath?
13. Why can concurrency bugs be intermittent?

---

# 21. Exit criteria

You are ready for Phase 3 when this feels obvious:

```text
We have:

multiple threads
+
shared mutable state
+
uncontrolled scheduling

Therefore we need:

a mechanism that can control access
to a critical section.
```

The next question becomes:

```text
Can we build a lock ourselves?
```

That is Phase 3:

```text
naive lock
→ break the lock
→ CAS
→ spin lock
→ CPU waste
→ blocking
```

---

# 22. Phase 2 final mental model

```text
                  SHARED HEAP
                      │
                      ▼
                 mutable state
                      ▲
                ┌─────┴─────┐
                │           │
             Thread A    Thread B
                │           │
                └─────┬─────┘
                      ▼
              uncontrolled order
                      │
         ┌────────────┼────────────┐
         ▼            ▼            ▼
     Atomicity    Visibility    Ordering
         │
         ▼
   Race Condition
         │
         ▼
   Broken Invariant
```

Next:

```text
protect the critical section
```

without blindly using `synchronized`.

We build the reason for locking first.
