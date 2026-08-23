# virtualthread.continuation

Package này là bước sâu hơn sau `virtualthread.cooperative`.

Nếu `cooperative` cho thấy một task có thể tự lưu `State`, `return`, rồi được chạy lại sau, thì `continuation` gom pattern đó thành một abstraction rõ hơn:

```text
Continuation = một computation có thể được resume nhiều lần.
```

Demo này vẫn là mô phỏng thủ công, không phải continuation thật của JVM. Nhưng nó làm rõ hơn khái niệm cốt lõi: một task không chỉ là `run()` một lần, mà là một chuỗi các lần `resume()`.

## Cooperative Là Gì?

Trong `virtualthread.cooperative`, task tự quản lý state:

```text
state = START
run Step 1
state = AFTER_SLEEP
Scheduler.sleep(this, 1000)
return

sau đó scheduler đưa task lại queue

state = AFTER_SLEEP
run Step 2
state = DONE
```

Điểm chính:

```text
task tự biết mình đang ở đâu
task tự gọi sleep/yield
task return để nhường worker
scheduler chỉ đưa task lại queue
```

Nó dễ hiểu, nhưng pattern bị rải trong từng task:

```text
state nằm trong task
yield nằm trong task
resume logic nằm trong task
scheduler không thật sự nhìn thấy "yield" như một event rõ ràng
```

## Continuation Khác Cooperative Ở Đâu?

Trong package này, ta tách ý tưởng thành interface:

```java
public interface Continuation {
    void resume();

    boolean isDone();
}
```

Một continuation có thể được gọi `resume()` nhiều lần.

Mỗi lần `resume()`:

```text
có thể chạy tiếp một đoạn
có thể yield
có thể done
có thể failed
```

Điểm khác biệt quan trọng nhất:

```text
cooperative:
  task catch/return để mô phỏng pause

continuation:
  task gọi yieldC(...)
  yield được ném lên scheduler
  scheduler thấy yield và quyết định requeue
```

Nói cách khác:

```text
Cooperative = task tự pause theo convention.
Continuation = scheduler nhìn thấy pause như một sự kiện.
```

## Các Thành Phần

`Continuation` là contract:

```java
void resume();
boolean isDone();
```

`ContinuationTask` là base class:

```java
protected int state = 0;
protected boolean done = false;
```

Nó cung cấp:

```java
protected void yieldC(int nextState) {
    state = nextState;
    throw new YieldException(nextState);
}
```

`yieldC(nextState)` làm hai việc:

```text
1. lưu state kế tiếp
2. ném YieldException để thoát khỏi resume()
```

`YieldException` không phải lỗi thật trong demo này. Nó là tín hiệu control-flow:

```text
task đang yield
scheduler hãy requeue task này
lần sau resume từ nextState
```

## Luồng Chạy Của MyTask

`MyTask` có 3 state:

```text
state=0 -> Step A -> yieldC(1)
state=1 -> Step B -> yieldC(2)
state=2 -> Step C -> done
```

Lần resume đầu:

```text
worker-0 run continuation-1 state=0 -> Step A
worker-0 yield continuation-1 nextState=1
```

Lần resume sau:

```text
worker-1 run continuation-1 state=1 -> Step B
worker-1 yield continuation-1 nextState=2
```

Lần cuối:

```text
worker-2 run continuation-1 state=2 -> Step C
worker-2 done continuation-1
```

Điểm cần để ý: cùng một continuation có thể được resume bởi worker khác nhau.

## Scheduler Là Người Nhìn Thấy Yield

Trong `ContinuationScheduler`, worker lấy continuation từ queue:

```text
take continuation
resume continuation
```

Sau đó có 3 trường hợp chính:

```text
resume() throw YieldException
  -> scheduler log yield
  -> scheduler requeue continuation

resume() return và isDone() = true
  -> scheduler log done
  -> không requeue

resume() return, không throw YieldException, và isDone() = false
  -> scheduler coi như task chưa xong nhưng cũng không yield rõ ràng
  -> scheduler log returned without yield
  -> scheduler requeue continuation

resume() throw exception thật
  -> scheduler log failed
  -> không requeue
```

Đây là phần làm demo sâu hơn cooperative: scheduler không còn mù trước việc task yield.

## Output Cần Nhìn

Khi chạy `Main`, ta submit 3 continuation:

```java
scheduler.submit(new MyTask(1));
scheduler.submit(new MyTask(2));
scheduler.submit(new MyTask(3));
scheduler.submit(new ImplicitReturnTask(4));
```

Output dạng:

```text
scheduler submit continuation-1
scheduler submit continuation-2
scheduler submit continuation-3

worker-0 take continuation-1
worker-0 run continuation-1 state=0 -> Step A
worker-0 yield continuation-1 nextState=1

worker-1 take continuation-1
worker-1 run continuation-1 state=1 -> Step B
worker-1 yield continuation-1 nextState=2

worker-2 take continuation-1
worker-2 run continuation-1 state=2 -> Step C
worker-2 done continuation-1

worker-0 take continuation-4
worker-0 run continuation-4 state=0 -> partial work, return without yield
worker-0 returned without yield continuation-4, requeue
```

Những dòng quan trọng:

```text
take      -> worker lấy continuation khỏi queue
run       -> resume continuation tại state hiện tại
yield     -> continuation pause, scheduler requeue
nextState -> state mà lần resume sau sẽ chạy
returned without yield -> resume() đã return nhưng task chưa done, scheduler vẫn requeue
done      -> continuation hoàn tất, scheduler không requeue nữa
```

`ImplicitReturnTask` là case cố tình thêm để thấy nhánh này. Nó không gọi `yieldC(...)`, nhưng `resume()` vẫn return. Vì `done = false`, scheduler hiểu rằng continuation chưa xong và đưa lại vào queue.

Điểm tinh tế:

```text
Không throw YieldException không có nghĩa là worker bị giữ mãi.
Nếu resume() return, quyền điều phối đã quay lại scheduler.
Chỉ là task không yield một cách explicit.
```

Nếu task thật sự không nhường và cũng không return, ví dụ chạy `while (true)`, scheduler không thể requeue nó. Worker đó sẽ bị kẹt trong `resume()`.

## So Sánh Trực Diện Với Cooperative

`virtualthread.cooperative`:

```text
Task cụ thể tự viết state machine.
Task tự gọi Scheduler.sleep(...).
Task return để nhường worker.
Scheduler chủ yếu chỉ take/run/offer.
```

`virtualthread.continuation`:

```text
State machine được gom vào ContinuationTask.
Task gọi yieldC(nextState).
yieldC ném YieldException.
Scheduler bắt YieldException.
Scheduler quyết định requeue.
```

Nâng cấp concept:

```text
cooperative:
  "Tôi tự return, sau đó ai đó đưa tôi lại queue."

continuation:
  "Tôi yield. Scheduler thấy tôi yield và resume tôi sau."
```

Đây là bước gần hơn với ý tưởng virtual thread thật: runtime/scheduler có quyền quản lý việc pause/resume của computation.

## Giới Hạn Của Demo

Demo này chưa phải continuation thật:

```text
Java call stack không được capture thật
local variable giữa các yield không tự được giữ
state vẫn phải viết thủ công bằng int
yield dùng exception để mô phỏng control-flow
```

Continuation thật sẽ có khả năng lưu lại execution context sâu hơn. Nhưng để học từng bước, demo này cố tình giữ mọi thứ lộ ra ngoài:

```text
state ở đâu
yield xảy ra khi nào
scheduler requeue ra sao
resume chạy tiếp từ đâu
```

## Bước Nhảy Quan Trọng

Hành trình tới đây:

```text
cooperative:
  task biết yield/resume nhưng scheduler chưa thấy yield như event rõ ràng

continuation:
  yield trở thành tín hiệu scheduler bắt được
  scheduler là nơi quyết định requeue/done/failed
```

Tóm lại:

```text
Continuation = resumable computation.
```

Và trong demo này:

```text
yieldC(nextState) = lưu điểm chạy tiếp + trả quyền điều phối về scheduler.
```
