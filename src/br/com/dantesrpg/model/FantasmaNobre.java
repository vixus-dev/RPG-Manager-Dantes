package br.com.dantesrpg.model;

import br.com.dantesrpg.model.enums.TipoAlvo;
import java.util.List;

public abstract class FantasmaNobre {

	// Para a UI (BotÃ£o e Tooltip)
	public abstract String getNome();

	public abstract String getDescricao();

	// Para a LÃ³gica de AtivaÃ§Ã£o (Custos)
	public abstract int getCustoMana();

	public abstract int getCustoTU();

	public abstract int getCooldownTU();

	public abstract TipoAlvo getTipoAlvo();

	/**
	 * Se TipoAlvo.AREA_QUADRADA, define o tamanho (ex: 3 para 3x3).
	 * 
	 * @return O tamanho (ex: 3, 5, 6).
	 */
	public abstract int getTamanhoArea();

	/**
	 * Se TipoAlvo.MULTIPLOS, define quantos alvos pode selecionar.
	 * 
	 * @return O nÃºmero de alvos.
	 */
	public abstract int getNumeroDeAlvos();

	public List<String> getOpcoesSelection() {
		return null; // PadrÃ£o: sem opÃ§Ãµes
	}

	public boolean podeExecutar(Personagem conjurador) {
		return getMotivoBloqueio(conjurador) == null;
	}

	public String getMotivoBloqueio(Personagem conjurador) {
		return null;
	}

	public void onCombatStart(Personagem conjurador, EstadoCombate estado, CombatManager manager) {
	}

	public void onTurnStart(Personagem conjurador, EstadoCombate estado, CombatManager manager) {
	}

	public void onDamageDealt(Personagem conjurador, Personagem alvo, double danoCausado, EstadoCombate estado,
			CombatManager manager) {
	}

	public void onCriticalHit(Personagem conjurador, Personagem alvo, EstadoCombate estado, CombatManager manager) {
	}

	public abstract void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			AcaoMestreInput input, CombatManager manager);
}
