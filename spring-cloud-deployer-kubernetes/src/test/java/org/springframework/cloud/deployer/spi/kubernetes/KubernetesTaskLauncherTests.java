/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.deployer.spi.kubernetes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.batch.v1.JobList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.BatchAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.V1BatchAPIGroupDSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.core.io.Resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Base Tests {@link KubernetesTaskLauncher}
 *
 * @author Glenn Renfro
*/
public class KubernetesTaskLauncherTests {

    private KubernetesTaskLauncher kubernetesTaskLauncher;
    private KubernetesDeployerProperties properties;
    KubernetesClient kubernetesClient;
    private V1BatchAPIGroupDSL v1BatchAPIGroupDSL;


    @BeforeEach
    public void setup() {
        properties = new KubernetesDeployerProperties();
        kubernetesClient = mock(KubernetesClient.class);
        given(kubernetesClient.pods()).willReturn(mock(MixedOperation.class));
        BatchAPIGroupDSL batchAPIGroupDSL = mock(BatchAPIGroupDSL.class);
        given(kubernetesClient.batch()).willReturn(batchAPIGroupDSL);
        v1BatchAPIGroupDSL = mock(V1BatchAPIGroupDSL.class);
        MixedOperation mixedOperation = mock(MixedOperation.class);
        MixedOperation jobList = mock(MixedOperation.class);
        given(jobList.list()).willReturn(mock(JobList.class));
        given(mixedOperation.withLabel(any(), any())).willReturn(jobList);
        FilterWatchListDeletable filterWatchListDeletable = mock(FilterWatchListDeletable.class);
        given(filterWatchListDeletable.list()).willReturn(mock(KubernetesResourceList.class));
        given(mixedOperation.list()).willReturn(mock(KubernetesResourceList.class));
        given(v1BatchAPIGroupDSL.jobs()).willReturn(mixedOperation);
        given(batchAPIGroupDSL.v1()).willReturn(v1BatchAPIGroupDSL);
        given(batchAPIGroupDSL.jobs()).willReturn(mixedOperation);

        given(mixedOperation.withLabel(any())).willReturn(filterWatchListDeletable);
        given(kubernetesClient.pods()).willReturn(mixedOperation);
    }

    @Test
    public void testDefaults() {
        kubernetesTaskLauncher = new KubernetesTaskLauncher(properties, kubernetesClient);
        AppDefinition appDefinition = new AppDefinition("foo", Collections.emptyMap());
        Map<String, String> deploymentProperties = new HashMap<>();
        Resource resource = new DockerResource("docker://foo");
        AppDeploymentRequest appDeploymentRequest = new AppDeploymentRequest(appDefinition, resource, deploymentProperties);
        kubernetesTaskLauncher.launch(appDeploymentRequest);
        // Pod is called when getting name, labels and finally on the create
        verify(kubernetesClient, times(3)).pods();
        verify(v1BatchAPIGroupDSL, times(0)).jobs();
    }

    @Test
    public void testTaskLauncherJobEnabled() {
        properties.setCreateJob(true);
        kubernetesTaskLauncher = new KubernetesTaskLauncher(properties, kubernetesClient);
        AppDefinition appDefinition = new AppDefinition("timestamp", Collections.emptyMap());

        Resource resource = new DockerResource("docker://foo");
        AppDeploymentRequest appDeploymentRequest = new AppDeploymentRequest(appDefinition, resource, Collections.emptyMap());
        kubernetesTaskLauncher.launch(appDeploymentRequest);
        // Pod is called when getting name, labels and finally on the create
        verify(v1BatchAPIGroupDSL, times(1)).jobs();
        // Pod is called when getting name, labels , but no create
        verify(kubernetesClient, times(1)).pods();
    }

    @Test
    public void testJob() {
        kubernetesTaskLauncher = new KubernetesTaskLauncher(properties, kubernetesClient);
        Map<String, String> deploymentProperties = new HashMap<>();
        deploymentProperties.put("spring.cloud.deployer.kubernetes.deploymentLabels", "spring.cloud.deployer.kubernetes.create-job:true");
        deploymentProperties.put("deployer.kubernetes.create-job", "true");
        AppDefinition appDefinition = new AppDefinition("timestamp", deploymentProperties);

        Resource resource = new DockerResource("docker://foo");
        AppDeploymentRequest appDeploymentRequest = new AppDeploymentRequest(appDefinition, resource, deploymentProperties);
        kubernetesTaskLauncher.launch(appDeploymentRequest);
        // Pod is called when getting name, labels and finally on the create
        verify(v1BatchAPIGroupDSL, times(1)).jobs();
        // Pod is called when getting name, labels , but no create
        verify(kubernetesClient, times(2)).pods();
    }
}
