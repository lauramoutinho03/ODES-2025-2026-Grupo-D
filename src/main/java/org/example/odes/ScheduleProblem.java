package org.example.odes;

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

                // Conflito se mesma sala + mesmo dia + overlap
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

        solution.objectives()[0] = conflicts;
        return solution;
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
