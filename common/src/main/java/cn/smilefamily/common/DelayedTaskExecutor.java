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

    public void addFirst(String taskName, Runnable task) {
        Task t = new Task(taskName, task);
        if (queue.contains(t)) {
            return;
        }
        queue.addFirst(t);
    }

    public void addLast(String taskName, Runnable task) {
        Task t = new Task(taskName, task);
        if (queue.contains(t)) {
            return;
        }
        queue.addLast(new Task(taskName, task));
    }

    public void addFirst(Runnable task) {
        queue.addFirst(new Task(task));
    }

    public void addLast(Runnable task) {
        queue.addLast(new Task(task));
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void execute() {
        Task task = null;
        while ((task = queue.pollLast()) != null) {
            task.runnable().run();
        }
    }

    @Aspect
    private static class Trigger {
        @AfterReturning("execution(public void add*(..))&&target(executor)&&within(DelayedTaskExecutor)")
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

    public static void main(String[] args) {
        DelayedTaskExecutor delayedTaskExecutor = new DelayedTaskExecutor("test", () -> true);
        delayedTaskExecutor.addFirst(() -> {
            System.out.println("test...");
        });
    }
}
