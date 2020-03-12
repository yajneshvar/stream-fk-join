package com.example.demo;

import com.example.demo.avro.Vehicle;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import com.example.demo.avro.Trade;
import com.example.demo.avro.TradeVehicle;

import java.util.HashMap;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
class DemoApplicationTests {

	class Processor {


		public KStream<Long, TradeVehicle> process(KTable<Long, Trade> tradeKTable, KTable<Long, Vehicle> vehicleKTable) {

			return tradeKTable.leftJoin(vehicleKTable, (trade, vehicle) -> {
				var result = TradeVehicle.newBuilder();
				var tradeVehicle = result.setTradePayload(trade.getTradePayload())
						.setTradeId(trade.getId())
//						.setVehicleId(vehicle.getId())
						.setVehiclePayload("")
						.build();
				return tradeVehicle;
			}).toStream();

		}

	}


	private TopologyTestDriver testDriver;


	private TestInputTopic<Long, Trade> inputTopic;
	private TestInputTopic<Long, Vehicle> inputTopic2;
	private KTable<Long, Trade> inputTopicKT;
	private KTable<Long, Vehicle> inputTopicKT2;
	private TestOutputTopic<Long, TradeVehicle> outputTopic;
	private KeyValueStore<String, Long> store;

	private Serde<String> stringSerde = new Serdes.StringSerde();
	private Serde<Long> longSerde = new Serdes.LongSerde();


	@BeforeEach
	void setup() {
		var serdeConfig = new HashMap<String, Object>();
		serdeConfig.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "mock://test");
		serdeConfig.put(AbstractKafkaAvroSerDeConfig.AUTO_REGISTER_SCHEMAS, "true");

		var config = new Properties();
		config.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
		config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
		config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Long().getClass().getName());
		config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class.getName());
		config.putAll(serdeConfig);

		var topology = new StreamsBuilder();

		var tradeSerde = new SpecificAvroSerde<Trade>();
		tradeSerde.configure(serdeConfig, false);

		var vehicleSerde = new SpecificAvroSerde<Vehicle>();
		vehicleSerde.configure(serdeConfig, false);

		var tradeVehicleSerde = new SpecificAvroSerde<TradeVehicle>();
		tradeVehicleSerde.configure(serdeConfig, false);


		inputTopicKT = topology.table("trade", Consumed.with(longSerde, tradeSerde));
		inputTopicKT2 = topology.table("vehicle", Consumed.with(longSerde, vehicleSerde));

		var processor = new Processor();
		processor.process(inputTopicKT, inputTopicKT2).to("tradeVehicle");

		testDriver = new TopologyTestDriver(topology.build(),config);

		inputTopic =  testDriver.createInputTopic("trade", longSerde.serializer(), tradeSerde.serializer());
		inputTopic2 = testDriver.createInputTopic("vehicle", longSerde.serializer(), vehicleSerde.serializer());
		outputTopic = testDriver.createOutputTopic("tradeVehicle", longSerde.deserializer(), tradeVehicleSerde.deserializer());

	}


	@AfterEach
	void tearDown() {
		testDriver.close();
	}


	@Test
	void test() {

		var trade = Trade.newBuilder()
					.setTradePayload("trade1")
					.setId(1l).build();

		inputTopic.pipeInput(1l, trade);

		var record = outputTopic.readRecord();

		assertThat(record.getValue().getTradeId() == 1l);


	}

}
