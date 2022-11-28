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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Base64;

import io.netty.buffer.PooledByteBufAllocator;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;

/**
 * TODO desc
 *
 * @author Alberto C. Ríos
 */
public class NOTGrpcWebGatewayFilterFactory
		extends AbstractGatewayFilterFactory<Object> {

	@Override
	public GatewayFilter apply(Object config) {
		GatewayFilter filter = new GatewayFilter() {
			@Override
			public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
				GRPCResponseDecorator modifiedResponse = new GRPCResponseDecorator(exchange);

//				ServerWebExchangeUtils.setAlreadyRouted(exchange);
				return modifiedResponse.writeWith(exchange.getRequest().getBody())
						.then(chain.filter(exchange.mutate().response(modifiedResponse).build()));
			}

			@Override
			public String toString() {
				return filterToStringCreator(NOTGrpcWebGatewayFilterFactory.this).toString();
			}
		};

		int order = NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1;
		return new OrderedGatewayFilter(filter, order);
	}

	class GRPCResponseDecorator extends ServerHttpResponseDecorator {

		private final ServerWebExchange exchange;

		GRPCResponseDecorator(ServerWebExchange exchange) {
			super(exchange.getResponse());
			this.exchange = exchange;
		}

		@Override
		public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
			return getDelegate().writeWith(deserializeRequest()
					.cast(DataBuffer.class).last());
		}

		private Flux<DataBuffer> deserializeRequest() {
			return exchange.getRequest().getBody().mapNotNull(dataBufferBody -> {
				if (dataBufferBody.capacity() == 0) {
					return Flux.empty();
				}
				ByteBuffer decodedBody = Base64.getDecoder().decode(dataBufferBody.toByteBuffer());
				CharBuffer decodedCharactersBody = Charset.defaultCharset().decode(decodedBody);
				String[] split = decodedCharactersBody.toString().split("\t\n");
//				String trailers = split[0];
				if(split.length == 2) {
				String payload = split[1];
					exchange.getRequest().mutate()
							.header("grpc-message", payload)
							.header("grpc-status", "0");
					return new NettyDataBufferFactory(new PooledByteBufAllocator()).wrap(payload.getBytes());
				}
				return new NettyDataBufferFactory(new PooledByteBufAllocator()).allocateBuffer();
			}).cast(DataBuffer.class);
		}

	}
}
