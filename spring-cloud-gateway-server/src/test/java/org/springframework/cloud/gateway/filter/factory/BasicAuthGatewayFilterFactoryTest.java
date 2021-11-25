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

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.test.BaseWebClientTests;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.Base64Utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.cloud.gateway.test.TestUtils.getMap;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@DirtiesContext
class BasicAuthGatewayFilterFactoryTest extends BaseWebClientTests {

	private static final String TEST_USER = "test_user";

	private static final String TEST_PASSWORD = "test_password";

	@Test
	public void basicAuthFilterRelaysHeader() {
		String encodedCredentials = Base64Utils.encodeToString((TEST_USER + ":" + TEST_PASSWORD).getBytes());

		testClient.get().uri("/headers").exchange().expectStatus().isOk().expectBody(Map.class).consumeWith(result -> {
			Map<String, Object> headers = getMap(result.getResponseBody(), "headers");
			assertThat(headers).hasEntrySatisfying("Authorization",
					val -> assertThat(val).isEqualTo("Basic " + encodedCredentials));
		});
	}

	@EnableAutoConfiguration
	@SpringBootConfiguration
	@Import(DefaultTestConfig.class)
	public static class TestConfig {

		@Value("${test.uri}")
		private String uri;

		@Bean
		public RouteLocator testRouteLocator(RouteLocatorBuilder builder) {
			String encodedCredentials = Base64Utils.encodeToString((TEST_USER + ":" + TEST_PASSWORD).getBytes());

			return builder.routes().route(r -> r.predicate(p -> true)
					.filters(f -> f.prefixPath("/httpbin").basicAuth(encodedCredentials)).uri(uri)).build();
		}

	}

}
