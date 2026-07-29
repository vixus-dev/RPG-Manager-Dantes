package br.com.dantesrpg.model.habilidades.fantasmasnobres;

import java.util.List;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.combat.FusaoMetamoru;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;

/** Técnica de fusão temporária concedida por O Anjo Nascido no Inferno. */
public class DancaMetamoru extends Habilidade {

	public static final String DADO_DESTREZA_USUARIO = "DADO_METAMORU_USUARIO";
	public static final String DADO_DESTREZA_ALIADO = "DADO_METAMORU_ALIADO";

	public DancaMetamoru() {
		super("Dança Metamoru",
				"Escolha um aliado e informe os dois testes de Destreza. Se ambos superarem 9, os dois se fundem por 420 TU.",
				TipoHabilidade.ATIVA, 0, 120, 1, TipoAlvo.INDIVIDUAL, 0, 0, 0, List.of());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public boolean afetaInimigos() {
		return false;
	}

	@Override
	public boolean afetaAliados() {
		return true;
	}

	@Override
	public boolean afetaSiMesmo() {
		return false;
	}

	@Override
	public String getMotivoBloqueio(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado) {
		if (FusaoMetamoru.participaDeFusao(estado, conjurador)) {
			return conjurador.getNome() + " já participa de uma fusão Metamoru.";
		}
		if (alvos == null || alvos.size() != 1 || alvos.get(0) == null) {
			return "Dança Metamoru requer um único aliado.";
		}
		Personagem aliado = alvos.get(0);
		if (aliado == conjurador || !aliado.isAtivoNoCombate()
				|| conjurador.getFaccao() == null || !conjurador.getFaccao().equals(aliado.getFaccao())) {
			return "Dança Metamoru só pode ser usada em um aliado ativo.";
		}
		return FusaoMetamoru.participaDeFusao(estado, aliado)
				? aliado.getNome() + " já participa de uma fusão Metamoru."
				: null;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		if (alvos == null || alvos.isEmpty()) return;
		int dadoUsuario = manager.getLastInput().getResultadoDado(DADO_DESTREZA_USUARIO);
		int dadoAliado = manager.getLastInput().getResultadoDado(DADO_DESTREZA_ALIADO);
		Personagem aliado = alvos.get(0);
		if (dadoUsuario <= 9 || dadoAliado <= 9) {
			System.out.println(">>> Dança Metamoru falhou: ambos os testes de Destreza devem superar 9."
					+ " (" + dadoUsuario + ", " + dadoAliado + ")");
			return;
		}
		FusaoMetamoru.criar(conjurador, aliado, estado, manager);
	}
}
