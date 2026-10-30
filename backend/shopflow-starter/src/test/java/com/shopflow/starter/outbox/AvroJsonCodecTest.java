package com.shopflow.starter.outbox;

import com.shopflow.contracts.common.Money;
import com.shopflow.contracts.product.ProductCreated;
import com.shopflow.contracts.product.ProductUnpublished;
import com.shopflow.contracts.product.ProductVariant;
import com.shopflow.contracts.product.UnpublishReason;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvroJsonCodecTest {

    @Test
    void round_trips_nested_records_and_arrays_as_readable_json() {
        ProductCreated event = ProductCreated.newBuilder()
                .setProductId("product-1").setSellerId("seller-1").setName("Air Max 90").setBrand("Nike")
                .setCategory("sneakers")
                .setVariants(List.of(ProductVariant.newBuilder().setSku("AM90-42").setSize("42").setColor("Infrared")
                        .setPrice(Money.newBuilder().setAmount(16_000).setCurrency("EUR").build()).build()))
                .build();

        String json = AvroJsonCodec.encode(event);

        assertThat(json).contains("\"name\":\"Air Max 90\"").contains("\"amount\":16000");
        assertThat(AvroJsonCodec.decode(json, ProductCreated.class.getName())).isEqualTo(event);
    }

    @Test
    void round_trips_enums() {
        ProductUnpublished event = ProductUnpublished.newBuilder()
                .setProductId("product-1").setSellerId("seller-1").setReason(UnpublishReason.MODERATION).build();

        assertThat(AvroJsonCodec.decode(AvroJsonCodec.encode(event), ProductUnpublished.class.getName()))
                .isEqualTo(event);
    }
}
