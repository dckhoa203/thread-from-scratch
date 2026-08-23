# virtualthread.simple

Package này là mô hình ngây thơ đầu tiên của scheduler: có nhiều task, một queue chung, và một số worker thread lấy task ra chạy.

Nó chưa phải virtual thread thật. Nó là bản tối giản để thấy ý tưởng:

```text
nhiều task nhẹ
ít worker platform thread
worker rảnh thì lấy task tiếp theo
```

## Thành Phần

`Task`:

```java
public interface Task {
    default String name() {
        return "anonymous";
    }

    void run();
}
```

`Task` là đơn vị công việc. Nó giống `Runnable`, nhưng có thêm `name()` để log dễ nhìn.

`MiniScheduler` có queue chung:

```java
private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>();
```

Khi submit:

```java
queue.offer(task);
```

Khi start:

```java
int workers = Runtime.getRuntime().availableProcessors();
```

Scheduler tạo số worker bằng số CPU available.

## Luồng Chạy

Trong `Main`:

```text
tạo MiniScheduler
start workers
submit 100_000 task
```

Mỗi worker chạy vòng lặp:

```java
while (true) {
    Task task = queue.take();
    task.run();
}
```

Ý nghĩa:

```text
queue có task  -> worker lấy ra chạy
queue rỗng     -> worker đứng chờ ở queue.take()
```

`queue.take()` là blocking call. Nó không trả về `null`. Nếu queue rỗng, worker ngủ/chờ cho tới khi có task mới.

## Ví Dụ

Giả sử có 4 worker và 10 task:

```text
queue:
task-1 task-2 task-3 task-4 task-5 task-6 task-7 task-8 task-9 task-10
```

Ban đầu:

```text
worker-0 take task-1
worker-1 take task-2
worker-2 take task-3
worker-3 take task-4
```

Khi worker nào xong trước:

```text
worker đó quay lại queue.take()
lấy task tiếp theo
```

Scheduler không chia sẵn kiểu:

```text
worker-0 nhận task 1..25
worker-1 nhận task 26..50
```

Nó phân phối động qua queue chung.

## Điểm Quan Trọng

Mô hình này giúp thấy:

```text
task nhiều hơn worker là bình thường
worker ít hơn task là chủ ý
tối đa chỉ có N task chạy đồng thời, với N = số worker
task còn lại chờ trong queue
```

Nhưng nó còn ngây thơ:

```text
task chạy một mạch từ đầu đến cuối
task không biết yield
task sleep/block thì worker cũng bị giữ
mọi worker tranh nhau một queue chung
```

## Bước Nhảy Tiếp Theo

`virtualthread.simple` trả lời câu hỏi:

```text
Làm sao chạy nhiều task bằng ít worker?
```

Package tiếp theo, `virtualthread.cooperative`, trả lời câu hỏi sâu hơn:

```text
Nếu task đang chạy giữa chừng rồi muốn sleep/yield thì sao?
Làm sao nhường worker rồi resume task sau?
```
