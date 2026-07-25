package br.com.dantesrpg.model.util;

import java.util.List;
import java.util.Map;

import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoEfeito;

/** Regras de vínculo entre um Ídolo e o inimigo abençoado por ele. */
public final class IdoloUtils {

	public static final String PROPRIEDADE_IDOLO = "IDOLO";
	public static final String PROPRIEDADE_SEM_TURNO = "SEM_TURNO";
	public static final String NOME_BENCAO = "Bênção do Ídolo";
	private static final String PREFIXO_VINCULO_BENCAO = "BENCAO_IDOLO:";

	private IdoloUtils() {
	}

	public static boolean isIdolo(Personagem personagem) {
		return personagem != null && personagem.temPropriedade(PROPRIEDADE_IDOLO);
	}

	public static boolean naoPossuiTurno(Personagem personagem) {
		return personagem != null && personagem.temPropriedade(PROPRIEDADE_SEM_TURNO);
	}

	public static boolean temBencao(Personagem personagem) {
		return personagem != null && personagem.getEfeitosAtivos().containsKey(NOME_BENCAO);
	}

	public static void aplicarBencao(Personagem idolo, Personagem alvo) {
		if (!isIdolo(idolo) || alvo == null) {
			return;
		}

		alvo.adicionarEfeito(new Efeito(NOME_BENCAO, TipoEfeito.BUFF, Integer.MAX_VALUE, Map.of(), 0, 0));
		alvo.adicionarPropriedade(PREFIXO_VINCULO_BENCAO + idolo.getNome());
		System.out.println(">>> ÍDOLO: " + alvo.getNome() + " recebeu a Bênção do Ídolo e está imune a dano.");
	}

	/** Remove somente a bênção concedida pelo Ídolo que acabou de morrer. */
	public static void removerBencaoConcedidaPor(Personagem idolo, EstadoCombate estado) {
		if (!isIdolo(idolo) || estado == null) {
			return;
		}

		String vinculoDoIdolo = PREFIXO_VINCULO_BENCAO + idolo.getNome();
		for (Personagem personagem : estado.getCombatentes()) {
			if (personagem == null || !personagem.getPropriedades().remove(vinculoDoIdolo)) {
				continue;
			}
			if (!possuiOutroVinculoDeIdolo(personagem.getPropriedades())) {
				personagem.removerEfeito(NOME_BENCAO);
				System.out.println(">>> ÍDOLO DESTRUÍDO: a Bênção do Ídolo foi removida de "
						+ personagem.getNome() + ".");
			}
		}
	}

	private static boolean possuiOutroVinculoDeIdolo(List<String> propriedades) {
		return propriedades.stream().anyMatch(propriedade -> propriedade.startsWith(PREFIXO_VINCULO_BENCAO));
	}
}
