package com.shopflow.starter.outbox;

import org.apache.avro.Schema;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Encode un message Avro en JSON (encodage JSON standard d'Avro) pour le stocker lisiblement dans l'outbox,
 * et le reconstruit à partir de ce JSON et du nom de sa classe.
 */
final class AvroJsonCodec {

    private AvroJsonCodec() {
    }

    static String encode(SpecificRecord message) {
        Schema schema = message.getSchema();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Encoder encoder = EncoderFactory.get().jsonEncoder(schema, out);
            new SpecificDatumWriter<SpecificRecord>(schema).write(message, encoder);
            encoder.flush();
            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot encode " + schema.getFullName(), e);
        }
    }

    static SpecificRecord decode(String json, String className) {
        try {
            Class<?> type = Class.forName(className);
            Schema schema = SpecificData.get().getSchema(type);
            Decoder decoder = DecoderFactory.get().jsonDecoder(schema, json);
            return new SpecificDatumReader<SpecificRecord>(schema).read(null, decoder);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unknown contract class " + className, e);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot decode " + className, e);
        }
    }
}
