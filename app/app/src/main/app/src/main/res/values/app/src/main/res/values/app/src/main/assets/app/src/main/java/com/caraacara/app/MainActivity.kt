package com.caraacara.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONObject

data class Interview(val id: String, val name: String, val line: String, val obs: String)
data class CaseData(
    val title: String,
    val victim: String,
    val summary: String,
    val companion: List<String>,
    val interviews: List<Interview>,
    val clue: String,
    val finalChoices: List<Pair<String, String>>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val caseData = loadCase001()
        setContent { MaterialTheme { App(caseData) } }
    }

    private fun loadCase001(): CaseData {
        val jsonText = assets.open("case001.json").bufferedReader().use { it.readText() }
        val j = JSONObject(jsonText)

        val companion = buildList {
            val arr = j.getJSONArray("companion")
            for (i in 0 until arr.length()) add(arr.getString(i))
        }

        val interviews = buildList {
            val arr = j.getJSONArray("interviews")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(Interview(o.getString("id"), o.getString("name"), o.getString("line"), o.getString("obs")))
            }
        }

        val finals = buildList {
            val arr = j.getJSONArray("final")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(o.getString("id") to o.getString("label"))
            }
        }

        return CaseData(
            title = j.getString("title"),
            victim = j.getString("victim"),
            summary = j.getString("summary"),
            companion = companion,
            interviews = interviews,
            clue = j.getString("clue"),
            finalChoices = finals
        )
    }
}

private enum class Screen { HOME, INTERVIEWS, COMPANION, CLUES, FINAL }

@Composable
private fun App(caseData: CaseData) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var interviewed by remember { mutableStateOf(setOf<String>()) }
    var decisionId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Caso 001") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = screen == Screen.HOME, onClick = { screen = Screen.HOME }, label = { Text("Expediente") }, icon = {})
                NavigationBarItem(selected = screen == Screen.INTERVIEWS, onClick = { screen = Screen.INTERVIEWS }, label = { Text("Entrevistas") }, icon = {})
                NavigationBarItem(selected = screen == Screen.COMPANION, onClick = { screen = Screen.COMPANION }, label = { Text("Compañero") }, icon = {})
                NavigationBarItem(selected = screen == Screen.CLUES, onClick = { screen = Screen.CLUES }, label = { Text("Pistas") }, icon = {})
                NavigationBarItem(selected = screen == Screen.FINAL, onClick = { screen = Screen.FINAL }, label = { Text("Decisión") }, icon = {})
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                Screen.HOME -> Home(caseData)
                Screen.INTERVIEWS -> Interviews(caseData, interviewed) { interviewed = interviewed + it }
                Screen.COMPANION -> Companion(caseData)
                Screen.CLUES -> Clues(caseData, interviewed)
                Screen.FINAL -> Final(caseData, interviewed, decisionId) { decisionId = it }
            }
        }
    }
}

@Composable private fun Home(caseData: CaseData) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(caseData.title, style = MaterialTheme.typography.headlineSmall)
        Text("Víctima: ${caseData.victim}")
        Text(caseData.summary)
        Divider()
        Text("Objetivo: entrevistar, destrabar la pista y decidir cómo cerrar el caso.")
    }
}

@Composable private fun Interviews(caseData: CaseData, interviewed: Set<String>, onInterviewed: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(caseData.interviews) { itv ->
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(itv.name, style = MaterialTheme.typography.titleMedium)
                    Text("“${itv.line}”")
                    Text("Observación: ${itv.obs}", style = MaterialTheme.typography.bodySmall)
                    val done = interviewed.contains(itv.id)
                    Button(onClick = { onInterviewed(itv.id) }, enabled = !done) {
                        Text(if (done) "Entrevistado" else "Marcar entrevista")
                    }
                }
            }
        }
    }
}

@Composable private fun Companion(caseData: CaseData) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Chat con tu compañero", style = MaterialTheme.typography.titleLarge)
        caseData.companion.forEach { Text("• $it") }
    }
}

@Composable private fun Clues(caseData: CaseData, interviewed: Set<String>) {
    val unlocked = interviewed.size >= 2
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pistas", style = MaterialTheme.typography.titleLarge)
        if (!unlocked) {
            Text("Entrevistá al menos a 2 personas para destrabar la pista principal.")
        } else {
            Text("Pista principal:")
            Text("“${caseData.clue}”", style = MaterialTheme.typography.titleMedium)
            Divider()
            Text("Giro: horarios no cierran. El caso no es “menor”.")
        }
    }
}

@Composable private fun Final(caseData: CaseData, interviewed: Set<String>, decisionId: String?, onDecision: (String) -> Unit) {
    val ready = interviewed.size >= 2
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Decisión final", style = MaterialTheme.typography.titleLarge)

        if (!ready) {
            Text("Antes, entrevistá al menos a 2 personas.")
            return@Column
        }

        caseData.finalChoices.forEach { (id, label) ->
            Button(onClick = { onDecision(id) }, enabled = decisionId == null) { Text(label) }
        }

        decisionId?.let { chosenId ->
            val label = caseData.finalChoices.first { it.first == chosenId }.second
            Text("Elegiste: $label")
            Text(if (chosenId == "fast") "Ascendés… pero dejás una sombra." else "La verdad te fortalece… y te pone en la mira.")
        }
    }
}
