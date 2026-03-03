package br.com.dantesrpg.model.racas;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.Raça;
import br.com.dantesrpg.model.enums.TipoEfeito;

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

	private Queue<Double> filaContratos = new LinkedList<>();
	private double contratoAtivoValorTotal = 0; // Define o Teto (Redução de HP Máx)
	private double contratoAtivoDividaRestante = 0; // Define quanto falta pagar (Barra Vermelha)

	private double vidaNegativada = 0;

	@Override
	public String getNome() {
		return nome;
	}

	@Override
	public String getDescricaoPassiva() {
		return descricaoPassiva;
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
		if (hasContratoAtivo() || !filaContratos.isEmpty()) {
			if (novaVida <= 0)
				return false;
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
		return false;
	}

	@Override
	public void onHpChanged(Personagem personagem, double vidaAntiga, double hpNovo, EstadoCombate estado,
			CombatController controller) {
		if (estadoAtual == EstadoEmprestimo.ATIVO && hpNovo < 0) {
			this.vidaNegativada = Math.abs(hpNovo);
			if (this.vidaNegativada >= personagem.getVidaMaxima()) {
				this.estadoAtual = EstadoEmprestimo.PENDENTE_RESOLUCAO;
			}
		}
	}

	@Override
	public void onTimeAdvanced(Personagem personagem, EstadoCombate estado, CombatController controller) {
		if (estadoAtual == EstadoEmprestimo.ATIVO) {
			if (!personagem.getEfeitosAtivos().containsKey("Empréstimo")) {
				this.estadoAtual = EstadoEmprestimo.PENDENTE_RESOLUCAO;
			}
		}
	}

	@Override
	public double getBonusDanoPercentual(Personagem personagem) {
		if (estadoAtual == EstadoEmprestimo.RECUPERADO)
			return 0.50;
		if (estadoAtual == EstadoEmprestimo.ATIVO) {
			double pctPerdida = this.vidaNegativada / personagem.getVidaMaximaBase();
			return pctPerdida * 1.5;
		}
		return 0.0;
	}

	@Override
	public double getReducaoTUPercentual(Personagem personagem) {
		if (estadoAtual == EstadoEmprestimo.RECUPERADO)
			return 0.25;
		if (estadoAtual == EstadoEmprestimo.ATIVO) {
			double pctPerdida = this.vidaNegativada / personagem.getVidaMaximaBase();
			return Math.min(0.60, pctPerdida);
		}
		return 0.0;
	}

	// --- SISTEMA DE CONTRATOS (LÓGICA NOVA) ---

	@Override
	public double getReducaoHpMaximo(Personagem personagem) {
		if (hasContratoAtivo()) {
			return this.contratoAtivoValorTotal;
		}
		return 0.0;
	}

	@Override
	public double onCuraAttempt(Personagem personagem, double curaRecebida) {
		if (hasContratoAtivo()) {
			double vidaAtual = personagem.getVidaAtual();
			double tetoAtual = personagem.getVidaMaxima(); // Agora isso retorna o valor reduzido (ex: 60)

			// Calcula o espaço vazio no HP (até o teto do contrato)
			double espacoNoHp = Math.max(0, tetoAtual - vidaAtual);

			double curaParaVida = 0;
			double curaParaDivida = 0;

			if (curaRecebida <= espacoNoHp) {
				// Se a cura cabe inteira na vida atual, usa tudo na vida
				curaParaVida = curaRecebida;
				curaParaDivida = 0;
			} else {
				// Se a cura transborda o teto, enche a vida e usa o resto na dívida
				curaParaVida = espacoNoHp;
				curaParaDivida = curaRecebida - espacoNoHp;
			}

			// Aplica o pagamento da dívida com o excedente
			if (curaParaDivida > 0) {
				if (curaParaDivida >= this.contratoAtivoDividaRestante) {
					// Quitou a dívida!
					double troco = curaParaDivida - this.contratoAtivoDividaRestante;

					System.out.println(">>> HUMANO: Contrato Quitado! Teto de vida liberado.");
					this.contratoAtivoDividaRestante = 0;
					this.contratoAtivoValorTotal = 0; // Remove o teto

					personagem.removerEfeito("Contrato de Vida");

					personagem.recalcularAtributosEstatisticas();

					return curaParaVida + troco;

				} else {
					// Abate a dívida parcialmente
					this.contratoAtivoDividaRestante -= curaParaDivida;
					System.out.println(">>> HUMANO: Dívida reduzida em " + (int) curaParaDivida + ". Resta: "
							+ (int) this.contratoAtivoDividaRestante);
					// O teto continua baixo, então só retornamos o que coube na vida
					return curaParaVida;
				}
			}

			return curaParaVida;
		}

		return curaRecebida; // Sem contrato, cura normal
	}

	// --- MÉTODOS DE CONTROLE ---

	private void iniciarEmprestimo(Personagem p) {
		this.estadoAtual = EstadoEmprestimo.ATIVO;
		this.vidaNegativada = 0;
		Efeito emp = new Efeito("Empréstimo", TipoEfeito.BUFF, 500, null, 0, 0);
		p.adicionarEfeito(emp);
		System.out.println(">>> HUMANO: Empréstimo Iniciado (500 TU).");
	}

	public void resolverResultadoTeste(Personagem p, boolean sucesso, int valorRolado) {
		if (estadoAtual == EstadoEmprestimo.RECUPERADO) {
			System.out.println(">>> HUMANO: Sobreviveu por recuperação.");
			resetarEstadoEmprestimo(p);
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

	public void avancarProximoContrato(Personagem p) {
		if (hasContratoAtivo())
			return;
		if (!filaContratos.isEmpty()) {
			double pctProximo = filaContratos.poll();
			this.contratoAtivoValorTotal = p.getVidaMaximaBase() * pctProximo;
			this.contratoAtivoDividaRestante = this.contratoAtivoValorTotal; // Dívida começa cheia

			Efeito visualContrato = new Efeito("Contrato de Vida", TipoEfeito.DEBUFF, 99999, null, 0, 0);
			p.adicionarEfeito(visualContrato);
			p.recalcularAtributosEstatisticas();
			System.out.println(">>> HUMANO: Novo Contrato Ativado (-" + (int) contratoAtivoValorTotal + " HP).");
		}
	}

	private void resetarEstadoEmprestimo(Personagem p) {
		this.estadoAtual = EstadoEmprestimo.NENHUM;
		this.vidaNegativada = 0;
		p.removerEfeito("Empréstimo");
		p.recalcularAtributosEstatisticas();
	}

	private boolean hasContratoAtivo() {
		return contratoAtivoValorTotal > 0;
	} // Verificação pelo Teto e não pela dívida

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

	// Getter para a UI (Barra Vermelha) saber o tamanho atual da dívida
	public double getDividaPendente() {
		return this.contratoAtivoDividaRestante;
	}

	public Queue<Double> getFilaContratos() {
		return this.filaContratos;
	}

	public double getVidaNegativaAcumulada() {
		return this.vidaNegativada;
	}

	// Setters para Persistência (Carregamento)
	public void setFilaContratos(List<Double> listaSalva) {
		this.filaContratos.clear();
		if (listaSalva != null) {
			this.filaContratos.addAll(listaSalva);
		}
	}

	public void setVidaNegativaAcumulada(double valor) {
		this.vidaNegativada = valor;
	}
}