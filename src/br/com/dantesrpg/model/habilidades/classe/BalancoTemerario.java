package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.util.BarbaroUtils;

import java.util.HashMap;
import java.util.List;

public class BalancoTemerario extends Habilidade {

	public BalancoTemerario() {
		super("Balanço Temerário",
				"Encanta a arma equipada; por 200 TU, cada golpe tem 50% de chance de aplicar Choque ao impacto.",
				TipoHabilidade.ATIVA, 2, 60, 3, TipoAlvo.SI_MESMO, 0, 0, java.util.Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		Efeito efeito = new Efeito(BarbaroUtils.EFEITO_BALANCO_TEMERARIO, TipoEfeito.BUFF, 200, new HashMap<>(), 0, 0);
		conjurador.adicionarEfeito(efeito);
		System.out.println(">>> " + conjurador.getNome() + " encantou a arma com Balanço Temerário por 200 TU.");
	}
}
