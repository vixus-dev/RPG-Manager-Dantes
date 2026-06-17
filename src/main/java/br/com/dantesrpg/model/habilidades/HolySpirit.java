package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class HolySpirit extends Habilidade {

	public HolySpirit() {
		super("Holy Spirit",
			  "Causa 9999 de dano fixo a um oponente. Pode critar.",
			  TipoHabilidade.ATIVA,
			  10, // Custo Mana
			  100, // Custo TU
			  1, // Nível Necessário
			  TipoAlvo.INDIVIDUAL,
			  0.0, // Multiplicador de dano (resolvido na execução)
			  0, // Ticks de dano
			  Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 4;
	}

	@Override
	public boolean afetaInimigos() {
		return true;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public boolean afetaSiMesmo() {
		return false;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (conjurador == null || alvos == null || alvos.isEmpty() || manager == null) {
			return;
		}

		Personagem target = alvos.get(0);
		if (target == null || !target.isAtivoNoCombate()) {
			return;
		}

		boolean isCrit = false;
		AcaoMestreInput input = manager.getLastInput();
		if (input != null && input.getCriticoManual() != null) {
			isCrit = input.getCriticoManual();
		} else {
			isCrit = Math.random() < conjurador.getTaxaCritica();
		}

		double danoFinal = 9999.0;
		if (isCrit) {
			danoFinal *= (1.0 + conjurador.getDanoCritico());
			System.out.println(">>> Holy Spirit: CRÍTICO! Dano aumentado para " + danoFinal);
		}

		// Aplica 9999 de dano resolvido (ignora armadura e reduções pré-armadura, e ignora escudo)
		manager.getDamageApplicator().aplicarDanoAoAlvoResolvido(conjurador, target, danoFinal, true, br.com.dantesrpg.model.enums.TipoAcao.HABILIDADE, estado);

		// Processa hooks de acerto crítico e dano causado no atacante/conjurador
		manager.getEffectProcessor().processarHooksDeSistema(conjurador, target, conjurador.getArmaEquipada(), input, danoFinal, estado, 1.0, isCrit);
	}
}
