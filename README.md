# thread-from-scratch

Repo này đi từ mô hình thread ngây thơ tới các ý tưởng nằm sau virtual thread: worker, queue, cooperative yield, work stealing, và continuation.

Nên đọc theo đúng thứ tự dưới đây. Mỗi package là một bước nhảy concept, không phải một implementation production.

## Lộ Trình Đọc

1. [thread.overall](thread-overal/src/thread/overall/README.md)

   Tổng quan lịch sử vấn đề: OS thread đắt, thread pool có thể starvation, virtual thread sinh ra để xử lý nhiều blocking task hơn.

2. [virtualthread.simple](virtual-thread/src/virtualthread/simple/README.md)

   Mô hình đầu tiên: nhiều task, một queue chung, một số worker platform thread lấy task ra chạy.

3. [virtualthread.cooperative](virtual-thread/src/virtualthread/cooperative/README.md)

   Task biết chạy một đoạn, lưu state, yield/sleep, rồi được scheduler đưa lại queue để resume.

4. [virtualthread.workstealing.localqueue](virtual-thread/src/virtualthread/workstealing.localqueue/README.md)

   Mỗi worker có deque riêng. Worker ưu tiên việc local; khi hết việc thì steal từ worker khác.

5. [virtualthread.workstealing.submissionqueue](virtual-thread/src/virtualthread/workstealing/submissionqueue/README.md)

   Bổ sung hai đường vào scheduler: task từ bên ngoài đi qua `SubmissionQueue`; task do worker tạo ra đi vào local deque. Timer cũng quay lại bằng đường external này.

6. [virtualthread.continuation](virtual-thread/src/virtualthread/continuation/README.md)

   Nâng cấp concept từ cooperative: task không chỉ tự return/yield theo convention, mà yield trở thành tín hiệu scheduler nhìn thấy để requeue/resume.

## Mạch Ý Tưởng

```text
thread.overall
  -> vì sao cần virtual thread?

virtualthread.simple
  -> nhiều task chạy trên ít worker như thế nào?

virtualthread.cooperative
  -> task yield rồi resume như thế nào?

virtualthread.workstealing.localqueue
  -> worker rảnh lấy việc từ worker bận như thế nào?

virtualthread.workstealing.submissionqueue
  -> việc từ main thread, timer, hay callback đi vào scheduler ở đâu?

virtualthread.continuation
  -> computation có thể resume nhiều lần được mô hình hóa ra sao?
```

## Câu Tóm Tắt

```text
Virtual thread không bắt đầu từ magic.
Nó bắt đầu từ việc tách task khỏi OS thread,
rồi để scheduler quản lý khi nào task chạy, yield, resume, hoặc được worker khác tiếp tục.
```
