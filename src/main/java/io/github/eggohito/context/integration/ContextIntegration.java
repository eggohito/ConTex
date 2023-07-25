package io.github.eggohito.context.integration;

import com.google.gson.*;
import io.github.eggohito.context.api.event.text.TextContentCallback;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.TypedActionResult;

import java.util.Optional;

/**
 *  A class for integrating serializers/deserializers for vanilla text contents.
 */
public class ContextIntegration {

    public static void registerVanilla() {

        TextContentCallback.DESERIALIZE.register(
            TextContentCallback.VANILLA_PHASE,
            (jsonElement, type, jsonDeserializationContext) -> {

                Text.Serializer serializer = new Text.Serializer();
                MutableText mutableText = null;

                if (jsonElement.isJsonPrimitive()) {
                    return TypedActionResult.success(Optional.of(Text.literal(jsonElement.getAsString())));
                }

                if (!jsonElement.isJsonObject()) {

                    if (!jsonElement.isJsonArray()) {
                        throw new JsonParseException("Don't know how to turn " + jsonElement + " into a Component");
                    }

                    for (JsonElement jsonArrayElement : jsonElement.getAsJsonArray()) {
                        MutableText mutableTextElement = serializer.deserialize(jsonArrayElement, jsonArrayElement.getClass(), jsonDeserializationContext);
                        if (mutableText == null) {
                            mutableText = mutableTextElement;
                        } else {
                            mutableText.append(mutableTextElement);
                        }
                    }

                    return TypedActionResult.success(Optional.ofNullable(mutableText));

                }

                JsonObject jsonObject = jsonElement.getAsJsonObject();
                if (jsonObject.has("text")) {
                    String textString = JsonHelper.getString(jsonObject, "text");
                    return TypedActionResult.success(Optional.of(textString.isEmpty() ? Text.empty() : Text.literal(textString)));
                }

                if (jsonObject.has("translate")) {

                    String translationKey = JsonHelper.getString(jsonObject, "translate");
                    String fallbackString = JsonHelper.getString(jsonObject, "fallback", null);

                    if (!jsonObject.has("with")) {
                        return TypedActionResult.success(Optional.of(Text.translatableWithFallback(translationKey, fallbackString)));
                    } else {

                        JsonArray argsJsonArray = JsonHelper.getArray(jsonObject, "with");
                        Object[] args = new Object[argsJsonArray.size()];

                        for (int i = 0; i < args.length; ++i) {
                            args[i] = Text.Serializer.optimizeArgument(serializer.deserialize(argsJsonArray.get(i), type, jsonDeserializationContext));
                        }

                        return TypedActionResult.success(Optional.of(Text.translatableWithFallback(translationKey, fallbackString, args)));

                    }

                }

                if (jsonObject.has("score")) {

                    JsonObject scoreJsonObject = JsonHelper.getObject(jsonObject, "score");
                    if (!(scoreJsonObject.has("name") || scoreJsonObject.has("objective"))) {
                        throw new JsonParseException("A score component needs at least a name and an objective");
                    }

                    String name = JsonHelper.getString(scoreJsonObject, "name");
                    String objective = JsonHelper.getString(scoreJsonObject, "objective");

                    return TypedActionResult.success(Optional.of(Text.score(name, objective)));

                }

                if (jsonObject.has("selector")) {
                    Optional<Text> separator = serializer.getSeparator(type, jsonDeserializationContext, jsonObject);
                    return TypedActionResult.success(Optional.of(Text.selector(JsonHelper.getString(jsonObject, "selector"), separator)));
                }

                if (jsonObject.has("keybind")) {
                    return TypedActionResult.success(Optional.of(Text.keybind(JsonHelper.getString(jsonObject, "keybind"))));
                }

                if (jsonObject.has("nbt")) {

                    String stringifiedNbt = JsonHelper.getString(jsonObject, "nbt");
                    Optional<Text> separator = serializer.getSeparator(type, jsonDeserializationContext, jsonObject);
                    boolean shouldInterpret = JsonHelper.getBoolean(jsonObject, "interpret", false);

                    NbtDataSource nbtDataSource;
                    if (jsonObject.has("block")) {
                        nbtDataSource = new BlockNbtDataSource(JsonHelper.getString(jsonObject, "block"));
                    } else if (jsonObject.has("entity")) {
                        nbtDataSource = new EntityNbtDataSource(JsonHelper.getString(jsonObject, "entity"));
                    } else if (jsonObject.has("storage")) {
                        nbtDataSource = new StorageNbtDataSource(new Identifier(JsonHelper.getString(jsonObject, "storage")));
                    } else {
                        throw new JsonParseException("Don't know how to turn " + jsonElement + " into a Component");
                    }

                    return TypedActionResult.success(Optional.of(Text.nbt(stringifiedNbt, shouldInterpret, separator, nbtDataSource)));

                }

                return TypedActionResult.pass(Optional.empty());

            }
        );

        TextContentCallback.SERIALIZE.register(
            TextContentCallback.VANILLA_PHASE,
            (text, type, jsonSerializationContext) -> {

                Text.Serializer serializer = new Text.Serializer();
                JsonObject jsonObject = new JsonObject();

                if (!text.getStyle().isEmpty()) {
                    serializer.addStyle(text.getStyle(), jsonObject, jsonSerializationContext);
                }

                if (!text.getSiblings().isEmpty()) {

                    JsonArray siblingsJsonArray = new JsonArray();
                    for (Text sibling : text.getSiblings()) {
                        siblingsJsonArray.add(serializer.serialize(sibling, Text.class, jsonSerializationContext));
                    }

                    jsonObject.add("extra", siblingsJsonArray);

                }

                TextContent textContent = text.getContent();
                if (textContent == TextContent.EMPTY) {
                    jsonObject.addProperty("text", "");
                    return TypedActionResult.success(Optional.of(jsonObject));
                }

                if (textContent instanceof LiteralTextContent literalTextContent) {
                    jsonObject.addProperty("text", literalTextContent.string());
                    return TypedActionResult.success(Optional.of(jsonObject));
                }

                if (textContent instanceof TranslatableTextContent translatableTextContent) {

                    jsonObject.addProperty("translate", translatableTextContent.getKey());
                    String fallbackString = translatableTextContent.getFallback();

                    if (fallbackString != null) {
                        jsonObject.addProperty("fallback", fallbackString);
                    }

                    if (translatableTextContent.getArgs().length > 0) {

                        JsonArray argsJsonArray = new JsonArray();
                        Object[] argsArray = translatableTextContent.getArgs();

                        for (Object arg : argsArray) {
                            if (arg instanceof Text argText) {
                                argsJsonArray.add(serializer.serialize(argText, argText.getClass(), jsonSerializationContext));
                            } else {
                                argsJsonArray.add(new JsonPrimitive(String.valueOf(arg)));
                            }
                        }

                        jsonObject.add("with", argsJsonArray);

                    }

                    return TypedActionResult.success(Optional.of(jsonObject));

                }

                if (textContent instanceof ScoreTextContent scoreTextContent) {

                    JsonObject scoreJsonObject = new JsonObject();
                    scoreJsonObject.addProperty("name", scoreTextContent.getName());
                    scoreJsonObject.addProperty("objective", scoreTextContent.getObjective());

                    jsonObject.add("score", scoreJsonObject);
                    return TypedActionResult.success(Optional.of(jsonObject));

                }

                if (textContent instanceof SelectorTextContent selectorTextContent) {

                    jsonObject.addProperty("selector", selectorTextContent.getPattern());
                    serializer.addSeparator(jsonSerializationContext, jsonObject, selectorTextContent.getSeparator());

                    return TypedActionResult.success(Optional.of(jsonObject));

                }

                if (textContent instanceof KeybindTextContent keybindTextContent) {
                    jsonObject.addProperty("keybind", keybindTextContent.getKey());
                    return TypedActionResult.success(Optional.of(jsonObject));
                }

                if (textContent instanceof NbtTextContent nbtTextContent) {

                    jsonObject.addProperty("nbt", nbtTextContent.getPath());
                    jsonObject.addProperty("interpret", nbtTextContent.shouldInterpret());
                    serializer.addSeparator(jsonSerializationContext, jsonObject, nbtTextContent.getSeparator());

                    NbtDataSource nbtDataSource = nbtTextContent.getDataSource();
                    if (nbtDataSource instanceof BlockNbtDataSource blockNbtDataSource) {
                        jsonObject.addProperty("block", blockNbtDataSource.rawPos());
                    } else if (nbtDataSource instanceof EntityNbtDataSource entityNbtDataSource) {
                        jsonObject.addProperty("entity", entityNbtDataSource.rawSelector());
                    } else if (nbtDataSource instanceof StorageNbtDataSource storageNbtDataSource) {
                        jsonObject.addProperty("storage", storageNbtDataSource.id().toString());
                    } else {
                        throw new JsonParseException("Don't know how to serialize " + textContent + " as a Component");
                    }

                    return TypedActionResult.success(Optional.of(jsonObject));

                }

                return TypedActionResult.pass(Optional.empty());

            }
        );

    }

}
