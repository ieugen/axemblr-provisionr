/*
 * Copyright (c) 2013 S.C. Axemblr Software Solutions S.R.L
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.axemblr.provisionr.cloudstack.activities;

import com.axemblr.provisionr.api.pool.Pool;
import com.axemblr.provisionr.cloudstack.NetworkOptions;
import com.axemblr.provisionr.cloudstack.ProcessVariables;
import com.google.common.base.Throwables;
import java.util.concurrent.TimeUnit;
import org.activiti.engine.delegate.DelegateExecution;
import org.jclouds.cloudstack.CloudStackClient;
import org.jclouds.cloudstack.domain.AsyncJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteNetwork extends CloudStackActivity {

    private static final Logger LOG = LoggerFactory.getLogger(DeleteNetwork.class);

    @Override
    public void execute(CloudStackClient cloudStackClient, Pool pool, DelegateExecution execution) {
        final String existingNetworkId = pool.getNetwork().getOption(NetworkOptions.EXISTING_NETWORK_ID);
        if (existingNetworkId == null) {
            final String networkId = (String) execution.getVariable(ProcessVariables.NETWORK_ID);
            LOG.info("Network with id {} was created, deleting", networkId);
            if (networkId != null) {
                LOG.info("Deleting network with id {}", networkId);
                final String deleteJobId = cloudStackClient.getNetworkClient().deleteNetwork(networkId);
                LOG.info("Wait for network {} to be deleted", networkId);
                waitForJobToFinish(cloudStackClient, deleteJobId);
            } else {
                LOG.info("No network defined in process variable {}", ProcessVariables.NETWORK_ID);
            }
        } else {
            LOG.info("Network with id {} was provided, not deleting", existingNetworkId);
        }
    }

    public void waitForJobToFinish(CloudStackClient client, String jobId) {
        AsyncJob job = client.getAsyncJobClient().getAsyncJob(jobId);
        while (job != null) {
            try {
                if (job.getStatus() != AsyncJob.Status.IN_PROGRESS) break;
                TimeUnit.SECONDS.sleep(5);
                job = client.getAsyncJobClient().getAsyncJob(jobId);
            } catch (InterruptedException e) {
                Throwables.propagateIfPossible(e);
            }
        }
    }
}
