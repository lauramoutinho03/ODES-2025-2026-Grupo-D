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

    // parser SpEL
    private ExpressionParser parser = new SpelExpressionParser();

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

        double conflicts = 0;
        double objectiveSum = 0;
        int constraintViolations = 0;

        // CONTAGEM DE CONFLITOS
        for (int i = 0; i < aulas.size(); i++) {

            Map<String, String> aula1 = aulas.get(i);
            int salaIndex1 = solution.variables().get(i);
            Map<String, String> sala1 = salas.get(salaIndex1);

            String salaNome1 = sala1.get("Nome_sala");
            String dia1 = aula1.get("Dia");
            LocalTime inicio1 = LocalTime.parse(aula1.get("Início"));
            LocalTime fim1 = LocalTime.parse(aula1.get("Fim"));

            for (int j = i + 1; j < aulas.size(); j++) {

                Map<String, String> aula2 = aulas.get(j);
                int salaIndex2 = solution.variables().get(j);
                Map<String, String> sala2 = salas.get(salaIndex2);

                if (salaNome1.equals(sala2.get("Nome_sala")) &&
                        dia1.equals(aula2.get("Dia"))) {

                    LocalTime inicio2 = LocalTime.parse(aula2.get("Início"));
                    LocalTime fim2 = LocalTime.parse(aula2.get("Fim"));

                    if (inicio1.isBefore(fim2) && inicio2.isBefore(fim1)) {
                        conflicts++;
                    }
                }
            }
        }

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

                // Limpar nomes das variáveis para SpEL (remove espaços e acentos)
                expr = cleanExpression(expr);

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

                objectiveSum += value;
            }

            // ---- RESTRIÇÕES ----
            for (String cons : constraints) {
                if (cons == null || cons.isBlank()) continue;

                cons = cleanExpression(cons);
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

        double penalty = constraintViolations * 1000.0;

        solution.objectives()[0] =
                conflicts      // conflitos
                        + objectiveSum // expressões do utilizador
                        + penalty;     // restrições violadas

        return solution;
    }

    // Remove espaços, acentos e caracteres inválidos para SpEL
    private String cleanExpression(String expr) {
        if (expr == null) return "";
        return expr.replaceAll("[^A-Za-z0-9()+\\-*/.<>=]", "");
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
            String key = cleanVarName(entry.getKey());
            context.setVariable(key, parseNumberOrDouble(entry.getValue()));
        }

        // Sala
        for (var entry : sala.entrySet()) {
            String key = cleanVarName(entry.getKey());
            context.setVariable(key, parseNumberOrDouble(entry.getValue()));
        }

        return context;
    }

    // Limpa nomes de colunas para variáveis válidas
    private String cleanVarName(String s) {
        if (s == null) return "";
        return s.replaceAll("[^A-Za-z0-9]", "");
    }

    // Converte valores para Double
    private Double parseNumberOrDouble(String v) {
        if (v == null) return 0.0;
        try {
            return Double.parseDouble(v.replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
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
}
