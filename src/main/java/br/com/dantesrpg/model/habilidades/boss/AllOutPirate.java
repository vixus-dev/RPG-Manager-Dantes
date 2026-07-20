package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/** Desperta a forma pirata de Xebros e libera suas técnicas temporárias. */
public class AllOutPirate extends Habilidade {
	private static final String EFEITO = "ALL OUT PIRATE";

	public AllOutPirate() {
		super("im... the bone of my sword...", "Ganha 25% de escudo de sangue e entra em ALL OUT PIRATE.",
				TipoHabilidade.ATIVA, 0, 100, 1, TipoAlvo.SI_MESMO, 0, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		conjurador.adicionarEscudoSangue(conjurador.getVidaMaxima() * 0.25);
		conjurador.adicionarPropriedade("VAMPIRISMO:10");
		manager.aplicarEfeito(conjurador, new Efeito(EFEITO, TipoEfeito.BUFF, 2000, new HashMap<>(), 0, 0));
		adicionarHabilidadeSeAusente(conjurador, new IsThatAllYouGot());
		adicionarHabilidadeSeAusente(conjurador, new WhereIsYourDream());
		adicionarHabilidadeSeAusente(conjurador, new ThisIsMySea());
		Arma arma = conjurador.getArmaEquipada();
		if (arma != null) arma.setBonusAlcanceTemporario(2);
		conjurador.recalcularAtributosEstatisticas();
	}

	public static void reverter(Personagem personagem) {
		personagem.removerPropriedade("VAMPIRISMO:10");
		personagem.removerHabilidadeExtraPorNome("IS THAT ALL YOU GOT?");
		personagem.removerHabilidadeExtraPorNome("WHERE IS YOUR DREAM?");
		personagem.removerHabilidadeExtraPorNome("THIS IS MY SEA.");
		Arma arma = personagem.getArmaEquipada();
		if (arma != null) arma.setBonusAlcanceTemporario(0);
		personagem.recalcularAtributosEstatisticas();
	}

	private void adicionarHabilidadeSeAusente(Personagem personagem, Habilidade habilidade) {
		boolean jaPossui = personagem.getHabilidadesExtras().stream()
				.anyMatch(extra -> extra.getNome().equalsIgnoreCase(habilidade.getNome()));
		if (!jaPossui) personagem.adicionarHabilidadeExtra(habilidade);
	}
}
