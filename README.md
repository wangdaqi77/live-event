live-event
==========

[![](https://jitpack.io/v/wangdaqi77/live-event.svg)](https://jitpack.io/#wangdaqi77/live-event) [![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](https://www.apache.org/licenses/LICENSE-2.0)

一个在给定的生命周期内可观察的事件库。

它主要包含以下两个类：

 * LiveEvent - 一个在给定的生命周期内可观察的事件持有类，它继承MutableLiveData，内部在主线程管理和分发事件。
 * BackgroundLiveEvent - 一个在给定的生命周期内可观察的事件持有类，内部在后台线程管理和分发事件。

如果[直接使用请点这里](#开始)，后续再来阅读文档。

前言
----
Jetpack中的LiveData是一个在给定的生命周期内可观察的数据持有类，它在主线程管理和分发数据（Observer在主线程接收数据），它为容纳ViewModel中的各个数据字段而设计，ViewModel的唯一职责是管理UI数据，因此目前LiveData主要是作为UI的唯一信任源，用于数据驱动UI。

LiveData在数据驱动UI处理的非常优秀，但是它不适用于事件场景。

那么，LiveData优秀在哪？？

我们通过调用`liveData.observe(lifecycleOwner, observer)`感受LiveData的强大之处：
 * `liveData`会感知`lifecycleOwner`的生命周期以管理`observer`的active、装载和卸载，这样能帮助开发者自动回收资源，避免内存泄漏和异常情况，提升了性能。
 * 当`liveData`第一次感知到`lifecycleOwner`是活跃的，`liveData`会粘一个最近的数据通知到该`observer`，这样保证了数据和UI的一致性。
 * 当`liveData`感知到`lifecycleOwner`是非活跃的后，依次调用`liveData.setValue(a)`、`liveData.setValue(b)`，`liveData.setValue(n)`，`liveData`一定会丢掉`n`之前的数据，这样`lifecycleOwner`在非活跃时能避免不必要的UI绘制。
 * 当`liveData`感知到`lifecycleOwner`是活跃的，在一个子线程依次调用`liveData.postValue(a)`、`liveData.postValue(b)`，`liveData.postValue(n)`，`liveData`可能会丢掉`a`，也可能会丢掉`b`，`liveData`只保证将最新的`n`通知到`observer`，这样能避免不必要的UI绘制提升了效率和性能。
 * 当`liveData`感知到`lifecycleOwner`是活跃的，调用`liveData.setValue(a)`，此时`observer`收到`a`并在onChanged调用`liveData.setValue(b)`，这种"嵌套"会导致在`observer`注册后注册的Observer收不到`a`只会收到`b`，这样能避免不必要的UI绘制提升了效率和性能。

LiveData的优秀设计产生了**粘性行为**和**丢弃行为**，它们并不适用于某些观察事件场景：
 * 当`liveData`第一次感知到`lifecycleOwner`活跃时，`observer`立即接收一个最近的数据/事件，这个场景称为**粘性行为**，在某些场景下这不可接受。
 * 当`liveData`感知到`lifecycleOwner`是非活跃的后，依次调用`liveData.setValue(a)`、`liveData.setValue(b)`，`liveData.setValue(n)`，`observer`一定接收不到`a`和`b`，这个场景称为非活跃时**丢弃行为**，在某些场景下这不可接受。
 * 当`liveData`感知到`lifecycleOwner`是活跃的，在一个子线程依次调用`liveData.postValue(a)`、`liveData.postValue(b)`，`liveData.postValue(n)`，`observer`可能接收不到`a`，也可能接收不到`b`，这个场景称为多次postValue**丢弃行为**，在某些场景下这不可接受。
 * 当`liveData`感知到`lifecycleOwner`是活跃的，调用`liveData.setValue(a)`，此时`observer`收到`a`并在onChanged调用`liveData.setValue(b)`，这种"嵌套"会导致在该`observer`注册后注册的Observer收不到`a`只会收到`b`，这个场景称为setValue嵌套**丢弃行为**，在某些场景下这不可接受。
 * 它不能在指定的线程观察事件。

*LiveData中的`observe`和`observeForever`方法既有**粘性行为**又有**丢弃行为**，本文中提到的**粘性行为**和**丢弃行为**都是围绕这两个观察方法进行阐述的。*

为事件而生
---------
LiveEvent是MutableLiveData的子类，它保留了LiveData的优点，新增了没有粘性行为和没有丢弃行为的观察方法，LiveEvent更适用于观察事件，它关联的Observer在主线程接收事件。

BackgroundLiveEvent适用于在后台线程观察事件，保持了与LiveEvent一致的方法调用方式，它不会占用主线程资源，也可以调用`backgroundLiveEvent.observeXX(BackgroundObserver)`为该BackgroundObserver单独设置一个事件分发器（指定线程接收事件）。

LiveEvent和BackgroundLiveEvent的observe相关方法：
| 观察方法                   |感知生命周期|没有粘性行为|没有丢弃行为|备注|
| :---                              |:---:      |:---:   |:---:      |:---|
| `observe`                         |✔️       |❌        |❌         |与LiveData中的observe观察行为一致，注意BackgroundLiveEvent中是在后台线程观察事件|
| `observeForever`                  |❌          |❌        |❌         |与LiveData中的observeForever观察行为一致，注意BackgroundLiveEvent中是在后台线程观察事件|
| `observeNoSticky`                 |✔️       |✔️     |❌         |相比`observe`，没有粘性行为|
| `observeForeverNoSticky`          |❌          |✔️     |❌         |相比`observeForever`，没有粘性行为|
| `observeNoLoss`                   |✔️       |❌        |✔️      |相比`observe`，没有丢弃行为|
| `observeForeverNoLoss`            |❌          |❌        |✔️       |相比`observeForever`，没有丢弃行为|
| `observeNoStickyNoLoss`           |✔️       |✔️     |✔️       |相比`observe`，既没有粘性行为，又没有丢弃行为|
| `observeForeverNoStickyNoLoss`    |❌          |✔️     |✔️       |相比`observeForever`，既没有粘性行为，又没有丢弃行为|

**关于没有丢弃行为的方法：**
 * 当调用`liveEvent.observeNoLoss(lifecycleOwner, observer)`，丢弃行为中丢弃的事件会有序的保存到`observer`关联的临时链表中，等待**适当的时机**将该链表中的事件有序的通知到`observer`。
 * **适当的时机**是什么时候？
    1. 当调用`liveEvent.observeNoLoss(lifecycleOwner, observer)`观察事件，这个时机是`liveEvent`感知到`lifecycleOwner`从非活跃变成活跃时，如`Activity`从后台返回到前台。
    2. 当调用`liveEvent.observeNoLoss(lifecycleOwner, observer)`观察事件，并且`liveEvent`感知到`lifecycleOwner`是活跃的，在一个子线程依次调用`liveEvent.postValue(a)`、`liveEvent.postValue(b)`，`liveEvent.postValue(n)`，丢弃行为中丢弃的事件会有序的保存到`observer`关联的临时链表中，这个时机是`liveEvent`内部触发调用`setValue(n)`时。
 * 如果`lifecycleOwner`处于非活跃状态后事件频繁，且事件对象占用内存较大，此时请根据应用场景选用`observeForeverNoLoss`或`observeForeverNoStickyNoLoss`。

自带的实现类
-----------
MixedLiveEvent和MixedBackgroundLiveEvent分别公开了LiveEvent和BackgroundLiveEvent所有的观察行为，其他的类则是为其支持的特定观察行为公开了`observe`和`observeForever`方法，方法内部并非一定直接调用`super.observe`和`super.observeForever`，具体如下：
| 类 | 类描述 | `observe` | `observeForever` | `observeNoSticky` | `observeForeverNoSticky` | `observeNoLoss` | `observeForeverNoLoss` | `observeNoStickyNoLoss` | `observeForeverNoStickyNoLoss` |
| :---         |     :---:      |     :---:      |     :---:      |     :---:      |     :---:      |     :---:      |     :---:      |     :---:      |     :---:      |
| MixedLiveEvent     | 可混合观察，支持所有的观察行为     | 调用super方法       | 调用super方法      | 调用super方法    | 调用super方法     | 调用super方法    | 调用super方法    | 调用super方法    | 调用super方法    |
| MixedBackgroundLiveEvent     | 可混合观察，支持所有的观察行为       | 调用super方法       | 调用super方法      | 调用super方法    | 调用super方法     | 调用super方法    | 调用super方法    | 调用super方法    | 调用super方法    |
| LikeLiveDataLiveEvent     | 与LiveData一致     | 调用super方法       | 调用super方法      | —    | —     | —    | —    | —    | —    |
| LikeLiveDataBackgroundLiveEvent     | 类似LiveData，但可在后台线程接收事件     | 调用super方法       | 调用super方法      | —    | —     | —    | —    | —    | —    |
| NoLossLiveEvent   | 没有丢弃行为     | 内部实际调用了`super.observeNoLoss`     | 内部实际调用了`super.observeForeverNoLoss`    | —    | —     | —    | —    | —    | —    |
| NoLossBackgroundLiveEvent     | 没有丢弃行为      | 内部实际调用了`super.observeNoLoss`       | 内部实际调用了`super.observeForeverNoLoss`      | —    | —     | —    | —    | —    | —    |
| NoStickyLiveEvent     | 没有粘性行为     | 内部实际调用了`super.observeNoSticky`       | 内部实际调用了`super.observeForeverNoSticky`      | —    | —     | —    | —    | —    | —    |
| NoStickyBackgroundLiveEvent     | 没有粘性行为     | 内部实际调用了`super.observeNoSticky`       | 内部实际调用了`super.observeForeverNoSticky`      | —    | —     | —    | —    | —    | —    |
| NoStickyNoLossLiveEvent     | 没有粘性行为且没有丢弃行为     | 内部实际调用了`super.observeNoStickyNoLoss`       | 内部实际调用了`super.observeForeverNoStickyNoLoss`      | —    | —     | —    | —    | —    | —    |
| NoStickyNoLossBackgroundLiveEvent     | 没有粘性行为且没有丢弃行为     | 内部实际调用了`super.observeNoStickyNoLoss`       | 内部实际调用了`super.observeForeverNoStickyNoLoss`      | —    | —     | —    | —    | —    | —    |

开始
====

依赖
----
```
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation "com.github.wangdaqi77.live-event:core:1.3.1"
    // 务必依赖官方组件
    implementation "androidx.lifecycle:lifecycle-core:2.3.1"
}
```

使用示例
-------
 * 可混合观察的LiveEvent
  ```kotlin
  // 1.创建一个可混合观察的LiveEvent，它包含一个初始事件
  val liveEvent = MixedLiveEvent<String>(EVENT_INIT) // as MutableLiveData<String>
  // val liveEvent = MixedBackgroundLiveEvent<String>(EVENT_INIT)

  // 2.观察
  // 2.1 与LiveData的observe观察行为一致
  liveEvent.observe(LifecycleOwner, Observer{ event ->
      // 将依次接收到EVENT_INIT，"事件B"
  })

  // 2.2 相比`observe`，没有粘性行为
  liveEvent.observeNoSticky(LifecycleOwner, Observer{ event ->
      // 只接收到"事件B"
  })

  // 2.3 相比`observe`，没有丢弃行为
  liveEvent.observeNoLoss(LifecycleOwner, Observer{ event ->
      // 将依次接收到EVENT_INIT，"事件A"，"事件B"
  })

  // 2.4 相比`observe`，没有粘性行为且没有丢弃行为
  liveEvent.observeNoStickyNoLoss(LifecycleOwner, Observer{ event ->
      // 将依次接收到"事件A"，"事件B"
  })

  // 3.发送事件
  liveEvent.postValue("事件A")
  liveEvent.postValue("事件B")
  ```


 * 与LiveData观察行为一致的LiveEvent
  ```kotlin
  // 1.创建一个与LiveData观察行为一致的LiveEvent，它包含一个初始事件
  val liveEvent = LikeLiveDataLiveEvent<String>(EVENT_INIT) // as MutableLiveData<String>
  // val liveEvent = LikeLiveDataBackgroundLiveEvent<String>(EVENT_INIT)

  // 2.观察
  liveEvent.observe(LifecycleOwner, Observer{ event ->
      // 将依次接收到EVENT_INIT，"事件B"
  })

  // 3.发送事件
  liveEvent.postValue("事件A")
  liveEvent.postValue("事件B")
  ```


 * 没有粘性行为的LiveEvent
  ```kotlin
  // 1.创建一个没有粘性行为的LiveEvent，它包含一个初始事件
  val liveEvent = NoStickyLiveEvent<String>(EVENT_INIT) // as MutableLiveData<String>
  // val liveEvent = NoStickyBackgroundLiveEvent<String>(EVENT_INIT)

  // 2.观察，内部实际调用的是super.observeNoSticky
  liveEvent.observe(LifecycleOwner, Observer{ event ->
      // 只接收到"事件B"
  })

  // 3.发送事件
  liveEvent.postValue("事件A")
  liveEvent.postValue("事件B")
  ```


 * 没有丢弃行为的LiveEvent
  ```kotlin
  // 1.创建一个没有丢弃行为的LiveEvent，它包含一个初始事件
  val liveEvent = NoLossLiveEvent<String>(EVENT_INIT) // as MutableLiveData<String>
  // val liveEvent = NoLossBackgroundLiveEvent<String>(EVENT_INIT)

  // 2.观察，内部实际调用的是super.observeNoLoss
  liveEvent.observe(LifecycleOwner, Observer{ event ->
      // 将依次接收到EVENT_INIT, "事件A", "事件B"
  })

  // 3.发送事件
  liveEvent.postValue("事件A")
  liveEvent.postValue("事件B")
  ```


 * 没有粘性行为且没有丢弃行为的LiveEvent
  ```kotlin
  // 1.创建一个没有粘性行为且没有丢弃行为的LiveEvent，它包含一个初始事件
  val liveEvent = NoStickyNoLossLiveEvent<String>(EVENT_INIT) // as MutableLiveData<String>
  // val liveEvent = NoStickyNoLossBackgroundLiveEvent<String>(EVENT_INIT)

  // 2.观察，内部实际调用的是super.observeNoStickyNoLoss
  liveEvent.observe(LifecycleOwner, Observer{ event ->
      // 将依次接收到"事件A", "事件B"
  })

  // 3.发送事件
  liveEvent.postValue("事件A")
  liveEvent.postValue("事件B")
  ```


 * 在后台线程接收事件的BackgroundLiveEvent
  ```kotlin
  // 1.创建一个在后台线程接收事件的BackgroundLiveEvent，它包含一个初始事件
  val liveEvent = MixedBackgroundLiveEvent<String>(EVENT_INIT)

  // 2.观察
  // 2.1 使用Observer类型观察时，在全局统一的后台线程分发事件，等同于BackgroundObserver<String>(dispatcher = EventDispatcher.DEFAULT)
  liveEvent.observe(LifecycleOwner, Observer{ event ->
      // 在后台线程中将依次接收到EVENT_INIT, "事件B"
      // 线程名为default-dispatcher
  })

  // 2.2 使用BackgroundObserver类型观察时，可以指定一个事件分发器（指定线程接收事件），作用范围仅限该BackgroundObserver.
  // SDK预先实现了EventDispatcher.DEFAULT、EventDispatcher.MAIN、EventDispatcher.BACKGROUND、EventDispatcher.ASYNC，其中EventDispatcher.DEFAULT作为默认实现
  liveEvent.observe(LifecycleOwner, object: BackgroundObserver<String>(dispatcher = EventDispatcher.BACKGROUND) {
      override fun onChanged(t: String) {
          // 在后台线程中将依次接收到EVENT_INIT, "事件B"
          // 线程名为background-event-dispatcher
      }
  })

  liveEvent.observe(LifecycleOwner, object: BackgroundObserver<String>(dispatcher = yourCustomDispatcher) {
      override fun onChanged(t: String) {
          // 在后台线程中将依次接收到EVENT_INIT, "事件B"
          // 线程名为yourCustom
      }
  })

  // 3.发送事件
  liveEvent.postValue("事件A")
  liveEvent.postValue("事件B")
  ```

混淆
----
```
-dontwarn androidx.lifecycle.LiveData
-keep class androidx.lifecycle.LiveData { *; }
-dontwarn androidx.lifecycle.LifecycleRegistry
-keep class androidx.lifecycle.LifecycleRegistry { *; }
```

Demo
====
Demo提供了丰富的示例，[下载Demo](./assets/demo-debug.apk)了解更多

Demo主界面：

![image](./assets/demo.webp)
