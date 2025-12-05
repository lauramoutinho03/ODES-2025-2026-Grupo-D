package org.example.odes;

import org.springframework.web.bind.annotation.*;
import org.uma.jmetal.algorithm.examples.AlgorithmRunner;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAII;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.IntegerSBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.IntegerPolynomialMutation;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;

import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:63342") // OU remove se não for preciso
public class ScheduleController {

    private ScheduleProblem lastProblem;

    @PostMapping("/problem")
    public Map<String, Object> createProblem(@RequestBody Map<String, Object> json) {

        Map<String, Object> dataset = (Map<String, Object>) json.get("dataset");

        List<Map<String, String>> salas =
                (List<Map<String, String>>) dataset.get("salas");

        List<Map<String, String>> horarios =
                (List<Map<String, String>>) dataset.get("horarios");

        List<Map<String, String>> objectives =
                (List<Map<String, String>>) json.get("objectives");

        List<String> constraints =
                (List<String>) json.get("constraints");

        // Criar o ScheduleProblem com o mínimo que ele precisa: aulas + salas
        lastProblem = new ScheduleProblem(horarios, salas, objectives, constraints);

        return Map.of(
                "message", "Problema criado com sucesso!",
                "numSalas", salas.size(),
                "numAulas", horarios.size()
        );
    }



    @PostMapping("/solve")
    public Map<String, Object> solveProblem() {
        if (lastProblem == null) {
            return Map.of("error", "Nenhum problema foi criado ainda.");
        }

        // Criar solução e avaliar
        var solution = runNSGAII(lastProblem);

        // Criar lista de atribuições
        List<Map<String, String>> assignments = new ArrayList<>();
        List<Map<String, String>> aulas = lastProblem.getAulas();
        List<Map<String, String>> salas = lastProblem.getSalas();

        for (int i = 0; i < aulas.size(); i++) {
            Map<String, String> aula = aulas.get(i);
            int salaIndex = solution.variables().get(i);
            Map<String, String> sala = salas.get(salaIndex);

            Map<String, String> item = new HashMap<>();
            item.put("aula", aula.get("Unidade de execução")); // garante que a chave seja "Aula"
            item.put("sala", sala.get("Nome_sala"));
            assignments.add(item);
        }

        // Contar conflitos
        int conflitos = (int) solution.objectives()[0];

        // Guardar no problema (opcional)
        lastProblem.setSolution(assignments, conflitos);

        // Devolver JSON para frontend
        return Map.of(
                "Atribuições", assignments,
                "objective", conflitos
        );
    }

    private IntegerSolution runNSGAII(ScheduleProblem problem) {

        int populationSize = 100;
        double crossoverProbability = 0.9;
        double mutationProbability = 1.0 / problem.getAulas().size();
        int maxEvaluations = 200;

        CrossoverOperator<IntegerSolution> crossover =
                new IntegerSBXCrossover(crossoverProbability, 20.0);

        MutationOperator<IntegerSolution> mutation =
                new IntegerPolynomialMutation(mutationProbability, 20.0);

        NSGAII<IntegerSolution> nsga2 =
                new NSGAIIBuilder<>(problem, crossover, mutation, populationSize)
                        .setSelectionOperator(new BinaryTournamentSelection<>(
                                new RankingAndCrowdingDistanceComparator<>()))
                        .setMaxEvaluations(maxEvaluations)
                        .build();

        new AlgorithmRunner.Executor(nsga2).execute();

        List<IntegerSolution> population = nsga2.result();

        // Escolher a melhor solução (menor número de conflitos)
        return population.stream()
                .min(Comparator.comparingDouble(s -> s.objectives()[0]))
                .orElseThrow();
    }

}


