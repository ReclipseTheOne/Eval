package com.reclipse.eval;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class EvalCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("eval")
                .then(Commands.literal("start")
                        .then(Commands.argument("pattern", StringArgumentType.string())
                                .executes(ctx -> {
                                    String pattern = StringArgumentType.getString(ctx, "pattern");
                                    EvalConfig.get().inlinePatternStart = pattern;
                                    EvalConfig.save();
                                    ctx.getSource().sendSuccess(() -> Component.literal("Inline start pattern set to: ").withStyle(ChatFormatting.GREEN)
                                            .append(Component.literal(pattern).withStyle(ChatFormatting.YELLOW)), false);
                                    return 1;
                                })))
                .then(Commands.literal("end")
                        .then(Commands.argument("pattern", StringArgumentType.string())
                                .executes(ctx -> {
                                    String pattern = StringArgumentType.getString(ctx, "pattern");
                                    EvalConfig.get().inlinePatternEnd = pattern;
                                    EvalConfig.save();
                                    ctx.getSource().sendSuccess(() -> Component.literal("Inline end pattern set to: ").withStyle(ChatFormatting.GREEN)
                                            .append(Component.literal(pattern).withStyle(ChatFormatting.YELLOW)), false);
                                    return 1;
                                })))
                .then(Commands.literal("inline")
                        .then(Commands.literal("on")
                                .executes(ctx -> {
                                    EvalConfig.get().inlineEnabled = true;
                                    EvalConfig.save();
                                    ctx.getSource().sendSuccess(() -> Component.literal("Inline expressions enabled").withStyle(ChatFormatting.GREEN), false);
                                    return 1;
                                }))
                        .then(Commands.literal("off")
                                .executes(ctx -> {
                                    EvalConfig.get().inlineEnabled = false;
                                    EvalConfig.save();
                                    ctx.getSource().sendSuccess(() -> Component.literal("Inline expressions disabled").withStyle(ChatFormatting.RED), false);
                                    return 1;
                                })))
                .then(Commands.literal("var")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("expression", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            String expression = StringArgumentType.getString(ctx, "expression");
                                            String result = EquationParser.evaluate(expression.toLowerCase(java.util.Locale.ROOT));
                                            try {
                                                double value = Double.parseDouble(result);
                                                EvalConfig.get().variables.put(name, value);
                                                EvalConfig.save();
                                                ctx.getSource().sendSuccess(() -> Component.literal("Variable ").withStyle(ChatFormatting.GREEN)
                                                        .append(Component.literal(name).withStyle(ChatFormatting.YELLOW))
                                                        .append(Component.literal(" set to ").withStyle(ChatFormatting.GREEN))
                                                        .append(Component.literal(expression).withStyle(ChatFormatting.GRAY))
                                                        .append(Component.literal(" = ").withStyle(ChatFormatting.GREEN))
                                                        .append(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.YELLOW)), false);
                                            } catch (NumberFormatException e) {
                                                ctx.getSource().sendSuccess(() -> Component.literal("Error: ").withStyle(ChatFormatting.RED)
                                                        .append(Component.literal(result).withStyle(ChatFormatting.YELLOW)), false);
                                            }
                                            return 1;
                                        }))))
                .then(Commands.literal("delvar")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    if (EvalConfig.get().variables.remove(name) != null) {
                                        EvalConfig.save();
                                        ctx.getSource().sendSuccess(() -> Component.literal("Variable ").withStyle(ChatFormatting.GREEN)
                                                .append(Component.literal(name).withStyle(ChatFormatting.YELLOW))
                                                .append(Component.literal(" deleted").withStyle(ChatFormatting.GREEN)), false);
                                    } else {
                                        ctx.getSource().sendSuccess(() -> Component.literal("Variable ").withStyle(ChatFormatting.RED)
                                                .append(Component.literal(name).withStyle(ChatFormatting.YELLOW))
                                                .append(Component.literal(" not found").withStyle(ChatFormatting.RED)), false);
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("vars")
                        .executes(ctx -> {
                            var vars = EvalConfig.get().variables;
                            if (vars.isEmpty()) {
                                ctx.getSource().sendSuccess(() -> Component.literal("No variables defined").withStyle(ChatFormatting.GRAY), false);
                            } else {
                                MutableComponent msg = Component.literal("Variables:").withStyle(ChatFormatting.GOLD);
                                vars.forEach((k, v) -> msg.append(Component.literal("\n"))
                                        .append(Component.literal(k).withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal(" = ").withStyle(ChatFormatting.WHITE))
                                        .append(Component.literal(String.valueOf(v)).withStyle(ChatFormatting.YELLOW)));
                                ctx.getSource().sendSuccess(() -> msg, false);
                            }
                            return 1;
                        }))
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            EvalConfig.load();
                            ctx.getSource().sendSuccess(() -> Component.literal("Config reloaded from file").withStyle(ChatFormatting.GREEN), false);
                            return 1;
                        }))
                .then(Commands.literal("status")
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
                            ctx.getSource().sendSuccess(() -> msg, false);
                            return 1;
                        }))
        );
    }
}
