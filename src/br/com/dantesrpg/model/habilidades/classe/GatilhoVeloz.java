package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GatilhoVeloz extends Habilidade {

	public static final String NOME_EFEITO = "Gatilho Veloz";

	public GatilhoVeloz() {
		super("Gatilho Veloz",
				"Prepara um contra-disparo reativo. Ao ser atacado, testa agilidade para anular a ação inimiga.",
				TipoHabilidade.ATIVA, 2, 120, 3, TipoAlvo.SI_MESMO, 0.0, 0,
				Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		Efeito efeito = new Efeito(NOME_EFEITO, TipoEfeito.BUFF, 300, Map.of(), 0, 0);
		conjurador.removerEfeito(NOME_EFEITO);
		conjurador.adicionarEfeito(efeito);
		System.out.println(">>> " + conjurador.getNome() + " está com o Gatilho Veloz preparado.");
	}
}
