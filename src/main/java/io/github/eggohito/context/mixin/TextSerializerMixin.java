package io.github.eggohito.context.mixin;

import com.google.gson.*;
import io.github.eggohito.context.api.event.text.TextContentCallback;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.TypedActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.lang.reflect.Type;
import java.util.Optional;

@Mixin(Text.Serializer.class)
public abstract class TextSerializerMixin implements JsonDeserializer<MutableText>, JsonSerializer<Text> {

    /**
     * @author  eggohito
     * @reason  To deserialize custom text contents with an event callback
     */
    @Overwrite
    public MutableText deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

        TypedActionResult<Optional<MutableText>> typedActionResult = TextContentCallback.DESERIALIZE.invoker().deserialize(jsonElement, type, jsonDeserializationContext);
        MutableText mutableText = typedActionResult.getValue().orElse(null);

        if (mutableText == null || typedActionResult.getResult() == ActionResult.FAIL) {
            throw new JsonParseException("Don't know how to turn " + jsonElement + " into a Component");
        }

        //  Deserialize siblings of the mutable text
        if (jsonElement instanceof JsonObject jsonObject && jsonObject.has("extra")) {

            JsonArray siblings = JsonHelper.getArray(jsonObject, "extra");
            if (siblings.isEmpty()) {
                throw new JsonParseException("Unexpected empty array of components");
            }

            for (JsonElement sibling : siblings) {
                mutableText.append(deserialize(sibling, type, jsonDeserializationContext));
            }

        }

        //  Set the style of the mutable text and return it
        mutableText.setStyle(jsonDeserializationContext.deserialize(jsonElement, Style.class));
        return mutableText;

    }

    /**
     * @author  eggohito
     * @reason  To serialize custom text contents with an event callback
     */
    @Overwrite
    public JsonElement serialize(Text text, Type type, JsonSerializationContext jsonSerializationContext) {

        TypedActionResult<Optional<JsonObject>> typedActionResult = TextContentCallback.SERIALIZE.invoker().serialize(text, type, jsonSerializationContext);
        JsonObject jsonObject = typedActionResult.getValue().orElse(null);

        if (jsonObject == null || typedActionResult.getResult() == ActionResult.FAIL) {
            throw new JsonParseException("Don't know how to serialize " + text.getContent() + " as a Component");
        }

        return jsonObject;

    }

}
