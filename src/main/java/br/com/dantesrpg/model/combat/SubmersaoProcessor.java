package br.com.dantesrpg.model.combat;

import java.util.Set;

import br.com.dantesrpg.controller.MapController;
import br.com.dantesrpg.model.Armadura;
import br.com.dantesrpg.model.Amuleto;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.util.EffectFactory;

/** Processa oxigênio e Asfixia para jogadores em água profunda. */
public final class SubmersaoProcessor {

	public static final String EFEITO_ASFIXIA = "Asfixia";
	private static final Set<String> ARMADURAS_COM_OXIGENIO_PRESERVADO = Set.of(
			"Armadura de Mergulho",
			"Armadura de Mergulho Militar",
			"Traje Tático \"Nautilus MK-I\"",
			"Traje Tático \"Nautilus MK-II\"",
			"Armadura Leviatã");
	private static final Set<String> PINGENTES_ECONOMIZADORES_DE_OXIGENIO = Set.of(
			"Ampulheta das Marés",
			"Medalhão da Maré",
			"Coração de Coral Vivo");

	private SubmersaoProcessor() {
	}

	public static void processarTick(Personagem jogador, MapController mapa, int tempoGlobalAtual) {
		if (jogador == null || mapa == null) {
			return;
		}

		if (!mapa.estaEmAguaProfunda(jogador)) {
			recuperarOxigenio(jogador, tempoGlobalAtual);
			return;
		}

		consumirOxigenio(jogador, tempoGlobalAtual);
		atualizarAsfixia(jogador);
	}

	private static void consumirOxigenio(Personagem jogador, int tempoGlobalAtual) {
		if (possuiArmaduraComOxigenioPreservado(jogador)
				|| tempoGlobalAtual % 20 != 0
				|| jogador.getOxigenio() <= 0) {
			return;
		}

		int consumo = possuiPingenteEconomizador(jogador) ? 4 : 5;
		jogador.setOxigenio(jogador.getOxigenio() - consumo);
	}

	private static void recuperarOxigenio(Personagem jogador, int tempoGlobalAtual) {
		if (tempoGlobalAtual % 10 != 0 || jogador.getOxigenio() >= 100) {
			return;
		}

		jogador.setOxigenio(jogador.getOxigenio() + 10);
		if (jogador.getOxigenio() > 0) {
			jogador.removerEfeito(EFEITO_ASFIXIA);
		}
	}

	private static void atualizarAsfixia(Personagem jogador) {
		if (jogador.getOxigenio() > 0) {
			jogador.removerEfeito(EFEITO_ASFIXIA);
			return;
		}
		if (!jogador.getEfeitosAtivos().containsKey(EFEITO_ASFIXIA)) {
			jogador.adicionarEfeito(EffectFactory.criarEfeito(EFEITO_ASFIXIA, 99999, 0));
		}
	}

	private static boolean possuiArmaduraComOxigenioPreservado(Personagem jogador) {
		Armadura armadura = jogador.getArmaduraEquipada();
		return armadura != null && ARMADURAS_COM_OXIGENIO_PRESERVADO.contains(armadura.getNome());
	}

	private static boolean possuiPingenteEconomizador(Personagem jogador) {
		return nomeEhPingenteEconomizador(jogador.getAmuleto1())
				|| nomeEhPingenteEconomizador(jogador.getAmuleto2());
	}

	private static boolean nomeEhPingenteEconomizador(Amuleto amuleto) {
		return amuleto != null && PINGENTES_ECONOMIZADORES_DE_OXIGENIO.contains(amuleto.getNome());
	}
}
