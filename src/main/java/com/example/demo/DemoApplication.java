package com.example.demo;

import com.example.demo.avro.Trade;
import com.example.demo.avro.TradeVehicle;
import com.example.demo.avro.Vehicle;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.messaging.handler.annotation.SendTo;

@SpringBootApplication
@EnableKafkaStreams
@EnableBinding(DemoApplication.KafkaBindings.class)
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}


	@StreamListener
	@SendTo("yajTradeVehicle")
	public KStream<Long, TradeVehicle> process(@Input("yajTrade") KTable<Long, Trade> tradeKTable, @Input("yajVehicle") KTable<Long, Vehicle> vehicleKTable ) {


		return tradeKTable.leftJoin(vehicleKTable, trade -> trade.getVehicleId(), (trade, vehicle) -> {
			var builder = TradeVehicle.newBuilder();
			if (trade != null) {
				builder.setTradeId(trade.getId())
						.setTradePayload(trade.getTradePayload());
			}
			if (vehicle != null) {
				builder.setVehicleId(vehicle.getId())
						.setVehiclePayload(vehicle.getVehiclePayload());
			}

			return builder.build();
		}).toStream();


	}


	interface KafkaBindings {

		@Input("yajTrade")
		KTable<Long, Trade> tradeKtable();

		@Input("yajVehicle")
		KTable<Long, Vehicle> vehicleKtable();

		@Output("yajTradeVehicle")
		KStream<Long, TradeVehicle> tradeVehicle();
	}



}
