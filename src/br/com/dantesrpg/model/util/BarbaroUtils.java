package br.com.dantesrpg.model.util;

import java.util.HashMap;
import java.util.Map;

import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoEfeito;

public final class BarbaroUtils {

	public static final String EFEITO_CONTRATO = "Contrato Bárbaro";
	public static final String EFEITO_BALANCO_TEMERARIO = "Balanço Temerário";

	private static final int DURACAO_CONTRATO = 99999;
	private static final int STACKS_POR_USO = 2; // 10% do HP máximo = 2 segmentos de 5%
	private static final int MAX_STACKS = 16; // 80% do HP máximo

	private BarbaroUtils() {
	}

	public static void acumularContrato(Personagem personagem) {
		if (personagem == null) {
			return;
		}

		int stacksAtuais = getStacksContrato(personagem);
		int novosStacks = Math.min(MAX_STACKS, stacksAtuais + STACKS_POR_USO);
		atualizarContrato(personagem, novosStacks);
	}

	public static void encerrarContrato(Personagem personagem) {
		if (personagem == null || !temContratoAtivo(personagem)) {
			return;
		}

		personagem.removerEfeito(EFEITO_CONTRATO);
		personagem.recalcularAtributosEstatisticas();
	}

	public static boolean temContratoAtivo(Personagem personagem) {
		return personagem != null && personagem.getEfeitosAtivos().containsKey(EFEITO_CONTRATO);
	}

	public static int getStacksContrato(Personagem personagem) {
		if (!temContratoAtivo(personagem)) {
			return 0;
		}

		Efeito efeito = personagem.getEfeitosAtivos().get(EFEITO_CONTRATO);
		return (efeito != null) ? efeito.getStacks() : 0;
	}

	public static int getPercentualContrato(Personagem personagem) {
		return getStacksContrato(personagem) * 5;
	}

	public static int getBonusDanoPercentual(Personagem personagem) {
		return getStacksContrato(personagem) * 25;
	}

	private static void atualizarContrato(Personagem personagem, int stacks) {
		if (stacks <= 0) {
			encerrarContrato(personagem);
			return;
		}

		double vidaMaximaSemContrato = calcularVidaMaximaSemContrato(personagem);
		double percentualContrato = stacks * 0.05;
		double valorContrato = vidaMaximaSemContrato * percentualContrato;
		double bonusDano = stacks * 0.25;

		Map<String, Double> modificadores = new HashMap<>();
		modificadores.put("HP_MAXIMO", -valorContrato);
		modificadores.put("DANO_BONUS_PERCENTUAL", bonusDano);

		Efeito efeitoExistente = personagem.getEfeitosAtivos().get(EFEITO_CONTRATO);

		if (efeitoExistente == null || efeitoExistente.getModificadores() == null) {
			if (efeitoExistente != null) {
				personagem.removerEfeito(EFEITO_CONTRATO);
			}

			Efeito novoEfeito = new Efeito(EFEITO_CONTRATO, TipoEfeito.BUFF, DURACAO_CONTRATO, modificadores, 0, 0);
			novoEfeito.setStacks(stacks);
			personagem.adicionarEfeito(novoEfeito);
		} else {
			efeitoExistente.getModificadores().clear();
			efeitoExistente.getModificadores().putAll(modificadores);
			efeitoExistente.setStacks(stacks);
			efeitoExistente.setDuracaoTURestante(DURACAO_CONTRATO);
		}

		personagem.recalcularAtributosEstatisticas();
	}

	private static double calcularVidaMaximaSemContrato(Personagem personagem) {
		if (!temContratoAtivo(personagem)) {
			return personagem.getVidaMaxima();
		}

		Efeito efeito = personagem.getEfeitosAtivos().get(EFEITO_CONTRATO);
		if (efeito == null || efeito.getModificadores() == null) {
			return personagem.getVidaMaxima();
		}

		double reducaoAtual = -efeito.getModificadores().getOrDefault("HP_MAXIMO", 0.0);
		return personagem.getVidaMaxima() + reducaoAtual;
	}
}
