package br.com.dantesrpg.model.personagem;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.racas.Humano;
import br.com.dantesrpg.model.util.ContratoDeVidaUtils;

import java.util.Queue;

/**
 * Gerencia HP, Mana, Escudo, regeneração, histórico de dano,
 * e verificações de estado vital (vivo, ativo no combate).
 */
public class PersonagemHealth {
	private static final int DURACAO_MAXIMA_HISTORICO_DANO_TU = 300;


	private final Personagem personagem;

	public PersonagemHealth(Personagem personagem) {
		this.personagem = personagem;
	}

	// ========== HP ==========

	/**
	 * Setter principal de vida com toda a lógica de cura, redução, hooks raciais
	 * e proteção de protagonista.
	 */
	public void setVidaAtual(double novaVida, EstadoCombate estado, CombatController controller) {

		if (personagem.isProtagonista() && novaVida < personagem.getVidaAtual()) {
			System.out.println(">>> " + personagem.getNome() + " é PROTAGONISTA e ignorou o dano.");
			return;
		}

		double vidaAntiga = personagem.getVidaAtual();

		if (Math.abs(novaVida - vidaAntiga) < 0.01)
			return;

		double delta = novaVida - vidaAntiga;

		if (delta > 0) {
			aplicarCura(delta, vidaAntiga, estado, controller);
			return;
		}

		// Queda de vida (dano ou efeito direto)
		boolean racaLidouComMudanca = false;
		if (personagem.getRaca() != null) {
			racaLidouComMudanca = personagem.getRaca().onHpChangeAttempt(personagem, vidaAntiga, novaVida, estado, controller);
		}

		if (racaLidouComMudanca) {
			personagem.setVidaAtualInterno(novaVida); // Permite negativo (Humano)
		} else {
			personagem.setVidaAtualInterno(Math.max(0.0, Math.min(novaVida, personagem.getVidaMaxima()))); // Clamp padrão
		}

		if (personagem.getRaca() != null && Math.abs(personagem.getVidaAtual() - vidaAntiga) > 0.01) {
			personagem.getRaca().onHpChanged(personagem, vidaAntiga, personagem.getVidaAtual(), estado, controller);
		}
	}

	/**
	 * Fluxo unificado de cura:
	 * 1) Aplica redução de cura + hook racial.
	 * 2) Preenche HP até o teto atual.
	 * 3) Excedente paga contratos em ordem FIFO (qualquer fonte).
	 * 4) Após cada contrato quitado o teto sobe; troco tenta encher mais HP.
	 * 5) Bônus pós-pagamento só existe para o Humano (via onCuraAttempt, que já rodou).
	 */
	private void aplicarCura(double curaBruta, double vidaAntiga, EstadoCombate estado, CombatController controller) {
		double curaRecebida = curaBruta;

		if (personagem.getReducaoCuraPercentual() > 0) {
			double fatorCura = Math.max(0.0, 1.0 - personagem.getReducaoCuraPercentual());
			System.out.println(">>> " + personagem.getNome() + " teve a cura reduzida em "
					+ String.format("%.0f", personagem.getReducaoCuraPercentual() * 100) + "%.");
			curaRecebida *= fatorCura;
		}

		if (personagem.getRaca() != null) {
			curaRecebida = personagem.getRaca().onCuraAttempt(personagem, curaRecebida);
		}

		if (curaRecebida <= 0)
			return;

		double restante = curaRecebida;

		// 1) Enche HP até o teto atual
		double espacoHp = Math.max(0, personagem.getVidaMaxima() - personagem.getVidaAtual());
		double usaNoHp = Math.min(restante, espacoHp);
		if (usaNoHp > 0) {
			personagem.setVidaAtualInterno(personagem.getVidaAtual() + usaNoHp);
			restante -= usaNoHp;
		}

		// 2) Excedente paga contratos; teto sobe; troco volta a encher HP
		int safetyCounter = 0;
		while (restante > 0 && ContratoDeVidaUtils.temContrato(personagem) && safetyCounter++ < 32) {
			double vidaMaxAntes = personagem.getVidaMaxima();
			double restanteAntes = restante;

			restante = ContratoDeVidaUtils.pagarDivida(personagem, restante);

			double vidaMaxDepois = personagem.getVidaMaxima();
			if (vidaMaxDepois > vidaMaxAntes) {
				double espacoExtra = Math.max(0, vidaMaxDepois - personagem.getVidaAtual());
				if (espacoExtra > 0 && restante > 0) {
					double usaNoHp2 = Math.min(restante, espacoExtra);
					personagem.setVidaAtualInterno(personagem.getVidaAtual() + usaNoHp2);
					restante -= usaNoHp2;
				}
			}

			if (vidaMaxDepois <= vidaMaxAntes && restante == restanteAntes) {
				break; // sem progresso
			}
		}

		if (personagem.getRaca() != null && Math.abs(personagem.getVidaAtual() - vidaAntiga) > 0.01) {
			personagem.getRaca().onHpChanged(personagem, vidaAntiga, personagem.getVidaAtual(), estado, controller);
		}
	}

	public void curarIgnorandoBloqueios(double valor, EstadoCombate estado, CombatController controller) {
		double vidaAntiga = personagem.getVidaAtual();
		double curaRecebida = valor;

		if (personagem.getReducaoCuraPercentual() > 0) {
			double fatorCura = Math.max(0.0, 1.0 - personagem.getReducaoCuraPercentual());
			System.out.println(">>> " + personagem.getNome() + " teve a cura reduzida em "
					+ String.format("%.0f", personagem.getReducaoCuraPercentual() * 100) + "%.");
			curaRecebida *= fatorCura;
		}

		if (curaRecebida <= 0)
			return;

		double restante = curaRecebida;

		// 1) Enche HP até o teto atual
		double espacoHp = Math.max(0, personagem.getVidaMaxima() - personagem.getVidaAtual());
		double usaNoHp = Math.min(restante, espacoHp);
		if (usaNoHp > 0) {
			personagem.setVidaAtualInterno(personagem.getVidaAtual() + usaNoHp);
			restante -= usaNoHp;
		}

		// 2) Excedente paga contratos; teto sobe; troco volta a encher HP
		int safetyCounter = 0;
		while (restante > 0 && ContratoDeVidaUtils.temContrato(personagem) && safetyCounter++ < 32) {
			double vidaMaxAntes = personagem.getVidaMaxima();
			double restanteAntes = restante;

			restante = ContratoDeVidaUtils.pagarDivida(personagem, restante);

			double vidaMaxDepois = personagem.getVidaMaxima();
			if (vidaMaxDepois > vidaMaxAntes) {
				double espacoExtra = Math.max(0, vidaMaxDepois - personagem.getVidaAtual());
				if (espacoExtra > 0 && restante > 0) {
					double usaNoHp2 = Math.min(restante, espacoExtra);
					personagem.setVidaAtualInterno(personagem.getVidaAtual() + usaNoHp2);
					restante -= usaNoHp2;
				}
			}

			if (vidaMaxDepois <= vidaMaxAntes && restante == restanteAntes) {
				break; // sem progresso
			}
		}

		if (personagem.getRaca() != null && Math.abs(personagem.getVidaAtual() - vidaAntiga) > 0.01) {
			personagem.getRaca().onHpChanged(personagem, vidaAntiga, personagem.getVidaAtual(), estado, controller);
		}
	}

	public void forcarCura(double valor) {
		personagem.setVidaAtualInterno(Math.min(personagem.getVidaMaxima(), personagem.getVidaAtual() + valor));
	}

	public void regenerarVidaFracionada(double quantidade, EstadoCombate estado, CombatController controller) {
		if (personagem.getVidaAtual() < personagem.getVidaMaxima()) {
			setVidaAtual(personagem.getVidaAtual() + quantidade, estado, controller);
			System.out.println(">>> Regeneração: +" + String.format("%.1f", quantidade) + " HP.");
		}
	}

	// ========== HISTÓRICO DE DANO ==========

	public void registrarDanoSofrido(double valor, int tickAtual) {
		if (valor > 0) {
			Queue<Personagem.DanoSofrido> historico = personagem.getHistoricoDano();
			removerDanosAnterioresAoTick(historico, tickAtual - DURACAO_MAXIMA_HISTORICO_DANO_TU);
			historico.add(new Personagem.DanoSofrido(valor, tickAtual));
		}
	}

	public double getDanoSofridoRecentemente(int duracaoTU, int tickAtual) {
		Queue<Personagem.DanoSofrido> historico = personagem.getHistoricoDano();
		int tempoLimite = tickAtual - duracaoTU;
		double danoTotalRecente = 0.0;

		removerDanosAnterioresAoTick(historico, tempoLimite);

		for (Personagem.DanoSofrido evento : historico) {
			danoTotalRecente += evento.valor;
		}
		return danoTotalRecente;
	}

	private void removerDanosAnterioresAoTick(Queue<Personagem.DanoSofrido> historico, int tempoLimite) {
		while (historico.peek() != null && historico.peek().tempoGlobalTU < tempoLimite) {
			historico.poll();
		}
	}

	// ========== ESTADO VITAL ==========

	public boolean isAtivoNoCombate() {
		if (personagem.isAusente())
			return false;
		if (personagem.isFugiu())
			return false;

		if (personagem.getRaca() instanceof Humano) {
			Humano h = (Humano) personagem.getRaca();
			if (h.getEstadoAtual() == Humano.EstadoEmprestimo.ATIVO) {
				return true;
			}
		}

		return personagem.getVidaAtual() > 0;
	}

	public boolean isVivo() {
		if (personagem.getRaca() instanceof Humano) {
			Humano h = (Humano) personagem.getRaca();
			if (h.getEstadoAtual() == Humano.EstadoEmprestimo.ATIVO
					|| h.getEstadoAtual() == Humano.EstadoEmprestimo.PENDENTE_RESOLUCAO) {
				return true;
			}
		}
		return personagem.getVidaAtual() > 0;
	}
}
