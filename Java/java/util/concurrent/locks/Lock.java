/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

/**
 * {@code Lock} 实现提供了相比于使用 {@code synchronized} 的方法和声明更多的
 * 拓展的锁操作.它们允许更加灵活的结构,可能拥有非常不同的属性,并且可能支持多种关联的
 * {@link Condition}对象.
 * {@code Lock} implementations provide more extensive locking
 * operations than can be obtained using {@code synchronized} methods
 * and statements.  They allow more flexible structuring, may have
 * quite different properties, and may support multiple associated
 * {@link Condition} objects.
 *
 * <p>一个锁是用来控制访问一个线程共享资源的工具.通常情况下,一个锁对一个共享资源提供
 * 了单独的访问权限:同一时间只有一个线程可以获取到锁并且所有访问这个共享资源的线程都需要
 * 先获取到这个锁.然而,一些锁允许异步访问一个共享资源,比如读取锁 {@link ReadWriteLock}
 * <p>A lock is a tool for controlling access to a shared resource by
 * multiple threads. Commonly, a lock provides exclusive access to a
 * shared resource: only one thread at a time can acquire the lock and
 * all access to the shared resource requires that the lock be
 * acquired first. However, some locks may allow concurrent access to
 * a shared resource, such as the read lock of a {@link ReadWriteLock}.
 *
 * <p>使用 {@code synchronized} 的方法或者声明对每一个对象的访问提供了隐式的监视锁,但是
 * 强制要求所有的锁获取和释放都要使用一个块状结构的方式:当获取多个锁的时候必须要使用相反的顺序
 * 来释放他们,并且所有的锁必须要在他们获取锁的相同的作用域中释放.
 * <p>The use of {@code synchronized} methods or statements provides
 * access to the implicit monitor lock associated with every object, but
 * forces all lock acquisition and release to occur in a block-structured way:
 * when multiple locks are acquired they must be released in the opposite
 * order, and all locks must be released in the same lexical scope in which
 * they were acquired.
 *
 * <p>虽然 {@code synchronized} 方法和声明的作用域机制使得监视器锁的编程变得更加
 * 简单了,并且有助于避免许多常见的锁编程异常,但是始终会有那么一些场景你会需要使用一些更
 * 灵活的锁.比如,一些用来遍历异步访问数据结构需要使用&quot;交替&quot;或者&quot;
 * 链式锁&quot;:你获取一个节点 A 的锁,然后获取节点 B 的锁,然后释放 A 并获取C, 然后
 * 释放 B 并获取 D 等等这样的操作.{@code Lock}接口的实现类通过允许在不同的作用域中
 * 获取和释放锁来实现这种操作,并且允许使用任意的顺序获取和释放多个锁.
 * <p>While the scoping mechanism for {@code synchronized} methods
 * and statements makes it much easier to program with monitor locks,
 * and helps avoid many common programming errors involving locks,
 * there are occasions where you need to work with locks in a more
 * flexible way.For example, some algorithms for traversing
 * concurrently accessed data structures require the use of
 * &quot;hand-over-hand&quot; or &quot;chain locking&quot;: you
 * acquire the lock of node A, then node B, then release A and acquire
 * C, then release B and acquire D and so on.  Implementations of the
 * {@code Lock} interface enable the use of such techniques by
 * allowing a lock to be acquired and released in different scopes,
 * and allowing multiple locks to be acquired and released in any
 * order.
 *
 * <p>伴随着灵活度的增加也带来了额外的责任.因为缺少了像 {@code synchronized} 方法
 * 和声明的块状锁结构,就失去了自动释放锁的功能.在大多数情况下,要使用下列的模板:
 * <p>With this increased flexibility comes additional
 * responsibility. The absence of block-structured locking removes the
 * automatic release of locks that occurs with {@code synchronized}
 * methods and statements. In most cases, the following idiom
 * should be used:
 *
 * <pre> {@code
 * Lock l = ...;
 * l.lock();
 * try {
 *   // access the resource protected by this lock
 * } finally {
 *   l.unlock();
 * }}</pre>
 *
 * 当加锁和解锁发生在不同的作用域的时候,一定要注意确保所有持有锁期间执行的代码都是被
 * try-finally 或者 try-catch 保护的,以此来确保锁可以在必要的时候被释放.
 * When locking and unlocking occur in different scopes, care must be
 * taken to ensure that all code that is executed while the lock is
 * held is protected by try-finally or try-catch to ensure that the
 * lock is released when necessary.
 *
 * <p>{@code Lock} 的实现类相比于 {@code synchronized} 方法和声明提供了额外的
 * 功能,比如提供了一个非阻塞的获取锁的操作({@link #tryLock()}),一个获取可以被打断
 * 的锁的操作 ({@link #lockInterruptibly}) ,和一个获取有超时功能的锁操作
 * ({@link #tryLock(long, TimeUnit)}).
 * <p>{@code Lock} implementations provide additional functionality
 * over the use of {@code synchronized} methods and statements by
 * providing a non-blocking attempt to acquire a lock ({@link
 * #tryLock()}), an attempt to acquire the lock that can be
 * interrupted ({@link #lockInterruptibly}, and an attempt to acquire
 * the lock that can timeout ({@link #tryLock(long, TimeUnit)}).
 *
 * <p>一个 {@code Lock} 类还可以提供与隐式监视器锁很不同的行为和语义,保证顺序,
 * 不可重入功能,或者死锁检测.如果一个实现类提供了这样特殊的语义那么实现类必须对这些
 * 语义书写文档.
 * <p>A {@code Lock} class can also provide behavior and semantics
 * that is quite different from that of the implicit monitor lock,
 * such as guaranteed ordering, non-reentrant usage, or deadlock
 * detection. If an implementation provides such specialized semantics
 * then the implementation must document those semantics.
 *
 * <p>注意 {@code Lock} 实例也是一个寻常的对象并且他们也可以当做 {@code synchronized}
 * 声明的目标.获取一个 {@code Lock} 实例的监视器锁和调用该实例的 {@link #lock}
 * 方法并没有特别的关系.为了防止阅读的困惑,推荐避免这样使用 {@code Lock} 实例,除非
 * 是在他们自己的实现中.
 * <p>Note that {@code Lock} instances are just normal objects and can
 * themselves be used as the target in a {@code synchronized} statement.
 * Acquiring the
 * monitor lock of a {@code Lock} instance has no specified relationship
 * with invoking any of the {@link #lock} methods of that instance.
 * It is recommended that to avoid confusion you never use {@code Lock}
 * instances in this way, except within their own implementation.
 *
 * <p>除了特殊说明的地方,传递一个 {@code null} 值来当做参数会导致抛出一个
 * {@link NullPointerException}
 * <p>Except where noted, passing a {@code null} value for any
 * parameter will result in a {@link NullPointerException} being
 * thrown.
 *
 * <h3>内存同步</h3>
 * <h3>Memory Synchronization</h3>
 *
 * <p>所有 {@code Lock} 实现 <em>必须</em> 实现和内置的监视器锁相同的内存同步语义
 * ,和 <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html#jls-17.4">
 * <cite>The Java&trade; Language Specification</cite> 的第17章</a>描述的那样:
 * <ul>
 * <li>一个成功的 {@code lock} 操作拥有和 <em>Lock</em> 动作相同的内存同步效果
 * <li>一个成功的 {@code unlock} 操作拥有和 <em>Unlock</em> 动作相同的内存同步效果
 * </ul>
 * <p>All {@code Lock} implementations <em>must</em> enforce the same
 * memory synchronization semantics as provided by the built-in monitor
 * lock, as described in
 * <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html#jls-17.4">
 * Chapter 17 of
 * <cite>The Java&trade; Language Specification</cite></a>:
 * <ul>
 * <li>A successful {@code lock} operation has the same memory
 * synchronization effects as a successful <em>Lock</em> action.
 * <li>A successful {@code unlock} operation has the same
 * memory synchronization effects as a successful <em>Unlock</em> action.
 * </ul>
 *
 * 不成功的锁定和解锁操作,和重入锁定/解锁操作,是没有内存同步效果的.
 * Unsuccessful locking and unlocking operations, and reentrant
 * locking/unlocking operations, do not require any memory
 * synchronization effects.
 *
 * <h3>实现时的注意事项</h3>
 * <h3>Implementation Considerations</h3>
 *
 * 三种获取锁的方式(可中断的,不可中断的,和可超时的)在他们的性能表现,顺序保证,
 * 或者其他的实现的质量等有所不同.并且,在给定的 {@code Lock} 类中断一个
 * <em>正在进行</em>的请求锁操作不一定是可行的.因此,这三种锁的实现不要求
 * 他们的锁请求要定义完全相同的保证和语义,也不要求要支持中断一个进行中的锁请求.
 * 一个实现要求清楚地对每一个锁方法的语义和保证进行注释.它必须要遵守这个接口中
 * 定义的中断语义,它同样支持拓展这个锁请求的中断:可以是总的层面的,也可以是在方
 * 法的层面上.
 * <p>The three forms of lock acquisition (interruptible,
 * non-interruptible, and timed) may differ in their performance
 * characteristics, ordering guarantees, or other implementation
 * qualities.  Further, the ability to interrupt the <em>ongoing</em>
 * acquisition of a lock may not be available in a given {@code Lock}
 * class.  Consequently, an implementation is not required to define
 * exactly the same guarantees or semantics for all three forms of
 * lock acquisition, nor is it required to support interruption of an
 * ongoing lock acquisition.  An implementation is required to clearly
 * document the semantics and guarantees provided by each of the
 * locking methods. It must also obey the interruption semantics as
 * defined in this interface, to the extent that interruption of lock
 * acquisition is supported: which is either totally, or only on
 * method entry.
 *
 * <p>因为中断通常意味着取消,而且检查中断通常是很少见的,实现可以对中断的响应而不是对普通
 * 方法的响应.这么做是对的,就算这个中断发生在另一个动作已经解除了线程阻塞的情况下.一个实现
 * 也应该对这种行为做一个记录.
 * <p>As interruption generally implies cancellation, and checks for
 * interruption are often infrequent, an implementation can favor responding
 * to an interrupt over normal method return. This is true even if it can be
 * shown that the interrupt occurred after another action may have unblocked
 * the thread. An implementation should document this behavior.
 *
 * @see ReentrantLock
 * @see Condition
 * @see ReadWriteLock
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Lock {

  /**
   * 请求锁
   * Acquires the lock.
   *
   * <p>如果锁是不可用的那么当前线程出于线程调度的目的变为不可用的并进入休眠状态直到
   * 获取到锁.
   * <p>If the lock is not available then the current thread becomes
   * disabled for thread scheduling purposes and lies dormant until the
   * lock has been acquired.
   *
   * <p><b>实现的注意事项</b>
   * <p><b>Implementation Considerations</b>
   *
   * <p>一个 {@code Lock} 的实现可能可以检测锁错误的用法,比如一个可能会造成死锁的调用,
   * 并且在这种情况下可能会抛出一个(没有检查)的异常.这种情况和异常类型必须记录在 {@code Lock}
   * 的实现中.
   * <p>A {@code Lock} implementation may be able to detect erroneous use
   * of the lock, such as an invocation that would cause deadlock, and
   * may throw an (unchecked) exception in such circumstances.  The
   * circumstances and the exception type must be documented by that
   * {@code Lock} implementation.
   */
  void lock();

  /**
   * 获得锁除非当前线程是 {@linkplain Thread#interrupt interrupted}.
   * Acquires the lock unless the current thread is
   * {@linkplain Thread#interrupt interrupted}.
   *
   * 当可用的时候获得锁,并且立即返回.
   * <p>Acquires the lock if it is available and returns immediately.
   *
   * 如果当前锁不可用那么当前线程出于线程调度目的变为不可用并进入休眠直到下列两种
   * 情况中的一种发生:
   * <p>If the lock is not available then the current thread becomes
   * disabled for thread scheduling purposes and lies dormant until
   * one of two things happens:
   *
   *
   * <ul>
   * <li>锁被当前线程获取
   * <li>其他的线程 {@linkplain Thread#interrupt interrupts} 当前线程,
   * 并且支持中断锁的获取.
   * </ul>
   * <ul>
   * <li>The lock is acquired by the current thread; or
   * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
   * current thread, and interruption of lock acquisition is supported.
   * </ul>
   *
   * 如果当前线程:
   * <p>If the current thread:
   * <ul>
   *在进入这个方法前设置了它的中断状态;或者
   * <li>has its interrupted status set on entry to this method; or
   * 在获取锁的过程中 {@linkplain Thread#interrupt interrupted} ,并且它
   * 支持中断获取锁过程.
   * <li>is {@linkplain Thread#interrupt interrupted} while acquiring the
   * lock, and interruption of lock acquisition is supported,
   * </ul>
   * 那么会抛出一个{@link InterruptedException} 并使得当前线程的中断标志被清除.
   * then {@link InterruptedException} is thrown and the current thread's
   * interrupted status is cleared.
   *
   * <p><b>实现的注意事项</b>
   * <p><b>Implementation Considerations</b>
   *
   * 在一些实现中是可以中断一个锁的获取过程的,并且它可能是比较昂贵的操作.程序员
   * 应该注意到这点可能会导致一些问题.当这个情况发生实现中应该记录这点.
   * <p>The ability to interrupt a lock acquisition in some
   * implementations may not be possible, and if possible may be an
   * expensive operation.  The programmer should be aware that this
   * may be the case. An implementation should document when this is
   * the case.
   *
   * 实现可以对中断的响应而不是对普通方法的响应.
   * <p>An implementation can favor responding to an interrupt over
   * normal method return.
   *
   * 一个 {@code Lock} 的实现可能可以检测锁错误的用法,比如一个可能会造成死锁的调用,
   * 并且在这种情况下可能会抛出一个(没有检查)的异常.这种情况和异常类型必须记录在
   * {@code Lock} 的实现中.
   * <p>A {@code Lock} implementation may be able to detect
   * erroneous use of the lock, such as an invocation that would
   * cause deadlock, and may throw an (unchecked) exception in such
   * circumstances.  The circumstances and the exception type must
   * be documented by that {@code Lock} implementation.
   *
   * @throws InterruptedException if the current thread is
   *         interrupted while acquiring the lock (and interruption
   *         of lock acquisition is supported)
   */
  void lockInterruptibly() throws InterruptedException;

  /**
   * 当调用的时候锁是自由的时候获取锁.
   * Acquires the lock only if it is free at the time of invocation.
   *
   * 当可用的时候获得锁,并且立即返回值 {@code true}.
   * 如果当前锁不可用那么这个方法会立刻返回值 {@code false}.
   * <p>Acquires the lock if it is available and returns immediately
   * with the value {@code true}.
   * If the lock is not available then this method will return
   * immediately with the value {@code false}.
   *
   *这个方法常用的模板为:
   * <p>A typical usage idiom for this method would be:
   * <pre> {@code
   * Lock lock = ...;
   * if (lock.tryLock()) {
   *   try {
   *     // manipulate protected state
   *   } finally {
   *     lock.unlock();
   *   }
   * } else {
   *   // perform alternative actions
   * }}</pre>
   *
   * 这个使用保证了当获得锁的时候锁是释放的,并且不会在没有获得锁的时候进行解锁.
   * This usage ensures that the lock is unlocked if it was acquired, and
   * doesn't try to unlock if the lock was not acquired.
   *
   * @return {@code true} if the lock was acquired and
   *         {@code false} otherwise
   */
  boolean tryLock();

  /**
   * 当这个锁在给定的等待时间内是自由的并且当前线程没有被
   *  {@linkplain Thread#interrupt interrupted} 的时候获取锁.
   * Acquires the lock if it is free within the given waiting time and the
   * current thread has not been {@linkplain Thread#interrupt interrupted}.
   *
   *  当可用的时候获得锁,并且立即返回值 {@code true}.
   *  如果当前锁不可用那么当前线程出于线程调度目的变为不可用并进入休眠直到下列三种
   * 情况中的一种发生:
   * <ul>
   * <li>锁被当前线程获取
   * <li>其他的线程 {@linkplain Thread#interrupt interrupts} 当前线程,
   * 并且支持中断锁的获取.
   * <li>超过了指定的等待时间
   * </ul>
   * <p>If the lock is available this method returns immediately
   * with the value {@code true}.
   * If the lock is not available then
   * the current thread becomes disabled for thread scheduling
   * purposes and lies dormant until one of three things happens:
   * <ul>
   * <li>The lock is acquired by the current thread; or
   * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
   * current thread, and interruption of lock acquisition is supported; or
   * <li>The specified waiting time elapses
   * </ul>
   *
   * 如果获取到锁那么返回值 {@code true}
   * <p>If the lock is acquired then the value {@code true} is returned.
   *
   * 如果当前线程:
   * <p>If the current thread:
   * <ul>
   *     在进入这个方法前设置了它的中断状态;或者
   * <li>has its interrupted status set on entry to this method; or
   * 在获取锁的过程中 {@linkplain Thread#interrupt interrupted} ,并且它
   * 支持中断获取锁过程.
   * <li>is {@linkplain Thread#interrupt interrupted} while acquiring
   * the lock, and interruption of lock acquisition is supported,
   * </ul>
   * 那么会抛出一个{@link InterruptedException} 并使得当前线程的中断标志被清除.
   * then {@link InterruptedException} is thrown and the current thread's
   * interrupted status is cleared.
   *
   * 如果超过了指定的等待时间那么返回值 {@code false}.
   * 如果时间小于或者等于0那么方法不会进行等待.
   * <p>If the specified waiting time elapses then the value {@code false}
   * is returned.
   * If the time is
   * less than or equal to zero, the method will not wait at all.
   *
   * 实现注意事项
   * <p><b>Implementation Considerations</b>
   *
   * 中断锁获取的能力在一些实现中并不可用,如果可用的话也可能是一个昂贵的操作.
   * 程序员需要知道这个可能会导致一些问题.实现需要把这种情况记录下来.
   * <p>The ability to interrupt a lock acquisition in some implementations
   * may not be possible, and if possible may
   * be an expensive operation.
   * The programmer should be aware that this may be the case. An
   * implementation should document when this is the case.
   *
   * 实现可以对中断的响应而不是对普通方法的响应.
   * <p>An implementation can favor responding to an interrupt over normal
   * method return, or reporting a timeout.
   *
   * 一个 {@code Lock} 的实现可能可以检测锁错误的用法,比如一个可能会造成死锁的调用,
   * 并且在这种情况下可能会抛出一个(没有检查)的异常.这种情况和异常类型必须记录在
   * {@code Lock} 的实现中.
   * <p>A {@code Lock} implementation may be able to detect
   * erroneous use of the lock, such as an invocation that would cause
   * deadlock, and may throw an (unchecked) exception in such circumstances.
   * The circumstances and the exception type must be documented by that
   * {@code Lock} implementation.
   *
   * @param time the maximum time to wait for the lock
   * @param unit the time unit of the {@code time} argument
   * @return {@code true} if the lock was acquired and {@code false}
   *         if the waiting time elapsed before the lock was acquired
   *
   * @throws InterruptedException if the current thread is interrupted
   *         while acquiring the lock (and interruption of lock
   *         acquisition is supported)
   */
  boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

  /**
   * 释放锁
   * Releases the lock.
   *
   * <p><b>Implementation Considerations</b>
   *
   * 一个 {@code Lock} 的实现通常会限制哪个线程可以释放一个锁(通常情况下只有锁持有者
   * 才可以释放锁)并且在违背了要求的时候可能会抛出一个(未检查)的异常.所有的限制和
   * 异常类型必须记录在 {@code Lock} 实现中.
   * <p>A {@code Lock} implementation will usually impose
   * restrictions on which thread can release a lock (typically only the
   * holder of the lock can release it) and may throw
   * an (unchecked) exception if the restriction is violated.
   * Any restrictions and the exception
   * type must be documented by that {@code Lock} implementation.
   */
  void unlock();

  /**
   * 返回一个新的绑定在这个{@code Lock}实例的 {@link Condition}实例
   * Returns a new {@link Condition} instance that is bound to this
   * {@code Lock} instance.
   *
   *在等待这个condition的时候当前线程必须持有锁.
   * 调用 {@link Condition#await()} 将在等待之前自动释放锁,并且在等待返回前重新
   * 请求锁.
   * <p>Before waiting on the condition the lock must be held by the
   * current thread.
   * A call to {@link Condition#await()} will atomically release the lock
   * before waiting and re-acquire the lock before the wait returns.
   *
   * <p><b>Implementation Considerations</b>
   *
   * {@link Condition}实例的具体操作依赖于{@code Lock}实现并且必须记录在实现中.
   * <p>The exact operation of the {@link Condition} instance depends on
   * the {@code Lock} implementation and must be documented by that
   * implementation.
   *
   * @return A new {@link Condition} instance for this {@code Lock} instance
   * @throws UnsupportedOperationException if this {@code Lock}
   *         implementation does not support conditions
   */
  Condition newCondition();
}
