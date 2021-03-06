/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.intent.impl.compiler;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MplsLabel;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.Link;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentTestsMocks;
import org.onosproject.net.intent.MockIdGenerator;
import org.onosproject.net.intent.MplsPathIntent;
import org.onosproject.store.trivial.SimpleLinkStore;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.PID;
import static org.onosproject.net.NetTestTools.connectPoint;

public class MplsPathIntentCompilerTest {

    private final ApplicationId appId = new TestApplicationId("test");

    private final ConnectPoint d1p1 = connectPoint("s1", 0);
    private final ConnectPoint d2p0 = connectPoint("s2", 0);
    private final ConnectPoint d2p1 = connectPoint("s2", 1);
    private final ConnectPoint d3p1 = connectPoint("s3", 1);

    private final TrafficSelector selector = DefaultTrafficSelector.builder().build();
    private final TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

    private final Optional<MplsLabel> ingressLabel =
            Optional.of(MplsLabel.mplsLabel(10));
    private final Optional<MplsLabel> egressLabel =
            Optional.of(MplsLabel.mplsLabel(20));

    private final List<Link> links = Arrays.asList(
            new DefaultLink(PID, d1p1, d2p0, DIRECT),
            new DefaultLink(PID, d2p1, d3p1, DIRECT)
    );

    private IdGenerator idGenerator = new MockIdGenerator();

    private final int hops = links.size() - 1;
    private MplsPathIntent intent;
    private MplsPathIntentCompiler sut;

    @Before
    public void setUp() {
        sut = new MplsPathIntentCompiler();
        CoreService coreService = createMock(CoreService.class);
        expect(coreService.registerApplication("org.onosproject.net.intent"))
                .andReturn(appId);
        sut.coreService = coreService;
        sut.linkStore = new SimpleLinkStore();
        sut.resourceService = new IntentTestsMocks.MockResourceService();

        Intent.bindIdGenerator(idGenerator);

        intent = MplsPathIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .path(new DefaultPath(PID, links, hops))
                .ingressLabel(ingressLabel)
                .egressLabel(egressLabel)
                .priority(55)
                .build();

        IntentExtensionService intentExtensionService = createMock(IntentExtensionService.class);
        intentExtensionService.registerCompiler(MplsPathIntent.class, sut);
        intentExtensionService.unregisterCompiler(MplsPathIntent.class);
        sut.intentExtensionService = intentExtensionService;

        replay(coreService, intentExtensionService);
    }

    @After
    public void tearDown() {
        Intent.unbindIdGenerator(idGenerator);
    }

    @Test
    public void testCompile() {
        sut.activate();

        List<Intent> compiled = sut.compile(intent, Collections.emptyList(), Collections.emptySet());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(1));

        FlowRule rule = rules.stream()
                .filter(x -> x.deviceId().equals(d2p0.deviceId()))
                .findFirst()
                .get();
        assertThat(rule.deviceId(), is(d2p0.deviceId()));

        sut.deactivate();

    }
}
