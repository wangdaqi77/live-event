# live-event
一个在给定的生命周期内可观察的事件持有类。

## 前言
### Jetpack中的LiveData是一个在给定的生命周期内可观察的数据持有类，它只能在主线程观察数据，它为容纳ViewModel中的各个数据字段而设计，ViewModel的唯一职责是管理UI数据，因此LiveData目前主要是用于数据驱动UI绘制。LiveData的内部针对性的做了一些优化，一如在observe时，在给定的生命周期活跃时（如Activity在前台时）LiveData会粘最新的值通知到观察处，二如当多个后台线程post数据时，LiveData会丢弃中间数据只将最新的数据通知到观察处以更新UI，这样能减少UI绘制次数。
### live-event是一个在给定的生命周期内可观察的事件持有类，新增了不丢失事件、屏蔽粘性（对新的观察者屏蔽粘一个最近的历史事件）等功能，因此它更适用于事件的收发，它主要包含LiveEvent、LiveBackgroundEvent，前者在主线程订阅和接收数据，后者在后台线程订阅和接收数据。

## 观察函数比对
| 函数名 | 自动取消观察 | 屏蔽粘性 | 支持不丢失数据 |
| :---         |     :---:      |     :---:      |     :---:      |
| *observe*   | yes     | no    | no    |
| *observeForever*     | no       | no      | no    |
| *observeNoSticky*     | yes       | yes      | no    |
| *observeForeverNoSticky*     | no       | yes      | no    |
| *observeNoLoss*     | yes       | no      | yes    |
| *observeForeverNoLoss*     | no       | no      | yes    |
| *observeNoStickyNoLoss*     | yes       | yes      | yes    |
| *observeForeverNoStickyNoLoss*     | no       | yes      | yes    |

## 调用函数的线程
| 函数名 | LiveData | LiveEvent | LiveBackgroundEvent |
| :---         |     :---:      |     :---:      |     :---:      |
| *setValue*   | 主线程     | 主线程    | 任意线程    |
| *postValue*   | 任意线程     | 任意线程    | 任意线程    |
| *observe*   | 主线程     | 主线程    | 任意线程    |
| *observeForever*     | 主线程       | 主线程      | 任意线程    |
| *observeNoSticky*     | ——       | 主线程      | 任意线程    |
| *observeForeverNoSticky*     | ——       | 主线程      | 任意线程    |
| *observeNoLoss*     | ——       | 主线程      | 任意线程    |
| *observeForeverNoLoss*     | ——       | 主线程      | 任意线程    |
| *observeNoStickyNoLoss*     | ——       | 主线程      | 任意线程    |
| *observeForeverNoStickyNoLoss*     | ——       | 主线程      | 任意线程    |
| *Observer.onChanged*     | 主线程       | 主线程      | 内部指定的后台线程    |
| *onActive*     | 主线程       | 主线程      | 内部指定的后台线程    |
| *onInactive*     | 主线程       | 主线程      | 内部指定的后台线程    |

## 说明

### 关于XXNoSticky
#### 1.observe、observeForever在给定的生命周期活跃时新的观察者支持粘最近的一个历史事件，在某些场景下这是不可接受的，XXNoSticky屏蔽粘性功能就是解决这个痛点。

### 关于XXNoLoss
#### 1.特性是**保证数据不丢失**，这里的数据是指setValue时观察者处于非活跃状态、后台线程postValue时的pending data，这些数据会有序的临时保存到链表中，在**适当的时机**将链表中的数据通知到观察处，因此建议数据为**新创建**的对象，否则观察此数据无意义。
#### 2.什么时机将临时保存的数据通知到订阅处？一，给定的生命周期非活跃变成活跃时通知。 二，多线程post数据时会将pending data保存起来，在**下一次触发setValue以覆盖当前的mPendingData数据时**通知，具体下一次触发setValue请查看Jetpack中的LiveData官方源码。
#### 3.observeNoLoss、observeNoStickyNoLoss不建议观察占用内存大的数据，试想一下在给定的生命周期非活跃时，可能会有多个内存大的数据保存到内存中，最终会导致OOM。

## 开始
### 依赖
```
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation "com.github.wangdaqi77.live-event:core:1.0.0"
    // 务必依赖官方组件
    implementation "androidx.lifecycle:lifecycle-core:version"
}
```

### 使用
```kotlin
// 1.创建
val liveEvent = MutableLiveEvent<String>() / MutableLiveBackgroundEvent<String>()

// 2.观察
liveEvent.observeXX(LifecycleOwner, Observer{ event ->
    // doing ...
})

// 3.发送事件
liveEvent.setValue("事件A")
liveEvent.postValue("事件B")
```

### 混淆
```
-keep class androidx.lifecycle.LiveData { <fields>; }
-keep class androidx.lifecycle.LiveData$* { <fields>; }
```
