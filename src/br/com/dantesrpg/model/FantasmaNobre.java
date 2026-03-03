package br.com.dantesrpg.model;

import br.com.dantesrpg.model.enums.TipoAlvo;
import java.util.List;

public abstract class FantasmaNobre {

	// Para a UI (Botão e Tooltip)
	public abstract String getNome();

	public abstract String getDescricao();

	// Para a Lógica de Ativação (Custos)
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
	 * @return O número de alvos.
	 */
	public abstract int getNumeroDeAlvos();

	public List<String> getOpcoesSelection() {
		return null; // Padrão: sem opções
	}

	public abstract void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			AcaoMestreInput input, CombatManager manager);
}