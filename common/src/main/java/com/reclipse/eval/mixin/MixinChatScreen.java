package com.reclipse.eval.mixin;

import com.reclipse.eval.EquationParser;
import com.reclipse.eval.EvalConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(ChatScreen.class)
public class MixinChatScreen {
    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void onChatInput(String message, boolean addToHistory, CallbackInfo ci) {
        boolean global = message.startsWith("*=") && message.length() > 2;
        boolean local = message.startsWith("=") && !message.startsWith("*=") && message.length() > 1;

        if (global || local) {
            String expression = global ? message.substring(2) : message.substring(1);
            String result = EquationParser.evaluate(expression.toLowerCase(Locale.ROOT));

            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                if (global) {
                    client.player.connection.sendChat(expression + " = " + result);
                } else {
                    client.player.displayClientMessage(
                            Component.literal(expression).withStyle(ChatFormatting.GREEN)
                                    .append(Component.literal(" = ").withStyle(ChatFormatting.GRAY))
                                    .append(Component.literal(result).withStyle(ChatFormatting.YELLOW)),
                            false);
                }
            }

            if (addToHistory) {
                Minecraft.getInstance().gui.getChat().addRecentChat(message);
            }

            ci.cancel();
            return;
        }

        // Handle inline expressions like {{2+2}}
        String processed = processInlineExpressions(message);
        if (!processed.equals(message)) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.connection.sendChat(processed);
            }

            if (addToHistory) {
                Minecraft.getInstance().gui.getChat().addRecentChat(message);
            }

            ci.cancel();
        }
    }

    private String processInlineExpressions(String message) {
        EvalConfig config = EvalConfig.get();
        if (!config.inlineEnabled) {
            return message;
        }

        String start = config.inlinePatternStart;
        String end = config.inlinePatternEnd;
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (true) {
            int startIdx = message.indexOf(start, lastEnd);
            if (startIdx == -1) break;

            int endIdx = message.indexOf(end, startIdx + start.length());
            if (endIdx == -1) break;

            result.append(message, lastEnd, startIdx);
            String expression = message.substring(startIdx + start.length(), endIdx);
            String evaluated = EquationParser.evaluate(expression.toLowerCase(Locale.ROOT));
            result.append(evaluated);
            lastEnd = endIdx + end.length();
        }

        result.append(message.substring(lastEnd));
        return result.toString();
    }
}
