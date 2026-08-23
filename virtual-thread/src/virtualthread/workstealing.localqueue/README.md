# virtualthread.workstealing

Package này là bản demo sắc hơn: mỗi worker có queue riêng, worker rảnh có thể steal task từ worker khác.

Ý tưởng chính:

```text
Worker ưu tiên làm việc của mình.
Nếu hết việc, worker đi lấy bớt việc từ worker khác.
```

Đây là work stealing.

## Vì Sao Cần Work Stealing?

Ở `virtualthread.cooperative`, mọi task nằm trong một queue chung:

```text
queue chung:
task-1 task-2 task-3 task-4
```

Cách này dễ hiểu, nhưng mọi worker cùng tranh nhau một queue.

Work stealing đổi sang:

```text
worker-0 deque: task-1 task-2 task-3 task-4
worker-1 deque:
worker-2 deque:
worker-3 deque:
```

Mỗi worker có `deque` riêng. Nếu worker nào rảnh, nó steal từ worker khác.

## Deque: Local Pop Và Steal Khác Đầu

Mỗi worker có:

```java
final ConcurrentLinkedDeque<Runnable> deque = new ConcurrentLinkedDeque<>();
```

Worker chủ sở hữu lấy task từ cuối:

```java
deque.pollLast();
```

Worker khác steal từ đầu:

```java
deque.pollFirst();
```

Hình dung:

```text
HEAD                              TAIL
task-1 -> task-2 -> task-3 -> task-4
```

Owner:

```text
worker-0 local-pop task-4
```

Thief:

```text
worker-1 steal task-1 from worker-0
```

Hai bên lấy hai đầu khác nhau để giảm tranh chấp. Đây là chi tiết quan trọng của work stealing.

## Demo Cố Tình Tạo Lệch Tải

Trong `Main`, toàn bộ task ban đầu được đưa vào `worker-0`:

```java
for (int i = 1; i <= 8; i++) {
    scheduler.submitToWorker(new VirtualTask(i), 0);
}
```

Ban đầu:

```text
worker-0 deque: task-1 task-2 task-3 task-4 task-5 task-6 task-7 task-8
worker-1 deque:
worker-2 deque:
worker-3 deque:
```

Nếu không có work stealing:

```text
worker-0 làm hết
worker-1 rảnh
worker-2 rảnh
worker-3 rảnh
```

Với work stealing:

```text
worker-0 tự local-pop task của mình
worker-1 steal từ worker-0
worker-2 steal từ worker-0
worker-3 steal từ worker-0
```

## Worker Loop

Mỗi worker chạy:

```text
1. thử lấy task local bằng pollLast()
2. nếu có task -> run
3. nếu không có -> scheduler.steal(id)
4. nếu vẫn không có -> nghỉ rất ngắn bằng LockSupport.parkNanos(...)
5. lặp lại
```

Dòng này:

```java
LockSupport.parkNanos(1_000_000);
```

là idle backoff. Khi worker không tìm được việc, nó nghỉ khoảng 1ms thay vì spin liên tục và ăn CPU.

## Sleep/Yield Và Resume

`VirtualTask` vẫn là task hai bước:

```text
step-1
yield for sleep
timer wake
submit lại scheduler
resume step-2
```

Khác với bản cooperative cũ, package này dùng `ScheduledExecutorService` làm timer:

```java
timer.schedule(() -> {
    System.out.println("timer wake " + task);
    submit(task);
}, millis, TimeUnit.MILLISECONDS);
```

Như vậy mỗi lần sleep không tạo một platform thread mới.

## Output Cần Nhìn

Output sẽ có dạng:

```text
scheduler submit task-1 -> worker-0
scheduler submit task-2 -> worker-0
...

worker-0 local-pop task-8
worker-0 run task-8 step-1
worker-0 yield task-8 for sleep

worker-2 steal task-3 from worker-0
worker-2 run task-3 step-1
worker-2 yield task-3 for sleep

timer wake task-8
scheduler submit task-8 -> worker-1
worker-0 steal task-8 from worker-1
worker-0 resume task-8 step-2
```

Những dòng quan trọng:

```text
local-pop  -> worker chạy task trong deque của chính nó
steal      -> worker rảnh lấy task từ worker khác
timer wake -> task sau sleep được đưa lại scheduler
resume     -> task chạy tiếp step-2
```

## Bước Nhảy Từ Ngây Thơ Đến Sắc Bén

Hành trình tới package này:

```text
thread.overall:
  OS thread đắt, thread pool có thể starvation, virtual thread sinh ra để xử lý nhiều blocking task.

virtualthread.simple:
  nhiều task dùng một queue chung, một số worker lấy task ra chạy.

virtualthread.cooperative:
  task biết yield, lưu state, sleep rồi được resume.

virtualthread.workstealing:
  mỗi worker có deque riêng, worker rảnh steal từ worker bận.
```

Tóm lại:

```text
Work stealing = local-first + steal-when-idle.
```

Demo này chưa phải scheduler thật của JVM, nhưng nó mô phỏng đủ các ý cốt lõi để nhìn thấy cơ chế.
