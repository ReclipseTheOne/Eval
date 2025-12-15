package com.reclipse.eval;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EquationParser {
    private static final Pattern LOG_BASE_PATTERN = Pattern.compile("log_([0-9.]+)\\(([^)]+)\\)");

    public static String evaluate(String expression) {
        try {
            String processed = preprocess(expression);
            double result = parseExpression(processed, new int[]{0});

            if (result == (long) result) {
                return String.valueOf((long) result);
            }
            return String.valueOf(result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private static String preprocess(String expr) {
        expr = expr.replaceAll("\\s+", "");

        // log_a(x)
        Matcher logBaseMatcher = LOG_BASE_PATTERN.matcher(expr);
        StringBuffer sb = new StringBuffer();
        while (logBaseMatcher.find()) {
            double base = Double.parseDouble(logBaseMatcher.group(1));
            String inner = logBaseMatcher.group(2);
            double innerResult = parseExpression(preprocess(inner), new int[]{0});
            double result = Math.log(innerResult) / Math.log(base);
            logBaseMatcher.appendReplacement(sb, String.valueOf(result));
        }
        logBaseMatcher.appendTail(sb);
        expr = sb.toString();

        // Replace vars
        var variables = EvalConfig.get().variables;
        var sortedVars = variables.entrySet().stream()
                .sorted((a, b) -> b.getKey().length() - a.getKey().length())
                .toList();
        for (var entry : sortedVars) {
            expr = expr.replace(entry.getKey(), String.valueOf(entry.getValue()));
        }

        expr = expr.replace("pi", String.valueOf(Math.PI));
        expr = expr.replace("e", String.valueOf(Math.E));

        return expr;
    }

    private static double parseExpression(String expr, int[] pos) {
        double result = parseTerm(expr, pos);

        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op == '+') {
                pos[0]++;
                result += parseTerm(expr, pos);
            } else if (op == '-') {
                pos[0]++;
                result -= parseTerm(expr, pos);
            } else {
                break;
            }
        }

        return result;
    }

    private static double parseTerm(String expr, int[] pos) {
        double result = parsePower(expr, pos);

        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op == '*') {
                pos[0]++;
                result *= parsePower(expr, pos);
            } else if (op == '/') {
                pos[0]++;
                result /= parsePower(expr, pos);
            } else if (op == '%') {
                pos[0]++;
                result %= parsePower(expr, pos);
            } else if (op == '(' || Character.isDigit(op) || op == '.') {
                result *= parsePower(expr, pos);
            } else {
                break;
            }
        }

        return result;
    }

    private static double parsePower(String expr, int[] pos) {
        double base = parseUnary(expr, pos);

        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '^') {
            pos[0]++;
            double exponent = parsePower(expr, pos); // Right assoc
            return Math.pow(base, exponent);
        }

        return base;
    }

    private static double parseUnary(String expr, int[] pos) {
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '-') {
            pos[0]++;
            return -parseUnary(expr, pos);
        }
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '+') {
            pos[0]++;
            return parseUnary(expr, pos);
        }
        return parseFactor(expr, pos);
    }

    private static double parseFactor(String expr, int[] pos) {
        if (pos[0] >= expr.length()) {
            throw new RuntimeException("Unexpected end of expression");
        }

        // Check for functions
        String[] functions = {"sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh",
                              "sqrt", "cbrt", "abs", "ln", "lg", "log", "exp", "floor", "ceil", "round"};

        for (String func : functions) {
            if (expr.startsWith(func, pos[0])) {
                pos[0] += func.length();
                if (pos[0] < expr.length() && expr.charAt(pos[0]) == '(') {
                    pos[0]++; // skip (
                    double arg = parseExpression(expr, pos);
                    if (pos[0] < expr.length() && expr.charAt(pos[0]) == ')') {
                        pos[0]++; // skip )
                    }
                    return applyFunction(func, arg);
                }
                throw new RuntimeException("Expected '(' after function " + func);
            }
        }

        // Parentheses
        if (expr.charAt(pos[0]) == '(') {
            pos[0]++; // skip (
            double result = parseExpression(expr, pos);
            if (pos[0] < expr.length() && expr.charAt(pos[0]) == ')') {
                pos[0]++; // skip )
            }
            return result;
        }

        return parseNumber(expr, pos);
    }

    private static double parseNumber(String expr, int[] pos) {
        int start = pos[0];
        while (pos[0] < expr.length() && (Character.isDigit(expr.charAt(pos[0])) || expr.charAt(pos[0]) == '.')) {
            pos[0]++;
        }
        if (start == pos[0]) {
            throw new RuntimeException("Expected number at position " + pos[0]);
        }
        return Double.parseDouble(expr.substring(start, pos[0]));
    }

    private static double applyFunction(String func, double arg) {
        return switch (func) {
            case "sin" -> Math.sin(Math.toRadians(arg));
            case "cos" -> Math.cos(Math.toRadians(arg));
            case "tan" -> Math.tan(Math.toRadians(arg));
            case "asin" -> Math.toDegrees(Math.asin(arg));
            case "acos" -> Math.toDegrees(Math.acos(arg));
            case "atan" -> Math.toDegrees(Math.atan(arg));
            case "sinh" -> Math.sinh(arg);
            case "cosh" -> Math.cosh(arg);
            case "tanh" -> Math.tanh(arg);
            case "sqrt" -> Math.sqrt(arg);
            case "cbrt" -> Math.cbrt(arg);
            case "abs" -> Math.abs(arg);
            case "ln" -> Math.log(arg);
            case "lg", "log" -> Math.log10(arg);
            case "exp" -> Math.exp(arg);
            case "floor" -> Math.floor(arg);
            case "ceil" -> Math.ceil(arg);
            case "round" -> Math.round(arg);
            default -> throw new RuntimeException("Unknown function: " + func);
        };
    }
}
