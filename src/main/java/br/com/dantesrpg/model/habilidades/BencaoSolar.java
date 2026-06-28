package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class BencaoSolar extends Habilidade {

	public BencaoSolar() {
		super("Bênção Solar",
			  "Concede 'Gnosis de Fogo' a um aliado, garantindo FORCA, Escudo de Sangue periódico e criando uma aura (Rock do Sol).",
			  TipoHabilidade.ATIVA,
			  2, // Custo Mana
			  100, // Custo TU
			  1, // Nível Necessário
			  TipoAlvo.INDIVIDUAL,
			  0.0, // Multiplicador de dano
			  0, // Ticks de dano
			  Collections.emptyList());
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
		return true;
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

		Map<String, Double> mods = new HashMap<>();
		mods.put("FORCA", 10.0);
		
		// Duração: 1000 TU, Tick Interval: 100 TU
		Efeito gnosis = new Efeito("Gnosis de Fogo", TipoEfeito.BUFF, 1000, mods, 0, 100);
		target.adicionarEfeito(gnosis);
		
		target.recalcularAtributosEstatisticas();
	}
}
