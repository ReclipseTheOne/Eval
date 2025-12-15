package com.reclipse.eval;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@Mod(Constants.MOD_ID)
public class Eval {
    public Eval(IEventBus eventBus, ModContainer container) {
        CommonClass.init();
    }

    @EventBusSubscriber(modid = Constants.MOD_ID)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            EvalCommand.register(event.getDispatcher());
        }
    }
}
