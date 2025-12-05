package org.example.odes;

import org.springframework.web.bind.annotation.*;
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

        // Criar o ScheduleProblem com o mínimo que ele precisa: aulas + salas
        lastProblem = new ScheduleProblem(horarios, salas);

        return Map.of(
                "message", "Problema criado com sucesso!",
                "numSalas", salas.size(),
                "numAulas", horarios.size()
        );
    }


    /*@PostMapping("/solve")
    public Map<String, Object> solve() {
        List<Map<String, String>> assignments = new ArrayList<>();

        for (ScheduleProblem sol : lastResult.getSolutionAssignments()) {
            Map<String, String> item = new HashMap<>();
            item.put("aula", sol.getAula());
            item.put("sala", sol.getSala());
            assignments.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("assignments", assignments);
        response.put("conflitos", lastResult.getConflitos());

        return response;
    }*/

    /*@PostMapping("/solve")
    public Map<String, Object> solveProblem() {
        if (lastProblem == null) {
            return Map.of("error", "Nenhum problema foi criado ainda.");
        }

        // Criar solução e avaliar
        var solution = lastProblem.createSolution();
        lastProblem.evaluate(solution);

        List<Map<String, String>> assignments = new ArrayList<>();
        assignments = lastProblem.getSolutionAssignments();
        int conflitos = lastProblem.getSolutionConflitos();
        return Map.of(
                "Atribuições", assignments,
                "objective", conflitos
        );
    }*/

    @PostMapping("/solve")
    public Map<String, Object> solveProblem() {
        if (lastProblem == null) {
            return Map.of("error", "Nenhum problema foi criado ainda.");
        }

        // Criar solução e avaliar
        var solution = lastProblem.createSolution();
        lastProblem.evaluate(solution);

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
}


