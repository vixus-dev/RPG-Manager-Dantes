package br.com.dantesrpg.model.habilidades.fantasmasnobres;

import java.util.List;
import java.util.Map;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;

/** Técnica concedida pelo BloodMoon para restringir um alvo e fortalecer Vicky. */
public class PrenderSangue extends Habilidade {

	public static final String MARCADOR_BLOQUEIO_MOVIMENTO = "BLOQUEAR_MOVIMENTO_PROXIMO_TURNO";

	public PrenderSangue() {
		super("Prender Sangue",
				"Imobiliza o alvo no próximo turno e concede +50% de dano ao usuário por 220 TU.",
				TipoHabilidade.ATIVA, 1, 100, 1, TipoAlvo.INDIVIDUAL, 0, 0, 0, List.of());
	}

	@Override
	public int getAlcanceMaximo() {
		return 5;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		if (conjurador == null || manager == null || alvos == null || alvos.isEmpty()) {
			return;
		}

		Personagem alvo = alvos.get(0);
		if (alvo == null || !alvo.isAtivoNoCombate()) {
			return;
		}

		Efeito imobilizacao = new Efeito("Sangue Imobilizado", TipoEfeito.DEBUFF, 9999,
				Map.of(MARCADOR_BLOQUEIO_MOVIMENTO, 1.0), 0, 0);
		manager.getEffectProcessor().aplicarEfeito(alvo, imobilizacao);

		Efeito bonusDano = new Efeito("BloodMoon: Prender Sangue", TipoEfeito.BUFF, 220,
				Map.of("DANO_BONUS_PERCENTUAL", 0.50), 0, 0);
		manager.getEffectProcessor().aplicarEfeito(conjurador, bonusDano);
		System.out.println(">>> " + conjurador.getNome() + " prende o sangue de " + alvo.getNome()
				+ ": movimento bloqueado no próximo turno e +50% de dano por 220 TU.");
	}
}
