live-event
==========

[![](https://jitpack.io/v/wangdaqi77/live-event.svg)](https://jitpack.io/#wangdaqi77/live-event) [![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](https://www.apache.org/licenses/LICENSE-2.0)

一个在给定的生命周期内可观察的事件库。

它主要包含以下两个类：

 * LiveEvent - 一个在给定的生命周期内可观察的事件持有类，它继承LiveData，内部在主线程管理和分发事件。
 * BackgroundLiveEvent - 一个在给定的生命周期内可观察的事件持有类，内部在全局统一的子线程管理和分发事件。

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
LiveEvent是LiveData的子类，它保留了LiveData的优点，新增了没有粘性行为和没有丢弃行为的观察方法，LiveEvent更适用于观察事件，它关联的Observer在主线程接收事件。

BackgroundLiveEvent适用于在后台线程观察事件，保持了与LiveEvent一致的方法名，他所有的方法可以在子线程调用，调用`backgroundLiveEvent.observeXX(BackgroundObserver)`可以为该观察器单独设置一个事件分发器（指定线程接收事件）。

LiveEvent和BackgroundLiveEvent的相关观察方法：
| 观察方法                   |感知生命周期|没有粘性行为|没有丢弃行为|备注|
| :---                              |:---:      |:---:   |:---:      |:---|
| `observe`                         |✔️       |❌        |❌         |与LiveData中的observe观察行为一致|
| `observeForever`                  |❌          |❌        |❌         |与LiveData中的observeForever观察行为一致|
| `observeNoSticky`                 |✔️       |✔️     |❌         |相比`observe`，没有粘性行为|
| `observeForeverNoSticky`          |❌          |✔️     |❌         |相比`observeForever`，没有粘性行为|
| `observeNoLoss`                   |✔️       |❌        |✔️      |相比`observe`，没有丢弃行为|
| `observeForeverNoLoss`            |❌          |❌        |✔️       |相比`observeForever`，没有丢弃行为|
| `observeNoStickyNoLoss`           |✔️       |✔️     |✔️       |相比`observe`，既没有粘性行为，又没有丢弃行为|
| `observeForeverNoStickyNoLoss`    |❌          |✔️     |✔️       |相比`observeForever`，既没有粘性行为，又没有丢弃行为|

**关于没有丢弃行为的观察方法：**
 * 当调用`liveEvent.observeNoLoss(lifecycleOwner, observer)`，丢弃行为中丢弃的事件会有序的保存到`observer`关联的临时链表中，等待**适当的时机**将该链表中的事件有序的通知到`observer`。
 * **适当的时机**是什么时候？
    1. 当调用`liveEvent.observeNoLoss(lifecycleOwner, observer)`观察事件，这个时机是`liveEvent`感知到`lifecycleOwner`从非活跃变成活跃时，如`Activity`从后台返回到前台。
    2. 当调用`liveEvent.observeNoLoss(lifecycleOwner, observer)`观察事件，并且`liveEvent`感知到`lifecycleOwner`是活跃的，在一个子线程依次调用`liveEvent.postValue(a)`、`liveEvent.postValue(b)`，`liveEvent.postValue(n)`，丢弃行为中丢弃的事件会有序的保存到`observer`关联的临时链表中，这个时机是`liveEvent`内部触发调用`setValue(n)`时。
 * 如果`lifecycleOwner`处于非活跃状态后事件频繁，且事件对象占用内存较大，此时请根据应用场景选用`observeForeverNoLoss`或`observeForeverNoStickyNoLoss`。

开始
====

依赖
----
```
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation "com.github.wangdaqi77.live-event:core:1.5.2"
    // 务必依赖官方组件
    implementation "androidx.lifecycle:lifecycle-core:2.3.1"
}
```

使用示例
-------
 * 多个观察行为
  ```kotlin
  // 1.创建一个可混合观察的LiveEvent，它包含一个初始事件
  val liveEvent = MutableLiveEvent<String>(EVENT_INIT) // as LiveData<String>
  val liveEvent = MutableBackgroundLiveEvent<String>(EVENT_INIT)

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

 * 在后台线程接收事件的BackgroundLiveEvent
  ```kotlin
  // 1.创建一个在后台线程接收事件的BackgroundLiveEvent，它包含一个初始事件
  val liveEvent = MutableBackgroundLiveEvent<String>(EVENT_INIT)

  // 2.观察
  // 2.1 使用Observer观察时，会在全局默全局统一的子线程接收事件，此线程也用于执行BackgroundLiveEvent内部的其他逻辑。
  liveEvent.observe(LifecycleOwner, Observer{ event ->
      // 线程名为default-dispatcher
      // 不建议在此做耗时操作
  })

  // 2.2 使用BackgroundObserver观察时，可以指定一个事件分发器，用于指定后台线程接收事件
  // SDK预先实现了EventDispatcher.BACKGROUND、EventDispatcher.ASYNC、EventDispatcher.MAIN

  // EventDispatcher.BACKGROUND
  liveEvent.observe(LifecycleOwner, object: BackgroundObserver<String>(dispatcher = EventDispatcher.BACKGROUND) {
      override fun onChanged(t: String) {
          // 线程名为background-event-dispatcher
          // 不建议在此做耗时操作
      }
  })

  // EventDispatcher.ASYNC
  liveEvent.observe(LifecycleOwner, object: BackgroundObserver<String>(dispatcher = EventDispatcher.ASYNC) {
      override fun onChanged(t: String) {
          // 线程名为pool-async-event-dispatcher-thread-xx
          // 可在此做耗时操作
      }
  })

  // EventDispatcher.MAIN
  liveEvent.observe(LifecycleOwner, object: BackgroundObserver<String>(dispatcher = EventDispatcher.MAIN) {
      override fun onChanged(t: String) {
          // 线程名为main
          // 不建议在此做耗时操作
      }
  })

  // custom
  liveEvent.observe(LifecycleOwner, object: BackgroundObserver<String>(dispatcher = yourCustomDispatcher) {
      override fun onChanged(t: String) {
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
