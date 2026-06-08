package br.com.dantesrpg.model.util;

import br.com.dantesrpg.model.Personagem;

/**
 * Fachada para o Contrato Bárbaro (Raiva Imparável).
 * A partir da unificação de contratos, toda a mecânica vive em
 * {@link ContratoDeVidaUtils}; este arquivo mantém os nomes antigos
 * para compatibilidade com código existente.
 */
public final class BarbaroUtils {

	public static final String EFEITO_CONTRATO = ContratoDeVidaUtils.EFEITO_BARBARO_BUFF;
	public static final String EFEITO_BALANCO_TEMERARIO = "Balanço Temerário";

	private static final int STACKS_POR_USO = 2;
	private static final int MAX_STACKS = 16;

	private BarbaroUtils() {
	}

	/** Acumula stacks no contrato Bárbaro, respeitando o limite de 16 (80% HP). */
	public static void acumularContrato(Personagem personagem) {
		if (personagem == null)
			return;

		ContratoDeVida existente = ContratoDeVidaUtils.getContratoBarbaro(personagem);
		int stacksAtuais = (existente != null) ? existente.getStacksBarbaroIniciais() : 0;
		int novosStacks = Math.min(MAX_STACKS, stacksAtuais + STACKS_POR_USO);
		if (novosStacks == stacksAtuais)
			return;

		double percentualTotal = novosStacks * 0.05;
		double valor = personagem.getVidaMaximaBase() * percentualTotal;

		if (existente != null) {
			// Remove o antigo e cria o novo com a dívida cheia (acumula do zero após incrementar).
			personagem.getContratosDeVida().remove(existente);
		}
		ContratoDeVida novo = new ContratoDeVida(ContratoDeVida.FONTE_BARBARO, valor, -1, false);
		novo.setStacksBarbaroIniciais(novosStacks);
		ContratoDeVidaUtils.adicionarContrato(personagem, novo);
	}

	/** Encerra o contrato bárbaro (usado no fim do combate). */
	public static void encerrarContrato(Personagem personagem) {
		if (personagem == null || !temContratoAtivo(personagem))
			return;
		ContratoDeVida c = ContratoDeVidaUtils.getContratoBarbaro(personagem);
		if (c != null) {
			personagem.getContratosDeVida().remove(c);
		}
		if (personagem.getEfeitosAtivos().containsKey(EFEITO_CONTRATO)) {
			personagem.removerEfeito(EFEITO_CONTRATO);
		}
		personagem.recalcularAtributosEstatisticas();
	}

	public static boolean temContratoAtivo(Personagem personagem) {
		return ContratoDeVidaUtils.getContratoBarbaro(personagem) != null;
	}

	public static int getStacksContrato(Personagem personagem) {
		return ContratoDeVidaUtils.getStacksBarbaro(personagem);
	}

	public static int getPercentualContrato(Personagem personagem) {
		return ContratoDeVidaUtils.getPercentualContratoBarbaro(personagem);
	}

	public static int getBonusDanoPercentual(Personagem personagem) {
		return ContratoDeVidaUtils.getBonusDanoPercentualBarbaro(personagem);
	}
}
