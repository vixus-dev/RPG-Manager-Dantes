package br.com.dantesrpg.model.racas;

import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Personagem;

/** Variante tanque do construto GutterMan, sem o escudo inicial de spawn. */
public class GutterTank extends GutterMan {

	private static final String EFEITO_SHIELD = "Shield";

	@Override
	public String getNome() {
		return "Maquina-Maior";
	}

	@Override
	public String getDescricaoPassiva() {
		return "Construto tanque variante do GutterMan que não possui escudo inicial no spawn.";
	}

	@Override
	public void onCombatStart(Personagem personagem, EstadoCombate estado) {
		// GutterTank não recebe Shield no spawn / início de combate
	}

	@Override
	public void onEffectUpdate(Personagem personagem, Efeito efeito, boolean isAplicado) {
		if (!isAplicado || personagem == null || efeito == null
				|| !personagem.getEfeitosAtivos().containsKey(EFEITO_SHIELD)
				|| !efeitoQuebraDefesa(efeito)) {
			return;
		}

		System.out.println(">>> GUTTERTANK: defesa quebrada; Shield removido.");
		personagem.removerEfeito(EFEITO_SHIELD);
	}

	private boolean efeitoQuebraDefesa(Efeito efeito) {
		if (efeito.getModificadores() == null) {
			return false;
		}
		Double reducaoDano = efeito.getModificadores().get("REDUCAO_DANO_MODIFICADOR");
		Double armadura = efeito.getModificadores().get("ARMADURA_TOTAL");
		return (reducaoDano != null && reducaoDano < 0)
				|| (armadura != null && armadura < 0);
	}
}

