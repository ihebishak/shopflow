package com.shopflow.starter.cloudevents;

import com.shopflow.contracts.product.ProductCreated;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CloudEventTypesTest {

    @Test
    void derives_the_type_from_the_avro_contract_with_version_1_by_default() {
        assertThat(CloudEventTypes.typeOf(ProductCreated.getClassSchema()))
                .isEqualTo("shopflow.product.ProductCreated.v1");
    }

    @Test
    void uses_the_version_property_of_the_schema() {
        Schema v2 = SchemaBuilder.record("OrderPlaced").namespace("com.shopflow.contracts.order")
                .prop("version", "2")
                .fields().requiredString("orderId").endRecord();

        assertThat(CloudEventTypes.typeOf(v2)).isEqualTo("shopflow.order.OrderPlaced.v2");
    }

    @Test
    void refuses_a_schema_outside_the_contracts() {
        Schema foreign = SchemaBuilder.record("Foo").namespace("org.example")
                .fields().requiredString("id").endRecord();

        assertThatThrownBy(() -> CloudEventTypes.typeOf(foreign))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("org.example.Foo");
    }
}
