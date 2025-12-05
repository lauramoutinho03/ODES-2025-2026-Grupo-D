package org.example.odes;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

public class ExpressionEvaluator {

    private final ExpressionParser parser;
    private final StandardEvaluationContext context;

    public ExpressionEvaluator() {
        parser = new SpelExpressionParser();
        context = new StandardEvaluationContext();

        // Adicionar funções matemáticas comuns
        try {
            context.registerFunction("abs", Math.class.getMethod("abs", double.class));
            context.registerFunction("min", Math.class.getMethod("min", double.class, double.class));
            context.registerFunction("max", Math.class.getMethod("max", double.class, double.class));
            context.registerFunction("pow", Math.class.getMethod("pow", double.class, double.class));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao registar funções matemáticas no SpEL.", e);
        }
    }

    /**
     * Avalia expressão usando um mapa de variáveis.
     * Exemplo:
     *   variables = { "lotacao": 30, "inscritos": 27 }
     *   expr = "abs(lotacao - inscritos)"
     */
    public double evaluate(String expr, Map<String, Object> variables) {
        variables.forEach(context::setVariable);

        Expression expression = parser.parseExpression(expr);

        Object result = expression.getValue(context);

        if (result instanceof Number num) {
            return num.doubleValue();
        }

        throw new RuntimeException("A expressão SpEL não retornou número: " + result);
    }
}

