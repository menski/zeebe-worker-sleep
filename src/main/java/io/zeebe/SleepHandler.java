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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SleepHandler implements JobHandler {
  private static final Logger LOG = LoggerFactory.getLogger(SleepHandler.class);

  private final long defaultDelay;
  private final boolean returnPayload;

  public SleepHandler(Config config) {
    this.defaultDelay = config.getDuration("handler.delay").toMillis();
    this.returnPayload = config.getBoolean("handler.returnPayload");
  }

  @Override
  public void handle(JobClient jobClient, ActivatedJob job) {
    long jobKey = job.getKey();
    long delay = getDelay(job);

    try {
      Thread.sleep(delay);
      CompleteJobCommandStep1 command = jobClient.newCompleteCommand(jobKey);
      if (returnPayload) {
        command.payload(job.getPayload());
      }

      command.send().join();
      LOG.info("Job with key {} completed with delay of {}ms", jobKey, delay);
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
    String delayHeader = (String) job.getCustomHeaders().get("delay");
    if (delayHeader != null) {
      try {
        return Long.parseLong(delayHeader);
      } catch (Exception e) {
        LOG.warn(
            "Failed to parse delay from custom headers of job {}, using default value",
            job.getKey(),
            e);
      }
    }

    return defaultDelay;
  }
}
