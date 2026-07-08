package br.com.dantesrpg.model.util;

import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoEfeito;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Gerencia o sistema unificado de Maldições de Personagens.
 */
public final class MaldicaoUtils {

	public static final String EFEITO_VISUAL = "Maldição";

	private MaldicaoUtils() {
	}

	/**
	 * Adiciona uma nova maldição à lista do personagem, atualiza o efeito visual
	 * e força recálculo de status.
	 */
	public static void adicionarMaldicao(Personagem p, Maldicao maldicao) {
		if (p == null || maldicao == null || maldicao.getPercentual() <= 0)
			return;

		p.getMaldicoes().add(maldicao);
		sincronizarEfeitoVisual(p);
		p.recalcularAtributosEstatisticas();

		if (p.getVidaAtual() > p.getVidaMaxima()) {
			p.setVidaAtualInterno(p.getVidaMaxima());
		}
	}

	/**
	 * Remove uma maldição do personagem, atualiza o efeito visual e força recálculo.
	 */
	public static void removerMaldicao(Personagem p, Maldicao maldicao) {
		if (p == null || maldicao == null)
			return;

		p.getMaldicoes().remove(maldicao);
		sincronizarEfeitoVisual(p);
		p.recalcularAtributosEstatisticas();
	}

	/**
	 * Remove todas as maldições ativas do personagem (purificação).
	 */
	public static void purificarMaldicoes(Personagem p) {
		if (p == null)
			return;

		p.getMaldicoes().clear();
		sincronizarEfeitoVisual(p);
		p.recalcularAtributosEstatisticas();
	}

	/**
	 * Retorna a soma de todos os percentuais de maldições ativas (ex: 0.25 para 25%).
	 */
	public static double getReducaoPercentualTotal(Personagem p) {
		if (p == null)
			return 0.0;

		double total = 0.0;
		for (Maldicao m : p.getMaldicoes()) {
			total += m.getPercentual();
		}
		return total;
	}

	/**
	 * Retorna o valor absoluto de redução de HP máximo provocado pelas maldições.
	 */
	public static double getReducaoHpMaximoTotal(Personagem p) {
		if (p == null)
			return 0.0;

		return p.getVidaMaximaBase() * getReducaoPercentualTotal(p);
	}

	/**
	 * Retorna se o personagem possui alguma maldição ativa.
	 */
	public static boolean temMaldicao(Personagem p) {
		return p != null && !p.getMaldicoes().isEmpty();
	}

	/**
	 * Remove todas as maldições que não persistem após o combate.
	 */
	public static void limparEfemeros(Personagem p) {
		if (p == null)
			return;

		Iterator<Maldicao> it = p.getMaldicoes().iterator();
		boolean mudou = false;
		while (it.hasNext()) {
			Maldicao m = it.next();
			if (!m.persisteAposCombate()) {
				it.remove();
				mudou = true;
			}
		}

		if (mudou) {
			sincronizarEfeitoVisual(p);
			p.recalcularAtributosEstatisticas();
		}
	}

	/**
	 * Sincroniza o Efeito de debuff de "Maldição" na lista de efeitos ativos do
	 * personagem para fins de renderização de badges e tooltips. Os stacks
	 * representam o valor total em porcentagem.
	 */
	public static void sincronizarEfeitoVisual(Personagem p) {
		double pctTotal = getReducaoPercentualTotal(p);
		boolean temAtivo = pctTotal > 0.0;
		Efeito existente = p.getEfeitosAtivos().get(EFEITO_VISUAL);

		if (temAtivo) {
			int pctInt = (int) Math.round(pctTotal * 100);
			if (existente == null) {
				Efeito novo = new Efeito(EFEITO_VISUAL, TipoEfeito.DEBUFF, 99999, null, 0, 0);
				novo.setStacks(pctInt);
				p.adicionarEfeito(novo);
			} else {
				existente.setStacks(pctInt);
				existente.setDuracaoTURestante(99999);
			}
		} else if (existente != null) {
			p.removerEfeito(EFEITO_VISUAL);
		}
	}

	/**
	 * Decrementa a duração de todas as maldições ativas em deltaTU.
	 * Chamado a cada tick de avanço de tempo no combate.
	 */
	public static void avancarTempoMaldicoes(Personagem p, int deltaTU) {
		if (p == null || deltaTU <= 0)
			return;

		List<Maldicao> lista = p.getMaldicoes();
		if (lista.isEmpty())
			return;

		Iterator<Maldicao> it = new ArrayList<>(lista).iterator(); // Evita ConcurrentModificationException
		boolean mudou = false;
		while (it.hasNext()) {
			Maldicao m = it.next();
			if (m.getDuracaoTURestante() > 0) {
				m.setDuracaoTURestante(m.getDuracaoTURestante() - deltaTU);
				if (m.getDuracaoTURestante() <= 0) {
					lista.remove(m); // Remove da lista original
					mudou = true;
				}
			}
		}

		if (mudou) {
			sincronizarEfeitoVisual(p);
			p.recalcularAtributosEstatisticas();
		}
	}
}
