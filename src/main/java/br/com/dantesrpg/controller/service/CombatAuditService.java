package br.com.dantesrpg.controller.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.GsonBuilder;

import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.combat.PlanoAcao;

/** Auditoria append-only da sessão; não executa rollback nem altera combate. */
public final class CombatAuditService {
	public record Registro(Instant momento, String planoId, String status, String ator,
			String acao, List<String> alvos, Map<String, Integer> dados, Map<String, String> origemDados, int custoMana,
			int custoTU, Map<String, Double> danoPrevisto, Map<String, String> reacoes,
			Map<String, Double> danoResolvido, List<String> filaContraAtaques,
			long milissegundosDesdePreparacao) { }

	private final List<Registro> registros = new ArrayList<>();

	public synchronized void registrar(PlanoAcao plano, String status) {
		if (plano == null) {
			return;
		}
		Map<String, Double> danos = new LinkedHashMap<>();
		plano.getDanos().forEach((alvo, eventos) -> danos.put(alvo.getNome(),
				eventos.stream().mapToDouble(evento -> evento.getValorDano()).sum()));
		registros.add(new Registro(Instant.now(), plano.getId().toString(), status,
				plano.getAtor().getNome(), plano.getNomeAcao(),
				plano.getInput().getAlvos().stream().map(Personagem::getNome).toList(),
				plano.getInput().getResultadosDados(), plano.getInput().getOrigensDados().entrySet().stream()
						.collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,
								e -> e.getValue().name(), (a, b) -> a, LinkedHashMap::new)),
				plano.getCustoMana(), plano.getCustoTU(), danos, plano.getReacoesResolvidas(),
				plano.getDanoResolvido(), plano.getFilaContraAtaques(),
				Duration.between(plano.getCriadoEm(), Instant.now()).toMillis()));
	}

	public synchronized List<Registro> getRegistros() {
		return List.copyOf(registros);
	}

	public synchronized String formatarResumo() {
		if (registros.isEmpty()) {
			return "Nenhuma ação transacional registrada nesta sessão.";
		}
		StringBuilder texto = new StringBuilder();
		for (Registro registro : registros) {
			texto.append(registro.momento()).append(" — ")
					.append(registro.status()).append(" — ")
					.append(registro.ator()).append(": ").append(registro.acao())
					.append(" [").append(registro.custoTU()).append(" TU")
					.append(registro.custoMana() > 0 ? ", " + registro.custoMana() + " MP" : "")
					.append("]\n");
		}
		return texto.toString();
	}

	public synchronized void exportar(Path destino) throws IOException {
		if (destino.getParent() != null) {
			Files.createDirectories(destino.getParent());
		}
		String json = new GsonBuilder().setPrettyPrinting().create().toJson(registros);
		Files.writeString(destino, json, StandardCharsets.UTF_8);
	}
}
