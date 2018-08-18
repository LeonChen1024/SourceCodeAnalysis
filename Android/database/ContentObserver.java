/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.database;

import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;

/**
 * 接收内容改变的回调。
 * 必须要通过一个要添加到 {@link ContentObservable} 的对象实现。
 * Receives call backs for changes to content.
 * Must be implemented by objects which are added to a {@link ContentObservable}.
 *
 * 核心思想即观察者老模式
 */
public abstract class ContentObserver {
    private final Object mLock = new Object();
    private Transport mTransport; // guarded by mLock

    Handler mHandler;

    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public ContentObserver(Handler handler) {
        mHandler = handler;
    }

    /**
     * 获得使用 Transport 对象的权限。它并不是公共课直接使用的
     * Gets access to the binder transport object. Not for public consumption.
     *
     * {@hide}
     */
    public IContentObserver getContentObserver() {
        //加锁确保只生成一个
        synchronized (mLock) {
            if (mTransport == null) {
                mTransport = new Transport(this);
            }
            return mTransport;
        }
    }

    /**
     * 获取绑定的 Transport 对象，并且将 ContentObserver 里的 Transport 对象释放。
     * Gets access to the binder transport object, and unlinks the transport object
     * from the ContentObserver. Not for public consumption.
     *
     * {@hide}
     */
    public IContentObserver releaseContentObserver() {
        synchronized (mLock) {
            final Transport oldTransport = mTransport;
            if (oldTransport != null) {
                oldTransport.releaseContentObserver();
                mTransport = null;
            }
            return oldTransport;
        }
    }

    /**
     * 当这个观察者想要接收自己改变订阅内容的通知的时候返回 true
     * Returns true if this observer is interested receiving self-change notifications.
     *
     * 子类要重写这个方法来表明这个观察者是否对自己改变内容产生的变化感兴趣。
     * Subclasses should override this method to indicate whether the observer
     * is interested in receiving notifications for changes that it made to the
     * content itself.
     *
     * @return True if self-change notifications should be delivered to the observer.
     */
    public boolean deliverSelfNotifications() {
        return false;
    }

    /**
     * 这个方法当内容变化的时候调用
     * This method is called when a content change occurs.
     * <p>
     * 子类需要重写这个方法来处理内容变化
     * Subclasses should override this method to handle content changes.
     * </p>
     *
     * @param selfChange   当是自发性改变的通知返回true
     *                     True if this is a self-change notification.
     */
    public void onChange(boolean selfChange) {
        // Do nothing.  Subclass should override.
    }

    /**
     * 这个方法当内容变化的时候调用。包含了变化内容的Uri
     * This method is called when a content change occurs.
     * Includes the changed content Uri when available.
     * <p>
     * 子类需要重写这个方法来处理内容变化。为了确保兼容旧版本没有提供 Uri 参数的框架，
     * 只要他们实现了{@link #onChange(boolean, Uri)} 这个重载方法，
     * 程序应该要同时实现  {@link #onChange(boolean)} 这个重载方法。
     * Subclasses should override this method to handle content changes.
     * To ensure correct operation on older versions of the framework that
     * did not provide a Uri argument, applications should also implement
     * the {@link #onChange(boolean)} overload of this method whenever they
     * implement the {@link #onChange(boolean, Uri)} overload.
     * </p><p>
     * 实现示范
     * Example implementation:
     * <pre><code>
     * // Implement the onChange(boolean) method to delegate the change notification to
     * // the onChange(boolean, Uri) method to ensure correct operation on older versions
     * // of the framework that did not have the onChange(boolean, Uri) method.
     * {@literal @Override}
     * public void onChange(boolean selfChange) {
     *     onChange(selfChange, null);
     * }
     *
     * // Implement the onChange(boolean, Uri) method to take advantage of the new Uri argument.
     * {@literal @Override}
     * public void onChange(boolean selfChange, Uri uri) {
     *     // Handle change.
     * }
     * </code></pre>
     * </p>
     *
     * @param selfChange True if this is a self-change notification.
     * @param uri The Uri of the changed content, or null if unknown.
     */
    public void onChange(boolean selfChange, Uri uri) {
        onChange(selfChange);
    }

    /**
     * 分派改变的通知给观察者。包含了内容的 Uri 和内容改变的用户
     * Dispatches a change notification to the observer. Includes the changed
     * content Uri when available and also the user whose content changed.
     *
     * @param selfChange True if this is a self-change notification.
     * @param uri The Uri of the changed content, or null if unknown.
     * @param userId The user whose content changed. Can be either a specific
     *         user or {@link UserHandle#USER_ALL}.
     *
     * @hide
     */
    public void onChange(boolean selfChange, Uri uri, int userId) {
        onChange(selfChange, uri);
    }

    /**
     * Dispatches a change notification to the observer.
     * <p>
     * If a {@link Handler} was supplied to the {@link android.database.ContentObserver} constructor,
     * then a call to the {@link #onChange} method is posted to the handler's message queue.
     * Otherwise, the {@link #onChange} method is invoked immediately on this thread.
     * </p>
     *
     * @param selfChange True if this is a self-change notification.
     *
     * @deprecated Use {@link #dispatchChange(boolean, Uri)} instead.
     */
    @Deprecated
    public final void dispatchChange(boolean selfChange) {
        dispatchChange(selfChange, null);
    }

    /**
     *
     * Dispatches a change notification to the observer.
     * Includes the changed content Uri when available.
     * <p>
     * If a {@link Handler} was supplied to the {@link android.database.ContentObserver} constructor,
     * then a call to the {@link #onChange} method is posted to the handler's message queue.
     * Otherwise, the {@link #onChange} method is invoked immediately on this thread.
     * </p>
     *
     * @param selfChange True if this is a self-change notification.
     * @param uri The Uri of the changed content, or null if unknown.
     */
    public final void dispatchChange(boolean selfChange, Uri uri) {
        dispatchChange(selfChange, uri, UserHandle.getCallingUserId());
    }

    /**
     * 传递一个变化通知给观察者。包含了改变内容的 Uri 和内容变化了的用户。
     * Dispatches a change notification to the observer. Includes the changed
     * content Uri when available and also the user whose content changed.
     * <p>
     * 如果在 {@link android.database.ContentObserver} 的构造器里提供了一个 {@link Handler}，
     * 那么将会在 handler 的消息队列中调用 {@link #onChange} 方法。否则，
     * 将会在当前线程上调用{@link #onChange}。
     * If a {@link Handler} was supplied to the {@link android.database.ContentObserver} constructor,
     * then a call to the {@link #onChange} method is posted to the handler's message queue.
     * Otherwise, the {@link #onChange} method is invoked immediately on this thread.
     * </p>
     *
     * @param selfChange True if this is a self-change notification.
     * @param uri The Uri of the changed content, or null if unknown.
     * @param userId The user whose content changed.
     */
    private void dispatchChange(boolean selfChange, Uri uri, int userId) {
        if (mHandler == null) {
            onChange(selfChange, uri, userId);
        } else {
            mHandler.post(new NotificationRunnable(selfChange, uri, userId));
        }
    }


    private final class NotificationRunnable implements Runnable {
        private final boolean mSelfChange;
        private final Uri mUri;
        private final int mUserId;

        public NotificationRunnable(boolean selfChange, Uri uri, int userId) {
            mSelfChange = selfChange;
            mUri = uri;
            mUserId = userId;
        }

        @Override
        public void run() {
            android.database.ContentObserver.this.onChange(mSelfChange, mUri, mUserId);
        }
    }

    /**
     * 传递调用，通过该接口实现类调用外面类的具体实现
     */
    private static final class Transport extends IContentObserver.Stub {
        private android.database.ContentObserver mContentObserver;

        public Transport(android.database.ContentObserver contentObserver) {
            mContentObserver = contentObserver;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri, int userId) {
            android.database.ContentObserver contentObserver = mContentObserver;
            if (contentObserver != null) {
                contentObserver.dispatchChange(selfChange, uri, userId);
            }
        }

        public void releaseContentObserver() {
            mContentObserver = null;
        }
    }
}
