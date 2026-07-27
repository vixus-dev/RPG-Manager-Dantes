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
import java.util.Map;

public class AmpliaçãoElemental extends Habilidade {
	public static final String NOME_EFEITO = "Ampliação elemental";

	public AmpliaçãoElemental() {
		super(NOME_EFEITO,
				"Durante 200 TU, concede 5% de dano por ponto de Inspiração. Bloqueada enquanto o usuário estiver transformado.",
				TipoHabilidade.ATIVA, 2, 50, 1, TipoAlvo.SI_MESMO, 0, 0, 0, Collections.emptyList());
	}

	@Override
	public String getMotivoBloqueio(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado) {
		if (conjurador != null && conjurador.getRaca() != null && conjurador.getRaca().isTransformed()) {
			return "Ampliação elemental está bloqueada enquanto o personagem estiver transformado.";
		}
		return null;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		if (conjurador == null) {
			return;
		}
		int inspiracao = Math.max(0, conjurador.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 0));
		double bonusDano = inspiracao * 0.05;
		Efeito efeito = new Efeito(NOME_EFEITO, TipoEfeito.BUFF, 200,
				Map.of("DANO_BONUS_PERCENTUAL", bonusDano), 0, 0);
		conjurador.adicionarEfeito(efeito);
		System.out.println(">>> " + conjurador.getNome() + " ativou Ampliação elemental: +"
				+ String.format("%.1f", bonusDano * 100) + "% de dano por 200 TU.");
	}
}
