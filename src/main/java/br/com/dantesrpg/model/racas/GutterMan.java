package br.com.dantesrpg.model.racas;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.Raça;
import br.com.dantesrpg.model.enums.TipoEfeito;

/** Comportamento exclusivo do construto GutterMan. */
public class GutterMan extends Raça {

	private static final String EFEITO_SHIELD = "Shield";
	private static final String EFEITO_COOLDOWN_RESTOCK = "CD:shield Restock";
	private static final int DURACAO_INFINITA = 99_999;

	@Override
	public String getNome() {
		return "Maquina-Maior";
	}

	@Override
	public String getDescricaoPassiva() {
		return "Construto protegido por um escudo que reduz 99% do dano recebido até ser quebrado.";
	}

	@Override
	public List<Habilidade> getRacialAbilities(Personagem personagem) {
		return Collections.emptyList();
	}

	@Override
	public void onCombatStart(Personagem personagem, EstadoCombate estado) {
		aplicarShield(personagem);
	}

	@Override
	public double getMultiplicadorDanoRecebidoPreArmadura(Personagem personagem, Personagem atacante,
			EstadoCombate estado) {
		return personagem != null && personagem.getEfeitosAtivos().containsKey(EFEITO_SHIELD) ? 0.01 : 1.0;
	}

	@Override
	public void onEffectUpdate(Personagem personagem, Efeito efeito, boolean isAplicado) {
		if (personagem == null || efeito == null) {
			return;
		}

		if (isAplicado && efeitoQuebraDefesa(efeito) && personagem.getEfeitosAtivos().containsKey(EFEITO_SHIELD)) {
			System.out.println(">>> GUTTERMAN: defesa quebrada; Shield removido.");
			personagem.removerEfeito(EFEITO_SHIELD);
			return;
		}

		if (!isAplicado && EFEITO_SHIELD.equalsIgnoreCase(efeito.getNome())
				&& !personagem.getEfeitosAtivos().containsKey(EFEITO_COOLDOWN_RESTOCK)) {
			Efeito cooldown = new Efeito(EFEITO_COOLDOWN_RESTOCK, TipoEfeito.DEBUFF, 500, null, 0, 0);
			personagem.adicionarEfeito(cooldown);
			System.out.println(">>> GUTTERMAN: Shield quebrado; shield Restock em cooldown por 500 TU.");
		}
	}

	@Override
	public void onCriticalHitTaken(Personagem personagem, Personagem atacante, EstadoCombate estado) {
		if (personagem != null && personagem.getEfeitosAtivos().containsKey(EFEITO_SHIELD)) {
			System.out.println(">>> GUTTERMAN: golpe crítico detectado; Shield removido.");
			personagem.removerEfeito(EFEITO_SHIELD);
		}
	}

	private void aplicarShield(Personagem personagem) {
		if (personagem == null || personagem.getEfeitosAtivos().containsKey(EFEITO_SHIELD)) {
			return;
		}
		personagem.adicionarEfeito(new Efeito(EFEITO_SHIELD, TipoEfeito.BUFF, DURACAO_INFINITA,
				Map.of(), 0, 0));
	}

	private boolean efeitoQuebraDefesa(Efeito efeito) {
		if (efeito.getModificadores() == null) {
			return false;
		}
		Double reducaoDano = efeito.getModificadores().get("REDUCAO_DANO_MODIFICADOR");
		Double armadura = efeito.getModificadores().get("ARMADURA_TOTAL");
		return (reducaoDano != null && reducaoDano < 0) || (armadura != null && armadura < 0);
	}
}
