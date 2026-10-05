# Phase 1 — Rebuild the Thread Mental Model

## Step 3 — Stack Is Per-thread

### Mục tiêu

Step này làm rõ hai điều thường bị nhập làm một:

1. Mỗi lần gọi method có local state riêng trong execution context của thread đang gọi.
2. Một local reference riêng vẫn có thể trỏ tới object được chia sẻ.

```text
local primitive per invocation  → state riêng
local reference per invocation  → reference riêng
object that reference points to → có thể được chia sẻ
```

Ta sẽ quan sát local primitive trước, rồi dùng đúng một `ArrayList` được hai worker đọc. Lab không mutate list; shared-state mutation và race condition để dành cho Phase 2.

## 1. Stack và method call frame

Khi thread gọi method, runtime cần duy trì thông tin để tiếp tục call chain, như tham số, local state và điểm quay lại sau lời gọi method khác.

```java
static void foo() {
    int a = 10;
    bar(a);
}

static void bar(int x) {
    int y = x + 1;
}
```

Mental model cho một thread:

```text
Thread stack / call chain
┌────────────────┐
│ bar(x=10, y=11)│  ← method hiện tại
├────────────────┤
│ foo(a=10)       │
├────────────────┤
│ main(...)      │
└────────────────┘
```

Mỗi method call tạo một invocation riêng; khi method return, invocation kết thúc và call chain quay về caller. Đây là mental model Java-level. JVM có thể tối ưu hoặc loại bỏ một số frame/local khi chạy; không nên hiểu rằng mọi biến source-level luôn nằm ở một ô stack vật lý.

## 2. Demo — primitive local và shared `ArrayList` cùng lúc

Chạy [StackPerThreadDemo.java](StackPerThreadDemo.java). Mỗi worker gọi `work()` một lần; trong cùng một method frame, nó có một primitive local và một local reference:

```java
private static void work() {
    int localPrimitive = 0;
    ArrayList<String> localReference = SHARED_LIST;

    for (int i = 0; i < 3; i++) {
        localPrimitive++;

        System.out.printf(
                "%s -> localPrimitive=%d, localReference==SHARED_LIST:%s, contents=%s%n",
                Thread.currentThread().getName(),
                localPrimitive,
                localReference == SHARED_LIST,
                localReference
        );
    }
}
```

`main()` tạo một `ArrayList`, thêm `"created by main"`, rồi khởi chạy hai worker. Output có dạng sau; thứ tự các dòng có thể thay đổi:

```text
worker-1 -> localPrimitive=1, localReference==SHARED_LIST:true, contents=[created by main]
worker-2 -> localPrimitive=1, localReference==SHARED_LIST:true, contents=[created by main]
worker-2 -> localPrimitive=2, localReference==SHARED_LIST:true, contents=[created by main]
worker-1 -> localPrimitive=2, localReference==SHARED_LIST:true, contents=[created by main]
worker-1 -> localPrimitive=3, localReference==SHARED_LIST:true, contents=[created by main]
worker-2 -> localPrimitive=3, localReference==SHARED_LIST:true, contents=[created by main]
```

Hai worker có frame riêng. `localPrimitive` bắt đầu từ `0` trong mỗi frame và tăng độc lập. `localReference` cũng là biến riêng trong mỗi frame, nhưng toán tử `==` cho thấy nó trỏ tới cùng object với `SHARED_LIST`.

```text
worker-1 stack frame                  worker-2 stack frame
┌────────────────────────┐            ┌────────────────────────┐
│ localPrimitive = 2     │            │ localPrimitive = 1     │
│ i = 1                  │            │ i = 0                  │
│ localReference ────────┼─────┐  ┌───┼──── localReference     │
└────────────────────────┘     │  │   └────────────────────────┘
                                ▼  ▼
                      one shared ArrayList
                      ["created by main"]
```

Giá trị `localPrimitive` và `i` trong sơ đồ chỉ là một snapshot minh họa; chúng thay đổi trong vòng lặp. Mỗi lần method return, frame của `work()` kết thúc. Object list vẫn còn vì static field `SHARED_LIST` tiếp tục tham chiếu tới nó.

Lab chỉ đọc list sau khi `main()` đã thêm phần tử và khởi chạy worker; không worker nào mutate list. Như vậy ta thấy rõ stack-local primitive, stack-local reference và object dùng chung mà chưa trộn thêm race condition. `ArrayList` không hỗ trợ concurrent mutation an toàn; Step này chưa thử hành vi đó.

`LocalReferenceDemo.java` vẫn có thể chạy riêng nếu muốn cô lập phần shared reference, nhưng `StackPerThreadDemo` là demo chính vì đặt cả hai khái niệm cạnh nhau.

## 3. Primitive local và reference local

### Primitive local

```java
int count = 0;
```

Mỗi invocation có `count` của riêng mình:

```text
Thread A invocation: count = 3
Thread B invocation: count = 3
```

Hai giá trị bằng nhau không có nghĩa chúng là cùng một biến.

### Reference local

```java
List<String> localList = SHARED_LIST;
```

Mỗi invocation có một local reference riêng, nhưng cả hai reference có thể cùng trỏ tới một object:

```text
Thread A localList ──┐
                     ├──► shared ArrayList
Thread B localList ──┘
```

Vì vậy:

```text
local reference riêng != object riêng
```

## 4. Parameters, recursion và backend mapping

Parameter cũng thuộc invocation nhận nó. Với:

```java
static void process(int amount, Account account) {
    // amount và reference account thuộc invocation này
}
```

`amount` là giá trị riêng trong call frame; biến reference `account` cũng thuộc invocation đó. Nhưng nhiều invocation vẫn có thể nhận cùng một `Account` object. Nếu code sửa `account.balance`, các invocation có thể cùng sửa shared state.

Recursion cho một ví dụ trực quan khác về nhiều invocation của cùng method:

```java
static void recurse(int n) {
    if (n == 0) {
        return;
    }

    recurse(n - 1);
}
```

```text
recurse(3)
└── recurse(2)
    └── recurse(1)
        └── recurse(0)
```

Mỗi lời gọi có state riêng. Recursion quá sâu có thể làm cạn thread stack và gây `StackOverflowError`.

Trong backend, mỗi request invocation có local state riêng, nhưng các request thread có thể cùng dùng Spring singleton service hoặc cache. Local reference tới object dùng chung không biến object đó thành request-local.

```text
Request A → Thread A → local variables riêng ─┐
                                              ├─► shared service / cache / client
Request B → Thread B → local variables riêng ─┘
```

Khi review code concurrency, hỏi:

1. State này thuộc invocation, instance field hay static field?
2. Nếu là reference local, object được trỏ tới có được thread khác dùng không?
3. Object đó có bị mutate đồng thời không?

## Checkpoint

- Mỗi thread có execution context và call chain riêng.
- Mỗi method invocation có local state riêng theo mental model Java.
- Một local reference riêng vẫn có thể trỏ tới shared heap object.
- Demo `ArrayList` hiện chỉ đọc; chưa chứng minh thread safety khi mutate.

Phase 1 Step 4 sẽ tiếp tục với heap là vùng các thread cùng truy cập. Phase 2 mới cố ý mutate shared state để quan sát race condition và lost update.

## Chạy demo từ repository root

Với JDK 21:

```bash
mkdir -p /tmp/fundamentals-step3
javac -d /tmp/fundamentals-step3 fundamentals/src/thread/fundamentals/step3/*.java
java -cp /tmp/fundamentals-step3 thread.fundamentals.step3.StackPerThreadDemo
java -cp /tmp/fundamentals-step3 thread.fundamentals.step3.LocalReferenceDemo
```
