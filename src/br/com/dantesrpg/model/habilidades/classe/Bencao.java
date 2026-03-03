package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class Bencao extends Habilidade {
	public Bencao() {
		super("Bênção", "Escolha um buff para um aliado.", TipoHabilidade.ATIVA, 2, 120, 5, TipoAlvo.INDIVIDUAL, 0, 0,
				Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 5;
	}

	@Override
	public List<String> getOpcoesSelection() {
		return Arrays.asList("Poder", "Rapidez", "Vida", "Fé");
	}

	public void executarComEscolha(Personagem alvo, String escolha) {
		if (alvo == null)
			return;

		Map<String, Double> mods = new HashMap<>();
		String nomeBuff = "Bênção Maior: " + escolha;

		switch (escolha) {
		case "Poder":
			mods.put("DANO_BONUS_PERCENTUAL", 0.30);
			break;
		case "Rapidez":
			alvo.setContadorTU(Math.max(0, alvo.getContadorTU() - 25));
			mods.put("MOVIMENTO", 3.0);
			break;
		case "Vida":
			alvo.setEscudoAtual(alvo.getEscudoAtual() + (int) (alvo.getVidaMaxima() * 0.25));
			break;
		case "Fé":
			mods.put("ARMADURA_TOTAL", 25.0);
			break;
		}

		// Cria efeito
		Efeito buff = new Efeito(nomeBuff, TipoEfeito.BUFF, 250, mods, 0, 0);
		alvo.adicionarEfeito(buff);

		System.out.println(">>> Bênção em " + alvo.getNome() + ": " + nomeBuff);
		alvo.recalcularAtributosEstatisticas();
	}

	// Mantém o executar padrão vazio ou chamando um default caso algo dê errado
	@Override
	public void executar(Personagem c, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
	}
}