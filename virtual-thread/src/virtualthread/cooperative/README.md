# virtualthread.cooperative

Package này thêm một ý tưởng rất quan trọng: task không nhất thiết phải chạy một mạch từ đầu đến cuối.

Task có thể:

```text
chạy step 1
sleep/yield
nhường worker
sau đó được đưa lại vào queue
resume step 2
```

Đây là mô phỏng thủ công cho cooperative scheduling.

## Vấn Đề Của Bản simple

Ở `virtualthread.simple`, worker gọi:

```java
task.run();
```

Nếu trong `run()` task sleep 1 giây, worker bị giữ 1 giây.

Ta muốn mô phỏng cách khác:

```text
task nói: tôi cần sleep
task lưu state hiện tại
task return
worker được rảnh
sau 1 giây, task được offer lại queue
task chạy tiếp từ state sau sleep
```

## State Là Bộ Nhớ Của Task

`State` có:

```java
START,
AFTER_SLEEP,
DONE
```

Vì Java method bình thường không tự "pause giữa dòng rồi resume lại", demo này dùng state machine thủ công.

`VirtualTask` có:

```java
private State state = State.START;
```

State cho task biết lần sau `run()` thì nên chạy đoạn nào.

## Luồng Chạy Của VirtualTask

Lần chạy đầu:

```text
state = START
in Step 1
state = AFTER_SLEEP
Scheduler.sleep(this, 1000)
return
```

Điểm mấu chốt:

```java
Scheduler.sleep(this, 1000);
return;
```

`return` nghĩa là task nhường worker. Worker không bị giữ bởi task này nữa.

Sau 1 giây, scheduler offer lại chính task đó vào queue.

Lần chạy thứ hai:

```text
state = AFTER_SLEEP
in Step 2
state = DONE
```

## Scheduler Hoạt Động Như Thế Nào?

Scheduler có queue chung:

```java
private static final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
```

`start()` tạo 4 worker:

```text
worker-0
worker-1
worker-2
worker-3
```

Mỗi worker:

```text
take task từ queue
run task
lặp lại
```

`sleep(task, millis)` tạo một timer thread:

```text
timer sleep millis
timer offer task lại queue
```

## Output Cần Nhìn

Với nhiều task, output sẽ kể câu chuyện kiểu:

```text
main offer task-1
worker-0 take task-1
worker-0 -> task-1 Step 1
worker-0 -> task-1 sleep 1000ms and yield

main/worker tiếp tục xử lý task khác

timer-task-1 offer task-1 back to queue
worker-2 take task-1
worker-2 -> task-1 Step 2
```

Điểm hay là `Step 2` không bắt buộc chạy trên cùng worker với `Step 1`.

## Điểm Quan Trọng

Package này mô phỏng được:

```text
cooperative yield
manual continuation bằng State
sleep không giữ worker chính
task được đưa lại queue để resume
```

Nhưng vẫn còn đơn giản:

```text
mọi task dùng một queue chung
mỗi sleep tạo thêm một platform thread timer
chưa có work stealing
```

## Bước Nhảy Tiếp Theo

`virtualthread.cooperative` trả lời câu hỏi:

```text
Task làm sao yield rồi resume?
```

Package tiếp theo, `virtualthread.workstealing`, trả lời câu hỏi:

```text
Nếu có nhiều worker, mỗi worker có queue riêng,
worker này hết việc còn worker kia quá nhiều việc thì sao?
```
