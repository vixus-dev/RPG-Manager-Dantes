package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class Fortificar extends Habilidade {
	public Fortificar() {
		super("Fortificar", "Ganha um escudo de sangue equivalente a (Inspiração % de vida máxima) por 200TU. Ao encerrar, metade do valor restante vira cura.", TipoHabilidade.ATIVA,
				1, 100, 1, TipoAlvo.SI_MESMO, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		int inspiracao = conjurador.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 0);
		double pct = Math.max(0.0, inspiracao) / 100.0;
		double escudoSangue = conjurador.getVidaMaxima() * (pct * 10);

		System.out.println(conjurador.getNome() + " usa Fortificar! Escudo de Sangue de " + (int) escudoSangue + ".");

		conjurador.adicionarEscudoSangue(escudoSangue);

		Efeito fortificacao = new Efeito("Fortificar", TipoEfeito.BUFF, 200, null, 0, 0);
		fortificacao.setEscudoSangueOutorgado(escudoSangue);
		conjurador.adicionarEfeito(fortificacao);

		conjurador.recalcularAtributosEstatisticas();
		System.out.println(">>> Efeito [Fortificar] aplicado: Escudo de Sangue (" + (int) escudoSangue + ") por 200TU.");
	}
}