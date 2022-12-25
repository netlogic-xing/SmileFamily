package cn.smilefamily.common;


import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class DelayedTaskExecutor {
    private static final Logger logger = LoggerFactory.getLogger(DelayedTaskExecutor.class);
    private static AtomicInteger counter = new AtomicInteger(0);
    private String name = "DelayedTaskExecutor-" + counter.getAndIncrement();

    private Condition executionCondition;
    private Deque<Task> queue = new ArrayDeque<>();

    public String getName() {
        return name;
    }

    public DelayedTaskExecutor(Condition executionCondition) {
        this.executionCondition = executionCondition;
    }

    public DelayedTaskExecutor(String name, Condition executionCondition) {
        this.executionCondition = executionCondition;
        this.name = name;
    }

    public void enqueue(String taskName, Runnable task) {
        Task t = new Task(taskName, task);
        if (queue.contains(t)) {
            return;
        }
        queue.add(t);
    }

    public void jumpqueue(String taskName, Runnable task) {
        Task t = new Task(taskName, task);
        if (queue.contains(t)) {
            return;
        }
        queue.addLast(new Task(taskName, task));
    }
    //加入队尾,后执行
    public void enqueue(Runnable task) {
        queue.add(new Task(task));
    }
    //加入队头，意味着先执行
    public void jumpqueue(Runnable task) {
        queue.addLast(new Task(task));
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * 从队列头部开始执行整个队列
     */
    public void execute() {
        Task task = null;
        while ((task = queue.poll()) != null) {
            task.runnable().run();
        }
    }

    @Aspect
    private static class Trigger {
        @AfterReturning("execution(public void *queue(..))&&target(executor)&&within(DelayedTaskExecutor)")
        public void trigger(DelayedTaskExecutor executor) {
            if (executor.executionCondition.met()) {
                executor.execute();
            }
        }
    }

    private record Task(String name, Runnable runnable) {
        private static AtomicInteger counter = new AtomicInteger(0);

        public Task(Runnable runnable) {
            this("Anonymous-" + counter.getAndIncrement(), runnable);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Task task = (Task) o;
            return name.equals(task.name);
        }

        @Override
        public String toString() {
            return "Task(" + name + ')';
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
    static boolean test;
    public static void main(String[] args) {

        DelayedTaskExecutor delayedTaskExecutor = new DelayedTaskExecutor("test", () -> test);
        delayedTaskExecutor.enqueue(() -> {
            System.out.println("test...1");
        });
        delayedTaskExecutor.enqueue(() -> {
            System.out.println("test...2");
        });
        delayedTaskExecutor.enqueue(() -> {
            System.out.println("test...3");
        });
        test=true;
        delayedTaskExecutor.execute();
    }
}
