/**
 * Copyright © 2019 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe;

import com.typesafe.config.Config;
import io.zeebe.client.api.clients.JobClient;
import io.zeebe.client.api.commands.CompleteJobCommandStep1;
import io.zeebe.client.api.response.ActivatedJob;
import io.zeebe.client.api.subscription.JobHandler;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SleepHandler implements JobHandler {
  private static final Logger LOG = LoggerFactory.getLogger(SleepHandler.class);

  private final long defaultDelay;
  private final boolean defaultReturnPayload;

  public SleepHandler(Config config) {
    this.defaultDelay = config.getDuration("handler.delay").toMillis();
    this.defaultReturnPayload = config.getBoolean("handler.returnPayload");
  }

  @Override
  public void handle(JobClient jobClient, ActivatedJob job) {
    long jobKey = job.getKey();
    long delay = getDelay(job);
    boolean returnPayload = getReturnPayload(job);

    try {
      Thread.sleep(delay);
      CompleteJobCommandStep1 command = jobClient.newCompleteCommand(jobKey);
      if (returnPayload) {
        command.payload(job.getPayload());
      }

      command.send().join();
      LOG.info(
          "Job with key {} completed with delay of {}ms and returned payload: {}",
          jobKey,
          delay,
          returnPayload);
    } catch (InterruptedException e) {
      LOG.error("Failed to sleep for {}ms, failing job {}", delay, jobKey, e);
      jobClient
          .newFailCommand(jobKey)
          .retries(job.getRetries() - 1)
          .errorMessage(e.getMessage())
          .send()
          .join();
    }
  }

  private long getDelay(ActivatedJob job) {
    return getHeader(job, "delay", Long::valueOf).orElse(defaultDelay);
  }

  private boolean getReturnPayload(ActivatedJob job) {
    return getHeader(job, "returnPayload", Boolean::valueOf).orElse(defaultReturnPayload);
  }

  private <T> Optional<T> getHeader(
      ActivatedJob job, String headerName, Function<String, T> convert) {
    return Optional.ofNullable(job.getCustomHeaders().get(headerName))
        .map(String::valueOf)
        .map(
            v -> {
              try {
                return convert.apply(v);
              } catch (Exception e) {
                LOG.warn(
                    "Failed to parse custom header '{}' with value '{}', using default value",
                    headerName,
                    v,
                    e);
                return null;
              }
            });
  }
}
