package io.github.eggohito.context.api.event.text;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import io.github.eggohito.context.Context;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;

import java.lang.reflect.Type;
import java.util.Optional;

/**
 *  A callback for serializing/deserializing text contents. This callback
 *  completely overwrites the {@link Text.Serializer#serialize} and {@link Text.Serializer#deserialize}
 *  methods to be able to serialize/deserialize vanilla and custom text contents.
 */
public final class TextContentCallback {

    public static final Identifier VANILLA_PHASE = Context.identifier("phase/vanilla");

    /**
     *  <p>An event for deserializing a {@link JsonElement} into a {@link MutableText}.
     *  Use {@link TypedActionResult} to return the {@link JsonElement} and its result.</p>
     *  <ul>
     *      <li>{@link ActionResult#SUCCESS}, {@link ActionResult#CONSUME} and {@link ActionResult#CONSUME_PARTIAL} - The text is deserialized and returned</li>
     *      <li>{@link ActionResult#PASS} - Pass control to the next handler (starting from the vanilla handlers)</li>
     *      <li>{@link ActionResult#FAIL} - The text could not be deserialized</li>
     *  </ul>
     *
     */
    public static final Event<DeserializeTextContent> DESERIALIZE = EventFactory.createWithPhases(
        DeserializeTextContent.class,
        handlers -> (jsonElement, type, jsonDeserializationContext) -> {

            TypedActionResult<Optional<MutableText>> typedActionResult;
            for (DeserializeTextContent handler : handlers) {
                typedActionResult = handler.deserialize(jsonElement, type, jsonDeserializationContext);
                if (typedActionResult.getResult() != ActionResult.PASS) {
                    return typedActionResult;
                }
            }

            return TypedActionResult.fail(Optional.empty());

        },
        VANILLA_PHASE,
        Event.DEFAULT_PHASE
    );

    /**
     *  <p>An event for serializing {@link Text} into a {@link JsonElement} (usually a {@link JsonObject}).
     *  Use {@link TypedActionResult} to return the serialized text and its result.</p>
     *  <ul>
     *      <li>{@link ActionResult#SUCCESS}, {@link ActionResult#CONSUME} and {@link ActionResult#CONSUME_PARTIAL} - The text is serialized and returned</li>
     *      <li>{@link ActionResult#PASS} - Pass control to the next handler (starting from the vanilla handlers)</li>
     *      <li>{@link ActionResult#FAIL} - The text could not be serialized</li>
     *  </ul>
     *
     */
    public static final Event<SerializeTextContent> SERIALIZE = EventFactory.createWithPhases(
        SerializeTextContent.class,
        handlers -> (text, type, jsonSerializationContext) -> {

            TypedActionResult<Optional<JsonObject>> typedActionResult;
            for (SerializeTextContent handler : handlers) {
                typedActionResult = handler.serialize(text, type, jsonSerializationContext);
                if (typedActionResult.getResult() != ActionResult.PASS) {
                    return typedActionResult;
                }
            }

            return TypedActionResult.fail(Optional.empty());

        },
        VANILLA_PHASE,
        Event.DEFAULT_PHASE
    );

    @FunctionalInterface
    public interface DeserializeTextContent {
        TypedActionResult<Optional<MutableText>> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext);
    }

    @FunctionalInterface
    public interface SerializeTextContent {
        TypedActionResult<Optional<JsonObject>> serialize(Text text, Type type, JsonSerializationContext jsonSerializationContext);
    }

}
