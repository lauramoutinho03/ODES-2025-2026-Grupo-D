package org.example.odes;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.solution.integersolution.impl.DefaultIntegerSolution;
import org.uma.jmetal.util.bounds.Bounds;
import org.uma.jmetal.util.errorchecking.Check;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ScheduleProblem extends AbstractIntegerProblem {

    private final List<Map<String, String>> aulas;
    private final List<Map<String, String>> salas;
    private List<Map<String, String>> solutionAssignments = new ArrayList<>();
    private int solutionConflitos;

    private List<Map<String, String>> objectives;
    private List<String> constraints;

    private double lastObjValue;
    private double lastPenalty;
    private int lastConstraintViolations;

    // parser SpEL
    private ExpressionParser parser = new SpelExpressionParser();

    // Construtor 1
    public ScheduleProblem(List<Map<String, String>> aulas, List<Map<String, String>> salas, List<Map<String, String>> objectives,
                           List<String> constraints) {
        this.aulas = aulas;
        this.salas = salas;
        this.objectives = objectives != null ? objectives : List.of();
        this.constraints = constraints != null ? constraints : List.of();

        // Cada variável representa a sala atribuída à aula i
        // bounds: 0..salas.size()-1
        List<Integer> lower = new ArrayList<>();
        List<Integer> upper = new ArrayList<>();

        for (int i = 0; i < aulas.size(); i++) {
            lower.add(0);
            upper.add(salas.size() - 1);
        }
        setVariableBounds(lower, upper);
    }

    // Construtor 2 para ScheduleMain2
    public ScheduleProblem(List<Map<String, String>> aulas, List<Map<String, String>> salas) {
        this.aulas = aulas;
        this.salas = salas;

        // Cada variável representa a sala atribuída à aula i
        // bounds: 0..salas.size()-1
        List<Integer> lower = new ArrayList<>();
        List<Integer> upper = new ArrayList<>();

        for (int i = 0; i < aulas.size(); i++) {
            lower.add(0);
            upper.add(salas.size() - 1);
        }
        setVariableBounds(lower, upper);
    }

    private void setVariableBounds(List<Integer> lower, List<Integer> upper) {
        //this.bounds = (List)IntStream.range(lower, upper);
        Check.notNull(lower);
        Check.notNull(upper);
        Check.that(lower.size() == upper.size(), "The size of the lower bound list is not equal to the size of the upper bound list");
        this.bounds = IntStream.range(0, lower.size()).mapToObj((i) -> Bounds.create((Integer)lower.get(i), (Integer)upper.get(i))).collect(Collectors.toList());
    }

    @Override
    public int numberOfVariables() {
        return aulas.size();
    }

    @Override
    public int numberOfObjectives() {
        return 1;
    }

    @Override
    public int numberOfConstraints() {
        return 0;
    }

    @Override
    public String name() {
        return "ScheduleProblem";
    }

    @Override
    public List<Bounds<Integer>> variableBounds() {
        return this.bounds;
    }

    @Override
    public IntegerSolution createSolution() {
        return new DefaultIntegerSolution(variableBounds(), numberOfObjectives(), numberOfConstraints());
    }

    @Override
    public IntegerSolution evaluate(IntegerSolution solution) {

        double objValue = 0;
        int constraintViolations = 0;

        // FUNÇÕES OBJETIVO E RESTRIÇÕES
        for (int i = 0; i < aulas.size(); i++) {

            Map<String, String> aula = aulas.get(i);
            int salaIndex = solution.variables().get(i);
            Map<String, String> sala = salas.get(salaIndex);

            // Construir contexto SpEL com variáveis da aula+sala
            StandardEvaluationContext context = buildContext(aula, sala);

            // ---- FUNÇÕES OBJETIVO ----
            for (Map<String, String> obj : objectives) {
                String expr = obj.get("expression");
                String sense = obj.get("sense"); // minimizar / maximizar

                if (expr == null || expr.isBlank()) continue;

                Double value;
                try {
                    value = parser.parseExpression(expr).getValue(context, Double.class);
                } catch (Exception e) {
                    value = 0.0; // se a expressão falhar, ignora e conta como 0
                }

                if (value == null) value = 0.0;

                if ("maximizar".equalsIgnoreCase(sense)) {
                    value = -value; // NSGA-II minimiza tudo
                }

                objValue += value;
            }

            // ---- RESTRIÇÕES ----
            for (String cons : constraints) {
                if (cons == null || cons.isBlank()) continue;

                Boolean ok;
                try {
                    ok = parser.parseExpression(cons).getValue(context, Boolean.class);
                } catch (Exception e) {
                    ok = true; // se a expressão falhar, não conta como violação
                }

                if (ok != null && !ok) {
                    constraintViolations++;
                }
            }
        }
        double penalty = constraintViolations * 100.0;

        this.lastObjValue = objValue;
        this.lastPenalty = penalty;
        this.lastConstraintViolations = constraintViolations;

        solution.objectives()[0] = objValue + penalty; // valor do objetivo + restrições violadas

        return solution;
    }

    // Construir contexto para SpEL
    private StandardEvaluationContext buildContext(Map<String, String> aula,
                                                   Map<String, String> sala) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        try {
            context.registerFunction("abs", Math.class.getMethod("abs", double.class));
            context.registerFunction("min", Math.class.getMethod("min", double.class, double.class));
            context.registerFunction("max", Math.class.getMethod("max", double.class, double.class));
            context.registerFunction("pow", Math.class.getMethod("pow", double.class, double.class));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao registar funções matemáticas no SpEL.", e);
        }

        // Aula
        for (var entry : aula.entrySet()) {
            context.setVariable(entry.getKey(), parseValue(entry.getValue()));
        }

        // Sala
        for (var entry : sala.entrySet()) {
            context.setVariable(entry.getKey(), parseValue(entry.getValue()));
        }

        return context;
    }

    private Object parseValue(String v) {
        if (v == null || v.isBlank()) return null;

        try {
            return Double.parseDouble(v.replace(",", "."));
        } catch (Exception ignored) {}

        if (v.equalsIgnoreCase("VERDADEIRO") || v.equalsIgnoreCase("TRUE") || v.equalsIgnoreCase("X"))
            return true;
        if (v.equalsIgnoreCase("FALSO") || v.equalsIgnoreCase("FALSE"))
            return false;

        try {
            return java.time.LocalTime.parse(v);
        } catch (Exception ignored) {}

        try {
            return java.time.LocalDate.parse(v, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception ignored) {}

        return v;
    }


    public void setSolution(List<Map<String, String>> assignments, int conflitos) {
        this.solutionAssignments = assignments;
        this.solutionConflitos = conflitos;
    }

    public List<Map<String, String>> getSolutionAssignments() {
        return solutionAssignments;
    }

    public int getSolutionConflitos() {
        return solutionConflitos;
    }

    public List<Map<String, String>> getAulas() {
        return aulas;
    }

    public List<Map<String, String>> getSalas() {
        return salas;
    }

    public double getLastObjValue() {
        return lastObjValue;
    }

    public double getLastPenalty() {
        return lastPenalty;
    }

    public int getLastConstraintViolations() {
        return lastConstraintViolations;
    }
}
