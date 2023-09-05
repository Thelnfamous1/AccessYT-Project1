package me.infamous.accessmod.common.loot;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import me.infamous.accessmod.common.registry.AccessModLootFunctions;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootFunction;
import net.minecraft.loot.LootFunctionType;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.util.JSONUtils;
import ovh.corail.flyingthings.item.ItemAbstractFlyingThing;

public class SetModelType extends LootFunction {
   final int[] options;

   SetModelType(ILootCondition[] conditions, int[] options) {
      super(conditions);
      this.options = options;
   }

   public LootFunctionType getType() {
      return AccessModLootFunctions.SET_MODEL_TYPE;
   }

   public ItemStack run(ItemStack pStack, LootContext pContext) {
      int typeIndex = pContext.getRandom().nextInt(options.length);
      ItemAbstractFlyingThing.setModelType(pStack, options[typeIndex]);
      return pStack;
   }

   public static LootFunction.Builder<?> setModelTypeOptions(int[] options) {
      return simpleBuilder((conditions) -> new SetModelType(conditions, options));
   }

   public static class Serializer extends LootFunction.Serializer<SetModelType> {
      public void serialize(JsonObject pJson, SetModelType pValue, JsonSerializationContext pSerializationContext) {
         super.serialize(pJson, pValue, pSerializationContext);
         JsonArray options = new JsonArray();
         for(int i = 0; i < pValue.options.length; i++){
            options.add(pValue.options[i]);
         }
         pJson.add("options", options);
      }

      public SetModelType deserialize(JsonObject pObject, JsonDeserializationContext pDeserializationContext, ILootCondition[] pConditions) {
         JsonArray s = JSONUtils.getAsJsonArray(pObject, "options");
         int[] options = new int[s.size()];
         for(int i = 0; i < s.size(); i++){
            options[i] = s.get(i).getAsInt();
         }
         return new SetModelType(pConditions, options);
      }
   }
}