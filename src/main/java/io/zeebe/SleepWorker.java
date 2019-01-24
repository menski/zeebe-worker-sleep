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
import com.typesafe.config.ConfigFactory;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.subscription.JobWorkerBuilderStep1.JobWorkerBuilderStep3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SleepWorker {

  private static final Logger LOG = LoggerFactory.getLogger(SleepWorker.class);
  private final Config config;

  private SleepWorker(Config config) {
    this.config = config;
  }

  private void run() {
    LOG.info("Starting job worker with config: {}", config.root().render());
    ZeebeClient client = createZeebeClient();
    createWorker(client).open();
  }

  private ZeebeClient createZeebeClient() {
    return ZeebeClient.newClientBuilder()
        .brokerContactPoint(config.getString("client.gateway"))
        .numJobWorkerExecutionThreads(config.getInt("client.threads"))
        .build();
  }

  private JobWorkerBuilderStep3 createWorker(ZeebeClient client) {
    SleepHandler handler = new SleepHandler(config);

    return client
        .jobClient()
        .newWorker()
        .jobType(config.getString("worker.jobType"))
        .handler(handler)
        .name(config.getString("worker.name"))
        .timeout(config.getDuration("worker.timeout"))
        .bufferSize(config.getInt("worker.bufferSize"))
        .pollInterval(config.getDuration("worker.pollInterval"));
  }

  public static void main(String[] args) {
    Config config = ConfigFactory.load();
    new SleepWorker(config).run();
  }
}
