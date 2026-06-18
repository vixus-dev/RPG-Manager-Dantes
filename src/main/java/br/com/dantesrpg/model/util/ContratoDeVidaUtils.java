package br.com.dantesrpg.model.util;

import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoEfeito;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Gerencia o sistema unificado de Contratos de Vida.
 *
 * Regras:
 * <ul>
 *   <li>Contratos de qualquer fonte (Humano racial, Bárbaro, Respirar, Sit in Balance)
 *       coexistem na lista {@link Personagem#getContratosDeVida()}.</li>
 *   <li>Contratos do Humano racial funcionam em FILA: apenas o primeiro reduz HP máximo
 *       por vez (os demais ficam aguardando).</li>
 *   <li>Contratos de outras fontes acumulam todos simultaneamente (sem teto de 40%).
 *       A soma pode ultrapassar 100% do HP base.</li>
 *   <li>Quando a soma das reduções atinge 100% do HP base, qualquer dano direto ao HP
 *       (não absorvido por escudo) é letal.</li>
 *   <li>Cura excedente paga as dívidas em ordem FIFO.</li>
 *   <li>Contratos não persistentes são removidos no fim do combate.</li>
 * </ul>
 */
public final class ContratoDeVidaUtils {

	public static final String EFEITO_VISUAL = "Contrato de Vida";
	public static final String EFEITO_BARBARO_BUFF = "Raiva Imparável";

	private static final double TAMANHO_STACK_BARBARO_PCT = 0.05;
	private static final double BONUS_DANO_POR_STACK_BARBARO = 0.25;

	private ContratoDeVidaUtils() {
	}

	// ========== ADIÇÃO ==========

	/**
	 * Adiciona um novo contrato à lista FIFO do personagem.
	 * Dispara recalculo de atributos (teto de HP é atualizado).
	 */
	public static void adicionarContrato(Personagem p, ContratoDeVida contrato) {
		if (p == null || contrato == null || contrato.getValorTotal() <= 0)
			return;
		p.getContratosDeVida().add(contrato);
		sincronizarEfeitoVisual(p);
		if (contrato.isBarbaro() && contrato.getStacksBarbaroIniciais() > 0) {
			atualizarBuffBarbaro(p, contrato);
		}
		p.recalcularAtributosEstatisticas();
		if (p.getVidaAtual() > p.getVidaMaxima()) {
			p.setVidaAtualInterno(p.getVidaMaxima());
		}
	}

	/**
	 * Remove um contrato diretamente da fila do personagem.
	 * Dispara recalculo de atributos e limpa buffs associados.
	 */
	public static void removerContrato(Personagem p, ContratoDeVida contrato) {
		if (p == null || contrato == null)
			return;
		p.getContratosDeVida().remove(contrato);
		if (contrato.isBarbaro()) {
			removerBuffBarbaro(p);
		}
		sincronizarEfeitoVisual(p);
		p.recalcularAtributosEstatisticas();
	}

	// ========== CONSULTAS ==========

	/**
	 * Redução total de HP máximo aplicada agora.
	 * Para humanos: apenas o primeiro contrato racial da fila conta.
	 * Para outras fontes: todos somam.
	 */
	public static double getReducaoHpMaximoTotal(Personagem p) {
		if (p == null)
			return 0;
		double total = 0;
		boolean humanoAtivoVisto = false;
		for (ContratoDeVida c : p.getContratosDeVida()) {
			if (c.isHumano()) {
				if (!humanoAtivoVisto) {
					total += c.getDividaRestante();
					humanoAtivoVisto = true;
				}
			} else {
				total += c.getDividaRestante();
			}
		}
		return total;
	}

	/** Dívida pendente do contrato humano atualmente ativo (primeiro da fila), ou 0. */
	public static double getDividaHumanoAtivo(Personagem p) {
		if (p == null)
			return 0;
		for (ContratoDeVida c : p.getContratosDeVida()) {
			if (c.isHumano())
				return c.getDividaRestante();
		}
		return 0;
	}

	/** Valor total do contrato humano atualmente ativo (primeiro da fila), ou 0. */
	public static double getValorHumanoAtivo(Personagem p) {
		if (p == null)
			return 0;
		for (ContratoDeVida c : p.getContratosDeVida()) {
			if (c.isHumano())
				return c.getValorTotal();
		}
		return 0;
	}

	/** Número de contratos humanos aguardando na fila (excluindo o ativo). */
	public static int getContratosHumanosNaFila(Personagem p) {
		if (p == null)
			return 0;
		int total = 0;
		for (ContratoDeVida c : p.getContratosDeVida()) {
			if (c.isHumano())
				total++;
		}
		return Math.max(0, total - 1);
	}

	public static boolean temContratoHumanoAtivo(Personagem p) {
		if (p == null)
			return false;
		for (ContratoDeVida c : p.getContratosDeVida()) {
			if (c.isHumano())
				return true;
		}
		return false;
	}

	public static boolean temContrato(Personagem p) {
		return p != null && !p.getContratosDeVida().isEmpty();
	}

	/** True quando a soma das reduções atinge o HP base → qualquer dano é letal. */
	public static boolean estaSobrecarregado(Personagem p) {
		if (p == null)
			return false;
		return getReducaoHpMaximoTotal(p) >= p.getVidaMaximaBase() - 0.01;
	}

	public static ContratoDeVida getContratoBarbaro(Personagem p) {
		if (p == null)
			return null;
		for (ContratoDeVida c : p.getContratosDeVida()) {
			if (c.isBarbaro())
				return c;
		}
		return null;
	}

	public static ContratoDeVida getContratoPorFonte(Personagem p, String fonte) {
		if (p == null || fonte == null)
			return null;
		for (ContratoDeVida c : p.getContratosDeVida()) {
			if (fonte.equals(c.getFonte()))
				return c;
		}
		return null;
	}

	// ========== PAGAMENTO ==========

	/**
	 * Paga dívida em ordem FIFO. Apenas contratos ativos recebem pagamento:
	 * - Para humanos, apenas o primeiro contrato racial recebe (os outros ficam na fila).
	 * - Para outras fontes, todos são elegíveis na ordem de inserção.
	 *
	 * Retorna o valor restante (troco) que não foi usado pois todas as dívidas já
	 * estavam quitadas.
	 */
	public static double pagarDivida(Personagem p, double valor) {
		if (p == null || valor <= 0)
			return valor;

		double restante = valor;
		List<ContratoDeVida> lista = p.getContratosDeVida();

		// Itera em ordem; ao quitar um humano, o próximo humano passa a ser o ativo.
		boolean humanoAtivoPagoNestaIteracao = false;
		int i = 0;
		while (i < lista.size() && restante > 0) {
			ContratoDeVida c = lista.get(i);
			boolean ehHumano = c.isHumano();
			if (ehHumano && humanoAtivoPagoNestaIteracao) {
				// Já pagamos o humano ativo e ele ainda não quitou → fila espera.
				i++;
				continue;
			}

			double pagamento = Math.min(restante, c.getDividaRestante());
			if (pagamento > 0) {
				c.setDividaRestante(c.getDividaRestante() - pagamento);
				restante -= pagamento;

				if (c.isBarbaro() && c.getStacksBarbaroIniciais() > 0) {
					atualizarBuffBarbaro(p, c);
				}
			}

			if (c.getDividaRestante() <= 0.001) {
				// Quitou: remove e continua na mesma posição (lista encolhe).
				if (c.isBarbaro())
					removerBuffBarbaro(p);
				lista.remove(i);
				if (ehHumano) {
					// Próximo humano (se existir) passa a ser o ativo nesta iteração.
					humanoAtivoPagoNestaIteracao = false;
				}
				// Não incrementa i — próxima iteração olha o que "deslizou" para cá.
			} else {
				if (ehHumano)
					humanoAtivoPagoNestaIteracao = true;
				i++;
			}
		}

		sincronizarEfeitoVisual(p);
		p.recalcularAtributosEstatisticas();
		return restante;
	}

	// ========== CICLO DE COMBATE ==========

	/** Remove todos os contratos não-persistentes (Bárbaro, Respirar, Sit in Balance). */
	public static void limparEfemeros(Personagem p) {
		if (p == null)
			return;
		Iterator<ContratoDeVida> it = p.getContratosDeVida().iterator();
		boolean removeuBarbaro = false;
		while (it.hasNext()) {
			ContratoDeVida c = it.next();
			if (!c.persisteAposCombate()) {
				if (c.isBarbaro())
					removeuBarbaro = true;
				it.remove();
			}
		}
		if (removeuBarbaro)
			removerBuffBarbaro(p);
		sincronizarEfeitoVisual(p);
		p.recalcularAtributosEstatisticas();
	}

	// ========== BÁRBARO ==========

	/** Recalcula stacks do buff bárbaro proporcionalmente à dívida restante. */
	private static int calcularStacksAtuaisBarbaro(ContratoDeVida c) {
		if (c.getValorTotal() <= 0)
			return 0;
		double ratio = c.getDividaRestante() / c.getValorTotal();
		int stacks = (int) Math.round(c.getStacksBarbaroIniciais() * ratio);
		return Math.max(0, stacks);
	}

	private static void atualizarBuffBarbaro(Personagem p, ContratoDeVida c) {
		int stacks = calcularStacksAtuaisBarbaro(c);
		if (stacks <= 0) {
			removerBuffBarbaro(p);
			return;
		}
		double bonusDano = stacks * BONUS_DANO_POR_STACK_BARBARO;
		Map<String, Double> mods = new HashMap<>();
		mods.put("DANO_BONUS_PERCENTUAL", bonusDano);

		Efeito buff = p.getEfeitosAtivos().get(EFEITO_BARBARO_BUFF);
		if (buff == null || buff.getModificadores() == null) {
			if (buff != null)
				p.removerEfeito(EFEITO_BARBARO_BUFF);
			Efeito novo = new Efeito(EFEITO_BARBARO_BUFF, TipoEfeito.BUFF, 99999, mods, 0, 0);
			novo.setStacks(stacks);
			p.adicionarEfeito(novo);
		} else {
			buff.getModificadores().clear();
			buff.getModificadores().putAll(mods);
			buff.setStacks(stacks);
			buff.setDuracaoTURestante(99999);
		}
	}

	private static void removerBuffBarbaro(Personagem p) {
		if (p.getEfeitosAtivos().containsKey(EFEITO_BARBARO_BUFF)) {
			p.removerEfeito(EFEITO_BARBARO_BUFF);
		}
	}

	public static int getStacksBarbaro(Personagem p) {
		ContratoDeVida c = getContratoBarbaro(p);
		return (c != null) ? calcularStacksAtuaisBarbaro(c) : 0;
	}

	public static int getBonusDanoPercentualBarbaro(Personagem p) {
		return (int) (getStacksBarbaro(p) * BONUS_DANO_POR_STACK_BARBARO * 100);
	}

	public static int getPercentualContratoBarbaro(Personagem p) {
		return (int) (getStacksBarbaro(p) * TAMANHO_STACK_BARBARO_PCT * 100);
	}

	// ========== EFEITO VISUAL (marca para tooltip/UI) ==========

	/**
	 * Cria/remove um efeito sem modificadores chamado "Contrato de Vida",
	 * apenas para aparecer na lista de efeitos e tooltips. A redução efetiva
	 * do HP máximo é aplicada em {@link Personagem#recalcularAtributosEstatisticas()}
	 * via {@link #getReducaoHpMaximoTotal(Personagem)}.
	 */
	private static void sincronizarEfeitoVisual(Personagem p) {
		boolean temAtivo = getReducaoHpMaximoTotal(p) > 0;
		Efeito existente = p.getEfeitosAtivos().get(EFEITO_VISUAL);
		if (temAtivo) {
			if (existente == null) {
				Efeito novo = new Efeito(EFEITO_VISUAL, TipoEfeito.DEBUFF, 99999, null, 0, 0);
				p.adicionarEfeito(novo);
			} else {
				existente.setDuracaoTURestante(99999);
			}
		} else if (existente != null) {
			p.removerEfeito(EFEITO_VISUAL);
		}
	}
}
