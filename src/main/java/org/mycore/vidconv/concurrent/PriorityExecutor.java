/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */
package org.mycore.vidconv.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class PriorityExecutor {

    /**
     * Creates a thread pool that reuses a fixed number of threads
     * operating off a shared unbounded queue.  At any point, at most
     * {@code nThreads} threads will be active processing tasks.
     * If additional tasks are submitted when all threads are active,
     * they will wait in the queue until a thread is available.
     * If any thread terminates due to a failure during execution
     * prior to shutdown, a new one will take its place if needed to
     * execute subsequent tasks.  The threads in the pool will exist
     * until it is explicitly {@link ExecutorService#shutdown shutdown}.
     *
     * @param nThreads the number of threads in the pool
     * @return the newly created thread pool
     * @throws IllegalArgumentException if {@code nThreads <= 0}
     */
    public static ExecutorService newFixedThreadPool(int nThreads) {
        return new PriorityThreadPoolExecutor(nThreads, nThreads,
            0L, TimeUnit.MILLISECONDS,
            new PriorityBlockingQueue<Runnable>());
    }

    /**
     * Creates a thread pool that reuses a fixed number of threads
     * operating off a shared unbounded queue, using the provided
     * ThreadFactory to create new threads when needed.  At any point,
     * at most {@code nThreads} threads will be active processing
     * tasks.  If additional tasks are submitted when all threads are
     * active, they will wait in the queue until a thread is
     * available.  If any thread terminates due to a failure during
     * execution prior to shutdown, a new one will take its place if
     * needed to execute subsequent tasks.  The threads in the pool will
     * exist until it is explicitly {@link ExecutorService#shutdown
     * shutdown}.
     *
     * @param nThreads the number of threads in the pool
     * @param threadFactory the factory to use when creating new threads
     * @return the newly created thread pool
     * @throws NullPointerException if threadFactory is null
     * @throws IllegalArgumentException if {@code nThreads <= 0}
     */
    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        return new PriorityThreadPoolExecutor(nThreads, nThreads,
            0L, TimeUnit.MILLISECONDS,
            new PriorityBlockingQueue<Runnable>(),
            threadFactory);
    }

    public static class PriorityThreadPoolExecutor extends ThreadPoolExecutor {

        /**
         * Creates a new {@code PriorityThreadPoolExecutor} with the given initial
         * parameters and default thread factory and rejected execution handler.
         * It may be more convenient to use one of the {@link PriorityExecutor} factory
         * methods instead of this general purpose constructor.
         *
         * @param corePoolSize the number of threads to keep in the pool, even
         *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
         * @param maximumPoolSize the maximum number of threads to allow in the
         *        pool
         * @param keepAliveTime when the number of threads is greater than
         *        the core, this is the maximum time that excess idle threads
         *        will wait for new tasks before terminating.
         * @param unit the time unit for the {@code keepAliveTime} argument
         * @param workQueue the queue to use for holding tasks before they are
         *        executed.  This queue will hold only the {@code Runnable}
         *        tasks submitted by the {@code execute} method.
         * @throws IllegalArgumentException if one of the following holds:<br>
         *         {@code corePoolSize < 0}<br>
         *         {@code keepAliveTime < 0}<br>
         *         {@code maximumPoolSize <= 0}<br>
         *         {@code maximumPoolSize < corePoolSize}
         * @throws NullPointerException if {@code workQueue} is null
         */
        public PriorityThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            PriorityBlockingQueue<Runnable> workQueue) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        }

        /**
         * Creates a new {@code PriorityThreadPoolExecutor} with the given initial
         * parameters and default rejected execution handler.
         *
         * @param corePoolSize the number of threads to keep in the pool, even
         *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
         * @param maximumPoolSize the maximum number of threads to allow in the
         *        pool
         * @param keepAliveTime when the number of threads is greater than
         *        the core, this is the maximum time that excess idle threads
         *        will wait for new tasks before terminating.
         * @param unit the time unit for the {@code keepAliveTime} argument
         * @param workQueue the queue to use for holding tasks before they are
         *        executed.  This queue will hold only the {@code Runnable}
         *        tasks submitted by the {@code execute} method.
         * @param threadFactory the factory to use when the executor
         *        creates a new thread
         * @throws IllegalArgumentException if one of the following holds:<br>
         *         {@code corePoolSize < 0}<br>
         *         {@code keepAliveTime < 0}<br>
         *         {@code maximumPoolSize <= 0}<br>
         *         {@code maximumPoolSize < corePoolSize}
         * @throws NullPointerException if {@code workQueue}
         *         or {@code threadFactory} is null
         */
        public PriorityThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            PriorityBlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        }

        /**
         * Creates a new {@code PriorityThreadPoolExecutor} with the given initial
         * parameters and default thread factory.
         *
         * @param corePoolSize the number of threads to keep in the pool, even
         *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
         * @param maximumPoolSize the maximum number of threads to allow in the
         *        pool
         * @param keepAliveTime when the number of threads is greater than
         *        the core, this is the maximum time that excess idle threads
         *        will wait for new tasks before terminating.
         * @param unit the time unit for the {@code keepAliveTime} argument
         * @param workQueue the queue to use for holding tasks before they are
         *        executed.  This queue will hold only the {@code Runnable}
         *        tasks submitted by the {@code execute} method.
         * @param handler the handler to use when execution is blocked
         *        because the thread bounds and queue capacities are reached
         * @throws IllegalArgumentException if one of the following holds:<br>
         *         {@code corePoolSize < 0}<br>
         *         {@code keepAliveTime < 0}<br>
         *         {@code maximumPoolSize <= 0}<br>
         *         {@code maximumPoolSize < corePoolSize}
         * @throws NullPointerException if {@code workQueue}
         *         or {@code handler} is null
         */
        public PriorityThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            PriorityBlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
        }

        /**
         * Creates a new {@code PriorityThreadPoolExecutor} with the given initial
         * parameters.
         *
         * @param corePoolSize the number of threads to keep in the pool, even
         *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
         * @param maximumPoolSize the maximum number of threads to allow in the
         *        pool
         * @param keepAliveTime when the number of threads is greater than
         *        the core, this is the maximum time that excess idle threads
         *        will wait for new tasks before terminating.
         * @param unit the time unit for the {@code keepAliveTime} argument
         * @param workQueue the queue to use for holding tasks before they are
         *        executed.  This queue will hold only the {@code Runnable}
         *        tasks submitted by the {@code execute} method.
         * @param threadFactory the factory to use when the executor
         *        creates a new thread
         * @param handler the handler to use when execution is blocked
         *        because the thread bounds and queue capacities are reached
         * @throws IllegalArgumentException if one of the following holds:<br>
         *         {@code corePoolSize < 0}<br>
         *         {@code keepAliveTime < 0}<br>
         *         {@code maximumPoolSize <= 0}<br>
         *         {@code maximumPoolSize < corePoolSize}
         * @throws NullPointerException if {@code workQueue}
         *         or {@code threadFactory} or {@code handler} is null
         */
        public PriorityThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            PriorityBlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
            RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        }

        protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value,
            int priority) {
            return new PriorityFutureTask<T>(runnable, value, priority);
        }

        protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable,
            int priority) {
            return new PriorityFutureTask<T>(callable, priority);
        }

        /**
         * Submits a Runnable task with priority for execution and returns a Future representing that task. 
         * The Future's get method will return null upon successful completion.
         * 
         * @param task the task to submit
         * @return a Future representing pending completion of the task
         */
        @Override
        public Future<?> submit(Runnable task) {
            return submit(task, null, 0);
        }

        /**
         * Submits a Runnable task with priority for execution and returns a Future representing that task. 
         * The Future's get method will return null upon successful completion.
         * 
         * @param task the task to submit
         * @param priority the priority of task
         * @return a Future representing pending completion of the task
         */
        public Future<?> submit(Runnable task, int priority) {
            return submit(task, null, priority);
        }

        /**
         * Submits a Runnable task with priority for execution and returns a Future representing that task. 
         * The Future's get method will return null upon successful completion.
         * 
         * @param task the task to submit
         * @param result the result to return
         * @return a Future representing pending completion of the task
         */
        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return submit(task, result, 0);
        }

        /**
         * Submits a Runnable task with priority for execution and returns a Future representing that task. 
         * The Future's get method will return null upon successful completion.
         * 
         * @param task the task to submit
         * @param result the result to return
         * @param priority the priority of task
         * @return a Future representing pending completion of the task
         */
        public <T> Future<T> submit(Runnable task, T result, int priority) {
            if (task == null)
                throw new NullPointerException();
            RunnableFuture<T> ftask = newTaskFor(task, result, priority);
            execute(ftask);
            return ftask;
        }

        /**
         * Submits a value-returning task for execution and returns a Future representing the pending results of the task.
         * The Future's get method will return the task's result upon successful completion.
         * <br />
         * <br />
         * If you would like to immediately block waiting for a task, you can use constructions of the form 
         * <code>result = exec.submit(aCallable).get();</code>
         * 
         * @param task the task to submit
         * @param priority the priority of task
         * @return a Future representing pending completion of the task
         */
        public <T> Future<T> submit(Callable<T> task, int priority) {
            if (task == null)
                throw new NullPointerException();
            RunnableFuture<T> ftask = newTaskFor(task, priority);
            execute(ftask);
            return ftask;
        }
    }

    private static class PriorityFutureTask<T> extends FutureTask<T>
        implements Runnable, Comparable<PriorityFutureTask<T>> {

        private final int priority;

        /**
         * Instantiates a new priority future task.
         *
         * @param callable the callable
         * @param priority the priority
         */
        public PriorityFutureTask(Callable<T> callable, int priority) {
            super(callable);
            this.priority = priority;
        }

        /**
         * Instantiates a new priority future task.
         *
         * @param runnable the runnable
         * @param result the result
         * @param priority the priority
         */
        public PriorityFutureTask(Runnable runnable, T result, int priority) {
            super(runnable, result);
            this.priority = priority;
        }

        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(PriorityFutureTask<T> o) {
            return priority - o.priority;
        }
    }
}
