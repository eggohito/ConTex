package io.github.eggohito.contex.mixin;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.lang.reflect.Type;
import java.util.Optional;

@Mixin(Text.Serializer.class)
public interface TextSerializerAccessor {

    @Invoker
    static Object callOptimizeArgument(Object text) {
        throw new AssertionError();
    }

    @Invoker
    Optional<Text> callGetSeparator(Type type, JsonDeserializationContext jsonDeserializationContext, JsonObject json);

    @Invoker
    void callAddSeparator(JsonSerializationContext jsonSerializationContext, JsonObject json, Optional<Text> optionalSeparator);

    @Invoker
    void callAddStyle(Style style, JsonObject json, JsonSerializationContext jsonSerializationContext);

}
