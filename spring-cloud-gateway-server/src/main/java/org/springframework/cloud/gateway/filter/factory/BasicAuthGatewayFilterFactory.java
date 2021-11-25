/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.gateway.filter.factory;

import java.util.Collections;
import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractNameValueGatewayFilterFactory.NameValueConfig;

/**
 * @author Alberto C. Ríos
 */
public class BasicAuthGatewayFilterFactory implements GatewayFilterFactory<BasicAuthenticationProperties> {

	private final AddRequestHeaderGatewayFilterFactory addRequestHeaderGatewayFilterFactory = new AddRequestHeaderGatewayFilterFactory();

	private NameValueConfig getAuthHeaderConfig(String encodedCredentials) {
		return new NameValueConfig().setName("Authorization").setValue("Basic " + encodedCredentials);
	}

	@Override
	public GatewayFilter apply(BasicAuthenticationProperties config) {
		return addRequestHeaderGatewayFilterFactory.apply(getAuthHeaderConfig(config.getEncodedCredentials()));
	}

	@Override
	public BasicAuthenticationProperties newConfig() {
		return new BasicAuthenticationProperties();
	}

	@Override
	public Class<BasicAuthenticationProperties> getConfigClass() {
		return BasicAuthenticationProperties.class;
	}

	@Override
	public List<String> shortcutFieldOrder() {
		return Collections.singletonList("encodedCredentials");
	}

}
