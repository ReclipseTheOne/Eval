package com.reclipse.eval;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class Eval implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Constants.LOG.info("Hello Fabric world!");
        CommonClass.init();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("eval")
                    .then(ClientCommandManager.literal("start")
                            .then(ClientCommandManager.argument("pattern", StringArgumentType.string())
                                    .executes(ctx -> {
                                        String pattern = StringArgumentType.getString(ctx, "pattern");
                                        EvalConfig.get().inlinePatternStart = pattern;
                                        EvalConfig.save();
                                        ctx.getSource().sendFeedback(Component.literal("Inline start pattern set to: ").withStyle(ChatFormatting.GREEN)
                                                .append(Component.literal(pattern).withStyle(ChatFormatting.YELLOW)));
                                        return 1;
                                    })))
                    .then(ClientCommandManager.literal("end")
                            .then(ClientCommandManager.argument("pattern", StringArgumentType.string())
                                    .executes(ctx -> {
                                        String pattern = StringArgumentType.getString(ctx, "pattern");
                                        EvalConfig.get().inlinePatternEnd = pattern;
                                        EvalConfig.save();
                                        ctx.getSource().sendFeedback(Component.literal("Inline end pattern set to: ").withStyle(ChatFormatting.GREEN)
                                                .append(Component.literal(pattern).withStyle(ChatFormatting.YELLOW)));
                                        return 1;
                                    })))
                    .then(ClientCommandManager.literal("inline")
                            .then(ClientCommandManager.literal("on")
                                    .executes(ctx -> {
                                        EvalConfig.get().inlineEnabled = true;
                                        EvalConfig.save();
                                        ctx.getSource().sendFeedback(Component.literal("Inline expressions enabled").withStyle(ChatFormatting.GREEN));
                                        return 1;
                                    }))
                            .then(ClientCommandManager.literal("off")
                                    .executes(ctx -> {
                                        EvalConfig.get().inlineEnabled = false;
                                        EvalConfig.save();
                                        ctx.getSource().sendFeedback(Component.literal("Inline expressions disabled").withStyle(ChatFormatting.RED));
                                        return 1;
                                    })))
                    .then(ClientCommandManager.literal("var")
                            .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                    .then(ClientCommandManager.argument("expression", StringArgumentType.greedyString())
                                            .executes(ctx -> {
                                                String name = StringArgumentType.getString(ctx, "name");
                                                String expression = StringArgumentType.getString(ctx, "expression");
                                                String result = EquationParser.evaluate(expression.toLowerCase(java.util.Locale.ROOT));
                                                try {
                                                    double value = Double.parseDouble(result);
                                                    EvalConfig.get().variables.put(name, value);
                                                    EvalConfig.save();
                                                    ctx.getSource().sendFeedback(Component.literal("Variable ").withStyle(ChatFormatting.GREEN)
                                                            .append(Component.literal(name).withStyle(ChatFormatting.YELLOW))
                                                            .append(Component.literal(" set to ").withStyle(ChatFormatting.GREEN))
                                                            .append(Component.literal(expression).withStyle(ChatFormatting.GRAY))
                                                            .append(Component.literal(" = ").withStyle(ChatFormatting.GREEN))
                                                            .append(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.YELLOW)));
                                                } catch (NumberFormatException e) {
                                                    ctx.getSource().sendFeedback(Component.literal("Error: ").withStyle(ChatFormatting.RED)
                                                            .append(Component.literal(result).withStyle(ChatFormatting.YELLOW)));
                                                }
                                                return 1;
                                            }))))
                    .then(ClientCommandManager.literal("delvar")
                            .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                    .executes(ctx -> {
                                        String name = StringArgumentType.getString(ctx, "name");
                                        if (EvalConfig.get().variables.remove(name) != null) {
                                            EvalConfig.save();
                                            ctx.getSource().sendFeedback(Component.literal("Variable ").withStyle(ChatFormatting.GREEN)
                                                    .append(Component.literal(name).withStyle(ChatFormatting.YELLOW))
                                                    .append(Component.literal(" deleted").withStyle(ChatFormatting.GREEN)));
                                        } else {
                                            ctx.getSource().sendFeedback(Component.literal("Variable ").withStyle(ChatFormatting.RED)
                                                    .append(Component.literal(name).withStyle(ChatFormatting.YELLOW))
                                                    .append(Component.literal(" not found").withStyle(ChatFormatting.RED)));
                                        }
                                        return 1;
                                    })))
                    .then(ClientCommandManager.literal("vars")
                            .executes(ctx -> {
                                var vars = EvalConfig.get().variables;
                                if (vars.isEmpty()) {
                                    ctx.getSource().sendFeedback(Component.literal("No variables defined").withStyle(ChatFormatting.GRAY));
                                } else {
                                    MutableComponent msg = Component.literal("Variables:").withStyle(ChatFormatting.GOLD);
                                    vars.forEach((k, v) -> msg.append(Component.literal("\n"))
                                            .append(Component.literal(k).withStyle(ChatFormatting.GRAY))
                                            .append(Component.literal(" = ").withStyle(ChatFormatting.WHITE))
                                            .append(Component.literal(String.valueOf(v)).withStyle(ChatFormatting.YELLOW)));
                                    ctx.getSource().sendFeedback(msg);
                                }
                                return 1;
                            }))
                    .then(ClientCommandManager.literal("reload")
                            .executes(ctx -> {
                                EvalConfig.load();
                                ctx.getSource().sendFeedback(Component.literal("Config reloaded from file").withStyle(ChatFormatting.GREEN));
                                return 1;
                            }))
                    .then(ClientCommandManager.literal("status")
                            .executes(ctx -> {
                                EvalConfig config = EvalConfig.get();
                                MutableComponent msg = Component.literal("Eval Config:").withStyle(ChatFormatting.GOLD)
                                        .append(Component.literal("\nInline: ").withStyle(ChatFormatting.GRAY))
                                        .append(config.inlineEnabled
                                                ? Component.literal("Enabled").withStyle(ChatFormatting.GREEN)
                                                : Component.literal("Disabled").withStyle(ChatFormatting.RED))
                                        .append(Component.literal("\nStart pattern: ").withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal(config.inlinePatternStart).withStyle(ChatFormatting.YELLOW))
                                        .append(Component.literal("\nEnd pattern: ").withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal(config.inlinePatternEnd).withStyle(ChatFormatting.YELLOW))
                                        .append(Component.literal("\nVariables: ").withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal(String.valueOf(config.variables.size())).withStyle(ChatFormatting.YELLOW));
                                ctx.getSource().sendFeedback(msg);
                                return 1;
                            }))
            );
        });
    }
}
