package org.flowable.engine.test.profiler;

import org.flowable.engine.common.impl.interceptor.CommandConfig;
import org.flowable.engine.impl.interceptor.AbstractCommandInterceptor;
import org.flowable.engine.impl.interceptor.Command;

/**
 * @author Joram Barrez
 */
public class TotalExecutionTimeCommandInterceptor extends AbstractCommandInterceptor {

    protected FlowableProfiler profiler;

    public TotalExecutionTimeCommandInterceptor() {
        this.profiler = FlowableProfiler.getInstance();
    }

    public <T> T execute(CommandConfig config, Command<T> command) {
        ProfileSession currentProfileSession = profiler.getCurrentProfileSession();

        if (currentProfileSession != null) {

            String className = command.getClass().getName();
            CommandExecutionResult commandExecutionResult = new CommandExecutionResult();
            currentProfileSession.setCurrentCommandExecution(commandExecutionResult);

            commandExecutionResult.setCommandFqn(className);

            long start = System.currentTimeMillis();
            T result = next.execute(config, command);
            long end = System.currentTimeMillis();
            long totalTime = end - start;
            commandExecutionResult.setTotalTimeInMs(totalTime);

            currentProfileSession.addCommandExecution(className, commandExecutionResult);
            currentProfileSession.clearCurrentCommandExecution();

            return result;

        } else {
            return next.execute(config, command);
        }

    }

}
