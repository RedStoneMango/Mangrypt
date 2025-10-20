package io.github.redstonemango.mangrypt.back;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * A simplified extension of Kryo for easy serialization and deserialization
 * of pre-registered classes. Also includes a warm-up mechanism to avoid
 * first-use latency.
 * @author RedStoneMango
 */
public class SimplifiedKryo extends Kryo {

    /**
     * Constructs a SimplifiedKryo instance and registers the provided classes.
     *
     * @param classes The classes to register for Kryo serialization.
     */
    public SimplifiedKryo(Class<?>... classes) {
        super();
        setDefaultSerializer(TaggedFieldSerializer.class);
        for (Class<?> aClass : classes) {
            register(aClass);
        }
    }

    /**
     * Serializes an object to a byte array using Kryo.
     *
     * @param obj The object to serialize.
     * @return A byte array representing the serialized object.
     */
    public byte[] serialize(Object obj) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             Output output = new Output(baos)) {
            writeObject(output, obj);
            output.flush(); // Ensure all data is written
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    /**
     * Serializes an object to a Base64 string using Kryo.
     *
     * @param obj The object to serialize.
     * @return A Base64 string representing the serialized object.
     */
    public String serializeToString(Object obj) {
        byte[] bytes = serialize(obj);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Deserializes an object from a byte array.
     *
     * @param bytes The byte array to deserialize.
     * @param clazz The expected class type of the object.
     * @param <T>   The type of the returned object.
     * @return The deserialized object.
     */
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             Input input = new Input(bais)) {
            return readObject(input, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Deserialization failed", e);
        }
    }

    /**
     * Deserializes an object from a Base64 string.
     *
     * @param base64String The Base64 string to deserialize.
     * @param clazz        The expected class type of the object.
     * @param <T>          The type of the returned object.
     * @return The deserialized object.
     */
    public <T> T deserializeFromString(String base64String, Class<T> clazz) {
        byte[] bytes = Base64.getDecoder().decode(base64String);
        return deserialize(bytes, clazz);
    }

    /**
     * Warms up Kryo by serializing and deserializing the provided sample objects.
     * <p>
     * This method helps avoid performance penalties associated with Kryo's first-time
     * serialization and deserialization of a class, which includes reflective analysis
     * and serializer generation.
     * <p>
     * By passing representative instances of the classes you intend to use, you ensure
     * Kryo initializes its internal serializers ahead of time, reducing latency during
     * actual runtime operations.
     * <p>
     * Note that this operation might take some time, depending on the object's complexity.
     * It is recommended to use minimalistic class instances for this method only.
     *
     * @param objects Sample objects whose classes should be pre-initialized by Kryo.
     *                Each object's runtime class will be used to perform a serialize-deserialize cycle.
     *                Must not be {@code null}, and should match the registered classes.
     *
     * @throws RuntimeException if serialization or deserialization of any object fails.
     */
    public void warmupObjectClasses(Object... objects) {
        for (Object object : objects) {
            deserialize(serialize(object), object.getClass());
        }
    }
}
