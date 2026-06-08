package br.com.dantesrpg.model.racas;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.Raça;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.util.ContratoDeVida;
import br.com.dantesrpg.model.util.ContratoDeVidaUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Humano extends Raça {

	private final String nome = "Humano";
	private final String descricaoPassiva = "Empréstimo: Luta com vida negativa. Paga com contratos.";

	public enum EstadoEmprestimo {
		NENHUM, ATIVO, PENDENTE_RESOLUCAO, RECUPERADO
	}

	private EstadoEmprestimo estadoAtual = EstadoEmprestimo.NENHUM;

	/**
	 * Fila de contratos aguardando ativação (em percentual do HP base).
	 * Após o teste de sucesso, gera vários deles; cada um vira um {@link ContratoDeVida}
	 * na lista central quando ativado via {@link #avancarProximoContrato(Personagem)}.
	 */
	private Queue<Double> filaContratos = new LinkedList<>();

	private double vidaNegativada = 0;

	@Override
	public String getNome() {
		return nome;
	}

	@Override
	public String getDescricaoPassiva() {
		if (isV2) {
			return "Overtime: Permanece com HP negativo durante o combate. Ao fim, a dívida vira contratos. Contratos geram escudo de sangue; cada 10 de escudo de sangue concede +5% de dano.";
		}
		return descricaoPassiva;
	}

	@Override
	public String getNomeV2() {
		return "Overtime";
	}

	@Override
	public List<Habilidade> getRacialAbilities(Personagem personagem) {
		return Collections.emptyList();
	}

	public EstadoEmprestimo getEstadoAtual() {
		return estadoAtual;
	}

	public int getContratosRestantes() {
		return filaContratos.size();
	}

	@Override
	public boolean onHpChangeAttempt(Personagem personagem, double vidaAntiga, double novaVida, EstadoCombate estado,
			CombatController controller) {
		if (isV2) {
			if (novaVida <= 0 && vidaAntiga > 0 && estadoAtual == EstadoEmprestimo.NENHUM) {
				iniciarEmprestimo(personagem);
				return true;
			}
			if (estadoAtual == EstadoEmprestimo.ATIVO && novaVida > 0) {
				resetarEstadoEmprestimo(personagem);
				return false;
			}
			if (estadoAtual == EstadoEmprestimo.ATIVO && novaVida <= 0) {
				return true;
			}
			return false;
		}

		// V1: Não permite empréstimo se já tem contratos RACIAIS (ativo ou fila).
		// Contratos de outras fontes (Bárbaro, Respirar, Sit in Balance) NÃO bloqueiam.
		if (!isV2) {
			boolean temRacialAtivo = ContratoDeVidaUtils.temContratoHumanoAtivo(personagem);
			if (temRacialAtivo || !filaContratos.isEmpty()) {
				if (novaVida <= 0)
					return false;
			}
		}
		if (novaVida <= 0 && vidaAntiga > 0 && estadoAtual == EstadoEmprestimo.NENHUM) {
			iniciarEmprestimo(personagem);
			return true;
		}
		if (estadoAtual == EstadoEmprestimo.ATIVO && novaVida > 0) {
			this.estadoAtual = EstadoEmprestimo.RECUPERADO;
			return false;
		}
		if (estadoAtual == EstadoEmprestimo.ATIVO && novaVida <= 0) {
			return true;
		}
		if (estadoAtual == EstadoEmprestimo.RECUPERADO && novaVida <= 0) {
			this.estadoAtual = EstadoEmprestimo.ATIVO;
			return true;
		}
		if (estadoAtual == EstadoEmprestimo.PENDENTE_RESOLUCAO && novaVida <= 0) {
			return true;
		}
		return false;
	}

	@Override
	public void onHpChanged(Personagem personagem, double vidaAntiga, double hpNovo, EstadoCombate estado,
			CombatController controller) {
		if (isV2) {
			if (estadoAtual == EstadoEmprestimo.ATIVO && hpNovo <= 0) {
				this.vidaNegativada = Math.abs(hpNovo);
			} else if (estadoAtual == EstadoEmprestimo.ATIVO && hpNovo > 0) {
				resetarEstadoEmprestimo(personagem);
			}
			return;
		}

		if (estadoAtual == EstadoEmprestimo.ATIVO && hpNovo <= 0) {
			this.vidaNegativada = Math.abs(hpNovo);
			if (this.vidaNegativada >= personagem.getVidaMaxima()) {
				this.estadoAtual = EstadoEmprestimo.PENDENTE_RESOLUCAO;
			}
		}
	}

	@Override
	public void onTimeAdvanced(Personagem personagem, EstadoCombate estado, CombatController controller) {
		if (isV2)
			return;

		if (estadoAtual == EstadoEmprestimo.ATIVO) {
			if (!personagem.getEfeitosAtivos().containsKey("Empréstimo")) {
				this.estadoAtual = EstadoEmprestimo.PENDENTE_RESOLUCAO;
			}
		}
	}

	@Override
	public double getBonusDanoPercentual(Personagem personagem) {
		if (isV2) {
			return Math.floor(personagem.getEscudoSangueAtual() / 10.0) * 0.05;
		}

		double bonus = 0.0;

		if (estadoAtual == EstadoEmprestimo.RECUPERADO)
			bonus += 0.50;
		else if (estadoAtual == EstadoEmprestimo.ATIVO) {
			double pctPerdida = this.vidaNegativada / personagem.getVidaMaximaBase();
			bonus += pctPerdida * 1.5;
		}

		return bonus;
	}

	public int getTotalContratosV2(Personagem personagem) {
		int count = filaContratos.size();
		if (personagem != null) {
			for (ContratoDeVida contrato : personagem.getContratosDeVida()) {
				if (contrato != null && contrato.isHumano()) {
					count++;
				}
			}
		}
		return count;
	}

	@Override
	public double getReducaoTUPercentual(Personagem personagem) {
		if (isV2)
			return 0.0;

		if (estadoAtual == EstadoEmprestimo.RECUPERADO)
			return 0.25;
		if (estadoAtual == EstadoEmprestimo.ATIVO) {
			double pctPerdida = this.vidaNegativada / personagem.getVidaMaximaBase();
			return Math.min(0.60, pctPerdida);
		}
		return 0.0;
	}

	@Override
	public double getReducaoHpMaximo(Personagem personagem) {
		return 0.0;
	}

	private void iniciarEmprestimo(Personagem p) {
		this.estadoAtual = EstadoEmprestimo.ATIVO;
		this.vidaNegativada = 0;
		int duracao = isV2 ? 99999 : 500;
		Efeito emp = new Efeito("Empréstimo", TipoEfeito.BUFF, duracao, null, 0, 0);
		p.adicionarEfeito(emp);
		if (isV2) {
			System.out.println(">>> OVERTIME: Empréstimo iniciado sem limite de TU até o fim do combate.");
		} else {
			System.out.println(">>> HUMANO: Empréstimo Iniciado (500 TU).");
		}
	}

	public void resolverResultadoTeste(Personagem p, boolean sucesso, int valorRolado) {
		if (isV2)
			return;

		if (estadoAtual == EstadoEmprestimo.RECUPERADO) {
			System.out.println(">>> HUMANO: Sobreviveu por recuperação. Bônus encerrado.");
			resetarEstadoEmprestimo(p);
			p.recalcularAtributosEstatisticas();
			return;
		}
		if (sucesso) {
			System.out.println(">>> HUMANO: Sucesso no Teste. Gerando Contratos...");
			gerarContratos(p);
			resetarEstadoEmprestimo(p);
			p.setVidaAtual(1);
			avancarProximoContrato(p);
		} else {
			System.out.println(">>> HUMANO: Falha no Teste. Morte.");
			p.setVidaAtual(0);
		}
	}

	@Override
	public void onCombatStart(Personagem personagem, EstadoCombate estado) {
		if (!isV2)
			return;

		int totalContratos = getTotalContratosV2(personagem);
		if (totalContratos > 0) {
			double escudo = personagem.getVidaMaxima() * 0.20 * totalContratos;
			personagem.adicionarEscudoSangue(escudo);
			System.out.println(">>> OVERTIME: Escudo de Sangue +" + (int) escudo + " (" + totalContratos
					+ " contratos x 20% HP).");
		}
	}

	public void encerrarCombateOvertime(Personagem p) {
		if (!isV2)
			return;

		double dividaFinal = Math.max(this.vidaNegativada, Math.max(0, -p.getVidaAtual()));
		if (dividaFinal <= 0) {
			if (estadoAtual == EstadoEmprestimo.ATIVO) {
				resetarEstadoEmprestimo(p);
			}
			return;
		}

		if (dividaFinal > 0) {
			gerarContratosDiretos(p, dividaFinal);
			System.out.println(">>> OVERTIME: Dívida final de " + (int) dividaFinal
					+ " HP convertida em Contratos de Vida.");
		}

		resetarEstadoEmprestimo(p);
		p.setVidaAtualInterno(Math.min(1.0, p.getVidaMaxima()));
		p.recalcularAtributosEstatisticas();
	}

	private void gerarContratos(Personagem p) {
		double dividaTotal = this.vidaNegativada;
		double maxHpBase = p.getVidaMaximaBase();
		double limiteContrato = maxHpBase * 0.40;

		while (dividaTotal > 0) {
			double valorDesteContrato = Math.min(dividaTotal, limiteContrato);
			double pct = valorDesteContrato / maxHpBase;
			this.filaContratos.add(pct);
			dividaTotal -= valorDesteContrato;
		}
	}

	private void gerarContratosDiretos(Personagem p, double dividaTotal) {
		double maxHpBase = p.getVidaMaximaBase();
		double limiteContrato = maxHpBase * 0.40;

		while (dividaTotal > 0) {
			double valorDesteContrato = Math.min(dividaTotal, limiteContrato);
			ContratoDeVida novo = new ContratoDeVida(ContratoDeVida.FONTE_HUMANO, valorDesteContrato, -1, true);
			ContratoDeVidaUtils.adicionarContrato(p, novo);
			dividaTotal -= valorDesteContrato;
		}
	}

	/**
	 * Ativa o próximo contrato da fila racial.
	 * Se já houver um contrato humano ativo, não faz nada.
	 */
	public void avancarProximoContrato(Personagem p) {
		if (ContratoDeVidaUtils.temContratoHumanoAtivo(p))
			return;
		if (!filaContratos.isEmpty()) {
			double pctProximo = filaContratos.poll();
			double valor = p.getVidaMaximaBase() * pctProximo;
			ContratoDeVida novo = new ContratoDeVida(ContratoDeVida.FONTE_HUMANO, valor, -1, true);
			ContratoDeVidaUtils.adicionarContrato(p, novo);
			System.out.println(">>> HUMANO: Novo Contrato Ativado (-" + (int) valor + " HP).");
		}
	}

	private void resetarEstadoEmprestimo(Personagem p) {
		this.estadoAtual = EstadoEmprestimo.NENHUM;
		this.vidaNegativada = 0;
		p.removerEfeito("Empréstimo");
		p.recalcularAtributosEstatisticas();
	}

	public int calcularDificuldadeTeste(Personagem p) {
		double pct = this.vidaNegativada / p.getVidaMaximaBase();
		if (pct <= 0.3)
			return 6;
		if (pct <= 0.6)
			return 8;
		if (pct <= 0.9)
			return 10;
		return 15;
	}

	public String getTextoDificuldade(int na) {
		if (na <= 6)
			return "Média";
		if (na <= 8)
			return "Difícil";
		if (na <= 10)
			return "Extrema";
		return "Quase Impossível";
	}

	/** Dívida pendente do contrato humano racial atualmente ativo. */
	public double getDividaPendente() {
		return 0;
	}

	public Queue<Double> getFilaContratos() {
		return this.filaContratos;
	}

	public double getVidaNegativaAcumulada() {
		return this.vidaNegativada;
	}

	public void setFilaContratos(List<Double> listaSalva) {
		this.filaContratos.clear();
		if (listaSalva != null) {
			this.filaContratos.addAll(listaSalva);
		}
	}

	public void setVidaNegativaAcumulada(double valor) {
		this.vidaNegativada = valor;
	}

	/** Retorna 0; valor total do contrato racial ativo é armazenado na lista central. */
	public double getContratoAtivoValorTotal() {
		return 0;
	}

	public void setContratoAtivoValorTotal(double valor) {
		// Compat: carregamento antigo aplica o contrato no CombatController.
	}

	public double getContratoAtivoDividaRestante() {
		return 0;
	}

	public void setContratoAtivoDividaRestante(double valor) {
		// Compat: carregamento antigo aplica o contrato no CombatController.
	}

	public void setEstadoAtual(EstadoEmprestimo estado) {
		this.estadoAtual = estado;
	}
}
