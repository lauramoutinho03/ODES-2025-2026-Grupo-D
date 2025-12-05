let csvSalas = [];
let csvHorarios = [];

let tableSalas;
let tableHorarios;

// =====================
// 1) Upload de ficheiros CSV
// =====================
function uploadTwoFiles() {
    const fileSalas = document.getElementById("salas").files[0];
    const fileHor = document.getElementById("horarios").files[0];

    if (!fileSalas || !fileHor) {
        alert("Por favor seleciona os dois ficheiros CSV.");
        return;
    }

    Papa.parse(fileSalas, {
        header: true,
        skipEmptyLines: true,
        complete: function (results) {
            csvSalas = results.data;
            renderTabulator("tabSalas", csvSalas, tableSalas, (t) => tableSalas = t);
        }
    });

    Papa.parse(fileHor, {
        header: true,
        skipEmptyLines: true,
        complete: function (results) {
            csvHorarios = results.data;
            renderTabulator("tabHor", csvHorarios, tableHorarios, (t) => tableHorarios = t);
        }
    });
}

// =====================
// 2) Função genérica para criar tabela Tabulator
// =====================
function renderTabulator(divId, data, tableInstance, assignFunc) {
    if (!data || data.length === 0) return;

    const columns = Object.keys(data[0]).map(key => ({
        title: key,
        field: key,
        headerFilter: true,
    }));

    const table = new Tabulator("#" + divId, {
        data: data,
        columns: columns,
        layout: "fitData",
        pagination: "local",
        paginationSize: 13,
        movableColumns: false,
        resizableRows: false,
        placeholder: "Sem dados",
        responsiveLayout: false,
    });

    assignFunc(table);
}

// ==========================
// 3) Variáveis de decisão
// ==========================
function addDecisionVar() {
    const container = document.getElementById("decisionVars");

    const div = document.createElement("div");
    div.innerHTML = `
        <input type="text" placeholder="label">
        <select>
            <option>discreto</option>
            <option>contínuo</option>
        </select>
        <input type="text" placeholder="Gama" style="width: 350px;">
    `;

    container.appendChild(div);
}

// ==========================
// 4) Objetivos
// ==========================
function addObjective() {
    const container = document.getElementById("objectives");

    const div = document.createElement("div");
    div.innerHTML = `
        <select>
            <option>Minimizar</option>
            <option>Maximizar</option>
        </select>
        <input type="text" placeholder="Expressão (ex: abs(lotacao - inscritos))" style="width: 500px;">
    `;

    container.appendChild(div);
}

// ==========================
// 5) Restrições
// ==========================
function addConstraint() {
    const container = document.getElementById("constraints");

    const div = document.createElement("div");
    div.innerHTML = `
        <input type="text" placeholder="Restrição (ex: lotacao >= inscritos)" style="width: 500px;">
    `;

    container.appendChild(div);
}

// =============================================
// 6) Submeter definição do problema ao backend
// =============================================
function submitProblemDefinition() {
    // Variáveis de decisão
    //debugger
    const decisionVars = [...document.querySelectorAll("#decisionVars div")].map(div => {
        const [labelInput, typeSelect, rangeInput ] = div.children;
        return {
            label: labelInput.value,
            type: typeSelect.value,
            range: rangeInput.value
        };
    });

    // Objetivos
    const objectives = [...document.querySelectorAll("#objectives div")].map(div => {
        const [optSelect, exprInput] = div.children;
        return {
            sense: optSelect.value.toLowerCase(),
            expression: exprInput.value
        };
    });

    // Restrições
    const constraints = [...document.querySelectorAll("#constraints div input")].map(i => i.value);

    // Montar JSON final
    const problemJSON = {
        problem_name: "UCTP - Atribuição de Salas a Aulas",
        description: "Problema de otimização multiobjetivo",
        dataset: {
            salas: csvSalas,
            horarios: csvHorarios
        },
        decision_variables: decisionVars,
        objectives: objectives,
        constraints: constraints
    };

    // Mostrar JSON no HTML
    document.getElementById("jsonOutput").textContent = JSON.stringify(problemJSON, null, 2);


    // Enviar para backend
    fetch("http://localhost:8080/problem", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(problemJSON)
    })
        .then(res => {
            if (!res.ok) throw new Error("Erro ao criar problema no backend");
            return res.json();
        })
        .then(data => alert("Problema criado com sucesso no backend!"))
        .catch(err => alert("Erro: " + err));


}

/*function solveProblem() {
    fetch("http://localhost:8080/solve", { method: "POST" })
        .then(r => r.json())
        .then(data => {
            document.getElementById("results").textContent =
                JSON.stringify(data, null, 2);
        })
        .catch(err => alert("Erro: " + err));
}*/

function solveProblem() {
    fetch("http://localhost:8080/solve", { method: "POST" })
        .then(r => r.json())
        .then(data => {
            const resultsDiv = document.getElementById("results");
            resultsDiv.innerHTML = ""; // Limpar resultados antigos

            if (data.error) {
                resultsDiv.textContent = data.error;
                return;
            }

            // Obter atribuições
            const assignments = data.Atribuições || data.assignments;

            if (!assignments || assignments.length === 0) {
                resultsDiv.textContent = "Nenhuma atribuição encontrada.";
                return;
            }

            // Criar div para tabela
            const tableDiv = document.createElement("div");
            tableDiv.id = "assignmentsTable"; // id para o Tabulator
            resultsDiv.appendChild(tableDiv);

            // Criar tabela Tabulator com paginação
            new Tabulator("#assignmentsTable", {
                data: assignments,
                layout: "fitColumns",
                columns: [
                    { title: "Aula", field: "aula" },
                    { title: "Sala", field: "sala" }
                ],
                placeholder: "Sem atribuições",
                pagination: "local",       // ativa a paginação local
                paginationSize: 10,        // 10 linhas por página
                paginationSizeSelector: [5, 10, 20, 50], // opcional: dropdown para mudar tamanho
                movableColumns: true,
                resizableRows: false
            });

            // Mostrar número de conflitos abaixo da tabela
            const conflictsP = document.createElement("p");
            conflictsP.innerHTML = `<strong>Número de conflitos:</strong> ${data.objective || data.conflitos || 0}`;
            resultsDiv.appendChild(conflictsP);
        })
        .catch(err => alert("Erro: " + err));
}


