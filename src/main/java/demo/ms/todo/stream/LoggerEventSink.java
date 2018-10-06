package demo.ms.todo.stream;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface LoggerEventSink {

    public static final String MESSAGE_QUEUE = "MESSAGE_QUEUE";

    @Output(MESSAGE_QUEUE)
    MessageChannel output();
}
