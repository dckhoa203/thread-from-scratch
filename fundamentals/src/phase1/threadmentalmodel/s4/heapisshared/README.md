# Phase 1 — Rebuild the Thread Mental Model

## Step 4 — Heap Is Shared

### Câu hỏi trung tâm

Hai object có thể cùng được tạo trên heap nhưng chỉ một object được nhiều thread truy cập. Vậy điều gì làm một object trở thành shared?

```text
heap allocation ≠ shared object

shared object = nhiều thread có đường reference tới cùng object
```

Sau step này, khi đọc code, hãy phân loại được:

1. Object nào được tạo riêng, object nào được chia sẻ?
2. Những thread nào có thể reach tới object đó?
3. Object có mutable không?
4. Có thread nào mutate đồng thời không?

Rủi ro concurrency cần xem cả ba điều: object được chia sẻ, state có thể thay đổi, và nhiều thread truy cập/mutate mà không có cơ chế bảo vệ phù hợp. Chỉ “nằm trên heap” chưa đủ để kết luận có race.

## Mental model

```text
JVM process
├── Thread A stack ── reference ──┐
├── Thread B stack ── reference ──┼──► cùng một object trên heap
└── static/shared roots ──────────┘
```

Heap là vùng object mà các thread trong process có thể cùng truy cập. Một object chỉ trở thành shared khi reference tới nó có thể được nhiều thread sử dụng. Reference đó có thể đến từ static field, object dùng chung, hoặc được truyền trực tiếp vào nhiều task.

Mental model “object trên heap, local trong stack” hữu ích để học, nhưng không phải sơ đồ vật lý tuyệt đối: JVM/JIT có thể tối ưu allocation và local state. Step này tập trung vào object nào được nhiều thread reach, không vào vị trí vật lý cuối cùng của từng giá trị.

## Lộ trình lab cốt lõi

Chạy các lab theo thứ tự để so sánh object riêng, object chung và object graph chung. Log có prefix trong ngoặc vuông để nhận ra điều cần quan sát.

### 1. Hai thread tạo hai object riêng

Chạy [Lab01SeparateObjects.java](Lab01SeparateObjects.java).

Mỗi thread tự gọi `new Box()`. Sau khi cả hai hoàn tất, `main()` so sánh hai reference bằng `==`:

```text
[ALLOC] thread=T1 created its own Box
[ALLOC] thread=T2 created its own Box
[COMPARE] sameObject=false (expected false)
```

Kết luận: cả hai object có thể ở heap, nhưng không phải cùng object và không tự động được chia sẻ.

### 2. Hai thread reach cùng object qua static field

Chạy [Lab02SharedObject.java](Lab02SharedObject.java).

Hai thread lấy local reference từ cùng `SHARED_BOX`:

```text
[SAME_OBJECT] thread=T1 localRef==SHARED_BOX:true
[SAME_OBJECT] thread=T2 localRef==SHARED_BOX:true
```

Thứ tự log không cố định. Hai local reference nằm trong các invocation riêng; `== true` cho biết mỗi reference trỏ tới object được giữ bởi cùng static field.

### 3. Cùng object được truyền, không cần static

Chạy [Lab05PassSameReference.java](Lab05PassSameReference.java).

`main()` tạo một `Box` local rồi capture cùng reference đó trong hai task:

```text
[PASSED_REFERENCE] thread=T1 received Box created once in main; value=0
[PASSED_REFERENCE] thread=T2 received Box created once in main; value=0
```

Kết luận: static field chỉ là một cách để nhiều thread reach object. Truyền cùng một reference cho nhiều task cũng tạo shared access.

### 4. Shared root làm object graph có thể được chia sẻ

Chạy [Lab06SharedObjectGraph.java](Lab06SharedObjectGraph.java), sau đó đối chiếu với [Lab07SeparateGraphs.java](Lab07SeparateGraphs.java).

Lab 6 cho hai thread cùng đi theo đường `SERVICE → config → cache`:

```text
[SHARED_GRAPH] thread=T1 service=true config=true cache=true
[SHARED_GRAPH] thread=T2 service=true config=true cache=true
```

Lab 7 cho mỗi thread tự tạo một `Service` cùng các object con:

```text
[GRAPH_COMPARE] sameService=false sameConfig=false sameCache=false (expected all false)
```

Kết luận: nếu nhiều thread reach một root chung, các object reachable qua field của root cũng có thể được chia sẻ. Nếu mỗi thread có root riêng, graph của chúng tách biệt trong lab này.

## Map sang backend: singleton và state theo request

### Singleton không giữ mutable request state

Chạy [Lab09SingletonStyle.java](Lab09SingletonStyle.java). Hai request gọi cùng một service instance, nhưng `localRetry` được khởi tạo trong mỗi lần gọi `process()`:

```text
[SINGLETON_LOCAL] thread=request-A sameService=true requestId=REQ-A localRetry=0
[SINGLETON_LOCAL] thread=request-B sameService=true requestId=REQ-B localRetry=0
```

### Field thuộc service dùng chung, local thuộc invocation

Chạy [Lab10TurnSingletonStyle.java](Lab10TurnSingletonStyle.java). Lab đặt `retryCount=7` trước khi start worker; các worker chỉ đọc field này:

```text
[FIELD_VS_LOCAL] thread=request-A sameService=true requestId=REQ-A sharedFieldRetry=7 localRetry=0
[FIELD_VS_LOCAL] thread=request-B sameService=true requestId=REQ-B sharedFieldRetry=7 localRetry=0
```

Điểm cần thấy: `retryCount` là field của object service dùng chung; `localRetry` thuộc từng invocation. Lab không increment field đồng thời, nên chưa thử race condition. Phase 2 mới cố ý mutate shared state.

## Lab mở rộng

Các lab này bổ sung nuance nhưng không cần chạy hết để nắm mental model chính.

- [Lab03SharedMutation.java](Lab03SharedMutation.java): writer hoàn tất trước khi reader được tạo/chạy. `join()` tạo ordering và visibility; đây là demo happens-before, không phải race condition.
- [Lab04LocalVsShared.java](Lab04LocalVsShared.java): đặt local primitive cạnh field của một shared `Box`; cả hai thread chỉ đọc field.
- [Lab08SharedImmutable.java](Lab08SharedImmutable.java): nhiều thread đọc cùng `String`; shared không đồng nghĩa unsafe khi object immutable.
- [Lab11Aliasing.java](Lab11Aliasing.java): hai reference trong một thread cùng trỏ tới `Box`; sửa qua `a`, đọc qua `b`. Đây là aliasing, chưa có concurrency.
- Lab 12 là bài vẽ object graph ở cuối README cũ; có thể dùng như checkpoint reachability thay vì một chương trình riêng.

## Static, object graph và reachability

Một object có thể được nhiều thread reach theo nhiều đường:

```text
static field ─────────────► object
shared Service ─► Config ─► Cache ─► Map
same reference ────────────► object
```

Khi review, hỏi “thread nào reach được object này?” thay vì chỉ hỏi “object nằm ở heap nào?”. Ví dụ, nếu request threads cùng gọi một singleton service, các field object mà service giữ có thể cũng được nhiều thread reach.

Shared immutable state thường dễ reasoning hơn shared mutable state. Tuy nhiên object mutable không tự tạo race; cần xem có truy cập đồng thời, có thao tác ghi, và có đồng bộ hóa phù hợp hay không.

## Không nhầm identity hash với identity check

`System.identityHashCode(object)` hữu ích để in một nhãn quan sát, nhưng hai object khác nhau có thể có cùng hash code. Vì vậy các lab dùng biểu thức `a == b` để kết luận hai reference có trỏ tới cùng object hay không; không dùng “hash khác nhau” làm bằng chứng chắc chắn rằng object khác nhau.

## Checkpoint

Hãy tự trả lời:

1. Heap có phải vùng riêng cho từng thread không?
2. Mọi object trên heap có tự động được chia sẻ không?
3. Một object có thể được nhiều thread reach bằng những đường nào?
4. Local reference riêng có thể trỏ tới shared object không?
5. Vì sao field của singleton khác local trong method invocation?
6. Điều gì cần thêm ngoài shared + mutable để xảy ra race?

Tóm lại:

```text
heap object          → nơi object được mô hình hóa là tồn tại
shared object        → nhiều thread reach cùng object
shared mutable state → cần xem concurrent access và synchronization
```

Phase 1 Step 5 chuyển sang execution model với program counter và context switch. Phase 2 sẽ dùng shared mutable state để cố ý tạo race condition và lost update.

## Chạy lab từ repository root

Với JDK 21:

```bash
mkdir -p /tmp/fundamentals-step4
javac -d /tmp/fundamentals-step4 fundamentals/src/phase1/threadmentalmodel/s4/heapisshared/*.java

java -cp /tmp/fundamentals-step4 phase1.threadmentalmodel.s4.heapisshared.Lab01SeparateObjects
java -cp /tmp/fundamentals-step4 phase1.threadmentalmodel.s4.heapisshared.Lab02SharedObject
java -cp /tmp/fundamentals-step4 phase1.threadmentalmodel.s4.heapisshared.Lab05PassSameReference
java -cp /tmp/fundamentals-step4 phase1.threadmentalmodel.s4.heapisshared.Lab06SharedObjectGraph
java -cp /tmp/fundamentals-step4 phase1.threadmentalmodel.s4.heapisshared.Lab07SeparateGraphs
java -cp /tmp/fundamentals-step4 phase1.threadmentalmodel.s4.heapisshared.Lab09SingletonStyle
java -cp /tmp/fundamentals-step4 phase1.threadmentalmodel.s4.heapisshared.Lab10TurnSingletonStyle
```

Muốn chạy lab mở rộng, thay class ở lệnh `java` bằng `Lab03SharedMutation`, `Lab04LocalVsShared`, `Lab08SharedImmutable` hoặc `Lab11Aliasing`.
