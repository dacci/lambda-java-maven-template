package org.dacci;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;

import software.amazon.awssdk.services.sqs.SqsClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Handler implements RequestHandler<S3Event, Void> {
  private static final String QUEUE_URL = System.getenv("QUEUE_URL");

  private final SqsClient sqs;

  public Handler() {
    sqs = SqsClient.create();
  }

  Handler(final SqsClient sqs) {
    this.sqs = sqs;
  }

  @Override
  public Void handleRequest(final S3Event event, final Context context) {
    try {
      for (var record : event.getRecords()) {
        handleRequest(record);
      }
    } catch (final RuntimeException e) {
      log.error("uncaught exception", e);
    }

    return null;
  }

  private void handleRequest(final S3EventNotificationRecord record) {
    final var uri =
        "s3://" + record.getS3().getBucket().getName() + "/" + record.getS3().getObject().getKey();
    sqs.sendMessage(
        b ->
            b.queueUrl(QUEUE_URL)
                .messageBody(uri)
                .messageGroupId(record.getS3().getBucket().getName())
                .messageDeduplicationId(uri));
  }
}
