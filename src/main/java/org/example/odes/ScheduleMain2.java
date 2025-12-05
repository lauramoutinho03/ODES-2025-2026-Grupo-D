package org.example.odes;

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

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ScheduleMain2 {

    public static void main(String[] args) {
        try {

            // === Carregar CSVs ===
            List<Map<String, String>> aulas =
                    CsvParser.parseCSV(new File("src/main/resources/horarios_semana_28nov_a_4dez.csv"));

            List<Map<String, String>> salas =
                    CsvParser.parseCSV(new File("src/main/resources/Caracterizacao_das_salas.csv"));

            // === Instanciar problema ===
            ScheduleProblem problem = new ScheduleProblem(aulas, salas);

            int populationSize = 100;
            double crossoverProbability = 0.9;
            double mutationProbability = 1.0 / aulas.size();
            int maxEvaluations = 100;

            // === Operadores ===
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

            // === Melhor solução encontrada ===
            List<IntegerSolution> population = nsga2.result();

            IntegerSolution best = population.stream()
                    .min(Comparator.comparingDouble(s -> s.objectives()[0]))
                    .orElseThrow();

            System.out.println("=== Melhor atribuição de salas ===");

            for (int i = 0; i < aulas.size(); i++) {
                Map<String, String> aula = aulas.get(i);

                int salaIndex = best.variables().get(i);
                String salaNome = salas.get(salaIndex).get("Nome_sala");

                System.out.println(aula.get("Unidade de execução") + " → " + salaNome);
            }

            System.out.println("\n️Total de conflitos: " + best.objectives()[0]);

        } catch (Exception e) {
            System.err.println("Erro ao executar: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
