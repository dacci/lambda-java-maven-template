package org.dacci;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import com.amazonaws.services.lambda.runtime.tests.annotations.Events;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

@ExtendWith(MockitoExtension.class)
public class HandlerTest {
  @Mock private Context context;
  @Mock private SqsClient sqs;
  @InjectMocks private Handler handler;
  @Captor private ArgumentCaptor<SendMessageRequest> captor;

  @ParameterizedTest
  @Events(type = S3Event.class, events = @Event("test_data/s3event.json"))
  @SuppressWarnings("unchecked")
  void handleRequest(final S3Event event) {
    when(sqs.sendMessage(any(Consumer.class))).thenCallRealMethod();
    when(sqs.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(SendMessageResponse.builder().build());

    handler.handleRequest(event, context);

    verify(sqs).sendMessage(captor.capture());
    final var request = captor.getValue();
    assertThat(request.queueUrl()).isEqualTo("https://queue");
    assertThat(request.messageBody())
        .isEqualTo("s3://lambda-artifacts-deafc19498e3f2df/b21b84d653bb07b05b1e6b33684dc11b");
    assertThat(request.messageGroupId()).isEqualTo("lambda-artifacts-deafc19498e3f2df");
    assertThat(request.messageDeduplicationId())
        .isEqualTo("s3://lambda-artifacts-deafc19498e3f2df/b21b84d653bb07b05b1e6b33684dc11b");
  }

  @ParameterizedTest
  @Events(type = S3Event.class, events = @Event("test_data/s3event.json"))
  @SuppressWarnings("unchecked")
  void handleRequest_SqsException(final S3Event event) {
    when(sqs.sendMessage(any(Consumer.class))).thenThrow(SqsException.class);

    try {
      handler.handleRequest(event, context);
    } catch (Throwable e) {
      fail("should not throw", e);
    }
  }
}
